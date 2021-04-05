/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.utilities.media;

import android.Manifest;
import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.DrawableRes;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomViewTarget;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import it.keybiz.lbsapp.corporate.BuildConfig;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.ProgressRequestBody;
import it.keybiz.lbsapp.corporate.features.createPost.CreatePostHelper;
import it.keybiz.lbsapp.corporate.models.HLUser;
import it.keybiz.lbsapp.corporate.models.Post;
import it.keybiz.lbsapp.corporate.models.enums.PostTypeEnum;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import jp.wasabeef.glide.transformations.BlurTransformation;
import jp.wasabeef.glide.transformations.RoundedCornersTransformation;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * @author mbaldrighi on 9/27/2017.
 */
public class MediaHelper {

	public static final String LOG_TAG = MediaHelper.class.getCanonicalName();

	private MediaPlayer mediaPlayer;


	//region == PUBLIC STATIC methods and classes ==

	//region ++ GLIDE ++

	/**
	 * Helper method to easily display a streamed image with Glide.
	 * @param context the application's/activity's {@link Context} instance.
	 * @param url the internet path of the image.
	 * @param glideScaleOptions a {@link RequestOptions} containing some Glide configuration.
	 * @param target the {@link CustomViewTarget} which is target container for the image.
	 */
	public static void loadPictureWithGlide(Context context, String url, RequestOptions glideScaleOptions,
											@DrawableRes int placeholder, @DrawableRes int fallback,
											CustomViewTarget target) {
		if (Utils.isContextValid(context) && Utils.isStringValid(url)) {
			if (glideScaleOptions == null)
				glideScaleOptions = RequestOptions.centerCropTransform();

			GlideApp.with(context).load(url)
					.apply(glideScaleOptions
							.placeholder(placeholder)
							.fallback(fallback)
					)
					.into(target);
		}
	}

	/**
	 * Helper method to easily display a streamed image with Glide.
	 * @param context the application's/activity's {@link Context} instance.
	 * @param url the internet path of the image.
	 * @param glideScaleOptions a {@link RequestOptions} containing some Glide configuration.
	 * @param target the {@link ImageView} which is target container for the image.
	 */
	public static void loadPictureWithGlide(Context context, String url, RequestOptions glideScaleOptions,
											@DrawableRes int placeholder, @DrawableRes int fallback,
											ImageView target) {
		if (Utils.isContextValid(context) && Utils.isStringValid(url)) {
			if (glideScaleOptions == null)
				glideScaleOptions = RequestOptions.centerCropTransform();

			GlideApp.with(context).load(url)
					.apply(glideScaleOptions
							.placeholder(placeholder)
							.fallback(fallback)
					)
					.into(target);
		}
	}

	/**
	 * Helper method to easily display a streamed image with Glide.
	 * @param context the application's/activity's {@link Context} instance.
	 * @param url the internet path of the image.
	 * @param target the {@link ImageView} which is target container for the image.
	 */
	public static void loadPictureWithGlide(Context context, String url, ImageView target) {
		loadPictureWithGlide(context, url, null, -1, -1, target);
	}

