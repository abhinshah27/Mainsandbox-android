/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.wishes;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.afollestad.materialdialogs.MaterialDialog;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.BasicInteractionListener;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.base.OnBackPressedListener;
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.features.createPost.AudioRecordFragment;
import it.keybiz.lbsapp.corporate.features.createPost.CreatePostActivityMod;
import it.keybiz.lbsapp.corporate.features.createPost.CreatePostHelper;
import it.keybiz.lbsapp.corporate.features.createPost.GalleryFragment;
import it.keybiz.lbsapp.corporate.features.createPost.TagFragment;
import it.keybiz.lbsapp.corporate.models.HLNotifications;
import it.keybiz.lbsapp.corporate.models.HLWish;
import it.keybiz.lbsapp.corporate.models.LifeEvent;
import it.keybiz.lbsapp.corporate.models.Tag;
import it.keybiz.lbsapp.corporate.models.WishListElement;
import it.keybiz.lbsapp.corporate.models.enums.PostTypeEnum;
import it.keybiz.lbsapp.corporate.models.enums.SearchTypeEnum;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.DialogUtils;
import it.keybiz.lbsapp.corporate.utilities.FragmentsUtils;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.CustomGalleryAdapter;
import it.keybiz.lbsapp.corporate.utilities.media.HLMediaType;

/**
 * @author mbaldrighi on 2/14/2017.
 */
