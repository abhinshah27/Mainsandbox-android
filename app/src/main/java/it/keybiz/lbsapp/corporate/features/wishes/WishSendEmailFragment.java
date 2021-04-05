/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.wishes;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.HLFragment;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.models.HLUser;
import it.keybiz.lbsapp.corporate.models.WishEmail;
import it.keybiz.lbsapp.corporate.models.WishListElement;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.RealmUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.GlideApp;
import it.keybiz.lbsapp.corporate.utilities.media.HLMediaType;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * A simple {@link Fragment} subclass.
 */
public class WishSendEmailFragment extends HLFragment implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener, WishesActivity.OnNextClickListener,
		MediaHelper.MediaUploadListenerWithCallback.OnUploadCompleteListener {

	public static final String LOG_TAG = WishSendEmailFragment.class.getCanonicalName();

	private EditText emailToEt;
	private EditText emailSubjectEt;
	private EditText emailMessageEt;

	private GridLayout viewAttachment;
	private View addProfilePic;

	private List<File> attachmentFiles;

	private long totalSize;

	private ImageView profilePicture;

	private WishEmail newEmail;
	private int uploadIndex;

	private WishListElement selectedElement;


	public WishSendEmailFragment() {
		// Required empty public constructor
	}

	public static WishSendEmailFragment newInstance() {

		Bundle args = new Bundle();
		WishSendEmailFragment fragment = new WishSendEmailFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		onRestoreInstanceState(savedInstanceState != null ? savedInstanceState : getArguments());

		View view = inflater.inflate(R.layout.fragment_wishes_send_email, container, false);
		configureLayout(view);

		return view;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		wishesActivityListener.setOnNextClickListener(this);
	}

	@Override
	public void onStart() {
		super.onStart();

		configureResponseReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.WISHES_SEND_MAIL);

		wishesActivityListener.callServer(WishesActivity.CallType.ELEMENTS, false);
		wishesActivityListener.handleSteps();

		setLayout();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public void onDetach() {
		super.onDetach();

		if (getActivity() instanceof WishesActivity)
			((WishesActivity) getActivity()).setBackListener(null);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {

		}
	}


	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.send_email_add_picture:
				v.setSelected(!v.isSelected());

				if (viewAttachment != null && Utils.isContextValid(getContext())) {
					if (v.isSelected()) {
						profilePicture = (ImageView) LayoutInflater.from(getContext())
								.inflate(R.layout.custom_layout_profile_picture, viewAttachment, false);
						if (profilePicture != null) {
							profilePicture.getLayoutParams().width = Utils.dpToPx(40f, getResources());
							profilePicture.getLayoutParams().height = Utils.dpToPx(40f, getResources());


							MediaHelper.loadProfilePictureWithPlaceholder(getContext(), mUser.getAvatarURL(), profilePicture);

							viewAttachment.addView(profilePicture, 0);

							GridLayout.LayoutParams lp = (GridLayout.LayoutParams) profilePicture.getLayoutParams();
							lp.setGravity(Gravity.CENTER);
							profilePicture.setLayoutParams(lp);
						}
					}
					else if (viewAttachment.getChildCount() > 0)
						viewAttachment.removeViewAt(0);
				}
				break;

			case R.id.send_email_attach:
				if (getActivity() instanceof HLActivity)
					MediaHelper.checkPermissionForGallery((HLActivity) getActivity(), HLMediaType.PHOTO, this);
				break;
		}
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (getActivity() == null || !(getActivity() instanceof HLActivity)) return;

		if (requestCode == Constants.RESULT_GALLERY && resultCode == Activity.RESULT_OK) {
			if (data != null && data.getData() != null) {
				File file = checkFileSize(data.getData());
				if (file != null && file.exists()) {

					if (attachmentFiles == null)
						attachmentFiles = new ArrayList<>();

					attachmentFiles.add(file);

					addAttachment(file);
				}
				else
					((HLActivity) getActivity()).showAlert(R.string.error_wish_attach);
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (Utils.isContextValid(getActivity()) && getActivity() instanceof HLActivity) {
			HLActivity activity = ((HLActivity) getActivity());

			switch (requestCode) {
				case Constants.PERMISSIONS_REQUEST_GALLERY:
					if (grantResults.length > 0) {
						if (grantResults.length == 1 &&
								grantResults[0] == PackageManager.PERMISSION_GRANTED) {
							MediaHelper.checkPermissionForGallery(activity, HLMediaType.PHOTO, this);
						}
					}
					break;
			}
		}
	}


	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		if (responseObject == null || responseObject.length() == 0)
			return;

		switch (operationId) {
			case Constants.SERVER_OP_WISH_GET_LIST_ELEMENTS:
				JSONArray json = responseObject.optJSONObject(0).optJSONArray("items");
				if (json != null && json.length() == 1) {
					selectedElement = new WishListElement().deserializeToClass(json.optJSONObject(0));
				}

				wishesActivityListener.setStepTitle(responseObject.optJSONObject(0).optString("title"));
				wishesActivityListener.setStepSubTitle(responseObject.optJSONObject(0).optString("subTitle"));
				break;

			case Constants.SERVER_OP_WISH_SAVE_EMAIL:
				String id = responseObject.optJSONObject(0).optString("_id");
				Bundle dataBundle = new Bundle();
				dataBundle.putString("emailID", id);
				wishesActivityListener.setDataBundle(dataBundle);

				wishesActivityListener.setSelectedWishListElement(selectedElement);
				wishesActivityListener.resumeNextNavigation();
				break;
		}

	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		switch (operationId) {
			case Constants.SERVER_OP_WISH_GET_LIST_ELEMENTS:
				activityListener.showGenericError();
				break;
		}
	}

	@Override
	public void onMissingConnection(int operationId) {
		activityListener.closeProgress();
	}


	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}


	@Override
	protected void configureLayout(@NonNull View view) {
		emailToEt = view.findViewById(R.id.email_to);
		emailToEt.addTextChangedListener(new EmailTextWatcher(EditFields.RECIPIENT));
		emailSubjectEt = view.findViewById(R.id.email_subject);
		emailSubjectEt.addTextChangedListener(new EmailTextWatcher(EditFields.SUBJECT));
		emailMessageEt = view.findViewById(R.id.email_message);
		emailMessageEt.addTextChangedListener(new EmailTextWatcher(EditFields.MESSAGE));

		view.findViewById(R.id.send_email_attach).setOnClickListener(this);

		addProfilePic = view.findViewById(R.id.send_email_add_picture);
		addProfilePic.setOnClickListener(this);

		viewAttachment = view.findViewById(R.id.send_email_attached_file);
//		viewAttachmentInfo = view.findViewById(R.id.attachment);
	}

	@Override
	protected void setLayout() {
		viewAttachment.removeAllViews();

		boolean hasUris = attachmentFiles != null && !attachmentFiles.isEmpty();
		if (hasUris) {
			for (File file : attachmentFiles)
				addAttachment(file);
		}

		if (viewAttachment != null && profilePicture != null) {
			if (addProfilePic.isSelected())
				viewAttachment.addView(profilePicture, 0);
			else if (viewAttachment.getChildCount() > 0 && viewAttachment.getChildAt(0) == profilePicture)
				viewAttachment.removeViewAt(0);
		}

		wishesActivityListener.enableDisableNextButton(
				Utils.isEmailValid(emailToEt.getText().toString()) &&
						Utils.areStringsValid(
								emailSubjectEt.getText().toString(),
								emailMessageEt.getText().toString()
						)
		);
	}


	private File checkFileSize(Uri uri) {
		if (Utils.isContextValid(getActivity()) && uri != null) {
			Cursor returnCursor =
					getActivity().getContentResolver().query(uri, null, null, null, null);

            /*
             * Get the column indexes of the data in the Cursor,
             * move to the first row in the Cursor, get the data,
             * and display it.
             */
			if (returnCursor != null) {
				int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
				returnCursor.moveToFirst();

				totalSize += returnCursor.getLong(sizeIndex);
				returnCursor.close();

				try {
					if (((int) (totalSize / Constants.ONE_MB_IN_BYTES)) < 5) {
						InputStream input = getActivity().getContentResolver().openInputStream(uri);
						String mime = getActivity().getContentResolver().getType(uri);
						return MediaHelper.getFileFromContentUri(input, mime, (HLActivity) getActivity(), false);
					}
					else
						return null;
				}
				catch (FileNotFoundException e) {
					LogUtils.e(LOG_TAG, e.getMessage(), e);

					return null;
				}
			}
		}

		return null;
	}

	private void addAttachment(File file) {
		if (Utils.isContextValid(getContext())) {
			View view = LayoutInflater.from(getContext())
					.inflate(R.layout.item_wish_attachment, viewAttachment, false);
			if (view != null) {
				view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						File f = (File) v.getTag();
						if (attachmentFiles != null)
							attachmentFiles.remove(f);
						viewAttachment.removeView(v);
					}
				});

				final ImageView image = view.findViewById(R.id.attached_image);
				GlideApp.with(getContext()).asDrawable().load(file).into(new SimpleTarget<Drawable>() {
					@Override
					public void onResourceReady(Drawable resource, Transition<? super Drawable> transition) {
						image.setImageDrawable(resource);
					}
				});

				view.setTag(file);

				viewAttachment.addView(view);
			}
		}
	}

	@Override
	public void onNextClick() {
		String recipient = emailToEt.getText().toString(),
				subject = emailSubjectEt.getText().toString(),
				message = emailMessageEt.getText().toString();

		if (Utils.areStringsValid(recipient, subject, message) && Utils.isEmailValid(recipient)) {
			newEmail = null;
			newEmail = new WishEmail(mUser.getEmail(), recipient, subject, message);

			newEmail.setProfilePictureAttached(addProfilePic.isSelected());
			// includes profile picture among attachments if wanted
			if (newEmail.getProfilePictureAttached()) {
				if (newEmail.getAttachments() == null)
					newEmail.setAttachments(new ArrayList<>());
				newEmail.getAttachments().add(mUser.getAvatarURL());
			}

			uploadIndex = 0;

			if (attachmentFiles != null && !attachmentFiles.isEmpty()) {
				int size = attachmentFiles.size();
				attemptAttachmentsUpload(attachmentFiles.get(uploadIndex), uploadIndex, size);
			}
			else callSaveEmail();
		}
	}

	@Override
	public void onUploadComplete(String path, String mediaLink) {

		if (newEmail == null || !(getActivity() instanceof HLActivity)) return;

		if (newEmail.getAttachments() == null)
			newEmail.setAttachments(new ArrayList<String>());

		newEmail.getAttachments().add(mediaLink);

		int size = !newEmail.getProfilePictureAttached() ? newEmail.getAttachments().size() : (newEmail.getAttachments().size() - 1);

		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (attachmentFiles.size() == size) {
					((HLActivity) getActivity()).setProgressMessage(R.string.wish_saving_email);
					((HLActivity) getActivity()).openProgress();
					callSaveEmail();
				}
				else {
					attemptAttachmentsUpload(attachmentFiles.get(++uploadIndex), uploadIndex,
							attachmentFiles.size());
				}
			}
		});
	}

	private void callSaveEmail() {
		Object[] result = null;
		Realm realm = null;
		try {
			realm = RealmUtils.getCheckedRealm();
			String id = new HLUser().readUser(realm).getId();
			result = HLServerCalls.saveEmailForWish(id, newEmail);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		finally {
			RealmUtils.closeRealm(realm);
		}

		if (getActivity() instanceof HLActivity)
			HLRequestTracker.getInstance(((LBSLinkApp) getActivity().getApplication()))
					.handleCallResult(this, (HLActivity) getActivity(), result);
	}

	private void attemptAttachmentsUpload(final File file, final int index, final int size) {
		if (file != null && file.exists() && getActivity() instanceof HLActivity) {
			((HLActivity) getActivity()).openProgress();
			((HLActivity) getActivity()).setShowProgressAnyway(true);

			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					((HLActivity) getActivity()).setProgressMessage(getString(R.string.uploading_attachment_of, index + 1, size));
				}
			}, 500);

			final MediaHelper.UploadService service = new MediaHelper.UploadService(((HLActivity) getActivity()));
			try {
				MediaHelper.MediaUploadListener listener = new MediaHelper.MediaUploadListenerWithCallback(((HLActivity) getActivity()),
						file.getAbsolutePath(), this);

				service.uploadMedia(getActivity(), file, mUser.getUserId(), listener);
			}
			catch (IllegalArgumentException e) {
				LogUtils.e(LOG_TAG, e.getMessage(), e);
				((HLActivity) getActivity()).showAlertWithRetry(R.string.error_upload_media, new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						((HLActivity) getActivity()).openProgress();
						try {
							MediaHelper.MediaUploadListener listener =
									new MediaHelper.MediaUploadListenerWithCallback(((HLActivity) getActivity()),
											file.getAbsolutePath(), WishSendEmailFragment.this);
							service.uploadMedia(getActivity(), file, mUser.getUserId(), listener);
						} catch (IllegalArgumentException e1) {
							LogUtils.e(LOG_TAG, e.getMessage() + "\n\n2nd time in a row!", e);
							((HLActivity) getActivity()).showAlert(R.string.error_upload_media);
						}
					}
				});

				if (getActivity() instanceof HLActivity)
					((HLActivity) getActivity()).closeProgress();
			}
		}


	}


	//region == Custom inner classes ==

	enum EditFields { RECIPIENT, SUBJECT, MESSAGE }
	class EmailTextWatcher implements TextWatcher {

		private EditFields field;

		EmailTextWatcher(EditFields field) {
			this.field = field;
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {}

		@Override
		public void afterTextChanged(Editable s) {
			if (field != null) {
				boolean enable;
				switch (field) {
					case RECIPIENT:
						enable = Utils.isEmailValid(s.toString()) &&
								Utils.areStringsValid(emailSubjectEt.getText().toString(), emailMessageEt.getText().toString());
						break;

					case SUBJECT:
						enable = Utils.isEmailValid(emailToEt.getText().toString()) &&
								Utils.areStringsValid(s.toString(), emailMessageEt.getText().toString());
						break;

					case MESSAGE:
						enable = Utils.isEmailValid(emailToEt.getText().toString()) &&
								Utils.areStringsValid(emailSubjectEt.getText().toString(), s.toString());
						break;

					default:
						enable = false;
				}

				wishesActivityListener.enableDisableNextButton(enable);
			}
		}
	}

	//endregion


}
