package it.keybiz.lbsapp.corporate.features.profile

import android.Manifest
import android.app.*
import android.content.*
import android.location.Location
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import io.realm.Realm
import it.keybiz.lbsapp.corporate.R
import it.keybiz.lbsapp.corporate.base.HLActivity
import it.keybiz.lbsapp.corporate.connection.*
import it.keybiz.lbsapp.corporate.models.HLUser
import it.keybiz.lbsapp.corporate.utilities.*
import org.json.JSONArray
import java.lang.ref.WeakReference


class LocationUpdateService: Service(), OnServerMessageReceivedListener, OnMissingConnectionListener,
        OnCompleteListener<Location>,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        HLWebSocketAdapter.OnSocketConnectionDetectedListener {

    enum class GroundType { BACKGROUND, FOREGROUND }

    companion object {
        val LOG_TAG = LocationUpdateService::class.qualifiedName

        const val ACTION_LOCATION_SERVICE_STOP = "stop_location_service"

        var activityContext: WeakReference<Activity>? = null

        var serviceConnection: ProfileActivity.LocationServiceConnection? = null

        var prevState: GroundType = GroundType.BACKGROUND
        var currState: GroundType = GroundType.BACKGROUND

        @JvmStatic
        fun startService(context: Context, serviceConnection: ServiceConnection?, state: GroundType = GroundType.BACKGROUND) {
            if (context is Activity)
                activityContext = WeakReference(context)

            this.serviceConnection = (serviceConnection as? ProfileActivity.LocationServiceConnection)

            if (hasAllPermissionsToRun(context)) {
                prevState = currState
                currState = state

                try {
                    val intent = Intent(context, LocationUpdateService::class.java).apply { putExtra(Constants.EXTRA_PARAM_1, state) }
                    context.startService(intent)
                    if (serviceConnection != null)
                        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
                } catch (e: IllegalStateException) {
                    LogUtils.e(LOG_TAG, "Cannot start background service: " + e.message, e)
                }
            }
        }

        fun hasAllPermissionsToRun(context: Context?): Boolean {
            return SharedPrefsUtils.hasLocationBackgroundBeenGranted(context) &&
                    Utils.hasApplicationPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private var receiver: ServerMessageReceiver? = null
    private var currentLocation: Location? = null
    private var userID: String? = null
    private var retriedSingle = false

    private val locationBinder = LocationServiceBinder()

    private val fusedLocationClient by lazy {
        if (Utils.checkPlayServices(this) && activityContext?.get() != null)
            LocationServices.getFusedLocationProviderClient(activityContext!!.get()!!)
        else null
    }

    val googleApiClient: GoogleApiClient by lazy {
        GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build()
    }

    private val settingsClient by lazy { LocationServices.getSettingsClient(this) }

    private val locationRequest by lazy {
        LocationRequest.create()?.apply {
            interval = 60000
            fastestInterval = 30000
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        }
    }

    private val locationRequestBuilder by lazy {
        LocationSettingsRequest.Builder().apply {
            if (locationRequest != null)
                addLocationRequest(locationRequest!!)

            // with the current settings it should never come up, but it's very invasive and, being
            // the service in background, if it does users won't even see a UI linked.
//            setAlwaysShow(true)
        }
    }

    private val locationCallback = object: LocationCallback() {

        override fun onLocationResult(p0: LocationResult?) {
            super.onLocationResult(p0)

            if (p0?.lastLocation != null) {
                // TODO: 2/20/19    send location to server
                // consider if filtering logic

                LogUtils.v(LOG_TAG, "Location UPDATED : $currentLocation")

                currentLocation = p0.lastLocation
                handleTempConnection(true)

            } else {
                // TODO: 2/20/19    work with locations list??
            }

        }

        override fun onLocationAvailability(p0: LocationAvailability?) {
            super.onLocationAvailability(p0)

            LogUtils.d(LOG_TAG, if (p0?.isLocationAvailable == true) "Location available" else "Location currently unavailable")
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return locationBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent?.action?.equals(ACTION_LOCATION_SERVICE_STOP) == true) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (currState == GroundType.BACKGROUND) stopForeground(true)
        if (currState == GroundType.FOREGROUND)
            startForeground(Constants.NOTIFICATION_LOCATION_ID, buildNotification())

        if (receiver == null)
            receiver = ServerMessageReceiver()
        receiver!!.setListener(this)
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver!!, IntentFilter(Constants.BROADCAST_SERVER_RESPONSE))

        googleApiClient.connect()

        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        receiver = ServerMessageReceiver()
    }


    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }


    override fun onDestroy() {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(Constants.NOTIFICATION_LOCATION_ID)
        stopLocationUpdates()
        googleApiClient.disconnect()
        exitOps(false)
        super.onDestroy()
    }

    override fun onConnected(p0: Bundle?) {

        settingsClient.checkLocationSettings(locationRequestBuilder.build())
                .addOnSuccessListener {
                    if (hasAllPermissionsToRun(activityContext?.get())) {
                        fusedLocationClient?.requestLocationUpdates(locationRequest, locationCallback, null)
                    }
                }
                .addOnFailureListener {
                    if (it is ResolvableApiException){
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            it.startResolutionForResult(activityContext?.get(), Constants.RESULT_SETTINGS_NEAR_ME)
                        } catch (sendEx: IntentSender.SendIntentException) {
                            // Ignore the error.
                        }
                    }
                }

    }

    override fun onConnectionSuspended(p0: Int) {}

    override fun onConnectionFailed(p0: ConnectionResult) {
        (activityContext?.get() as? HLActivity)?.showAlert(R.string.error_chat_location)
    }

    override fun onComplete(p0: Task<Location>) {
        if (p0.isSuccessful) {
            // TODO: 2/20/19    send location to server
            // consider if filtering logic

            currentLocation = p0.result

            if (currentLocation == null && !retriedSingle) {
                fusedLocationClient?.lastLocation?.addOnCompleteListener(this)
                retriedSingle = true
            } else if (currentLocation != null) {
                handleTempConnection(true)
                retriedSingle = false
            }
        }
    }

    override fun handleSuccessResponse(operationId: Int, responseObject: JSONArray?) {
        if (operationId == Constants.SERVER_OP_SEND_LOCATION) {
            handleTempConnection(false)

            serviceConnection?.callServerFromFragment()

            LogUtils.v(LOG_TAG, "Location SENT : $currentLocation")
        }
    }

    override fun handleErrorResponse(operationId: Int, errorCode: Int) {
        if (operationId == Constants.SERVER_OP_SEND_LOCATION) {
            handleTempConnection(false)
            LogUtils.v(LOG_TAG, "Location SENT : $currentLocation")
        }
    }

    override fun onMissingConnection(operationId: Int) {
        handleTempConnection(false)
    }

    override fun onSocketConnected() {
        callServer()
    }


    //region == Class custom methods ==

    fun stopLocationUpdates() {
        fusedLocationClient?.removeLocationUpdates(locationCallback)
    }

    private fun callServer() {

        if (userID.isNullOrBlank()) {
            var realm: Realm? = null
            try {
                realm = RealmUtils.getCheckedRealm()
                userID = HLUser().readUser(realm).userId
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                RealmUtils.closeRealm(realm)
            }
        }

        if (currentLocation != null && !userID.isNullOrBlank()) {
            HLServerCalls.sendLocation(userID!!, "${currentLocation?.latitude};${currentLocation?.longitude}")
//            publishResult(true)
        }
    }

    private fun handleTempConnection(open: Boolean) {
        if (open) {
            if (currState == GroundType.FOREGROUND)
                HLSocketConnection.getInstance().openTempConnectionForLocation(this)
            else
                callServer()
        }
        else if (currState == GroundType.FOREGROUND) HLSocketConnection.getInstance().closeConnection()
    }

    fun askForSingleUpdate() {
        if (hasAllPermissionsToRun(activityContext?.get())) {
            fusedLocationClient?.lastLocation?.addOnCompleteListener(this)
        }
    }

    private fun exitOps(stop: Boolean = true) {
        try {
            if (receiver != null)
                LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver!!)
        } catch (e: IllegalArgumentException) {
            LogUtils.d(LOG_TAG, e.message)
        }
        if (stop) stopSelf()
    }


    private fun buildNotification(): Notification {
        val channelID = getString(R.string.notif_channel_service)
        if (Utils.hasOreo()) {
            val serviceChannel = NotificationChannel(channelID,
                    getString(R.string.service_notification_channel), NotificationManager.IMPORTANCE_LOW)
            serviceChannel.description = getString(R.string.notif_channel_service_description)
            serviceChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            serviceChannel.enableVibration(false)
            serviceChannel.enableLights(false)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(serviceChannel)
        }

        val stopIntent = Intent(this, LocationUpdateService::class.java)
        stopIntent.action = ACTION_LOCATION_SERVICE_STOP
        val stopPending = PendingIntent.getService(this, 0, stopIntent, 0)

        return NotificationCompat.Builder(this, channelID)
                .setSmallIcon(R.drawable.ic_notification_tray)
                .setContentTitle(getString(R.string.notif_service_loc_title))
                .setContentText(getString(R.string.notif_service_loc_body))
                .setStyle(NotificationCompat.BigTextStyle().bigText(getString(R.string.notif_service_loc_body)))
                .setOngoing(true)
                .addAction(NotificationCompat.Action.Builder(0, getString(R.string.action_stop), stopPending).build())
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build()
    }


    // INFO: 2/21/19    test method SIMULATING RESPONSE
    /**
     * @param success whether the method is wanted to simulate a SUCCESS or a FAILURE
     */
    private fun publishResult(success: Boolean) {
        Handler().postDelayed(
                {
                    if (success) { handleTempConnection(false) }
                    else handleTempConnection(false)

                },
                3000
        )
    }

    //endregion


    inner class LocationServiceBinder: Binder() {
        fun getService(): LocationUpdateService {
            return this@LocationUpdateService
        }
    }

}