public class WishesActivity extends HLActivity implements View.OnClickListener, OnServerMessageReceivedListener,
		OnMissingConnectionListener, BasicInteractionListener, WishesActivityListener,
		DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener,
		GalleryFragment.OnGalleryFragmentInteractionListener, CustomGalleryAdapter.OnMediaClickListener,
		AudioRecordFragment.OnAudioRecordFragmentInteractionListener,
		TagFragment.OnTagFragmentInteractionListener {

	public static final String LOG_TAG = WishesActivity.class.getCanonicalName();

	public enum TimePickerClockType { HOURS_24, HOURS_12 }

	private FragmentManager fragmentManager = getSupportFragmentManager();

	private HLWish wishToEdit;

	private View stepBarLayout;
	private LinearLayout navigationStepBar;
	private ValueAnimator alphaAnimation;
	private View nextBtn;

	private List<WishListElement> elementsStack = new ArrayList<>();
	private boolean removeElementFromStack = true;
	private WishListElement currentElementToSend;
	private String triggerFriendId;
	private Bundle dataBundle;

	private String wishName;

	private TextView toolbarTitle;
	private TextView stepTitle, stepSubTitle;

	private View notificationDot;

	private OnBackPressedListener backListener;

	private boolean specificDateSet = false;

	private MaterialDialog dialogAddEvent;
	private TextView addEventDateText;
	private LifeEvent newLifeEvent = null;

	private boolean callPreviousElement;

	private String postToEdit;

	private boolean ignoreTitlesInResponse = false;
	private boolean nextAlwaysOn = false;
	private boolean ignoreCallElement = true;


	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wish_create);
		setRootContent(R.id.root_content);
		setProgressIndicator(R.id.generic_progress_indicator);

		configureToolbar(findViewById(R.id.toolbar));

		stepTitle = findViewById(R.id.title);
		stepSubTitle = findViewById(R.id.sub_title);

		configureWishNavigationBar(stepBarLayout = findViewById(R.id.navigation_progress_bar));

		manageIntent();
	}

	@Override
	protected void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (notificationDot != null)
			notificationDot.setVisibility(HLNotifications.getInstance().getUnreadCount(true) > 0 ?
					View.VISIBLE : View.GONE);
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	public void onClick(final View view) {
		switch (view.getId()) {
			case R.id.back_arrow:
				onBackPressed(true);
				break;

			case R.id.wish_cancel_btn:
//			case R.id.bottom_global_search:
//			case R.id.bottom_timeline:
//			case R.id.bottom_profile:
//			case R.id.bottom_settings:
//			case R.id.main_action_btn:
				final MaterialDialog dialog = DialogUtils.createGenericAlertCustomView(this, R.layout.custom_dialog_exit_wish);
				if (dialog != null) {
					View v = dialog.getCustomView();
					if (v != null) {
						v.findViewById(R.id.button_negative).setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								dialog.dismiss();
							}
						});
						Button positive = v.findViewById(R.id.button_positive);
						positive.setText(R.string.exit);
						positive.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								if (view.getId() == R.id.wish_cancel_btn) {
									setResult(RESULT_CANCELED);
								}
								else if (view.getId() == R.id.main_action_btn) {
									Intent intent = new Intent(WishesActivity.this, CreatePostActivityMod.class);
									intent.putExtra(Constants.EXTRA_PARAM_2, true);
									startActivity(intent);
								}

								finish();
								dialog.dismiss();
							}
						});
					}
					dialog.show();
				}
				break;

			case R.id.wish_next_btn:
				if (nextListener != null)
					nextListener.onNextClick();
				else
					navigate();
				break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onBackPressed() {
		onBackPressed(false);
	}

	private void onBackPressed(boolean fromToolbar) {
		if (!fromToolbar && backListener != null)
			backListener.onBackPressed();
		else {
			callPreviousElement = true;
			removeElementFromStack = true;
			nextListener = null;

			if (fragmentManager.getBackStackEntryCount() == 1) {
				setResult(RESULT_CANCELED);
				finish();
				return;
			} else {
				callServer(CallType.BACK, false);
				super.onBackPressed();
			}

			resetUIAndData();
		}
	}


	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		switch (operationId) {
			case Constants.SERVER_OP_WISH_SAVE_NEW_LIFE_EVENT:
				callPreviousElement = true;
				removeElementFromStack = false;
				callServer(CallType.ELEMENTS, false);
				dialogAddEvent.dismiss();

//				// so that the subsequent call made by fragment has only the fragment as receiver
//				disableReceiver();
				break;

			case Constants.SERVER_OP_WISH_GET_LIST_ELEMENTS:
				if (!ignoreCallElement) {
					if (responseObject == null || responseObject.length() == 0)
						return;

					JSONArray json = responseObject.optJSONObject(0).optJSONArray("items");
					if (json != null && json.length() == 1) {
						WishListElement wli = new WishListElement().deserializeToClass(json.optJSONObject(0));
						setSelectedWishListElement(wli);
					}

					if (!ignoreTitlesInResponse) {
						setStepTitle(responseObject.optJSONObject(0).optString("title"));
						setStepSubTitle(responseObject.optJSONObject(0).optString("subTitle"));
					}

					ignoreCallElement = true;
				}
				break;

			case Constants.SERVER_OP_WISH_BACK:
				LogUtils.d(LOG_TAG, "WISHES - Back action registered");
				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		switch (operationId) {
			case Constants.SERVER_OP_WISH_BACK:
				LogUtils.e(LOG_TAG, "WISHES - Back action ERROR with code: " + errorCode);
				break;
		}
	}

	@Override
	public void onMissingConnection(int operationId) {}


	@Override
	protected void manageIntent() {
		Intent intent = getIntent();
		if(intent == null)
			return;

		int showFragment = intent.getIntExtra(Constants.FRAGMENT_KEY_CODE,
				Constants.FRAGMENT_INVALID);
		int requestCode = intent.getIntExtra(Constants.REQUEST_CODE_KEY, Constants.NO_RESULT);
		Bundle extras = intent.getExtras();
		String userId = null;
		String name = null;
		String avatar = null;

		if (extras != null && extras.containsKey(Constants.EXTRA_PARAM_1))
			wishToEdit = (HLWish) extras.getSerializable(Constants.EXTRA_PARAM_1);

		switch (showFragment) {
			case Constants.FRAGMENT_WISH_NAME:
				addWishNameFragment();
				break;

			case Constants.FRAGMENT_WISH_PREVIEW:
				addWishPreviewFragment(
						null,
						wishToEdit != null ? wishToEdit.getId() : null,
						wishToEdit != null ? wishToEdit.getName() : null,
						WishPreviewFragment.UsageType.VIEW_SAVED_WISH,
						Constants.NO_RESULT,
						true
				);
				break;

		}
	}


	//region == Class custom methods ==

	private void configureToolbar(View viewById) {
		if (viewById != null) {
			View v = viewById.findViewById(R.id.back_arrow);
			if (v != null)
				v.setOnClickListener(this);

			toolbarTitle = viewById.findViewById(R.id.toolbar_title);
		}
	}

	private void configureWishNavigationBar(View viewById) {
		if (viewById != null) {
			View cancel = viewById.findViewById(R.id.wish_cancel_btn);
			if (cancel != null)
				cancel.setOnClickListener(this);
			nextBtn = viewById.findViewById(R.id.wish_next_btn);
			if (nextBtn != null) {
				nextBtn.setOnClickListener(this);
				nextBtn.setEnabled(false);
			}

			navigationStepBar = viewById.findViewById(R.id.wish_progress_bar);
		}
	}

	public void animate(final View view) {
		alphaAnimation = ValueAnimator.ofFloat(1f, 0f);
		alphaAnimation.setDuration(1000);
		alphaAnimation.setRepeatMode(ValueAnimator.REVERSE);
		alphaAnimation.setRepeatCount(ValueAnimator.INFINITE);
		alphaAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				float value = (Float) animation.getAnimatedValue();
				view.setAlpha(value);
			}
		});
		alphaAnimation.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animation) {}

			@Override
			public void onAnimationEnd(Animator animation) {
				view.setAlpha(1);
			}

			@Override
			public void onAnimationCancel(Animator animation) {}

			@Override
			public void onAnimationRepeat(Animator animation) {}
		});

		alphaAnimation.start();
	}

	private void endAlphaAnimation() {
		if (alphaAnimation != null && alphaAnimation.isStarted())
			alphaAnimation.end();

		alphaAnimation = null;
	}

	private void configureBottomBar(final View bar) {
		if (bar != null) {
			View l1 = bar.findViewById(R.id.bottom_timeline);
			l1.setOnClickListener(this);
			View l2 = bar.findViewById(R.id.bottom_profile);
			l2.setOnClickListener(this);
			View l3 = bar.findViewById(R.id.bottom_wishes);
			l3.setOnClickListener(this);
			l3.setSelected(true);
			View l4 = bar.findViewById(R.id.bottom_global_search);
			l4.setOnClickListener(this);

			ImageView ib1 = bar.findViewById(R.id.icon_timeline);
			TransitionDrawable td1 = (TransitionDrawable) ib1.getDrawable();
			td1.setCrossFadeEnabled(true);
			ImageView ib2 = bar.findViewById(R.id.icon_profile);
			TransitionDrawable td2 = (TransitionDrawable) ib2.getDrawable();
			td2.setCrossFadeEnabled(true);
			ImageView ib3 = bar.findViewById(R.id.icon_wishes);
			TransitionDrawable td3 = (TransitionDrawable) ib3.getDrawable();
			td3.setCrossFadeEnabled(true);
			td3.startTransition(0);
			ImageView ib4 = bar.findViewById(R.id.icon_global_search);
			TransitionDrawable td4 = (TransitionDrawable) ib4.getDrawable();
			td4.setCrossFadeEnabled(true);

			ImageView main = bar.findViewById(R.id.main_action_btn);
			main.setOnClickListener(this);

			notificationDot = bar.findViewById(R.id.notification_dot);
		}
	}


	private void hideNextButton() {
		if (nextBtn != null)
			nextBtn.setVisibility(View.INVISIBLE);
	}
	private void showNextButton() {
		if (nextBtn != null)
			nextBtn.setVisibility(View.VISIBLE);
	}

	//endregion


	//region == Wishes interface methods ==

	@Override
	public TextView getStepTitle() {
		return stepTitle;
	}

	@Override
	public TextView getStepSubTitle() {
		return stepSubTitle;
	}

	@Override
	public void setStepTitle(String title) {
		if (stepTitle != null) {
			stepTitle.setText(title);
			stepTitle.setVisibility(Utils.isStringValid(title) ? View.VISIBLE : View.GONE);
		}
	}

	@Override
	public void setStepSubTitle(String subTitle) {
		if (stepSubTitle != null) {
			stepSubTitle.setText(subTitle);
			stepSubTitle.setVisibility(Utils.isStringValid(subTitle) ? View.VISIBLE : View.INVISIBLE);
		}
	}

	@Override
	public void setToolbarTitle(int titleResId) {
		toolbarTitle.setText(titleResId);
	}

	@Override
	public void handleSteps(boolean fromNameFragment) {
		if (currentElementToSend == null || currentElementToSend.getStepsTotal() <= 0 ||
				navigationStepBar == null) return;

		// depending on the number of total steps, it chooses the correct amount of weight
		float weight = 1f / currentElementToSend.getStepsTotal();

		navigationStepBar.removeAllViews();
		endAlphaAnimation();

		for (int i = 0; i < currentElementToSend.getStepsTotal(); i++) {
			View step = LayoutInflater.from(this).inflate(R.layout.item_progress_step_wish, navigationStepBar, false);
			((LinearLayout.LayoutParams) step.getLayoutParams()).weight = weight;

			step.setSelected(i <= (fromNameFragment ? 0 : currentElementToSend.getStep()));
			if (i == (fromNameFragment ? 0 : currentElementToSend.getStep()))
				animate(step);

			navigationStepBar.addView(step);
		}

	}

	@Override
	public void handleSteps() {
		handleSteps(false);
	}

	@Override
	public void enableDisableNextButton(boolean enable) {
		if (nextBtn != null)
			nextBtn.setEnabled(enable);
	}

	@Override
	public void setNextAlwaysOn() {
		nextAlwaysOn = true;
	}

	@Override
	public void hideStepsBar() {
		if (stepBarLayout != null)
			stepBarLayout.setVisibility(View.GONE);
	}
	@Override
	public void showStepsBar() {
		if (stepBarLayout != null)
			stepBarLayout.setVisibility(View.VISIBLE);
	}

	@Override
	public void hideTitles() {
		if (stepTitle != null)
			stepTitle.setVisibility(View.GONE);
		if (stepSubTitle != null)
			stepSubTitle.setVisibility(View.GONE);
	}
	@Override
	public void showTitles() {
		if (stepTitle != null)
			stepTitle.setVisibility(View.VISIBLE);
		if (stepSubTitle != null)
			stepSubTitle.setVisibility(View.VISIBLE);
	}

	@Override
	public void setIgnoreTitlesInResponse(boolean ignore) {
		ignoreTitlesInResponse = ignore;
	}

	public enum CallType { ELEMENTS, NEW_EVENT, BACK }
	@Override
	public void callServer(CallType type, boolean root) {
		Object[] result = null;

		try {
			if (type == CallType.ELEMENTS) {
				if (removeElementFromStack && hasValidStack())
					elementsStack.remove(elementsStack.size() - 1);

				currentElementToSend = (callPreviousElement && hasValidStack()) ?
						elementsStack.get(elementsStack.size()-1) : currentElementToSend;

				if (currentElementToSend != null) {
					result = HLServerCalls.getListElementForWish(
							mUser.getUserId(),
							triggerFriendId,
							currentElementToSend,
							root,
							dataBundle,
							wishToEdit != null ? wishToEdit.getId() : null
					);

					if (!callPreviousElement) {
						if (elementsStack == null)
							elementsStack = new ArrayList<>();

						if (!elementsStack.contains(currentElementToSend)) {

							// currently disabled.
							// TODO: 9/21/2018    CHECK IF NEEDED
//							if (!elementsStack.isEmpty()) {
//								WishListElement lastEl = elementsStack.get(elementsStack.size()-1);
//								if (currentElementToSend.needsToReplaceLastStackElement(lastEl))
//									elementsStack.remove(elementsStack.size()-1);
//							}

							elementsStack.add(currentElementToSend);
						}
					}

					callPreviousElement = false;
					removeElementFromStack = false;
					dataBundle = null;
				}
			}
			else if (type == CallType.NEW_EVENT) {
				if (newLifeEvent != null) {
					result = HLServerCalls.saveNewLifeEvent(mUser.getUserId(), newLifeEvent);
				}
			}
			else if (type == CallType.BACK) {
				result = HLServerCalls.backActionOnWish(mUser.getUserId());
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		HLRequestTracker.getInstance(((LBSLinkApp) getApplication())).handleCallResult(this, this, result);
	}

	private boolean hasValidStack() {
		return elementsStack != null && !elementsStack.isEmpty();
	}

	@Override
	public HLWish getWishToEdit() {
		return wishToEdit;
	}

	@Override
	public boolean isEditMode() {
		return wishToEdit != null;
	}

	@Override
	public WishListElement getSelectedWishListElement() {
		return currentElementToSend;
	}

	@Override
	public void setSelectedWishListElement(WishListElement element) {
		this.currentElementToSend = element;
	}

	@Override
	public void setTriggerFriendId(String friendId) {
		this.triggerFriendId = friendId;
	}

	@Override
	public void setDataBundle(@NonNull Bundle dataBundle) {
		this.dataBundle = dataBundle;
	}

	@Override
	public void setWishName(@NonNull String wishName) {
		this.wishName = wishName;
	}

	@Override
	public String getWishName() {
		return wishName;
	}

	@Override
	public String getSpecificDateString() {
		if (specificDate != null) {
			return new SimpleDateFormat("MMMM dd, yyyy - HH:mm", Locale.getDefault()).format(specificDate.getTime());
		}
		return null;
	}

	@Override
	public void restoreReceiver() {
		configureResponseReceiver();
//		registerReceiver(serverMessageReceiver, new IntentFilter(Constants.BROADCAST_SERVER_RESPONSE));
		LocalBroadcastManager.getInstance(this).registerReceiver(serverMessageReceiver, new IntentFilter(Constants.BROADCAST_SERVER_RESPONSE));
	}

	@Override
	public void disableReceiver() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(serverMessageReceiver);
	}

	@Override
	public void resumeNextNavigation() {
		nextListener = null;
		navigate();
	}

	@Override
	public void setProgressMessageForFinalOps(int stringResId) {
		setProgressMessage(stringResId);
	}

	//endregion


	//region == Time and Date methods ==

	private enum DatePickerType { SPECIFIC_DATE, LT_EVENT }
	private DatePickerType datePickerType;
	private void openDatePicker(DatePickerType type) {
		Calendar date = specificDate != null ? specificDate : Calendar.getInstance();
		DatePickerDialog dpd = DatePickerDialog.newInstance(
				this,
				date.get(Calendar.YEAR),
				date.get(Calendar.MONTH),
				date.get(Calendar.DAY_OF_MONTH)
		);
		dpd.setVersion(DatePickerDialog.Version.VERSION_2);
		dpd.setAccentColor(Utils.getColor(this, R.color.colorAccent));
		dpd.setTitle(getString(R.string.wish_pick_specific_date));
		dpd.setOkText(R.string.action_set);
		if (type == DatePickerType.SPECIFIC_DATE)
			dpd.setMinDate(Calendar.getInstance());

		dpd.show(getSupportFragmentManager(), "DatePickerDialog");
	}

	private enum TimePickerType { SPECIFIC_DATE, CALENDAR }
	private TimePickerType timePickerType;
	private void openTimePicker(TimePickerType type) {
		TimePickerDialog tpd = TimePickerDialog.newInstance(
				this,
				false
		);
		tpd.setVersion(TimePickerDialog.Version.VERSION_2);
		tpd.setAccentColor(Utils.getColor(this, R.color.colorAccent));
		tpd.setTitle(getString(R.string.wish_pick_specific_time));
		tpd.setOkText(type == TimePickerType.SPECIFIC_DATE ? R.string.action_set_next : R.string.action_set);

		tpd.show(getSupportFragmentManager(), "TimePickerDialog");
	}

	private Calendar specificDate;
	@Override
	public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
		specificDate = Calendar.getInstance();
		specificDate.set(year, monthOfYear, dayOfMonth);

		if (datePickerType == DatePickerType.SPECIFIC_DATE) {
			openTimePicker(timePickerType = TimePickerType.SPECIFIC_DATE);
		}
		else if (datePickerType == DatePickerType.LT_EVENT) {
			SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
			addEventDateText.setText(sdf.format(specificDate.getTime()));
		}
	}

	@Override
	public void onTimeSet(TimePickerDialog view, int hourOfDay, int minute, int second) {
		if (timePickerType == TimePickerType.SPECIFIC_DATE) {

			nextBtn.setEnabled(true);

			if (specificDate != null) {
				specificDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
				specificDate.set(Calendar.MINUTE, minute);
				specificDateSet = true;

				// storing set date in dataBundle for next navigation
				if (dataBundle == null)
					dataBundle = new Bundle();
				else
					dataBundle.clear();
				dataBundle.putString("date", Utils.formatDateForDB(specificDate.getTime()));

				String date = getSpecificDateString();
				if (Utils.isStringValid(date) && specificDateSetListener != null) {
					specificDateSetListener.forwardDate(date);

					enableDisableNextButton(false);

					new Handler().postDelayed(new Runnable() {
						@Override
						public void run() {
							navigate();
						}
					}, 700);
				}
			}
		}
	}

	private OnSpecificDateSetListener specificDateSetListener;
	public interface OnSpecificDateSetListener {
		void forwardDate(@NonNull String date);
		void dismissCallResult();
	}

	public void setSpecificDateSetListener(OnSpecificDateSetListener specificDateSetListener) {
		this.specificDateSetListener = specificDateSetListener;
	}

	//endregion


	//region == Fragment section ==

	public void navigate() {
		if (currentElementToSend != null) {
			resetUIAndData();

			if (currentElementToSend.getNextItem() != null && currentElementToSend.getNextItem().equals(Constants.KEY_ITEM_ROOT)) {
				// stepToHighlight here is always 2 because at the beginning of the process
				showBaseListFragment(true);
			}
			else {
				if (currentElementToSend.getAction() != null) {
					switch (currentElementToSend.getAction()) {

						case Constants.KEY_ACTION_ADD_NEW_EVENT:
							openDialogNewLifeEvent();
							return;

						case Constants.KEY_ACTION_PHONE_CALL:
							openInfoDialogPhoneCall();
							return;

						default:
							if (currentElementToSend.getNextNavigationID() != null) {
								switch (currentElementToSend.getNextNavigationID()) {

									case Constants.KEY_NAV_ID_EMAIL:
										addSendEmailFragment(null, Constants.NO_RESULT, true);
										break;

									case Constants.KEY_NAV_ID_SPECIFIC_DATE:
										if (!specificDateSet) {
											ignoreCallElement = false;
											specificDateSetListener.dismissCallResult();
											callServer(CallType.ELEMENTS, false);
											openDatePicker(datePickerType = DatePickerType.SPECIFIC_DATE);
										}
										else {
											showBaseListFragment(false);
										}
										break;

									case Constants.KEY_NAV_ID_CALENDAR:
										// TODO: 3/1/2018    check condition for 12- or 24-hour picker
										showPickersFragment(TimePickerClockType.HOURS_24);
										break;

									case Constants.KEY_NAV_ID_REPETITIONS:
										nextBtn.setEnabled(true);
										addRepetitionsFragment(null, Constants.NO_RESULT, true);
										break;

									case Constants.KEY_NAV_ID_LIST_TILES:
										if (!currentElementToSend.hasNextItem()) break;

										String nextItem = currentElementToSend.getNextItem();
										addSelectPostsFragment(null, getRightType(nextItem),
												isOneSelection(nextItem), Constants.NO_RESULT, true);

										break;

									case Constants.KEY_NAV_ID_FILTER_CONTAINS:
										addFilterListFragment(null, Constants.NO_RESULT, true);
										break;

									case Constants.KEY_NAV_ID_COVER:
										addChooseCoverFragment(null, Constants.NO_RESULT, true);
										break;

									case Constants.KEY_NAV_ID_SAVE_WISH:
										hideNextButton();
										addWishPreviewFragment(
												null,
												wishToEdit != null ? wishToEdit.getId() : null,
												wishToEdit != null ? wishToEdit.getName() : null,
												WishPreviewFragment.UsageType.IN_WIZARD,
												Constants.NO_RESULT,
												true
										);
										break;

									case Constants.KEY_NAV_ID_RECIPIENT_IC:
										addRecipientsInnerCircleFragment(null, Constants.NO_RESULT, true);
										break;

									case Constants.KEY_NAV_ID_SEARCH_LIST:
										SearchTypeEnum type = null;
										if (currentElementToSend.hasNextItem()) {
											if (currentElementToSend.getNextItem().equals(Constants.KEY_ITEM_SEARCH_IC))
												type = SearchTypeEnum.INNER_CIRCLE;
											else if (currentElementToSend.getNextItem().equals(Constants.KEY_ITEM_SEARCH_INTERESTS))
												type = SearchTypeEnum.INTERESTS;
										}
										showSearchListFragment(type);
										break;

									case Constants.KEY_NAV_ID_BASE_LIST:
										showBaseListFragment(false);
										break;

									case Constants.KEY_NAV_ID_LIST_AVATAR:
										showBaseListFragment(false, true);

										if (specificDateSet)
											specificDateSet = false;
										break;

									case Constants.KEY_NAV_ID_CREATE_POST:
										addWishCreatePostFragment(null, postToEdit, Constants.NO_RESULT, true);
										break;

									case Constants.KEY_NAV_ID_PHONE_VERIFY:
										addVerifyPhoneFragment(null, Constants.NO_RESULT, true);
										break;

									case Constants.KEY_NAV_ID_RECORD_AUDIO:
										hideStepsBar();
										// FIXME: 3/9/2018    check for condition for editAudio
										addRecordAudioFragment(null, false, Constants.NO_RESULT, true);
										break;

									case Constants.KEY_NAV_ID_WISH_MY_DIARY:
										addSelectPostFoldersFragment(null, Constants.NO_RESULT, true);
										break;

								}
							}
					}
				}
			}
		}
		else showAlert(R.string.error_wish_no_selection);
	}

	private void resetUIAndData() {
		datePickerType = null;
		ignoreTitlesInResponse = false;
		ignoreCallElement = true;

		// wishToEdit is commented but here it might be the only way to get "Next" button always ready in edit mode
		if (nextBtn != null && !nextAlwaysOn/* && wishToEdit == null*/)
			nextBtn.setEnabled(false);
		nextAlwaysOn = false;

		showStepsBar();
		showNextButton();
		showTitles();

//		restoreReceiver();
	}

	private PostTypeEnum getRightType(String nextItem) {
		if (nextItem != null) {
			switch (nextItem) {
				case Constants.KEY_ITEM_POST_TEXT:
					return PostTypeEnum.TEXT;
				case Constants.KEY_ITEM_POSTS_PIC:
					return PostTypeEnum.PHOTO;
				case Constants.KEY_ITEM_POSTS_AUDIO:
				case Constants.KEY_ITEM_POSTS_AUDIO_ONE:
					return PostTypeEnum.AUDIO;
				case Constants.KEY_ITEM_POSTS_MOVIES:
					return PostTypeEnum.VIDEO;
			}
		}

		return PostTypeEnum.TEXT;
	}

	private boolean isOneSelection(String nextItem) {
		return Utils.isStringValid(nextItem) && nextItem.equals(Constants.KEY_ITEM_POSTS_AUDIO_ONE);
	}

	private void openDialogNewLifeEvent() {
		dialogAddEvent = DialogUtils.createGenericAlertCustomView(this, R.layout.custom_dialog_wish_add_life_event);
		if (dialogAddEvent != null) {
			View v = dialogAddEvent.getCustomView();
			if (v != null) {
				addEventDateText = v.findViewById(R.id.lt_event_date_add);
				addEventDateText.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						openDatePicker(datePickerType = DatePickerType.LT_EVENT);
					}
				});

				final EditText description = v.findViewById(R.id.lt_event_description_edit_add);
				description.setVisibility(View.VISIBLE);
				v.findViewById(R.id.lt_event_description_add).setVisibility(View.GONE);
				v.findViewById(R.id.btn_delete_add).setVisibility(View.GONE);

				Button pos = v.findViewById(R.id.button_positive);
				pos.setText(R.string.wish_add_new_event_btn);
				pos.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						newLifeEvent = new LifeEvent(addEventDateText.getText().toString(), description.getText().toString());
						callServer(CallType.NEW_EVENT, false);
					}
				});

				v.findViewById(R.id.button_negative).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						dialogAddEvent.dismiss();
					}
				});
			}

			dialogAddEvent.show();
		}
	}

	private void openInfoDialogPhoneCall() {
		final MaterialDialog dialog = DialogUtils.createGenericAlertCustomView(this, R.layout.custom_dialog_wish_verify_phone);
		if (dialog != null) {
			View v = dialog.getCustomView();
			if (v != null) {
				Button pos = v.findViewById(R.id.button_positive);
				pos.setText(R.string.action_verify);
				pos.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						addVerifyPhoneFragment(null, Constants.NO_RESULT, true);
						dialog.dismiss();
					}
				});

				v.findViewById(R.id.button_negative).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						dialog.dismiss();
					}
				});
			}

			dialog.show();
		}
	}


	private OnNextClickListener nextListener;
	interface OnNextClickListener {
		void onNextClick();
	}

	@Override
	public OnNextClickListener getOnNextClickListener() {
		return nextListener;
	}

	@Override
	public void setOnNextClickListener(OnNextClickListener listener) {
		nextListener = listener;
	}



	/* NAME FRAGMENT */
	public static void openWishNameFragment(Context context) {
		openWishNameFragment(context, null);}

	public static void openWishNameFragment(Context context, HLWish wish) {
		Bundle bundle = new Bundle();
		if (wish != null)
			bundle.putSerializable(Constants.EXTRA_PARAM_1, wish);
		FragmentsUtils.openFragment(context, bundle, Constants.FRAGMENT_WISH_NAME, Constants.RESULT_WISH_CREATE, WishesActivity.class);
	}

	@Override
	public void showWishNameFragment() {
		addWishNameFragment();
	}

	private void addWishNameFragment() {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
				R.anim.slide_in_left, R.anim.slide_out_right);

		WishNameFragment fragment = (WishNameFragment) fragmentManager.findFragmentByTag(WishNameFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = WishNameFragment.newInstance();
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				WishNameFragment.LOG_TAG, null, Constants.NO_RESULT);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, null, Constants.NO_RESULT);
		fragmentTransaction.commit();
	}


	/* BASE LIST FRAGMENT */
	private void addBaseListFragment(Fragment target, boolean root, boolean withIcons, int requestCode,
	                                 boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		WishBaseListFragment fragment = (WishBaseListFragment) fragmentManager.findFragmentByTag(WishBaseListFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = WishBaseListFragment.newInstance(root, withIcons);
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				WishBaseListFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}

	private void showBaseListFragment(boolean root) {
		showBaseListFragment(root, false);
	}

	private void showBaseListFragment(boolean root, boolean withIcons) {
		addBaseListFragment(null, root, withIcons, Constants.NO_RESULT, true);
	}

	/* FILTERS FRAGMENT */
	private void addFilterListFragment(Fragment target, int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		WishFilterListFragment fragment = (WishFilterListFragment) fragmentManager.findFragmentByTag(WishFilterListFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = WishFilterListFragment.newInstance();
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				WishFilterListFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}

	/* SEARCH FRAGMENT */
	private void addSearchListFragment(Fragment target, SearchTypeEnum type, int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		WishesSearchFragment fragment = (WishesSearchFragment) fragmentManager.findFragmentByTag(WishesSearchFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = WishesSearchFragment.newInstance(type);
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				WishesSearchFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}

	private void showSearchListFragment(SearchTypeEnum type) {
		addSearchListFragment(null, type, Constants.NO_RESULT, true);
	}

	/* PICKERS FRAGMENT */
	private void addWishPickersFragment(Fragment target, TimePickerClockType type, int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		WishPickersFragment fragment = (WishPickersFragment) fragmentManager.findFragmentByTag(WishPickersFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = WishPickersFragment.newInstance(type);
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				WishPickersFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}

	private void showPickersFragment(TimePickerClockType type) {
		addWishPickersFragment(null, type, Constants.NO_RESULT, true);
	}

	/* REPETITIONS FRAGMENT */
	private void addRepetitionsFragment(Fragment target, int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		WishRepetitionsFragment fragment = (WishRepetitionsFragment) fragmentManager.findFragmentByTag(WishRepetitionsFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = WishRepetitionsFragment.newInstance(triggerFriendId);
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				WishRepetitionsFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}


	/* SEND EMAIL FRAGMENT */
	private void addSendEmailFragment(Fragment target, int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		WishSendEmailFragment fragment = (WishSendEmailFragment) fragmentManager.findFragmentByTag(WishSendEmailFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = WishSendEmailFragment.newInstance();
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				WishSendEmailFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}


	/* SELECT POSTS FRAGMENT */
	private void addSelectPostsFragment(Fragment target, PostTypeEnum type, boolean oneSelection,
	                                    int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		WishPostSelectionFragment fragment = (WishPostSelectionFragment) fragmentManager.findFragmentByTag(WishPostSelectionFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = WishPostSelectionFragment.newInstance(type, oneSelection);
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				WishPostSelectionFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}

	/* SELECT POSTS FRAGMENT */
	private void addSelectPostFoldersFragment(Fragment target,int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		WishPostSelectionFoldersFragment fragment = (WishPostSelectionFoldersFragment) fragmentManager.findFragmentByTag(WishPostSelectionFoldersFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = WishPostSelectionFoldersFragment.newInstance();
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				WishPostSelectionFoldersFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}

	/* VERIFY PHONE FRAGMENT */
	private void addVerifyPhoneFragment(Fragment target, int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		WishVerifyPhoneFragment fragment = (WishVerifyPhoneFragment) fragmentManager.findFragmentByTag(WishVerifyPhoneFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = WishVerifyPhoneFragment.newInstance();
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				WishVerifyPhoneFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}

	/* VERIFY PHONE FRAGMENT */
	private void addRecordAudioFragment(Fragment target, boolean editAudio, int requestCode,
	                                    boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		WishAudioRecordFragment fragment = (WishAudioRecordFragment) fragmentManager.findFragmentByTag(WishAudioRecordFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = WishAudioRecordFragment.newInstance(editAudio);
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				WishAudioRecordFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}

	/* RECIPIENTS INNER CIRCLE FRAGMENT */
	private void addRecipientsInnerCircleFragment(Fragment target, int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		WishInnerCircleRecipientsFragment fragment = (WishInnerCircleRecipientsFragment) fragmentManager.findFragmentByTag(WishInnerCircleRecipientsFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = WishInnerCircleRecipientsFragment.newInstance();
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				WishInnerCircleRecipientsFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}

	/* CHOOSE COVER FRAGMENT */
	private void addChooseCoverFragment(Fragment target, int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		WishChooseCoverFragment fragment = (WishChooseCoverFragment) fragmentManager.findFragmentByTag(WishChooseCoverFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = WishChooseCoverFragment.newInstance();
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				WishChooseCoverFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}

	/* WISH PREVIEW FRAGMENT */
	public static void openWishPreviewFragment(Context context, HLWish wish) {
		Bundle bundle = new Bundle();
		if (wish != null)
			bundle.putSerializable(Constants.EXTRA_PARAM_1, wish);
		FragmentsUtils.openFragment(context, bundle, Constants.FRAGMENT_WISH_PREVIEW, Constants.RESULT_WISH_EDIT, WishesActivity.class);
	}

	private void addWishPreviewFragment(Fragment target, String wishId, String wishName, WishPreviewFragment.UsageType type,
	                                    int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		WishPreviewFragment fragment = (WishPreviewFragment) fragmentManager.findFragmentByTag(WishPreviewFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = WishPreviewFragment.newInstance(wishId, wishName, type);
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				WishPreviewFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}

	/* WISH CREATE POST FRAGMENT */
	private void addWishCreatePostFragment(Fragment target, String postToEdit, int requestCode, boolean animate) {
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		if(animate)
			fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
					R.anim.slide_in_left, R.anim.slide_out_right);

		WishCreatePostFragment fragment = (WishCreatePostFragment) fragmentManager.findFragmentByTag(WishCreatePostFragment.LOG_TAG);
//		if (fragment == null) {
		fragment = WishCreatePostFragment.newInstance(postToEdit, null, null);
		FragmentsUtils.addFragmentNull(fragmentTransaction, R.id.pages_container, fragment,
				WishCreatePostFragment.LOG_TAG, target, requestCode);
//		} else
//			FragmentsUtils.addFragmentNotNull(fragmentTransaction, fragment, target, requestCode);
		fragmentTransaction.commit();
	}


	//region == Getters and setters ==

	public OnBackPressedListener getBackListener() {
		return backListener;
	}
	public void setBackListener(OnBackPressedListener backListener) {
		this.backListener = backListener;
	}

	//endregion


	//region == CreatePost Fragments' interfaces ==

	@Override
	public CreatePostHelper getCreatePostHelper() {
		WishCreatePostFragment fragment =
				(WishCreatePostFragment) getSupportFragmentManager().findFragmentByTag(WishCreatePostFragment.LOG_TAG);

		if (fragment != null && fragment.isVisible())
			return fragment.getCreatePostHelper();

		return null;
	}


	// GALLERY
	@Override
	public void setPostType(PostTypeEnum type) {
		if (getCreatePostHelper() != null)
			getCreatePostHelper().setPostType(type);
	}

	@Override
	public void setMediaCaptureType(HLMediaType type) {
		if (getCreatePostHelper() != null)
			getCreatePostHelper().setMediaCaptureType(type);
	}

	@Override
	public void checkPermissionForGallery(HLMediaType type) {
		if (getCreatePostHelper() != null)
			getCreatePostHelper().checkPermissionForGallery(type);
	}

	@Override
	public void onClickImage(String imageUri) {
		if (getCreatePostHelper() != null)
			getCreatePostHelper().onClickImage(imageUri);
	}

	@Override
	public void onClickVideo(String videoUri) {
		if (getCreatePostHelper() != null)
			getCreatePostHelper().onClickVideo(videoUri);
	}


	// AUDIO RECORDING
	@NonNull
	@Override
	public String getAudioMediaFileUri() {
		if (getCreatePostHelper() != null)
			return getCreatePostHelper().getAudioMediaFileUri();

		return "";
	}

	@Override
	public void exitFromRecordingAndSetAudioBackground() {
		if (getCreatePostHelper() != null)
			getCreatePostHelper().exitFromRecordingAndSetAudioBackground();
	}

	@Override
	public boolean isEditAudioPost() {
		return getCreatePostHelper() != null && getCreatePostHelper().isEditAudioPost();
	}

	@NonNull
	@Override
	public String getAudioUrl() {
		if (getCreatePostHelper() != null)
			return getCreatePostHelper().getAudioUrl();

		return "";
	}


	// TAG
	@Override
	public Object[] isObjectForTagSelected(String id, boolean fromInitiative) {
		if (getCreatePostHelper() != null)
			return getCreatePostHelper().isObjectForTagSelected(id, fromInitiative);

		return null;
	}

	@Override
	public void addTagToSearchList(Tag tag) {
		if (getCreatePostHelper() != null)
			getCreatePostHelper().addTagToSearchList(tag);
	}

	@Override
	public void updateSearchData(String query) {
		if (getCreatePostHelper() != null)
			getCreatePostHelper().updateSearchData(query);
	}

	//endregion

}
