/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.tags;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.BasicAdapterInteractionsListener;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.features.HomeActivity;
import it.keybiz.lbsapp.corporate.features.profile.ProfileActivity;
import it.keybiz.lbsapp.corporate.features.profile.ProfileHelper;
import it.keybiz.lbsapp.corporate.models.Tag;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * Activity displaying the {@link it.keybiz.lbsapp.corporate.models.Post}'s
 * {@link it.keybiz.lbsapp.corporate.models.Tag} objects.
 */
public class ViewAllTagsActivity extends HLActivity implements View.OnClickListener, BasicAdapterInteractionsListener {

	public static final String LOG_TAG = ViewAllTagsActivity.class.getCanonicalName();

	private boolean allowLookUp = false;

	private TextView toolbarTitle;
	private View profilePicture;

	private RecyclerView mRecView;
	private List<Object> mList = new ArrayList<>();
	private LinearLayoutManager llm;
	private TagAdapter mAdapter;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		View mainView;
		setContentView(R.layout.activity_view_tags);
		setRootContent(mainView = findViewById(R.id.root_content));

		manageIntent();

		Utils.getGenericSwipeLayout(mainView, null).setEnabled(false);

		llm = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
		mAdapter = new TagAdapter(mList, this, true, false);

		mRecView = findViewById(R.id.base_list);

		configureToolbar(null, null, false);
	}

	@Override
	protected void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(this, AnalyticsUtils.FEED_VIEW_TAGS);

		toolbarTitle.setText(R.string.title_activity_all_tags);
		profilePicture.setVisibility(View.INVISIBLE);

		mRecView.setLayoutManager(llm);
		mRecView.setAdapter(mAdapter);
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.back_arrow)
			finish();
	}

	@Override
	public void onItemClick(Object object) {
		String id = null;
		ProfileHelper.ProfileType type = null;
		if (object instanceof Tag) {
			id = ((Tag) object).getId();
			type = ((Tag) object).isInterest() ?
					ProfileHelper.ProfileType.INTEREST_NOT_CLAIMED : ProfileHelper.ProfileType.NOT_FRIEND;
		}

		if (allowLookUp && Utils.isStringValid(id) && type != null) {
			if ((mUser.isActingAsInterest() && id.equals(mUser.getId())) ||
					(!mUser.isActingAsInterest() && id.equals(mUser.getUserId()))) {
				Intent intent = new Intent(this, HomeActivity.class);
				intent.putExtra(Constants.EXTRA_PARAM_1, HomeActivity.PAGER_ITEM_PROFILE);
				startActivity(intent);
				finish();
			}
			else
				ProfileActivity.openProfileCardFragment(this, type, id, HomeActivity.PAGER_ITEM_TIMELINE);
		}
	}

	@Override
	public void onItemClick(Object object, View view) {}


	/*
	 * NO NEED TO OVERRIDE THIS
	 */
	@Override
	protected void configureResponseReceiver() {}

	@Override
	protected void manageIntent() {
		Intent intent = getIntent();
		if (intent != null) {
			if (intent.hasExtra(Constants.EXTRA_PARAM_1)) {
				List<Parcelable> temp = intent.getParcelableArrayListExtra(Constants.EXTRA_PARAM_1);

				if (temp != null && !temp.isEmpty()) {
					mList.addAll(temp);
				}
			}
			if (intent.hasExtra(Constants.EXTRA_PARAM_2)) {
				allowLookUp = intent.getBooleanExtra(Constants.EXTRA_PARAM_2, false);
			}
		}
	}


	//region == Class custom methods ==

	@Override
	protected void configureToolbar(Toolbar toolbar, String title, boolean showBack) {
		toolbar = findViewById(R.id.toolbar);
		toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		profilePicture = toolbar.findViewById(R.id.profile_picture);

		toolbar.findViewById(R.id.back_arrow).setOnClickListener(this);
	}

	//endregion

}

