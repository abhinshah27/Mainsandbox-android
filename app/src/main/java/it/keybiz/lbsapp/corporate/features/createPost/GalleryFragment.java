/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.createPost;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.HLFragment;
import it.keybiz.lbsapp.corporate.models.enums.PostTypeEnum;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.CustomGalleryAdapter;
import it.keybiz.lbsapp.corporate.utilities.media.HLMediaType;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link GalleryFragment.OnGalleryFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link GalleryFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GalleryFragment extends HLFragment implements View.OnClickListener,
		LoaderManager.LoaderCallbacks<Cursor> {

	public static final String LOG_TAG = GalleryFragment.class.getCanonicalName();

	private RecyclerView galleryRecView;
	private LinearLayoutManager llm;
	private CustomGalleryAdapter galleryAdapter;

	private OnGalleryFragmentInteractionListener mListener;

	public GalleryFragment() {
		// Required empty public constructor
	}

	public static GalleryFragment newInstance() {
		GalleryFragment fragment = new GalleryFragment();
		Bundle args = new Bundle();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View v =  inflater.inflate(R.layout.cpost_fragment_gallery, container, false);

		configureLayout(v);

		return v;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		llm = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
		galleryAdapter = new CustomGalleryAdapter(getActivity());

		if (getActivity() instanceof HLActivity)
			MediaHelper.checkPermissionForCustomGallery((HLActivity) getActivity(), this);
	}

	@Override
	public void onResume() {
		super.onResume();

		setLayout();
	}

	@Override
	public void onClick(View view) {
		MediaHelper.openPopupMenu(getContext(), R.menu.popup_menu_gallery, view,
				new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem menuItem) {
				int id = menuItem.getItemId();

				switch (id) {
					case R.id.choose_picture:
						fireGalleryIntent(HLMediaType.PHOTO);
						return true;
					case R.id.choose_video:
						fireGalleryIntent(HLMediaType.VIDEO);
						return true;
				}
				return false;
			}
		});
	}


	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		if (context instanceof OnGalleryFragmentInteractionListener) {
			mListener = (OnGalleryFragmentInteractionListener) context;
		} else {
			throw new RuntimeException(context.toString()
					+ " must implement OnGalleryFragmentInteractionListener");
		}
	}

	@Override
	public void onAttach(Activity context) {
		super.onAttach(context);
		if (context instanceof OnGalleryFragmentInteractionListener) {
			mListener = (OnGalleryFragmentInteractionListener) context;
		} else {
			throw new RuntimeException(context.toString()
					+ " must implement OnGalleryFragmentInteractionListener");
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (Utils.isContextValid(getActivity()) && getActivity() instanceof HLActivity) {
			HLActivity activity = ((HLActivity) getActivity());

			switch (requestCode) {
				case Constants.PERMISSIONS_REQUEST_GALLERY_CUSTOM:
					if (grantResults.length > 0) {
						if (grantResults.length == 1 &&
								grantResults[0] == PackageManager.PERMISSION_GRANTED) {
							MediaHelper.checkPermissionForCustomGallery(activity, this);
						}
					}
					break;
			}
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {}

	@Override
	protected void configureResponseReceiver() {}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
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
	public interface OnGalleryFragmentInteractionListener {
		void setPostType(PostTypeEnum type);
		void setMediaCaptureType(HLMediaType type);
		void checkPermissionForGallery(HLMediaType type);
	}


	/* INTERFACES CALLBACKS */

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String[] projection = {
				MediaStore.Files.FileColumns._ID,
				MediaStore.Files.FileColumns.DATE_ADDED,
				MediaStore.Files.FileColumns.DATA,
				MediaStore.Files.FileColumns.MEDIA_TYPE
		};
		String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "="
				+ MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
				+ " OR "
				+ MediaStore.Files.FileColumns.MEDIA_TYPE + "="
				+ MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;
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
	public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
		if (galleryAdapter != null)
			galleryAdapter.changeCursor(data);
	}

	@Override
	public void onLoaderReset(@NonNull Loader<Cursor> loader) {
		if (galleryAdapter != null)
			galleryAdapter.changeCursor(null);
	}


	@Override
	protected void configureLayout(@NonNull View view) {
		galleryRecView = view.findViewById(R.id.custom_gallery_rv);

		view.findViewById(R.id.gallery_btn).setOnClickListener(this);
	}

	@Override
	protected void setLayout() {
		galleryRecView.setLayoutManager(llm);
		galleryRecView.setAdapter(galleryAdapter);
	}


	//region == Class custom methods ==

	private void fireGalleryIntent(HLMediaType type) {
//		mListener.setPostType(type);
		mListener.setMediaCaptureType(type);
		mListener.checkPermissionForGallery(type);
	}

	//endregion
}