	/**
	 * Helper method to easily blur the user's wall picture.<p>
	 * It needs CenterCrop() applied beforehand because it is a transformation which would override the rounded corners.
	 * @param context the application's/activity's {@link Context} instance.
	 * @param drawable the drawable previously retrieved .
	 * @param target the {@link ImageView} which is target container for the image.
	 */
	public static void blurWallPicture(Context context, Drawable drawable, ImageView target) {
		if (Utils.isContextValid(context)) {

			GlideApp.with(context)
					.asDrawable()
					.load(drawable)
					.apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.RESOURCE))
//                      .apply(RequestOptions.skipMemoryCacheOf(true))
//                      .apply(RequestOptions.onlyRetrieveFromCache(true)
					.transforms(new CenterCrop(), new BlurTransformation(25))
					/*apply(RequestOptions.bitmapTransform(new BlurTransformation(25)))*/
					.into(target);
		}
	}


	/**
	 * Helper method to easily round picture corners for devices running < 21.<p>
	 * It needs CenterCrop() applied beforehand because it is a transformation which would override the rounded corners.
	 * @param target the {@link ImageView} which is target container for the image.
	 * @param url the url to load.
	 */
	public static void roundPictureCorners(ImageView target, String url) {
		roundPictureCorners(target, url, 4f, true);
	}

	/**
	 * Helper method to easily round picture corners for devices running < 21.<p>
	 * It needs CenterCrop() applied beforehand because it is a transformation which would override the rounded corners.
	 * @param target the {@link ImageView} which is target container for the image.
	 * @param url the url to load.
	 * @param radius the wanted radius expressed in DPIs.
	 * @param all whether all the corners should be rounded or just the top ones.
	 */
	public static void roundPictureCorners(ImageView target, String url, float radius, boolean all) {
		if (Utils.isContextValid(target.getContext())) {

			GlideApp.with(target)
					.load(url)
					.transforms(new CenterCrop(), new RoundedCornersTransformation(
							Utils.dpToPx(radius, target.getResources()),
							0,
							all ? RoundedCornersTransformation.CornerType.ALL : RoundedCornersTransformation.CornerType.TOP
					))
					.into(target);
		}
	}

	/**
	 * Helper method to easily round picture corners for devices running < 21.<p>
	 * It needs CenterCrop() applied beforehand because it is a transformation which would override the rounded corners.
	 * @param target the {@link ImageView} which is target container for the image.
	 * @param drawable the drawable to show.
	 */
	public static void roundPictureCorners(ImageView target, Drawable drawable) {
		roundPictureCorners(target, drawable, 4f, true);
	}

	/**
	 * Helper method to easily round picture corners for devices running < 21.<p>
	 * It needs CenterCrop() applied beforehand because it is a transformation which would override the rounded corners.
	 * @param target the {@link ImageView} which is target container for the image.
	 * @param drawable the drawable to show.
	 * @param radius the wanted radius expressed in DPIs.
	 * @param all whether all the corners should be rounded or just the top ones.
	 */
	public static void roundPictureCorners(ImageView target, Drawable drawable, float radius, boolean all) {
		if (Utils.isContextValid(target.getContext())) {

			GlideApp.with(target)
					.load(drawable)
					.transforms(new CenterCrop(), new RoundedCornersTransformation(
							Utils.dpToPx(radius, target.getResources()),
							0,
							all ? RoundedCornersTransformation.CornerType.ALL : RoundedCornersTransformation.CornerType.TOP
					))
					.into(target);
		}
	}


	/**
	 * Helper method to easily display a streamed image with Glide into a profile picture container.
	 * <p>
	 * It provides a default place holder as well.
	 * @param context the application's/activity's {@link Context} instance.
	 * @param url the internet path of the image.
	 * @param target the {@link ImageView} which is target container for the image.
	 */
	public static void loadProfilePictureWithPlaceholder(Context context, String url, ImageView target) {
		loadPictureWithGlide(context, url, new RequestOptions().fitCenter(), R.drawable.ic_profile_placeholder/* -1*/,
				R.drawable.ic_profile_placeholder/*-1*/, target);
	}

	//endregion


	//region ++ TAKE MEDIA ACTIONS ++

	public static Drawable getDrawableFromPath(Context context, @NonNull String filePath, int requestCode) {
		Drawable d = null;
		Bitmap bitmap = BitmapFactory.decodeFile(filePath);
//		if (requestCode != Constants.RESULT_GALLERY) {
		try {
			d = MediaHelper.modifyOrientation(context, bitmap, filePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
//		}
//		bitmap.recycle();

		return d;
	}


	public static PopupMenu openPopupMenu(Context context, @MenuRes int menuResId, @NonNull View anchor,
										  PopupMenu.OnMenuItemClickListener listener) {
		if (Utils.isContextValid(context)) {
			PopupMenu menu = new PopupMenu(context, anchor);
			menu.inflate(menuResId);
			if (listener != null)
				menu.setOnMenuItemClickListener(listener);

			for (int i = 0; i < menu.getMenu().size(); i++) {
				MenuItem mi = menu.getMenu().getItem(i);
				Utils.applyRegularFontToMenuItem(context, mi);
			}

			menu.show();

			return menu;
		}

		return null;
	}

	public static String checkPermissionAndFireCameraIntent(HLActivity activity, HLMediaType type, Fragment fragment) {
		if (Utils.hasMarshmallow()) {
			boolean hasRead = Utils.hasApplicationPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
			boolean hasWrite = Utils.hasApplicationPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
			boolean hasCamera = Utils.hasApplicationPermission(activity, Manifest.permission.CAMERA);

			Set<String> strings = new HashSet<>();
			if (!hasRead)
				strings.add(Manifest.permission.READ_EXTERNAL_STORAGE);
			if (!hasWrite)
				strings.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
			if (!hasCamera)
				strings.add(Manifest.permission.CAMERA);

			if (strings.isEmpty()) {
				return MediaHelper.dispatchTakeMediaIntent(activity, type, fragment);
			}
			else {
				if (fragment != null)
					Utils.askRequiredPermissionForFragment(fragment, strings.toArray(new String[strings.size()]), Constants.PERMISSIONS_REQUEST_READ_WRITE_CAMERA);
				else
					Utils.askRequiredPermission(activity, strings.toArray(new String[strings.size()]), Constants.PERMISSIONS_REQUEST_READ_WRITE_CAMERA);

				return null;
			}
		}
		else return MediaHelper.dispatchTakeMediaIntent(activity, type, fragment);
	}

	public static void checkPermissionForGallery(HLActivity activity, HLMediaType type, Fragment fragment) {
		if (Utils.hasMarshmallow()) {
			boolean hasRead = Utils.hasApplicationPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
			boolean hasWrite = Utils.hasApplicationPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

			Set<String> strings = new HashSet<>();
			if (!hasRead)
				strings.add(Manifest.permission.READ_EXTERNAL_STORAGE);
			if (!hasWrite)
				strings.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

			if (strings.isEmpty()) {
				MediaHelper.dispatchGalleryIntent(activity, type, fragment);
			}
			else {
				if (fragment != null)
					Utils.askRequiredPermissionForFragment(fragment, strings.toArray(new String[strings.size()]), Constants.PERMISSIONS_REQUEST_GALLERY);
				else
					Utils.askRequiredPermission(activity, strings.toArray(new String[strings.size()]), Constants.PERMISSIONS_REQUEST_GALLERY);
			}
		}
		else
			MediaHelper.dispatchGalleryIntent(activity, type, fragment);
	}

	public static void checkPermissionForCustomGallery(HLActivity activity, Fragment fragment) {
		LoaderManager.LoaderCallbacks callbacks = null;
		if (fragment instanceof LoaderManager.LoaderCallbacks)
			callbacks = (LoaderManager.LoaderCallbacks) fragment;
		else if (activity instanceof LoaderManager.LoaderCallbacks)
			callbacks = (LoaderManager.LoaderCallbacks) activity;

		if (Utils.hasMarshmallow()) {
			boolean hasRead = Utils.hasApplicationPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);

			if (!hasRead) {
				if (fragment != null) {
					Utils.askRequiredPermissionForFragment(fragment,
							new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
							Constants.PERMISSIONS_REQUEST_GALLERY_CUSTOM);
				}
				else {
					Utils.askRequiredPermission(activity,
							new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
							Constants.PERMISSIONS_REQUEST_GALLERY_CUSTOM);
				}
			}
			else if (callbacks != null) {
				activity.getSupportLoaderManager().restartLoader(0, null, callbacks);
			}
		}
		else activity.getSupportLoaderManager().restartLoader(0, null, callbacks);

//		if (Utils.hasMarshmallow()) {
//			boolean hasRead = Utils.hasApplicationPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
//			boolean hasWrite = Utils.hasApplicationPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
//
//			Set<String> strings = new HashSet<>();
//			if (!hasRead)
//				strings.add(Manifest.permission.READ_EXTERNAL_STORAGE);
//			if (!hasWrite)
//				strings.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
//
//			if (strings.isEmpty()) {
//				activity.getSupportLoaderManager().restartLoader(0, null, callbacks);
//			}
//			else {
//				if (fragment != null)
//					Utils.askRequiredPermissionForFragment(fragment, strings.toArray(new String[strings.size()]), Constants.PERMISSIONS_REQUEST_GALLERY_CUSTOM);
//				else
//					Utils.askRequiredPermission(activity, strings.toArray(new String[strings.size()]), Constants.PERMISSIONS_REQUEST_GALLERY_CUSTOM);
//			}
//		}
//		else activity.getSupportLoaderManager().restartLoader(0, null, callbacks);
	}

	public static void checkPermissionForDocuments(HLActivity activity, Fragment fragment, boolean forClaim) {
		if (Utils.hasMarshmallow()) {
			boolean hasRead = Utils.hasApplicationPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);

			if (!hasRead) {
				String[] strings = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
				if (fragment != null)
					Utils.askRequiredPermissionForFragment(fragment, strings, Constants.PERMISSIONS_REQUEST_DOCUMENTS);
				else
					Utils.askRequiredPermission(activity, strings, Constants.PERMISSIONS_REQUEST_DOCUMENTS);
			}
			else
				MediaHelper.dispatchDocumentsIntent(activity, fragment, forClaim);
		}
		else
			MediaHelper.dispatchDocumentsIntent(activity, fragment, forClaim);
	}

	public static String checkPermissionForAudio(HLActivity activity, HLMediaType type, Fragment fragment) {
		if (Utils.hasMarshmallow()) {
			boolean hasRead = Utils.hasApplicationPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
			boolean hasWrite = Utils.hasApplicationPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
			boolean hasAudio = Utils.hasApplicationPermission(activity, Manifest.permission.RECORD_AUDIO);

			Set<String> strings = new HashSet<>();
			if (!hasRead)
				strings.add(Manifest.permission.READ_EXTERNAL_STORAGE);
			if (!hasWrite)
				strings.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
			if (!hasAudio)
				strings.add(Manifest.permission.RECORD_AUDIO);

			if (strings.isEmpty()) {
				return MediaHelper.dispatchTakeMediaIntent(activity, type, fragment);
			}
			else {
				if (fragment != null)
					Utils.askRequiredPermissionForFragment(fragment, strings.toArray(new String[strings.size()]), Constants.PERMISSIONS_REQUEST_READ_WRITE_AUDIO);
				else
					Utils.askRequiredPermission(activity, strings.toArray(new String[strings.size()]), Constants.PERMISSIONS_REQUEST_READ_WRITE_AUDIO);

				return null;
			}
		}
		else return MediaHelper.dispatchTakeMediaIntent(activity, type, fragment);
	}

	/**
	 * Generally fires an intent to the OS camera to capture a new Image.
	 * @param context the application's/activity's {@link Context} instance.
	 * @param action  the {@link HLMediaType} linked to the operation.
	 * @return The {@link Uri} of the saved media.
	 */
	public static String dispatchTakeMediaIntent(HLActivity context, HLMediaType action) {
		return dispatchTakeMediaIntent(context, action, null);
	}

	/**
	 * Generally fires an intent to the OS camera to capture a new Image.
	 * @param context  the application's/activity's {@link Context} instance.
	 * @param action   the {@link HLMediaType} linked to the operation.
	 * @param fragment the {@link Fragment} calling the result.
	 * @return The {@link Uri} of the saved media.
	 */
	public static String dispatchTakeMediaIntent(HLActivity context, HLMediaType action, Fragment fragment) {
		PackageManager packageManager = context.getPackageManager();

		boolean frontCam, rearCam;
		frontCam = packageManager.hasSystemFeature("android.hardware.camera.front");
		rearCam = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA);

		Intent intent = new Intent();
		String intentAction = "";
		String fileName = "tmp";
		String extension = "";
		int result = -1;
		switch (action) {
			case AUDIO:
				extension += ".mp4";
				break;
			case PHOTO:
			case PHOTO_PROFILE:
			case PHOTO_WALL:
			case VIDEO:
				if (frontCam || rearCam) {
					if (action == HLMediaType.VIDEO) {
						intentAction = MediaStore.ACTION_VIDEO_CAPTURE;
						result = Constants.RESULT_VIDEO;
						intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
						extension += ".mp4";
					}
					else {
						intentAction = MediaStore.ACTION_IMAGE_CAPTURE;
						result = Constants.RESULT_PHOTO;
						extension += ".jpg";
					}
				}
				else context.showAlert(R.string.error_device_no_camera);
				break;
		}

		intent.setAction(intentAction);


		File mediaFile;
		try {
			mediaFile = File.createTempFile(fileName, extension, context.getExternalFilesDir("tmp"));
		} catch (IOException ex) {
			// Error occurred while creating the File
			LogUtils.e(LOG_TAG, ex.getMessage(), ex);
			context.showAlert(R.string.error_upload_media);
			return null;
		}

		String finalPath;
		if (mediaFile.exists()) {
			finalPath = "file:" + mediaFile.getAbsolutePath();
			if (action == HLMediaType.AUDIO)
				return finalPath;

			try {
				Uri uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", mediaFile);

				List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
				for (ResolveInfo resolveInfo : resInfoList) {
					String packageName = resolveInfo.activityInfo.packageName;
					context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
				}

				intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);

				if (fragment != null)
					fragment.startActivityForResult(intent, result);
				else
					context.startActivityForResult(intent, result);

				return finalPath;
			}
			catch (Exception e) {
				LogUtils.e(LOG_TAG, e.getMessage(), e);
			}
		}

		context.showAlert(R.string.error_upload_media);
		return null;
	}

	public static File getNewTempFile(Context context, String fileName, String extension) {
		try {
			return File.createTempFile(fileName, extension, context.getExternalFilesDir("tmp"));
		} catch (IOException ex) {
			// Error occurred while creating the File
			LogUtils.e(LOG_TAG, ex.getMessage(), ex);
			return null;
		}
	}


	public static File getNewMediaFile(Context context, String urlFromServer, HLMediaType type) {

		try {
			String fileName = getFileNameForMedia(urlFromServer, type);
			String typePath = null;
			switch (type) {
				case AUDIO: typePath = Constants.PATH_EXTERNAL_DIR_MEDIA_AUDIO; break;
				case PHOTO: typePath = Constants.PATH_EXTERNAL_DIR_MEDIA_PHOTO; break;
				case VIDEO: typePath = Constants.PATH_EXTERNAL_DIR_MEDIA_VIDEO; break;
			}
			if (Utils.areStringsValid(typePath, fileName)) {
				File parent = context.getExternalFilesDir(Constants.PATH_CUSTOM_HIGHLANDERS + "/" + typePath);
				File media = new File(parent, fileName);

				if (media.createNewFile()) return media;
			}
		} catch (IOException ex) {
			// Error occurred while creating the File
			LogUtils.e(LOG_TAG, ex.getMessage(), ex);
		}

		return null;
	}

	public static String getFileNameForMedia(String urlFromServer, HLMediaType type) {
		if (Utils.isStringValid(urlFromServer)) {
			int lastIndex = urlFromServer.lastIndexOf("/") + 1;
			String name = urlFromServer.substring(lastIndex);

			String prefix = null;
			switch (type) {
				case AUDIO: prefix = Constants.FILENAME_MEDIA_AUDIO; break;
				case PHOTO: prefix = Constants.FILENAME_MEDIA_PHOTO; break;
				case VIDEO: prefix = Constants.FILENAME_MEDIA_VIDEO; break;
			}

			if (Utils.areStringsValid(prefix, name)) return prefix + "_" + name;
		}

		return null;
	}

	public static String getFileNameFromPath(String path) {
		if (Utils.isStringValid(path)) {
			int lastIndex = path.lastIndexOf("/") + 1;
			return path.substring(lastIndex);
		}

		return null;
	}

	/**
	 * Generally fires an intent to the OS camera to capture a new Image.
	 * @param context the application's/activity's {@link Context} instance.
	 * @param action  the {@link HLMediaType} linked to the operation.
	 * @return The {@link Uri} of the saved media.
	 */
	public static void dispatchGalleryIntent(HLActivity context, HLMediaType action) {
		dispatchGalleryIntent(context, action, null);
	}

	public static void dispatchGalleryIntent(HLActivity context, HLMediaType action, Fragment fragment) {
		Uri uri = null;
		if (action == HLMediaType.PHOTO ||
				action == HLMediaType.PHOTO_PROFILE ||
				action == HLMediaType.PHOTO_WALL)
			uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
		else if (action == HLMediaType.VIDEO)
			uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
		Intent intent = new Intent(Intent.ACTION_PICK, uri);
		if (fragment != null)
			fragment.startActivityForResult(intent, Constants.RESULT_GALLERY);
		else
			context.startActivityForResult(intent, Constants.RESULT_GALLERY);
	}

	public static final String MIME_DOC = "application/msword";
	public static final String MIME_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
	public static final String MIME_XLS = "application/vnd.ms-excel";
	public static final String MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
	public static final String MIME_PPT = "application/vnd.ms-powerpoint";
	public static final String MIME_PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
	public static final String MIME_PDF = "application/pdf";
	public static final String MIME_JPEG_JPG = "image/jpeg";
	public static final String MIME_PNG = "image/png";
	public static final String MIME_TXT = "text/plain";
	public static final String MIME_RTF = "application/rtf";
	public static void dispatchDocumentsIntent(HLActivity context, Fragment fragment, boolean forClaim) {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("*/*");
		intent.addCategory(Intent.CATEGORY_OPENABLE);

		if (!forClaim) {
			// TODO: 12/13/18    wait for instructions on RTF and TXT
			String[] types = new String[] {
					MIME_DOC, MIME_DOCX, MIME_PPT, MIME_PPTX, MIME_XLS, MIME_XLSX, MIME_PDF
//					, MIME_RTF, MIME_TXT
			};
			intent.putExtra(Intent.EXTRA_MIME_TYPES, types);
		}
		// TODO: 1/23/2018    consider getting back here
//		intent.setType(MIME_DOC);           // MS WORD
//		intent.setType(MIME_DOCX);          // MS WORD post Word2010
//		intent.setType(MIME_PDF);           // ADOBE PDF
//		intent.setType(MIME_JPEG_JPG);      // image JPEG/JPG
//		intent.setType(MIME_PNG);           // image PNG
		if (fragment != null && intent.resolveActivity(context.getPackageManager()) != null)
			fragment.startActivityForResult(intent, Constants.RESULT_DOCUMENTS);
		else
			context.startActivityForResult(intent, Constants.RESULT_DOCUMENTS);
	}

	public static void getFileFromContentUri(Uri fileUri, FileFromUriListener listener, boolean forClaim) {

		new FileFromUriTask(listener, forClaim).execute(fileUri);
	}

	public static File getFileFromContentUri(InputStream inputStream, String mime,
											 HLActivity context, boolean forClaim) {

		context.setProgressMessage(R.string.processing_file);
		context.openProgress();

		OutputStream outputStream = null;

		String extension = getFileExtension(mime, true);
		if (!Utils.isStringValid(extension)) {
			context.closeProgress();
			context.showAlert(context.getString(R.string.error_processing_file));
			return null;
		}
		else if (forClaim && !mime.equals(MIME_DOC) && !mime.equals(MIME_DOCX) && !mime.equals(MIME_PDF) &&
				!mime.equals(MIME_JPEG_JPG) && !mime.equals(MIME_PNG)) {
			context.closeProgress();
			context.showAlert(context.getString(R.string.error_claim_files_allowed_extensions));
			return null;
		}

		File mediaFile;
		try {
			try {
				mediaFile = File.createTempFile("tmp", "." + extension, context.getExternalFilesDir("tmp"));
			} catch (IOException ex) {
				// Error occurred while creating the File
				LogUtils.e(LOG_TAG, ex.getMessage(), ex);
				return null;
			}

			if (mediaFile.exists()) {
				// write the inputStream to a FileOutputStream
				outputStream =
						new FileOutputStream(mediaFile);

				int read = 0;
				byte[] bytes = new byte[1024];

				while ((read = inputStream.read(bytes)) != -1) {
					outputStream.write(bytes, 0, read);
				}

				LogUtils.d(LOG_TAG, "File processed from Uri: DONE");
			}
		}
		catch(IOException e){
			e.printStackTrace();
			context.closeProgress();
			return null;
		}
		finally{
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
					context.closeProgress();
				}
			}
			if (outputStream != null) {
				try {
					// outputStream.flush();
					outputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			context.closeProgress();
		}

		return mediaFile;
	}

	public static void scanFile(final Context context, final String mediaFileUri) {
		new Handler().post(new Runnable() {
			@Override
			public void run() {
				if (Utils.isStringValid(mediaFileUri)) {
					File f = new File(Uri.parse(mediaFileUri).getPath());
					if (f.exists()) {

						String type = getMimeType(mediaFileUri);
						MediaScannerConnection.scanFile(
								context,
								new String[] { f.getAbsolutePath() },
								new String[] { Utils.isStringValid(type) ? type : ""  },
								new MediaScannerConnection.OnScanCompletedListener() {
									@Override
									public void onScanCompleted(String path, Uri uri) {
										LogUtils.d(LOG_TAG,
												"file " + path + " was scanned successfully: " + uri);
									}
								}
						);
					}
				}
			}
		});
	}

	public static String getMimeType(String uri) {
		String extension = getFileExtension(uri);
		if (extension != null) {
			return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
		}

		return null;
	}

	public static String getFileExtension(String uri) {
		return getFileExtension(uri, false);
	}

	public static String getFileExtension(String uri, boolean fromMime) {
		String extension;

		uri = uri.replaceAll(" ", "%20");

		if (fromMime)
			extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(uri);
		else
			extension = MimeTypeMap.getFileExtensionFromUrl(uri);
		return Utils.isStringValid(extension) ? extension : null;
	}


	public static class MediaUploadListener implements Callback {

		private Post mPost;
		private HLActivity activity;
		private HLMediaType mediaCaptureType;
		private CreatePostHelper.ViewType viewType;

		public MediaUploadListener(@Nullable Post post, HLActivity activity, HLMediaType mediaCaptureType,
								   CreatePostHelper.ViewType viewType) {
			this.mPost = post;
			this.activity = activity;
			this.mediaCaptureType = mediaCaptureType;
			this.viewType = viewType;
		}

		@Override
		public void onFailure(@NonNull Call call, @NonNull IOException e) {
			LogUtils.e(LOG_TAG, "Media upload FAIL: " + e.getMessage(), e);
			activity.closeProgress();
			activity.showAlert(R.string.error_upload_media);
		}

		@Override
		public void onResponse(@NonNull Call call, @NonNull Response response) {
			if (response.isSuccessful()) {
				LogUtils.d(LOG_TAG, "Media upload SUCCESS");

				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						activity.setProgressMessage(R.string.sending_post);
					}
				});

				ResponseBody body = response.body();
				String link = parseMediaUploadResponse(activity, body);
				if (!Utils.isStringValid(link))
					return;

				if (mPost != null) {
					mPost.setContent(link);
					mPost.setType(mediaCaptureType.toString());

					try {
						if (viewType == CreatePostHelper.ViewType.NORMAL)
							HLServerCalls.createEditPost(mPost);
						else if (viewType == CreatePostHelper.ViewType.WISH)
							HLServerCalls.createEditPostForWish(mPost);
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}
			else {
				LogUtils.e(LOG_TAG, "Media upload FAIL: " + response.message());
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						activity.closeProgress();
						activity.showAlert(R.string.error_upload_media);
					}
				});
			}
		}
	}

	public static class MediaUploadListenerWithCallback extends MediaUploadListener implements Callback {

		private HLActivity activity;
		private String path;

		private OnUploadCompleteListener mListener;

		public MediaUploadListenerWithCallback(@NonNull Post post, HLActivity activity, HLMediaType mediaCaptureType) {
			super(post, activity, mediaCaptureType, null);
		}

		public MediaUploadListenerWithCallback(HLActivity activity, String path,
											   OnUploadCompleteListener listener) {
			super(null, activity, null, null);

			this.activity = activity;
			this.path = path;
			this.mListener = listener;
		}

		@Override
		public void onFailure(@NonNull Call call, @NonNull IOException e) {
			LogUtils.e(LOG_TAG, "Media upload FAIL: " + e.getMessage(), e);
			activity.setShowProgressAnyway(false);
			activity.closeProgress();
			activity.showAlert(R.string.error_upload_media);
		}

		@Override
		public void onResponse(@NonNull Call call, @NonNull Response response) {
			activity.setShowProgressAnyway(false);
			activity.closeProgress();

			if (response.isSuccessful()) {
				LogUtils.d(LOG_TAG, "Media upload SUCCESS");

				ResponseBody body = response.body();
				String link = parseMediaUploadResponse(activity, body);
				if (!Utils.isStringValid(link))
					return;

				if (mListener != null)
					mListener.onUploadComplete(path, link);
			}
			else {
				LogUtils.e(LOG_TAG, "Media upload FAIL: " + response.message());
				activity.showAlert(R.string.error_upload_media);
			}
		}

		public interface OnUploadCompleteListener {
			void onUploadComplete(String path, String mediaLink);
		}
	}


	private static String parseMediaUploadResponse(HLActivity activity, ResponseBody body) {
		if (body != null) {
			try {
				String s = body.string();
				if (Utils.isStringValid(s)) {
					JSONObject jResponse = new JSONObject(s);
					if (jResponse.length() > 0) {
						JSONArray result = jResponse.getJSONArray("results");
						if (result != null && result.length() == 1) {
							JSONObject res = result.getJSONObject(0);
							if (res.length() > 0) {

								if (res.has("error") && res.has("responseStatus") && res.has("description")) {
									int resStatus = res.getInt("responseStatus");
									if (resStatus == Constants.SERVER_ERROR_PIC_MODERATION ||
											resStatus == Constants.SERVER_ERROR_UPLOAD_NO_SIZE) {
										activity.showAlert(res.getString("description"));
									}

									LogUtils.e(LOG_TAG, "Media upload FAIL with error code " + resStatus + " and error " + res.getString("error"));
								} else if (res.has("url")) {
									String link = res.getString("url");
									LogUtils.d(LOG_TAG, "Media link: " + link);

									return link;
								}
							}
						}
					}
				}
			} catch(JSONException | IOException e){
				LogUtils.e(LOG_TAG, "Media upload FAIL: " + e.getMessage(), e);
				activity.showAlert(R.string.error_upload_media);
			}
		}

		return null;
	}


	private static final String MEDIA_UPLOAD_VALUE = Constants.HTTP_KEY;
	private static final String MEDIA_UPLOAD_URL = BuildConfig.USE_PROD_CONNECTION ? Constants.MEDIA_UPLOAD_URL_PROD : Constants.MEDIA_UPLOAD_URL_DEV;
	public static final String MEDIA_UPLOAD_NEW_PIC_PROFILE = "pp";
	public static final String MEDIA_UPLOAD_NEW_PIC_PROFILE_INTEREST = "ppi";
	public static final String MEDIA_UPLOAD_NEW_PIC_WALL = "wp";
	public static final String MEDIA_UPLOAD_NEW_PIC_WALL_INTEREST = "wpi";
	public static final String MEDIA_UPLOAD_CLAIM = "claim";
	public static class UploadService {

		private HLActivity activity;

		private String mediaType;
		private String format;

		public UploadService(HLActivity activity) {
			this.activity = activity;
		}

		/**
		 * Connects to provided URL to send the media file.
		 * @param context  the application's/activity's {@link Context} instance.
		 * @param file     the file to be uploaded.
		 * @param listener the callback receiver when uploading is concluded.
		 * @return True if the operation is successful.
		 * @throws IOException if the operation is not successful.
		 * @throws IllegalArgumentException if some of or all the provided pieces are invalid.
		 */
		public boolean uploadMedia(@NonNull Context context, @NonNull File file, @NonNull MediaUploadListener listener)
				throws IllegalArgumentException {

			return uploadMedia(context, file, null, null, null, listener);
		}

		/**
		 * Connects to provided URL to send the media file.
		 * @param context  the application's/activity's {@link Context} instance.
		 * @param file     the file to be uploaded.
		 * @param userId   the {@link it.keybiz.lbsapp.corporate.models.HLUser#_id} of the user uploading the picture.
		 * @param listener the callback receiver when uploading is concluded.
		 * @return True if the operation is successful.
		 * @throws IOException if the operation is not successful.
		 * @throws IllegalArgumentException if some of or all the provided pieces are invalid.
		 */
		public boolean uploadMedia(@NonNull Context context, @NonNull File file, @NonNull String userId,
								   @NonNull MediaUploadListener listener)
				throws IllegalArgumentException {

			return uploadMedia(context, file, null, userId, null, listener);
		}


		/**
		 * Connects to provided URL to send the media file.
		 * @param context  the application's/activity's {@link Context} instance.
		 * @param file     the file to be uploaded.
		 * @param type     the eventual type of upload. Possible values: "pp" (profile picture) and "wp" (wall picture).
		 * @param userId   the {@link it.keybiz.lbsapp.corporate.models.HLUser#_id} of the user uploading the picture.
		 * @param name     the {@link HLUser#getCompleteName()} of the user uploading the picture.
		 * @param listener the callback receiver when uploading is concluded.
		 * @return True if the operation is successful.
		 * @throws IOException if the operation is not successful.
		 * @throws IllegalArgumentException if some of or all the provided pieces are invalid.
		 */
		public boolean uploadMedia(@NonNull Context context, @NonNull File file, @Nullable String type,
								   @Nullable String userId, @Nullable String name,
								   @NonNull MediaUploadListener listener)
				throws IllegalArgumentException {

			if (!Utils.isDeviceConnected(context))
				return false;

			mediaType = MediaHelper.getMimeType(file.getAbsolutePath());
			format = MediaHelper.getFileExtension(file.getAbsolutePath());

			if (Utils.areStringsValid(mediaType, format, file.getName())) {
				OkHttpClient client = new OkHttpClient.Builder()
						.connectTimeout(1, TimeUnit.MINUTES)
						.readTimeout(1, TimeUnit.MINUTES)
						.writeTimeout(1, TimeUnit.MINUTES)
						.build();

				try {
					final RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
							.addFormDataPart(
									"file",
									file.getName(),
									new ProgressRequestBody(
											file,
											mediaType,
											num -> {
												LogUtils.v(LOG_TAG, "UPLOAD FILE PROGRESS - Percent: " + num);
												activity.setProgressMessage(
														activity.getString(
																R.string.uploading_media_progress,
																num
														)
												);
											}
									)
							)
							.addFormDataPart("k", MEDIA_UPLOAD_VALUE)
							.addFormDataPart("f", format)
							.addFormDataPart("m", mediaType)
							.addFormDataPart("t", Utils.isStringValid(type) ? type : "")
							.addFormDataPart("userID", Utils.isStringValid(userId) ? userId : "")
							.addFormDataPart("name", Utils.isStringValid(name) ? name : "")
							.addFormDataPart("date", Utils.isStringValid(type) ?
									Utils.formatDateForDB(System.currentTimeMillis()) : "")
							.build();

					final Request request = new Request.Builder().url(MEDIA_UPLOAD_URL)
							.post(requestBody).build();


					client.newCall(request).enqueue(listener);
				} catch (Exception e) {
					LogUtils.e(LOG_TAG, e.getMessage(), e);
					if (Utils.isContextValid(activity)) {
						activity.setShowProgressAnyway(false);
						activity.closeProgress();
						activity.showAlert(R.string.error_upload_media);
					}
				}

				return true;
			}

			activity.setShowProgressAnyway(false);
			activity.closeProgress();
			throw new IllegalArgumentException("Provided arguments are not valid");
		}
	}


	/**
	 * It deletes the file at the specified path.
	 * @param filePath the provided file path.
	 * @param deleteOnClose boolean value needed only in some places (default = false).
	 */
	public static void deleteMediaFile(String filePath, boolean deleteOnClose) {
		if (Utils.isStringValid(filePath)) {
			File f = new File(Uri.parse(filePath).getPath());
			deleteMediaFile(f, deleteOnClose);
		}
	}

	/**
	 * It deletes the specified file.
	 * @param file the provided file.
	 * @param deleteOnClose boolean value needed only in some places (default = false).
	 */
	public static void deleteMediaFile(File file, boolean deleteOnClose) {
		if (file != null) {
			boolean deleteAnyway =  file.length() == 0 || file.length() < 1000;
			if (deleteOnClose || deleteAnyway) {
				if (file.exists()) {
					String filePath = file.getPath();
					if (file.delete())
						LogUtils.d(LOG_TAG, "Audio file deleted successfully @ " + filePath);
					else
						LogUtils.e(LOG_TAG, "COULDN'T DELETE audio file");
				}
			}
		}
	}

	//endregion


	//region == Bitmap transformation from path ==

	public static Drawable modifyOrientation(Context context, Bitmap bitmap, String image_absolute_path) throws IOException {
		ExifInterface ei = new ExifInterface(image_absolute_path);
		int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

		Bitmap b;
		switch (orientation) {
			case ExifInterface.ORIENTATION_ROTATE_90:
				b = rotate(bitmap, 90);
				break;

			case ExifInterface.ORIENTATION_ROTATE_180:
				b = rotate(bitmap, 180);
				break;

			case ExifInterface.ORIENTATION_ROTATE_270:
				b = rotate(bitmap, 270);
				break;

			case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
				b = flip(bitmap, true, false);
				break;

			case ExifInterface.ORIENTATION_FLIP_VERTICAL:
				b = flip(bitmap, false, true);
				break;

			default:
				b = bitmap;
		}

		return new BitmapDrawable(context.getResources(), b);
	}

	private static Bitmap rotate(Bitmap bitmap, float degrees) {
		Matrix matrix = new Matrix();
		matrix.postRotate(degrees);
		return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
	}

	private static Bitmap flip(Bitmap bitmap, boolean horizontal, boolean vertical) {
		Matrix matrix = new Matrix();
		matrix.preScale(horizontal ? -1 : 1, vertical ? -1 : 1);
		return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
	}

	//endregion

	//endregion


	//region == Media player methods ==

	public void configureMediaPlayer(final Context context, String videoUri, VideoView view, boolean wantsMediaController) {
		if (view != null && Utils.isStringValid(videoUri)) {
			final MediaController mc;
			mc = new MediaController(context);
			mc.setAnchorView(view);
			mc.setVisibility(wantsMediaController ? View.VISIBLE : View.GONE);
			view.setMediaController(mc);

			view.setVideoPath(videoUri);
			view.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
				@Override
				public void onPrepared(MediaPlayer mediaPlayer) {
					LogUtils.d(LOG_TAG, "VIDEO: resource READY, video STARTING");
					mediaPlayer.start();
				}
			});
			view.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
				@Override
				public void onCompletion(MediaPlayer mediaPlayer) {
					mediaPlayer.stop();
					mc.hide();
				}
			});
			view.setOnInfoListener(new MediaPlayer.OnInfoListener() {
				@Override
				public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
					return false;
				}
			});
			view.setOnErrorListener(new MediaPlayer.OnErrorListener() {
				@Override
				public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
					if (context != null && context instanceof HLActivity)
						// TODO: 9/27/2017    REPLACE STRING
						((HLActivity) context).showAlert("Not able to play the video at the moment");
					return true;
				}
			});
		}
	}


	public static void playVideoWithResources(final VideoView mainView, final View thumbnail,
											  Object media, final View playBtn,
											  final View progressView, final TextView progressMessage,
											  final boolean loop, final int position) {

		playBtn.animate().alpha(0).setListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animator) {}

			@Override
			public void onAnimationEnd(Animator animator) {
//				progressView.setVisibility(View.VISIBLE);
			}

			@Override
			public void onAnimationCancel(Animator animator) {}

			@Override
			public void onAnimationRepeat(Animator animator) {}
		}).start();

		if (media != null) {
			mainView.setVideoURI(media instanceof Uri ? Uri.fromFile(new File(((Uri) media).getPath())) : Uri.parse((String) media));
			mainView.requestFocus();
			mainView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
				@Override
				public void onPrepared(MediaPlayer mediaPlayer) {
					progressView.setVisibility(View.GONE);
					mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
						@Override
						public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
							progressMessage.setText(progressMessage.getContext().getString(R.string.buffering_video_perc, String.valueOf(i)));
							if (i == 100)
								progressView.setVisibility(View.GONE);
						}
					});

					if (position >= 0) {
						mainView.seekTo(position);
					}

					thumbnail.setVisibility(View.GONE);
					mainView.start();
				}
			});

			mainView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
				@Override
				public void onCompletion(MediaPlayer mediaPlayer) {
					mainView.seekTo(0);

					if (loop) {
						mainView.start();
					}
					else {
						playBtn.animate().alpha(1).setListener(new Animator.AnimatorListener() {
							@Override
							public void onAnimationStart(Animator animator) {
								progressView.setVisibility(View.GONE);
							}

							@Override
							public void onAnimationEnd(Animator animator) {
							}

							@Override
							public void onAnimationCancel(Animator animator) {
							}

							@Override
							public void onAnimationRepeat(Animator animator) {
							}
						}).start();
					}

				}
			});

			mainView.setOnInfoListener(new MediaPlayer.OnInfoListener() {

				boolean hasStarted = false;

				@Override
				public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
					if (i == MediaPlayer.MEDIA_INFO_BUFFERING_END && hasStarted) {
						progressView.setVisibility(View.GONE);
						return true;
					} else if (i == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
						progressView.setVisibility(View.VISIBLE);
						hasStarted = true;
					}

					return false;
				}
			});
		}
	}

	public static void playVideo(final VideoView mainView, String url, final boolean loop) {
		if (Utils.isStringValid(url)) {
			mainView.setVideoURI(Uri.parse(url));
			mainView.requestFocus();
			mainView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
				@Override
				public void onPrepared(MediaPlayer mediaPlayer) {
					mainView.start();
				}
			});

			mainView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
				@Override
				public void onCompletion(MediaPlayer mediaPlayer) {
					if (loop) {
						mainView.seekTo(0);
						mainView.stopPlayback();
					}
				}
			});
		}
	}

	public static void playVideo(Context context, final PlayerView mainView, Object media,
								 final boolean loop, final boolean fit, final boolean playWhenReady,
								 final Long currentPosition, final Player.EventListener listener) {

		Uri uri = null;
		if (media instanceof String)
			uri = Uri.parse((String) media);
		else if (media instanceof File)
			uri = Uri.parse(((File) media).getPath());
		else if (media instanceof Uri)
			uri = (Uri) media;

		if (uri != null) {

			SimpleExoPlayer player = (SimpleExoPlayer) mainView.getPlayer();
			if (player == null)
				player = ExoPlayerFactory.newSimpleInstance(context);

			SimpleExoPlayer finalPlayer = player;

			finalPlayer.setRepeatMode(loop ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
			mainView.setResizeMode(fit ? AspectRatioFrameLayout.RESIZE_MODE_FIT : AspectRatioFrameLayout.RESIZE_MODE_ZOOM);

			finalPlayer.setAudioAttributes(
					new AudioAttributes.Builder()
							.setUsage(C.USAGE_MEDIA)
							.setContentType(C.CONTENT_TYPE_MOVIE)
							.build(),
					true
			);

			finalPlayer.setPlayWhenReady(playWhenReady);

			if (listener != null)
				finalPlayer.addListener(listener);
			else if (currentPosition != null && currentPosition > 0) {
				finalPlayer.addListener(new Player.EventListener() {
					@Override
					public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
						if (playbackState == Player.STATE_READY)
							finalPlayer.seekTo(currentPosition);
					}

					@Override
					public void onPlayerError(ExoPlaybackException error) {
						// TODO   add error handling
					}

					@Override
					public void onSeekProcessed() {
						// TODO   is this useful?
					}
				});
			}

			// Produces DataSource instances through which media data is loaded.
			DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(
					context,
					Util.getUserAgent(context, BuildConfig.APPLICATION_ID)
			);

			// This is the MediaSource representing the media to be played.
			MediaSource videoSource = new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(
					uri
//					Uri.parse("http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
			);
			// Prepare the player with the source.
			finalPlayer.prepare(videoSource);

			mainView.setPlayer(finalPlayer);
		}
	}


	public static void playAudio(final MediaPlayer mediaPlayer, String url, final boolean loop) {
		if (mediaPlayer == null)
			return;

		try {
			mediaPlayer.setDataSource(url);
			mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
				@Override
				public void onCompletion(MediaPlayer mediaPlayer) {
					mediaPlayer.stop();

//					try {
//						mediaPlayer.reset();
//					} catch (IllegalStateException e) {
//						e.printStackTrace();
//					}

//					if (loop)
//						mediaPlayer.start();
				}
			});

			mediaPlayer.prepare();
			mediaPlayer.start();
		}
		catch (Exception e) {
			LogUtils.e("MyAudioStreamingApp", Utils.isStringValid(e.getMessage()) ?
					e.getMessage() : "");
		}
	}

	public static void playAudioWithResources(@NonNull final MediaPlayer mediaPlayer, int position,
											  String url, final View playBtn, @Nullable final View pauseLayout,
											  final View progressView, final TextView progressMessage,
											  final LottieAnimationView lottieView) {

		playBtn.animate().alpha(0).setListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animator) {}

			@Override
			public void onAnimationEnd(Animator animator) {
//				progressView.setVisibility(View.VISIBLE);
			}

			@Override
			public void onAnimationCancel(Animator animator) {}

			@Override
			public void onAnimationRepeat(Animator animator) {}
		}).start();

		if (Utils.isStringValid(url)) {
			try {
				mediaPlayer.setDataSource(url);

				mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
					@Override
					public void onPrepared(MediaPlayer mediaPlayer) {
						progressView.setVisibility(View.GONE);
//						mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
//							@Override
//							public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
//								progressMessage.setText(progressMessage.getContext()
//										.getString(R.string.buffering_audio_perc, String.valueOf(i)));
//								lottieView.pauseAnimation();
//
//								if (i == 100) {
//									progressView.setVisibility(View.GONE);
//									if (lottieView.getFrame() != 0)
//										lottieView.resumeAnimation();
//									else
//										lottieView.playAnimation();
//								}
//							}
//						});
						mediaPlayer.start();
						if (lottieView.getFrame() != 0)
							lottieView.resumeAnimation();
						else
							lottieView.playAnimation();
					}
				});

				mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
					@Override
					public void onCompletion(MediaPlayer mediaPlayer) {
						playBtn.animate().alpha(1).setListener(new Animator.AnimatorListener() {
							@Override
							public void onAnimationStart(Animator animator) {
								progressView.setVisibility(View.GONE);
							}

							@Override
							public void onAnimationEnd(Animator animator) {}

							@Override
							public void onAnimationCancel(Animator animator) {}

							@Override
							public void onAnimationRepeat(Animator animator) {}
						}).start();

						mediaPlayer.stop();
						mediaPlayer.reset();

						lottieView.pauseAnimation();
						lottieView.cancelAnimation();
						lottieView.setFrame(0);
					}
				});

				mediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {

					boolean hasStarted = false;

					@Override
					public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
						if (i == MediaPlayer.MEDIA_INFO_BUFFERING_END && hasStarted) {
							progressView.setVisibility(View.GONE);
							return true;
						} else if (i == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
							progressView.setVisibility(View.VISIBLE);
							hasStarted = true;
						}

						return false;
					}
				});

				if (mediaPlayer.getCurrentPosition() == 0)
					mediaPlayer.prepare();

				if (position != 0) {
					mediaPlayer.seekTo(position);
					mediaPlayer.start();
					lottieView.playAnimation();
				}
			} catch (IOException e) {
				LogUtils.e("MyAudioStreamingApp", Utils.isStringValid(e.getMessage()) ?
						e.getMessage() : "");
			}
		}
	}

	public static void playAudio(final MediaPlayer mediaPlayer, String url, final boolean loop,
								 LottieAnimationView lottieView) {
		if (mediaPlayer == null)
			return;

		try {
			mediaPlayer.setDataSource(url);
			mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
				@Override
				public void onCompletion(MediaPlayer mediaPlayer) {
					mediaPlayer.stop();

//					try {
//						mediaPlayer.reset();
//					} catch (IllegalStateException e) {
//						e.printStackTrace();
//					}

//					if (loop)
//						mediaPlayer.start();
				}
			});

			mediaPlayer.prepare();
			mediaPlayer.start();
		}
		catch (Exception e) {
			LogUtils.e("MyAudioStreamingApp", Utils.isStringValid(e.getMessage()) ?
					e.getMessage() : "");
		}
	}


	public void play(VideoView view, int position) {
		view.seekTo(position);
		view.start();
	}

	public void stop(VideoView view) {
		view.stopPlayback();
	}

	public void closingOperations() {
		if (mediaPlayer != null) {
			mediaPlayer.stop();
			mediaPlayer.release();
		}
	}


	public static PlayAudioTask getAudioTask(MediaPlayer mediaPlayer, View playBtn, View pauseBtn, View progressView,
											 boolean playing, int lastPlayerPosition, LottieAnimationView siriAnimation, int pauseFrame) {
		return new PlayAudioTask(mediaPlayer, playBtn, pauseBtn, progressView, playing, lastPlayerPosition, siriAnimation, pauseFrame);
	}



	public static class PlayAudioTask extends AsyncTask<Object, Void, Boolean> {

		private MediaPlayer mediaPlayer;
		private WeakReference<View> playBtn, pauseBtn;
		private WeakReference<View> progressView;
		private WeakReference<LottieAnimationView> siriAnimation;

		private Boolean playing;
		private Integer lastPlayerPosition;
		private Integer pauseFrame;

		PlayAudioTask(MediaPlayer mediaPlayer, View playBtn, View pauseBtn, View progressView,
					  boolean playing, int lastPlayerPosition, LottieAnimationView siriAnimation, int pauseFrame) {

			this.mediaPlayer = mediaPlayer;
			this.playBtn = new WeakReference<>(playBtn);
			this.pauseBtn = new WeakReference<>(pauseBtn);
			this.progressView = new WeakReference<>(progressView);
			this.playing = playing;
			this.lastPlayerPosition = lastPlayerPosition;
			this.siriAnimation = new WeakReference<>(siriAnimation);
			this.pauseFrame = pauseFrame;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			playBtn.get().setVisibility(View.GONE);
			progressView.get().setVisibility(View.VISIBLE);

			siriAnimation.get().setFrame(pauseFrame);

			if (mediaPlayer.isPlaying()){
				playBtn.get().setVisibility(View.GONE);
			}
			else {
				siriAnimation.get().cancelAnimation();
			}
		}

		@Override
		protected Boolean doInBackground(Object... objects) {
			Boolean prepared = null;

			if (mediaPlayer != null) {
				try {
					Object obj = objects[0];
					if (obj instanceof Uri)
						mediaPlayer.setDataSource(playBtn.get().getContext(), (Uri) obj);
					else if (obj instanceof String && Utils.isStringValid((String) obj))
						mediaPlayer.setDataSource((String) obj);

					mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
						@Override
						public void onCompletion(MediaPlayer mediaPlayer) {
							playBtn.get().setVisibility(View.VISIBLE);
							pauseBtn.get().setVisibility(View.GONE);

							mediaPlayer.stop();
							mediaPlayer.reset();
							siriAnimation.get().cancelAnimation();
							siriAnimation.get().setFrame(0);

							playing = false;
							lastPlayerPosition = 0;
						}
					});

					mediaPlayer.prepare();
					prepared = true;

				} catch (Exception e) {
					LogUtils.e("MyAudioStreamingApp", Utils.isStringValid(e.getMessage()) ?
							e.getMessage() : "");
					prepared = false;
				}
			}

			return prepared;
		}

		@Override
		protected void onPostExecute(Boolean aBoolean) {
			super.onPostExecute(aBoolean);

			progressView.get().setVisibility(View.GONE);

			if (mediaPlayer != null) {
				if (lastPlayerPosition != null)
					mediaPlayer.seekTo(lastPlayerPosition);
				if (pauseFrame != null) {
					siriAnimation.get().setFrame(pauseFrame);
					siriAnimation.get().resumeAnimation();
				}
				else siriAnimation.get().playAnimation();

				mediaPlayer.start();
				pauseBtn.get().setVisibility(View.VISIBLE);
			}

			playing = true;
		}
	}


	public static PlayVideoTask getVideoTask(final MediaPlayer mediaPlayer, final View thumbnail,
											 final View playBtn, final View progressView,
											 final TextView progressMessage,
											 final boolean loop, final int position) {
		return new PlayVideoTask(mediaPlayer, thumbnail, playBtn, progressView, progressMessage,
				loop, position);
	}


	public static class PlayVideoTask extends AsyncTask<Void, Void, Boolean> {

		private MediaPlayer mediaPlayer;
		private WeakReference<View> playBtn;
		private WeakReference<View> progressView;
		private WeakReference<View> thumbnail;
		private WeakReference<TextView> progressMessage;

		private Boolean playing;
		private int position;
		private boolean loop;

		PlayVideoTask(final MediaPlayer mediaPlayer, final View thumbnail, final View playBtn,
					  final View progressView, final TextView progressMessage,
					  final boolean loop, final int position) {

			this.mediaPlayer = mediaPlayer;

			this.thumbnail = new WeakReference<>(thumbnail);
			this.playBtn = new WeakReference<>(playBtn);
			this.progressView = new WeakReference<>(progressView);
			this.progressMessage = new WeakReference<>(progressMessage);
			this.loop = loop;
			this.position = position;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			thumbnail.get().setVisibility(View.GONE);
		}

		@Override
		protected Boolean doInBackground(Void... objects) {

			try {
				mediaPlayer.setOnBufferingUpdateListener((mediaPlayer1, i) -> {
					progressMessage.get().setText(progressMessage.get().getContext().getString(R.string.buffering_video_perc, String.valueOf(i)));
					if (i == 100)
						progressView.get().setVisibility(View.GONE);
				});

				if (position >= 0) {
					mediaPlayer.seekTo(position);
				}

				mediaPlayer.start();

				mediaPlayer.setOnCompletionListener(mediaPlayer -> {
					mediaPlayer.seekTo(0);

					if (loop) {
						mediaPlayer.start();
					}
					else {
						playBtn.get().animate().alpha(1).setListener(new Animator.AnimatorListener() {
							@Override
							public void onAnimationStart(Animator animator) {
								playBtn.get().setVisibility(View.VISIBLE);
							}

							@Override
							public void onAnimationEnd(Animator animator) {}

							@Override
							public void onAnimationCancel(Animator animator) {}

							@Override
							public void onAnimationRepeat(Animator animator) {}
						}).start();
					}

				});

				mediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {

					boolean hasStarted = false;

					@Override
					public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
						if (i == MediaPlayer.MEDIA_INFO_BUFFERING_END && hasStarted) {
							progressView.get().setVisibility(View.GONE);
							return true;
						} else if (i == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
							progressView.get().setVisibility(View.VISIBLE);
							hasStarted = true;
						}

						return false;
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}

	}

	//endregion



	public boolean doesMediaWantLandscape(PostTypeEnum type, View view, int vw, int vh) {
		int w = 0, h = 0;
		switch (type) {
			case PHOTO:
			case PHOTO_PROFILE:
			case PHOTO_WALL:
				if (view != null && view instanceof ImageView) {
					Drawable d = ((ImageView) view).getDrawable();
					if (d != null) {
						w = d.getIntrinsicWidth();
						h = d.getIntrinsicHeight();
					}
				}
				break;
			case VIDEO:
				if (view != null/* && view instanceof ExoPlayerView*/) {
					w = vw;
					h = vh;
				}
				break;
			default:
				return false;
		}

		return w > h;
	}

}
