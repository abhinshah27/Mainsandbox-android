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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;

import java.io.File;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.HLFragment;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.models.WishListElement;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.CustomGalleryAdapter;
import it.keybiz.lbsapp.corporate.utilities.media.HLMediaType;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * A simple {@link Fragment} subclass.
 * @author mbaldrighi on 3/9/2018.
 */
public class WishChooseCoverFragment extends HLFragment implements View.OnClickListener, LoaderManager.LoaderCallbacks<Cursor>,
		OnServerMessageReceivedListener, OnMissingConnectionListener, WishesActivity.OnNextClickListener,
		CustomGalleryAdapter.OnMediaClickListener, MediaHelper.MediaUploadListenerWithCallback.OnUploadCompleteListener {

	public static final String LOG_TAG = WishChooseCoverFragment.class.getCanonicalName();

	private RecyclerView galleryRecView;
	private LinearLayoutManager llm;
	private CustomGalleryAdapter galleryAdapter;

	private String mediaFileUri;
	private File file;

	private WishListElement selectedElement;


	public WishChooseCoverFragment() {
		// Required empty public constructor
	}

	public static WishChooseCoverFragment newInstance() {
		Bundle args = new Bundle();
		WishChooseCoverFragment fragment = new WishChooseCoverFragment();
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

		View view = inflater.inflate(R.layout.fragment_wish_choose_pic, container, false);
		configureLayout(view);

		return view;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		llm = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
		galleryAdapter = new CustomGalleryAdapter(getActivity(), this);

		if (getActivity() instanceof HLActivity)
			MediaHelper.checkPermissionForCustomGallery((HLActivity) getActivity(), this);

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

		AnalyticsUtils.trackScreen(getContext(), AnalyticsUtils.WISHES_COVER_PHOTO);

		wishesActivityListener.setNextAlwaysOn();
		wishesActivityListener.enableDisableNextButton(true);

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
			case R.id.take_pic:
				if (getActivity() instanceof HLActivity)
					mediaFileUri = MediaHelper.checkPermissionAndFireCameraIntent(((HLActivity) getActivity()),
							HLMediaType.PHOTO, this);
				break;
		}
	}

	@Override
	public void onNextClick() {
		if (file != null && file.exists())
			attemptFileUpload();
		else {
			wishesActivityListener.setSelectedWishListElement(selectedElement);
			wishesActivityListener.resumeNextNavigation();
		}
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (getActivity() == null || !(getActivity() instanceof HLActivity)) return;

		if (resultCode == Activity.RESULT_OK) {
			file = null;
			if (requestCode == Constants.RESULT_PHOTO ||
					requestCode == Constants.RESULT_VIDEO) {

				Uri u = Uri.parse(mediaFileUri);
				file = new File(u.getPath());
				if (file.exists()) {
					switch (requestCode) {
						case Constants.RESULT_PHOTO:
							LogUtils.d(LOG_TAG, "Media captured with type=PHOTO and path: " + file.getAbsolutePath()
									+ " and file exists=" + file.exists());
							break;
					}

//					MediaHelper.scanFile(getActivity(), file.getAbsolutePath());

					activityListener.showAlert(getString(R.string.media_upload_ready, file.getName()));
				}
				else {
					file = null;
					activityListener.showAlert(R.string.error_upload_media);
				}
			}
		}

	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (Utils.isContextValid(getActivity()) && getActivity() instanceof HLActivity) {
			HLActivity activity = ((HLActivity) getActivity());

			switch (requestCode) {
				case Constants.PERMISSIONS_REQUEST_READ_WRITE_CAMERA:
					if (grantResults.length > 0) {
						if (grantResults.length == 3 &&
								grantResults[0] == PackageManager.PERMISSION_GRANTED &&
								grantResults[1] == PackageManager.PERMISSION_GRANTED &&
								grantResults[2] == PackageManager.PERMISSION_GRANTED) {
							mediaFileUri = MediaHelper.checkPermissionAndFireCameraIntent(activity, HLMediaType.PHOTO, this);
						}
						else if (grantResults.length == 2 &&
								grantResults[0] == PackageManager.PERMISSION_GRANTED &&
								grantResults[1] == PackageManager.PERMISSION_GRANTED) {
							mediaFileUri = MediaHelper.checkPermissionAndFireCameraIntent(activity, HLMediaType.PHOTO, this);
						}
						else if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
							mediaFileUri = MediaHelper.checkPermissionAndFireCameraIntent(activity, HLMediaType.PHOTO, this);
					}
					break;

				case Constants.PERMISSIONS_REQUEST_GALLERY_CUSTOM:
					if (grantResults.length > 0) {
						if (grantResults.length == 1 &&
								grantResults[0] == PackageManager.PERMISSION_GRANTED) {
							MediaHelper.checkPermissionForCustomGallery((HLActivity) getActivity(), this);
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
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		switch (operationId) {

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
		view.findViewById(R.id.take_pic).setOnClickListener(this);
		galleryRecView = view.findViewById(R.id.custom_gallery_rv);
	}

	@Override
	protected void setLayout() {
		galleryRecView.setLayoutManager(llm);
		galleryRecView.setAdapter(galleryAdapter);
	}


	//region == Loader callbacks ==

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String[] projection = {
				MediaStore.Files.FileColumns._ID,
				MediaStore.Files.FileColumns.DATE_ADDED,
				MediaStore.Files.FileColumns.DATA,
				MediaStore.Files.FileColumns.MEDIA_TYPE
		};
		String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
		String sortOrder = String.format("%s limit 50 ", MediaStore.Files.FileColumns.DATE_ADDED +" DESC");

		if (getActivity() != null) {
			return new CursorLoader(
					getActivity(),
					MediaStore.Files.getContentUri("external"),
					projection,
					selection,
					null,
					sortOrder
			);
		}
		else return null;

	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		galleryAdapter.changeCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		galleryAdapter.changeCursor(null);
	}

	//endregion


	//region == Gallery interface ==

	@Override
	public void onClickImage(String imageUri) {
		file = null;
		file = new File(Uri.parse(imageUri).getPath());
		if (!file.exists()) {
			file = null;
			activityListener.showAlert(R.string.error_upload_media);
		}
		else
			activityListener.showAlert(getString(R.string.media_upload_ready, file.getName()));
	}

	@Override
	public void onClickVideo(String videoUri) {}

	//endregion


	//region == Upload media methods ==

	@Override
	public void onUploadComplete(String path, final String mediaLink) {
		if (Utils.isStringValid(mediaLink) && Utils.isContextValid(getActivity())) {
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Bundle bundle = new Bundle();
					bundle.putString("wishImageURL", mediaLink);
					wishesActivityListener.setDataBundle(bundle);

					wishesActivityListener.setSelectedWishListElement(selectedElement);
					wishesActivityListener.resumeNextNavigation();

				}
			});
		}
	}

	private void attemptFileUpload() {
		if (file != null && file.exists() && getActivity() instanceof HLActivity) {
			((HLActivity) getActivity()).openProgress();
			((HLActivity) getActivity()).setShowProgressAnyway(true);

			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					((HLActivity) getActivity()).setProgressMessage(getString(R.string.uploading_media));
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
											file.getAbsolutePath(), WishChooseCoverFragment.this);
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

	//endregion

}


