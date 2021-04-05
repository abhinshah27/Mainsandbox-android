/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.createPost;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.realm.RealmList;
import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.base.LBSLinkApp;
import it.keybiz.lbsapp.corporate.connection.HLRequestTracker;
import it.keybiz.lbsapp.corporate.connection.HLServerCalls;
import it.keybiz.lbsapp.corporate.connection.OnMissingConnectionListener;
import it.keybiz.lbsapp.corporate.connection.OnServerMessageReceivedListener;
import it.keybiz.lbsapp.corporate.connection.ServerMessageReceiver;
import it.keybiz.lbsapp.corporate.models.HLCircle;
import it.keybiz.lbsapp.corporate.models.HLPosts;
import it.keybiz.lbsapp.corporate.models.HLUser;
import it.keybiz.lbsapp.corporate.models.Post;
import it.keybiz.lbsapp.corporate.models.PostVisibility;
import it.keybiz.lbsapp.corporate.models.enums.PrivacyPostVisibilityEnum;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * Post visibility selection activity.
 *
 * @author mbaldrighi on 4/6/2018.
 */
public class PostVisibilityActivity extends HLActivity implements View.OnClickListener,
		OnServerMessageReceivedListener, OnMissingConnectionListener {

	public static final String LOG_TAG = PostVisibilityActivity.class.getCanonicalName();

	private TextView toolbarTitle;
	private View profilePicture;

	private ListView mListView;
	private List<PostVisibilityItem> mList = new ArrayList<>();
	private VisibilityAdapter mAdapter;

	private ArrayList<String> selValues = new ArrayList<>();
	private int selIndex = 0;

	private boolean reset = false;

	private PostVisibility originalVisibility, newVisibility;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_post_visibility);
		setRootContent(R.id.root_content);

		mAdapter = new VisibilityAdapter(this, R.layout.item_post_visibility, mList);

		mListView = findViewById(R.id.base_list);
		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

				PostVisibilityItem item = mList.get(position);

				if (item.type == VisibilityAdapter.TYPE_CIRCLE)
					item.setActivated(!item.isActivated());
				else
					item.setActivated(true);

				if (item.isActivated()) {
					item.setEnabled(true);

					if (item.type == VisibilityAdapter.TYPE_CIRCLE)
						newVisibility.addValue(item.name);
					else
						newVisibility.resetValues();

					newVisibility.setRawVisibilityType(PrivacyPostVisibilityEnum.convertSelectionIndexToEnumValue(item.type));
				}
				else if (item.type == VisibilityAdapter.TYPE_CIRCLE) {
					if (newVisibility.removeValue(item.name))
						reset = newVisibility.hasValues();
				}

				handleItems(item);
			}
		});

		configureToolbar(null, null, false);

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

		AnalyticsUtils.trackScreen(this, AnalyticsUtils.CPOST_VISIBILITY);

		callCircles();

		toolbarTitle.setText(R.string.title_activity_post_visibility);
		profilePicture.setVisibility(View.INVISIBLE);

		mListView.setAdapter(mAdapter);

		setData(mUser);
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
			setResult(RESULT_CANCELED);
		else if (v.getId() == R.id.save) {
			Intent intent = new Intent();
			boolean changed = originalVisibility.hasChanged(newVisibility);
			intent.putExtra(Constants.EXTRA_PARAM_1, changed);
			if (changed) {
				intent.putExtra(Constants.EXTRA_PARAM_2, newVisibility.getRawVisibilityType());
				if (newVisibility.hasValues())
					intent.putExtra(Constants.EXTRA_PARAM_3, newVisibility.getValuesArrayList());
			}
			setResult(RESULT_OK, intent);
		}

		finish();
		overridePendingTransition(R.anim.no_animation, R.anim.slide_out_right);
	}


	/*
	 * NO NEED TO OVERRIDE THIS
	 */
	@Override
	protected void configureResponseReceiver() {
		if (serverMessageReceiver == null)
			serverMessageReceiver = new ServerMessageReceiver();
		serverMessageReceiver.setListener(this);
	}

	@Override
	protected void manageIntent() {
		Intent intent = getIntent();
		if (intent != null) {
			if (intent.hasExtra(Constants.EXTRA_PARAM_1)) {
				String id = intent.getStringExtra(Constants.EXTRA_PARAM_1);
				if (Utils.isStringValid(id)) {
					Post post = HLPosts.getInstance().getPost(id);
					if (post != null)
						originalVisibility = post.getVisibility();
				}
			}
			else originalVisibility = new PostVisibility();

			if (originalVisibility != null) {
				int indexSelUsers = PrivacyPostVisibilityEnum.ONLY_SELECTED_USERS.getValue();
				int indexSelCircles = PrivacyPostVisibilityEnum.ONLY_SELECTED_CIRCLES.getValue();
				int index = (
						(originalVisibility.getRawVisibilityType() == indexSelUsers) ||
								(originalVisibility.getRawVisibilityType() == indexSelCircles)
				) ?
						PrivacyPostVisibilityEnum.INNER_CIRCLE.getValue() : originalVisibility.getRawVisibilityType();
				newVisibility = new PostVisibility(
						index,
						originalVisibility.getValues()
				);
			}
		}
	}

	@Override
	public void handleSuccessResponse(int operationId, JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		if (operationId == Constants.SERVER_OP_SETTINGS_IC_CIRCLES_GET) {

			if (responseObject == null || responseObject.length() == 0) {
				handleErrorResponse(operationId, 0);
				return;
			}

			realm.executeTransactionAsync(
					realm -> {
						HLUser user = new HLUser().readUser(realm);
						JSONObject jObj = responseObject.optJSONObject(0);
						JSONArray circles = jObj.optJSONArray("circles");
						if (circles != null && circles.length() > 0) {
							RealmList<String> list = user.getCircles();
							RealmList<HLCircle> listObject = user.getCircleObjects();

							if (list == null) list = new RealmList<>();
							else list.clear();

							if (listObject == null) listObject = new RealmList<>();
							else listObject.clear();

							for (int i = 0; i < circles.length(); i++) {
								JSONObject jName = circles.optJSONObject(i);
								if (jName != null) {
									HLCircle circle = new HLCircle().deserializeToClass(jName);
									if (circle != null) {
										list.add(circle.getName());
										listObject.add(circle);
									}
								}
							}
						}
						else {
							user.setCircles(new RealmList<>());
							user.setCircleObjects(new RealmList<>());
						}

						setData(user);
					}
			);
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		if (operationId == Constants.SERVER_OP_SETTINGS_IC_CIRCLES_GET)
			showAlert(R.string.error_generic_list);
	}

	@Override
	public void onMissingConnection(int operationId) { }


	//region == Class custom methods ==


	@Override
	protected void configureToolbar(Toolbar toolbar, String title, boolean showBack) {
		toolbar = findViewById(R.id.toolbar);
		toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		profilePicture = toolbar.findViewById(R.id.profile_picture);

		toolbar.findViewById(R.id.back_arrow).setOnClickListener(this);
	}

	private void callCircles() {
		Object[] result = null;

		try {
			result = HLServerCalls.getSettings(mUser.getUserId(), HLServerCalls.SettingType.CIRCLES);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		HLRequestTracker.getInstance(((LBSLinkApp) getApplication()))
				.handleCallResult(this, this, result);
	}

	private void setData(HLUser user) {
		if (mList == null)
			mList = new RealmList<>();
		else
			mList.clear();

		mList.add(
				new PostVisibilityItem(
						VisibilityAdapter.TYPE_PUBLIC,
						getString(R.string.label_privacy_public),
						getString(R.string.label_privacy_public),
						R.drawable.ic_public_post_black,
						R.drawable.ic_public_post_orange,
						R.drawable.ic_public_post_black_alpha
				)
		);
//		mList.add(
//				new PostVisibilityItem(
//						VisibilityAdapter.TYPE_INNER_CIRCLE,
//						getString(R.string.label_privacy_ic),
//						R.drawable.ic_timeline_inactive_black,
//						R.drawable.ic_timeline_active,
//						R.drawable.ic_privacy_inner_circle_alpha_black
//				)
//		);
		if (user.getCircleObjects() != null && !user.getCircleObjects().isEmpty()) {
			for (HLCircle circle : user.getCircleObjects()) {
				mList.add(
						new PostVisibilityItem(
								circle.getName().equals(Constants.INNER_CIRCLE_NAME) ? VisibilityAdapter.TYPE_INNER_CIRCLE : VisibilityAdapter.TYPE_CIRCLE,
								circle.getName(),
								circle.getNameToDisplay(),
								circle.getName().equals(Constants.INNER_CIRCLE_NAME) ? R.drawable.ic_timeline_inactive_black : R.drawable.ic_selected_people_circles_black,
								circle.getName().equals(Constants.INNER_CIRCLE_NAME) ? R.drawable.ic_timeline_active : R.drawable.ic_selected_people_circles_orange,
								circle.getName().equals(Constants.INNER_CIRCLE_NAME) ? R.drawable.ic_privacy_inner_circle_alpha_black : R.drawable.ic_selected_people_circles_black_alpha
						)
				);
			}
		}
		mList.add(
				new PostVisibilityItem(
						VisibilityAdapter.TYPE_ONLY_ME,
						getString(R.string.label_privacy_only_me),
						getString(R.string.label_privacy_only_me),
						R.drawable.ic_onlyme_post_black,
						R.drawable.ic_onlyme_post_orange,
						R.drawable.ic_onlyme_post_black_alpha
				)
		);


		if (newVisibility != null) {
			for (PostVisibilityItem item : mList) {
				int visibilityIndexEnum = PrivacyPostVisibilityEnum.convertEnumToSelectionIndex(PrivacyPostVisibilityEnum.toEnum(newVisibility.getRawVisibilityType()));
				if (visibilityIndexEnum != VisibilityAdapter.TYPE_CIRCLE) {
					item.setEnabled(item.type == visibilityIndexEnum);
					item.setActivated(item.type == visibilityIndexEnum);
				}
				else {
					if (item.type != visibilityIndexEnum) {
						item.setActivated(false);
						item.setEnabled(false);
					}
					else {
						item.setEnabled(true);

						if (newVisibility.getValues() != null) {
							for (String value : newVisibility.getValues()) {
								item.setActivated(item.name.equals(value));
							}
						}
					}
				}
			}
		}

		runOnUiThread(
				() -> mAdapter.notifyDataSetChanged()
		);
	}


	private void handleItems(PostVisibilityItem item) {
		if (item != null) {
			for (PostVisibilityItem i : mList) {
				if (!reset) {
					if (item != i) {
						if (item.type != VisibilityAdapter.TYPE_CIRCLE || i.type != VisibilityAdapter.TYPE_CIRCLE) {
							i.setEnabled(false);
							i.setActivated(false);
						}
						else i.setEnabled(true);
					}
				}
				else {
					i.setEnabled(false);
					i.setActivated(false);
				}
			}

			if (reset) {
				mList.get(0).setEnabled(true);
				mList.get(0).setActivated(true);

				newVisibility.resetValues();

				reset = false;
			}
		}

		mAdapter.notifyDataSetChanged();
	}

	//endregion


	//region == Class custom inner classes ==

	private class VisibilityAdapter extends ArrayAdapter<PostVisibilityItem> {

		private final static int TYPE_PUBLIC = 0;
		private final static int TYPE_INNER_CIRCLE = 1;
		private final static int TYPE_CIRCLE = 2;
		private final static int TYPE_ONLY_ME = 3;

		private @LayoutRes int layoutResId;

		private List<PostVisibilityItem> objects;

		VisibilityAdapter(@NonNull Context context, int layoutResId, @NonNull List<PostVisibilityItem> objects) {
			super(context, layoutResId, objects);

			this.layoutResId = layoutResId;
			this.objects = objects;
		}

		@NonNull
		@Override
		public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
			PostVisibilityItem obj = objects.get(position);

			VisibilityVH viewHolder;
			if (convertView == null) {
				convertView = LayoutInflater.from(getContext()).inflate(layoutResId, parent, false);

				viewHolder = new VisibilityVH();
				viewHolder.item = convertView;
				viewHolder.circleIcon = convertView.findViewById(R.id.circle_icon);
				viewHolder.icon = convertView.findViewById(R.id.icon);
				viewHolder.selector = convertView.findViewById(R.id.selector);
				viewHolder.name = convertView.findViewById(R.id.name);
				viewHolder.overlay = convertView.findViewById(R.id.overlay);

				convertView.setTag(viewHolder);
			}
			else {
				viewHolder = (VisibilityVH) convertView.getTag();
			}

			viewHolder.name.setText(obj.nameToDisplay);

			ImageView view = viewHolder.icon;
//			if (getItemViewType(position) == TYPE_CIRCLE)
//				view = viewHolder.circleIcon;

			viewHolder.item.setEnabled(obj.isEnabled());
			viewHolder.item.setActivated(obj.isActivated());

			if (obj.isActivated() && obj.isEnabled()) {
				view.setImageResource(obj.drawActivated);
				viewHolder.overlay.setVisibility(View.GONE);
			}
			else if (!obj.isEnabled()) {
				view.setImageResource(obj.drawNormal);
				viewHolder.overlay.setVisibility(View.VISIBLE);
			}
			else {
				view.setImageResource(obj.drawNormal);
				viewHolder.overlay.setVisibility(View.GONE);
			}

			viewHolder.selector.setVisibility(obj.isEnabled() && (getItemViewType(position) == TYPE_CIRCLE) ? View.VISIBLE : View.GONE);

			return convertView;
		}

		@Override
		public int getItemViewType(int position) {
			final int lastItem = objects.size() - 1;
			if (position == 0)
				return TYPE_PUBLIC;
			else if (position == 1)
				return TYPE_INNER_CIRCLE;
			else if (position == lastItem)
				return TYPE_ONLY_ME;
			else if (objects.size() > 3)
				return TYPE_CIRCLE;

			return super.getItemViewType(position);
		}

		@Override
		public int getViewTypeCount() {
			return 4;
		}


		final class VisibilityVH {
			ImageView icon, circleIcon;
			View item, selector, overlay;
			TextView name;
		}

	}


	private class PostVisibilityItem {

		private final int type;
		private final String name;
		private final String nameToDisplay;
		@DrawableRes private final int drawNormal;
		@DrawableRes private final int drawActivated;
		@DrawableRes private final int drawDisabled;

		private boolean enabled = true;
		private boolean activated = false;


		PostVisibilityItem(int type, String name, String nameToDisplay, int drawNormal, int drawActivated, int drawDisabled) {
			this.type = type;
			this.name = name;
			this.nameToDisplay = nameToDisplay;
			this.drawNormal = drawNormal;
			this.drawActivated = drawActivated;
			this.drawDisabled = drawDisabled;
		}


		boolean isEnabled() {
			return enabled;
		}
		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		boolean isActivated() {
			return activated;
		}
		void setActivated(boolean activated) {
			this.activated = activated;
		}
	}

	//endregion

}