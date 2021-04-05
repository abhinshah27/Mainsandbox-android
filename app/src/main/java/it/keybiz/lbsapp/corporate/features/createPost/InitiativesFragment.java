/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.createPost;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.shawnlin.numberpicker.NumberPicker;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.HLFragment;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.features.tags.TagAdapter;
import it.keybiz.lbsapp.corporate.models.HLUserGeneric;
import it.keybiz.lbsapp.corporate.models.Initiative;
import it.keybiz.lbsapp.corporate.models.Interest;
import it.keybiz.lbsapp.corporate.models.Tag;
import it.keybiz.lbsapp.corporate.models.enums.InitiativeTypeEnum;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.helpers.SearchHelper;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnInitiativesFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link InitiativesFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class InitiativesFragment extends HLFragment implements SearchHelper.OnQuerySubmitted,
		OnServerMessageReceivedListener, OnMissingConnectionListener, View.OnClickListener {

	private boolean isEditMode = false;

	private enum CreationStep { BUTTONS, RECIPIENT, AMOUNT_DATE, CONFIRMATION }
	private CreationStep currentStep = CreationStep.BUTTONS;
	private boolean initializeToDate = false;

	private Initiative mInitiative;
	private InitiativeTypeEnum initiativeType;
	private Date initiativeDate;
	private long initiativeHearts;
	private Tag recipient;

	private View navigationProgress;
	private ViewGroup navigationStepBar;
	private TextView progressNextBtn, progressCancelBtn;
	private ValueAnimator alphaAnimation;

	private View layout1;
	private Button btnTransfer, btnSupport, btnCollect;

	private View layout2;
	private EditText searchBox;
	private View layoutInterests;
	private TextView textPeople, noResultPeople, noResultInterests;
	private RecyclerView recViewPeople, recViewInterests;
	private LinearLayoutManager llmPeople, llmInterests;
	private TagAdapter adapterPeople, adapterInterests;

	private List<HLUserGeneric> users = new ArrayList<>();
	private List<Interest> interests = new ArrayList<>();
	private List<Object> usersToShow = new ArrayList<>(),
			interestsToShow = new ArrayList<>();

	private SearchHelper mSearchHelper;

	private String query;

	private View layout3a, layout3b;
	private TextView amountDateTxt, expiration, availableHearts, dataDescription;
	private ViewGroup pickersContainer;

	private View layout4;

	private OnInitiativesFragmentInteractionListener mListener;


	public InitiativesFragment() {
		// Required empty public constructor
	}

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 * @return A new instance of fragment ProfileFragment.
	 */
	public static InitiativesFragment newInstance(boolean isEditMode, Initiative initiative) {
		InitiativesFragment fragment = new InitiativesFragment();
		Bundle args = new Bundle();
		args.putBoolean(Constants.EXTRA_PARAM_1, isEditMode);
		args.putSerializable(Constants.EXTRA_PARAM_2, initiative);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSearchHelper = new SearchHelper(this);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.cpost_fragment_initiatives, container, false);

		configureLayout(view);

		return view;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		llmPeople = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
		llmInterests = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);

		adapterPeople = new TagAdapter(usersToShow, mListener.getCreatePostHelper(), true);
		adapterPeople.setHasStableIds(true);
		adapterInterests = new TagAdapter(interestsToShow, mListener.getCreatePostHelper(), true);
		adapterInterests.setHasStableIds(true);
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();

		getData();

		setLayout();
	}

	@Override
	public void onPause() {
		super.onPause();

		Utils.closeKeyboard(searchBox);
	}


	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		if (context instanceof OnInitiativesFragmentInteractionListener) {
			mListener = (OnInitiativesFragmentInteractionListener) context;
		} else {
			throw new RuntimeException(context.toString()
					+ " must implement OnInitiativesFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}


	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btn_initiative_transfer:
				btnTransfer.setSelected(!btnTransfer.isSelected());
				initiativeType = btnTransfer.isSelected() ? InitiativeTypeEnum.TRANSFER_HEARTS : null;
				btnCollect.setSelected(false);
				btnSupport.setSelected(false);
				progressNextBtn.setEnabled(btnTransfer.isSelected());

				initiativeDate = null;      // resets opposite value
				break;

			case R.id.btn_initiative_collect:
				btnCollect.setSelected(!btnCollect.isSelected());
				initiativeType = btnCollect.isSelected() ? InitiativeTypeEnum.COLLECT_HEARTS : null;
				btnTransfer.setSelected(false);
				btnSupport.setSelected(false);
				progressNextBtn.setEnabled(btnCollect.isSelected());

				initiativeHearts = 0;      // resets opposite value
				break;

			case R.id.btn_initiative_support:
				btnSupport.setSelected(!btnSupport.isSelected());
				initiativeType = btnSupport.isSelected() ? InitiativeTypeEnum.GIVE_SUPPORT : null;
				btnTransfer.setSelected(false);
				btnCollect.setSelected(false);
				progressNextBtn.setEnabled(btnSupport.isSelected());

				initiativeHearts = 0;      // resets opposite value
				break;

			case R.id.wish_cancel_btn:
				navigateWizard(true);
				break;

			case R.id.wish_next_btn:
				navigateWizard(false);
				break;

			case R.id.initiative_enter_data_layout:
				openDialogOrPickers();
				break;

			case R.id.pickers_close_btn:
				Object[] objs = buildHeartsAmount();
				boolean success = (boolean) objs[0];
				String amount = (String) objs[1];
				if (success) {
					long i = Long.parseLong(amount);
					progressNextBtn.setEnabled(i <= mUser.getTotHeartsAvailable());
				}
				else progressNextBtn.setEnabled(false);

				amountDateTxt.setText(amount);
				animatePickersPanel(false);
				break;
		}
	}

	private void animatePickersPanel(boolean open) {
		if (layout3b != null) {
			int mediaPanelHeight = getResources().getDimensionPixelSize(R.dimen.create_post_media_panel_height);
			final ValueAnimator anim;
			if (open)
				anim = ValueAnimator.ofInt(0, mediaPanelHeight);
			else
				anim = ValueAnimator.ofInt(mediaPanelHeight, 0);
			anim.setDuration(300);
			anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {
					FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) layout3b.getLayoutParams();
					lp.height = (int) animation.getAnimatedValue();
					layout3b.setLayoutParams(lp);
				}
			});
			anim.addListener(new Animator.AnimatorListener() {
				@Override
				public void onAnimationStart(Animator animation) {
					if (open)
						layout3b.setVisibility(View.VISIBLE);
				}

				@Override
				public void onAnimationEnd(Animator animation) {
					if (!open)
						layout3b.setVisibility(View.GONE);
				}

				@Override
				public void onAnimationCancel(Animator animation) {}

				@Override
				public void onAnimationRepeat(Animator animation) {}
			});
			anim.start();
		}
	}

	private void openDialogOrPickers() {
		if (initiativeType == InitiativeTypeEnum.TRANSFER_HEARTS)
			animatePickersPanel(true);
		else
			openDatePicker();
	}

	private void openDatePicker() {
		if (Utils.isContextValid(getActivity())) {
			Calendar minDate = Calendar.getInstance();
			Calendar selectedDate = Calendar.getInstance();
			if (initiativeDate != null)
				selectedDate.setTime(initiativeDate);
			if (isEditMode && mInitiative != null) {
				minDate.setTime(mInitiative.getDateCreationObject());
				selectedDate.setTime(mInitiative.getDateUpUntilObject());
			}

			long datePlus60Days = minDate.getTimeInMillis() + (Constants.TIME_UNIT_DAY * 61);
			Calendar maxDate = Calendar.getInstance();
			maxDate.setTimeInMillis(datePlus60Days);

			DatePickerDialog dpd = DatePickerDialog.newInstance(
					new DatePickerDialog.OnDateSetListener() {
						@Override
						public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
							Calendar cal = Calendar.getInstance();
							cal.set(year, monthOfYear, dayOfMonth);
							initiativeDate = cal.getTime();
							amountDateTxt.setText(Utils.formatDate(initiativeDate, "MMMM dd, yyyy"));
							progressNextBtn.setEnabled(true);
						}
					},
					selectedDate.get(Calendar.YEAR),
					selectedDate.get(Calendar.MONTH),
					selectedDate.get(Calendar.DAY_OF_MONTH)
			);
			dpd.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					if (initiativeDate == null) {
						amountDateTxt.setText(getResources().getString(R.string.initiative_date_hint));
						progressNextBtn.setEnabled(false);
					}
					else {
						SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
						amountDateTxt.setText(sdf.format(initiativeDate));
						progressNextBtn.setEnabled(true);
					}
				}
			});
			dpd.setVersion(DatePickerDialog.Version.VERSION_2);
			dpd.setAccentColor(Utils.getColor(getContext(), R.color.colorAccent));
			dpd.setTitle(getString(initiativeType == InitiativeTypeEnum.GIVE_SUPPORT ?
					R.string.initiative_date_support : R.string.initiative_date_collect));
			dpd.setMinDate(minDate);
			dpd.setMaxDate(maxDate);
			dpd.show(getActivity().getSupportFragmentManager(), "DatePickerDialog");
		}
	}

	private void navigateWizard(boolean back) {
		switch (currentStep) {
			case BUTTONS:
				currentStep = CreationStep.RECIPIENT;
				progressCancelBtn.setVisibility(View.VISIBLE);
				progressNextBtn.setEnabled(mListener.getHelperObject().haveTagsInitiativeRecipient());

				if (mInitiative == null)
					mInitiative = new Initiative();
				mInitiative.setType(initiativeType.toString());

				layout1.setVisibility(View.GONE);
				layout2.setVisibility(View.VISIBLE);
				layout3a.setVisibility(View.GONE);
				layout3b.setVisibility(View.GONE);
				layout4.setVisibility(View.GONE);

				animate(1, false);
				break;

			case RECIPIENT:
				currentStep = back ? CreationStep.BUTTONS : CreationStep.AMOUNT_DATE;
				progressNextBtn.setEnabled(back);
				progressNextBtn.setText(back ? R.string.action_next : R.string.action_create);
				progressCancelBtn.setVisibility(back ? View.GONE : View.VISIBLE);

				handleAmountDateStep(back);
				break;

			case AMOUNT_DATE:
				currentStep = back ? CreationStep.RECIPIENT : CreationStep.CONFIRMATION;

				if (!back) {
					if (initiativeType == InitiativeTypeEnum.TRANSFER_HEARTS && initiativeHearts > 0)
						mInitiative.setHeartsToTransfer(initiativeHearts);
					else if (initiativeDate != null) {
						mInitiative.setDateUpUntilObject(initiativeDate);
						mInitiative.setDateUpUntil(Utils.formatDateForDB(initiativeDate));
					}

					String label = mInitiative.getText();
					if (!Utils.isStringValid(label) && recipient != null) {
						switch (initiativeType) {
							case TRANSFER_HEARTS:
								label = getString(
										R.string.label_initiative_transfer,
										Utils.getReadableCount(
												getResources(),
												(int) initiativeHearts
										),
										recipient.getUserName()
								);
								break;
							case GIVE_SUPPORT:
							case COLLECT_HEARTS:
								label = getString(
										initiativeType == InitiativeTypeEnum.GIVE_SUPPORT ?
												R.string.label_initiative_support : R.string.label_initiative_collect,
										recipient.getUserName()
								);
								break;
						}

						if (initializeToDate)
							progressCancelBtn.setVisibility(View.VISIBLE);
					}

					mInitiative.setText(label);
					mListener.showHideInitiativeLabel(recipient != null, label);
					mListener.updateVisibilityForInitiative();
					mListener.attachInitiativeToPost(mInitiative);
				}

				progressNextBtn.setText(back ? R.string.action_next : R.string.done);
				progressNextBtn.setEnabled(true);

				layout1.setVisibility(View.GONE);
				layout2.setVisibility(back ? View.VISIBLE : View.GONE);
				layout3a.setVisibility(View.GONE);
				layout3b.setVisibility(View.GONE);
				layout4.setVisibility(back ? View.GONE : View.VISIBLE);

				navigationStepBar.setVisibility(back ? View.VISIBLE : View.GONE);

				if (back)
					animate(1, true);
				break;

			case CONFIRMATION:
				if (back) {
					currentStep = CreationStep.AMOUNT_DATE;
					progressNextBtn.setText(R.string.action_create);

					if (initializeToDate)
						progressCancelBtn.setVisibility(View.GONE);

					layout1.setVisibility(View.GONE);
					layout2.setVisibility(View.GONE);
					layout3a.setVisibility(View.VISIBLE);
					layout3b.setVisibility(View.GONE);
					layout4.setVisibility(View.GONE);

					navigationStepBar.setVisibility(View.VISIBLE);
					animate(2, true);
				}
				else
					mListener.getHelperObject().animateMediaPanel(false);
				break;
		}
	}

	private void handleAmountDateStep(boolean back) {
		if (!back) {
			mInitiative.setRecipient(recipient.getUserId());

			String res = getResources().getString(R.string.initiative_amount_txt),
					res2 = getResources().getString(R.string.initiative_amount_hint);
			switch (initiativeType) {
				case TRANSFER_HEARTS:
					configurePickers();
					break;
				case GIVE_SUPPORT:
					res = getResources().getString(R.string.initiative_date_support);
					res2 = initiativeDate != null ?
							Utils.formatDate(initiativeDate, "MMMM dd, yyyy") : getResources().getString(R.string.initiative_date_hint);
					availableHearts.setVisibility(View.GONE);
					break;
				case COLLECT_HEARTS:
					res = getResources().getString(R.string.initiative_date_collect);
					res2 = initiativeDate != null ?
							Utils.formatDate(initiativeDate, "MMMM dd, yyyy") : getResources().getString(R.string.initiative_date_hint);
					availableHearts.setVisibility(View.GONE);
					break;
			}
			dataDescription.setText(res);
			amountDateTxt.setText(res2);
		}

		layout1.setVisibility(back ? View.VISIBLE : View.GONE);
		layout2.setVisibility(View.GONE);
		layout3a.setVisibility(back ? View.GONE : View.VISIBLE);
		layout3b.setVisibility(View.GONE);
		layout4.setVisibility(View.GONE);

		animate(back ? 0 : 2, back);
	}

	private void configurePickers() {
		int availHearts = mUser.getTotHeartsAvailable();

		if (pickersContainer != null && availHearts > 0) {
			pickersContainer.removeAllViews();

			char[] chars = String.valueOf(availHearts).toCharArray();
			if (chars.length > 0) {
				for (int i = 0; i < chars.length; i++) {
					char c = chars[i];
					int digit = c - '0';

					View view = LayoutInflater.from(getContext()).inflate(R.layout.cpost_initiative_hearts_picker, pickersContainer, false);
					if (view instanceof NumberPicker) {
						NumberPicker picker = ((NumberPicker) view);
						picker.setId(String.valueOf(i).hashCode());
						picker.setValue(digit);

						if (i == 0)
							picker.setMaxValue(digit);

						pickersContainer.addView(view);
					}
				}
			}
		}
	}

	private Object[] buildHeartsAmount() {
		StringBuilder sb;
		if (pickersContainer != null && pickersContainer.getChildCount() > 0) {
			sb = new StringBuilder();
			for (int i = 0; i < pickersContainer.getChildCount(); i++) {
				int hash = String.valueOf(i).hashCode();
				View picker = pickersContainer.getChildAt(i);
				if (picker.getId() == hash && picker instanceof NumberPicker)
					sb.append(((NumberPicker) picker).getValue());
			}

			initiativeHearts = Long.parseLong(sb.toString());

			return new Object[] { initiativeHearts > 0, Utils.formatNumberWithCommas(initiativeHearts) };
		}

		return new Object[] { false, getString(R.string.initiative_amount_hint) };
	}


	/**
	 * This interface must be implemented by activities that contain this
	 * fragment to allow an interaction in this fragment to be communicated
	 * to the activity and potentially other fragments contained in that
	 * activity.
	 * <p>
	 * See the Android Training lesson <a href=
	 * "http://developer.android.com/training/basics/fragments/communicating.html"
	 * >Communicating with Other Fragments</a> for more information.
	 */
	public interface OnInitiativesFragmentInteractionListener extends TagFragment.OnTagFragmentInteractionListener {
		void attachInitiativeToPost(Initiative initiative);
		CreatePostHelper getHelperObject();
		void showHideInitiativeLabel(boolean show, @Nullable String text);
		void updateVisibilityForInitiative();
	}

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		setData(responseObject, operationId);
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);
	}

	@Override
	public void onMissingConnection(int operationId) {}


	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_1))
				isEditMode = savedInstanceState.getBoolean(Constants.EXTRA_PARAM_1, false);
			if (savedInstanceState.containsKey(Constants.EXTRA_PARAM_2)) {
				mInitiative = (Initiative) savedInstanceState.getSerializable(Constants.EXTRA_PARAM_2);
				if (mInitiative != null) {
					initiativeType = mInitiative.getTypeEnum();
					recipient = new Tag(mInitiative.getRecipient(), "", "", false);
					if (initiativeType == InitiativeTypeEnum.TRANSFER_HEARTS)
						initiativeHearts = mInitiative.getHeartsToTransfer();
					else if (initiativeType != null)
						initiativeDate = mInitiative.getDateUpUntilObject();
				}

				currentStep = CreationStep.BUTTONS;
				if (isEditMode && mInitiative != null) {
					if (initiativeType != InitiativeTypeEnum.TRANSFER_HEARTS) {
						currentStep = CreationStep.AMOUNT_DATE;
						initializeToDate = true;
					}
				}
			}
		}

	}

	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	protected void configureLayout(@NonNull View view) {

		configureNavigationProgress(view);

		layout1 = view.findViewById(R.id.layout_initiative_1);
		layout2 = view.findViewById(R.id.layout_initiative_2);
		layout3a = view.findViewById(R.id.layout_initiative_3a);
		layout3b = view.findViewById(R.id.layout_initiative_3b);
		layout4 = view.findViewById(R.id.layout_initiative_4);

		// LAYOUT 1
		btnTransfer = view.findViewById(R.id.btn_initiative_transfer);
		btnSupport = view.findViewById(R.id.btn_initiative_support);
		btnCollect = view.findViewById(R.id.btn_initiative_collect);
		btnTransfer.setOnClickListener(this);
		btnCollect.setOnClickListener(this);
		btnSupport.setOnClickListener(this);

		// LAYOUT 2
		View searchLayout = view.findViewById(R.id.search_box);
		searchBox = searchLayout.findViewById(R.id.search_field);
//		searchBox.addTextChangedListener(new SearchTextWatcher(this,
//				SearchTextWatcher.SearchType.SINGLE_CHAR));
		searchBox.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					adapterPeople.notifyDataSetChanged();
					adapterInterests.notifyDataSetChanged();
				}
			}
		});
		searchBox.setHint(R.string.cpost_initiative_recipient_hint);

		if (mSearchHelper == null)
			mSearchHelper = new SearchHelper(this);
		mSearchHelper.configureViews(searchLayout, searchBox);

		layoutInterests = view.findViewById(R.id.layout_list_interests);

		textPeople = view.findViewById(R.id.tag_people_text);
		noResultPeople = view.findViewById(R.id.no_result_tag_people);
		noResultInterests = view.findViewById(R.id.no_result_tag_interests);

		recViewPeople = view.findViewById(R.id.tag_rv_people);
		recViewInterests = view.findViewById(R.id.tag_rv_interests);

		// LAYOUT 3
		layout3a.findViewById(R.id.initiative_enter_data_layout).setOnClickListener(this);
		amountDateTxt = view.findViewById(R.id.amount_date);
		expiration = view.findViewById(R.id.expiration);
		availableHearts = view.findViewById(R.id.count_heart_available);
		dataDescription = view.findViewById(R.id.enter_text_description);

		pickersContainer = view.findViewById(R.id.initiative_pickers_container);
		layout3b.findViewById(R.id.pickers_close_btn).setOnClickListener(this);
	}

	private void configureNavigationProgress(View view) {
		if (view != null) {
			navigationProgress = view.findViewById(R.id.navigation_progress_bar);

			navigationProgress.findViewById(R.id.wish_progress_bar).setVisibility(View.GONE);

			progressCancelBtn = navigationProgress.findViewById(R.id.wish_cancel_btn);
			progressCancelBtn.setOnClickListener(this);
			progressCancelBtn.setText(R.string.action_back);
			progressCancelBtn.setTextColor(Utils.getColor(getContext(), R.color.black_26));
			progressCancelBtn.setVisibility(View.GONE);

			progressNextBtn = navigationProgress.findViewById(R.id.wish_next_btn);
			progressNextBtn.setOnClickListener(this);
			progressNextBtn.setEnabled(false);

			navigationStepBar = navigationProgress.findViewById(R.id.initiative_progress_bar);
			navigationStepBar.setVisibility(View.VISIBLE);

			TypedValue outValue = new TypedValue();
			getResources().getValue(R.dimen.btn_profile_weight_me_show, outValue, true);
			float value = outValue.getFloat();
			for (int i = 0; i < 3; i++) {
				View step = LayoutInflater
						.from(getContext())
						.inflate(R.layout.item_progress_step_initiative, navigationStepBar, false);
				((LinearLayout.LayoutParams) step.getLayoutParams()).weight = value;
				navigationStepBar.addView(step);
			}

			animate(currentStep == CreationStep.AMOUNT_DATE ? 2 : 0, false);
		}
	}

	private void animate(int stepIndex, boolean back) {
		endAlphaAnimation();
		if (navigationStepBar != null && navigationStepBar.getChildCount() > 0) {
			for (int i = 0; i < navigationStepBar.getChildCount(); i++) {
				View stepView = navigationStepBar.getChildAt(i);
				if (i < stepIndex)
					stepView.setSelected(true);
				else if (i == stepIndex) {
					stepView.setSelected(true);
					animate(stepView);
				}
				else if (back)
					stepView.setSelected(false);
			}
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

	@Override
	protected void setLayout() {
		recViewPeople.setLayoutManager(llmPeople);
		recViewPeople.setAdapter(adapterPeople);
		recViewInterests.setLayoutManager(llmInterests);
		recViewInterests.setAdapter(adapterInterests);

		layoutInterests.setVisibility(mUser.isActingAsInterest() ? View.GONE : View.VISIBLE);

		if (Utils.isStringValid(query))
			searchBox.setText(query);

		if (mUser.isActingAsInterest()) {
			layoutInterests.setVisibility(View.GONE);
			noResultPeople.setText(R.string.no_followers_for_interest_short);
			textPeople.setText(R.string.tag_section_followers);
		}
		else {
			layoutInterests.setVisibility(View.VISIBLE);
			noResultPeople.setText(R.string.no_people_in_ic_short);
			textPeople.setText(R.string.tag_section_people);
		}

		// expiration no longer handled. better maxDate in dialog
		expiration.setVisibility(View.GONE);
		availableHearts.setText(
				getString(
						R.string.settings_redeem_available_hearts,
						Utils.formatNumberWithCommas(mUser.getTotHeartsAvailable())
				)
		);

		btnTransfer.setEnabled(mUser.hasAvailableHearts());
		btnSupport.setEnabled(!mUser.hasActiveGiveSupportInitiative());

		if (initializeToDate) {
			progressCancelBtn.setVisibility(View.GONE);
			progressNextBtn.setText(R.string.action_create);
			handleAmountDateStep(false);
		}
	}

	@Override
	public void onQueryReceived(String query) {
		this.query = query;

		mListener.updateSearchData(query);
	}


	private void getData() {
		if (mUser.isActingAsInterest())
			callServer(CallType.FOLLOWERS);
		else {
			callServer(CallType.USERS);
			callServer(CallType.INTERESTS);
		}

	}

	private enum CallType { USERS, INTERESTS, FOLLOWERS }
	private void callServer(CallType type) {
		Object[] result = null;

		try {
			if (type == CallType.USERS)
				result = HLServerCalls.getInnerCircle(mUser.getUserId(), 1);
			else if (type == CallType.INTERESTS)
				result = HLServerCalls.getMyInterests(mUser.getUserId());
			else if (type == CallType.FOLLOWERS)
				result = HLServerCalls.getFollowers(mUser.getId(), mUser.getId(), 1);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (getActivity() instanceof HLActivity)
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, ((HLActivity) getActivity()), result);
	}


	private void setData(JSONArray response, int operationId) {
		switch (operationId) {
			case Constants.SERVER_OP_GET_CIRCLE:
			case Constants.SERVER_OP_GET_FOLLOWERS:
				JSONArray jUsers = new JSONArray();
				JSONObject object0 = response.optJSONObject(0);
				if (operationId == Constants.SERVER_OP_GET_FOLLOWERS) {
					if (object0 != null && object0.length() > 0) {
						jUsers = object0.optJSONArray("items");
					}
				}
				else {
					if (object0 != null) {
						JSONArray lists = object0.optJSONArray("lists");
						if (lists != null && lists.length() > 0) {
							JSONObject list = lists.optJSONObject(0);
							if (list != null && list.length() > 0) {
								jUsers = list.optJSONArray("users");
							}
						}
					}
				}

				if (jUsers == null || jUsers.length() == 0) {
					recViewPeople.setVisibility(View.GONE);
					noResultPeople.setVisibility(View.VISIBLE);
					return;
				}

				recViewPeople.setVisibility(View.VISIBLE);
				noResultPeople.setVisibility(View.GONE);

				if (usersToShow == null)
					usersToShow = new ArrayList<>();
				else
					usersToShow.clear();
				if (users == null)
					users = new ArrayList<>();
				else
					users.clear();

				if (jUsers.length() > 0) {
					for (int i = 0; i < jUsers.length(); i++) {
						JSONObject jUser = jUsers.optJSONObject(i);
						if (jUser != null) {
							HLUserGeneric user = new HLUserGeneric().deserializeToClass(jUser);
							if (user != null) {
								users.add(user);

								mListener.addTagToSearchList(Tag.convertFromGenericUser(user));
							}
						}
					}
				}

				Collections.sort(users);
				usersToShow.addAll(users);
				adapterPeople.notifyDataSetChanged();
				break;


			case Constants.SERVER_OP_GET_MY_INTERESTS:
				if (response == null || response.length() == 0) {
					recViewInterests.setVisibility(View.GONE);
					noResultInterests.setVisibility(View.VISIBLE);
					return;
				}

				recViewInterests.setVisibility(View.VISIBLE);
				noResultInterests.setVisibility(View.GONE);

				if (interestsToShow == null)
					interestsToShow = new ArrayList<>();
				else
					interestsToShow.clear();
				if (this.interests == null)
					this.interests = new ArrayList<>();
				else
					this.interests.clear();

				for (int i = 0; i < response.length(); i++) {
					JSONObject jInterest = response.optJSONObject(i);
					if (jInterest != null) {
						Interest interest = new Interest().deserializeToClass(jInterest);
						if (interest != null) {
							interests.add(interest);

							mListener.addTagToSearchList(Tag.convertFromInterest(interest));
						}
					}
				}

				Collections.sort(interests);
				interestsToShow.addAll(interests);
				adapterInterests.notifyDataSetChanged();
				break;
		}
	}

	public void setInitiativeRecipient(@Nullable Tag recipient) {
		this.recipient = recipient;

		progressNextBtn.setEnabled(recipient != null && Utils.isStringValid(recipient.getUserId()));
	}


	//region == Getters and setters ==

	public TagAdapter getAdapterPeople() {
		return adapterPeople;
	}

	public TagAdapter getAdapterInterests() {
		return adapterInterests;
	}

	//endregion
}
