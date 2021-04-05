/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.wishes;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.afollestad.materialdialogs.MaterialDialog;

import org.json.JSONArray;
import org.json.JSONException;

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
import it.keybiz.lbsapp.corporate.models.HLWish;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.DialogUtils;
import it.keybiz.lbsapp.corporate.utilities.RealmUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * Displays saved wishes list.
 */
public class SavedWishesActivity extends HLActivity implements View.OnClickListener, WishesAdapter.WishesSwipeAdapterInterface,
		OnServerMessageReceivedListener, OnMissingConnectionListener {

	public static final String LOG_TAG = SavedWishesActivity.class.getCanonicalName();

	private View root;

	private TextView toolbarTitle;

	private RecyclerView mRecView;
	private List<HLWish> mList = new ArrayList<>();
	private LinearLayoutManager llm;
	private WishesAdapter mAdapter;

	private TextView noResult;

	private HLWish wishToDelete;
	private int positionToDelete;
	private MaterialDialog dialogDelete;

	private SwipeRefreshLayout srl;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_saved_wishes);

		View root;
		setRootContent(root = findViewById(R.id.root_content));

		llm = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
		mAdapter = new WishesAdapter(mList, this);

//		final SwipeRemoveCallback src = new SwipeRemoveCallback(this, new SwipeRemoveActions() {
//			@Override
//			public void onRightClicked(RecyclerView.ViewHolder viewHolder) {
//				if (viewHolder instanceof WishesAdapter.WishVH) {
//					wishToDelete = ((WishesAdapter.WishVH) viewHolder).getCurrentObject();
//					if (wishToDelete != null) {
//						callServer(CallType.DELETE, wishToDelete.getId());
//						mAdapter.notifyItemRemoved(viewHolder.getAdapterPosition());
//					}
//				}
//			}
//		});
//		ItemTouchHelper itemTouchHelper = new ItemTouchHelper(src);

		mRecView = findViewById(R.id.base_list);
//		mRecView.addItemDecoration(new RecyclerView.ItemDecoration() {
//			@Override
//			public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
//				src.onDraw(c);
//			}
//		});
		noResult = findViewById(R.id.no_result);


//		itemTouchHelper.attachToRecyclerView(mRecView);


		srl = Utils.getGenericSwipeLayout(root, new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				Utils.setRefreshingForSwipeLayout(srl, true);

				callServer(CallType.GET_WISHES, null);
			}
		});


		configureResponseReceiver();

		configureToolbar(null, null, false);
	}

	@Override
	protected void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(this, AnalyticsUtils.WISHES_EDIT);

		toolbarTitle.setText(R.string.title_activity_saved_wishes);

		noResult.setText(R.string.no_result_save_wishes);

		callServer(CallType.GET_WISHES, null);

		mRecView.setLayoutManager(llm);
		mRecView.setAdapter(mAdapter);

		setData(null);
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
		if (v.getId() == R.id.back_arrow) {
			onBackPressed();
		}
	}

	@Override
	public void onBackPressed() {
		finish();
		overridePendingTransition(R.anim.no_animation, R.anim.slide_out_down);
	}

	@Override
	public void onItemClick(final HLWish object) {
		WishesActivity.openWishPreviewFragment(this, object);
	}

	@Override
	public void onRemove(WishesAdapter.WishVH viewHolder) {
		wishToDelete = viewHolder.getCurrentObject();
		positionToDelete = viewHolder.getAdapterPosition();
		if (wishToDelete != null) {

			dialogDelete = DialogUtils.createGenericAlertCustomView(this, R.layout.custom_dialog_delete_wish);
			if (dialogDelete != null) {
				View view = dialogDelete.getCustomView();
				if (view != null) {
					Button positive = view.findViewById(R.id.button_positive);
					positive.setText(R.string.option_comment_delete);
					positive.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							callServer(CallType.DELETE, wishToDelete.getId());
						}
					});

					view.findViewById(R.id.button_negative).setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							dialogDelete.dismiss();
						}
					});
				}

				dialogDelete.show();
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
	public void handleSuccessResponse(int operationId, final JSONArray responseObject) {
		super.handleSuccessResponse(operationId, responseObject);

		Utils.setRefreshingForSwipeLayout(srl, false);

		switch (operationId) {
			case Constants.SERVER_OP_GET_SAVED_WISHES:
				setData(responseObject);
				break;

			case Constants.SERVER_OP_WISH_DELETE:
				if (wishToDelete != null && mList != null && mList.contains(wishToDelete)) {
					mList.remove(wishToDelete);
					mAdapter.notifyItemRemoved(positionToDelete);
					dialogDelete.dismiss();

					if (mList.isEmpty())
						setData(null);
				}
				break;
		}
	}

	@Override
	public void handleErrorResponse(int operationId, int errorCode) {
		super.handleErrorResponse(operationId, errorCode);

		Utils.setRefreshingForSwipeLayout(srl, false);

		switch (operationId) {
			case Constants.SERVER_OP_GET_SAVED_WISHES:
				showAlert(R.string.error_generic_list);
				break;

			case Constants.SERVER_OP_WISH_DELETE:
				mAdapter.notifyDataSetChanged();
				showGenericError();
				break;
		}
	}

	@Override
	public void onMissingConnection(int operationId) {
		Utils.setRefreshingForSwipeLayout(srl, false);
	}

	@Override
	protected void manageIntent() {}


	//region == Class custom methods ==


	@Override
	protected void configureToolbar(Toolbar toolbar, String title, boolean showBack) {
		toolbar = findViewById(R.id.toolbar);
		toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		toolbar.findViewById(R.id.back_arrow).setOnClickListener(this);
	}

	private enum CallType { GET_WISHES, DELETE }
	private void callServer(CallType type, @Nullable String wishId) {
		Object[] result = null;

		try {
			if (type == CallType.GET_WISHES) {
				// fixes Crashlyt. #40
				if (mUser == null) {
					realm = RealmUtils.checkAndFetchRealm(realm);
					mUser = RealmUtils.checkAndFetchUser(realm, mUser);
				}
				if (mUser != null)

					result = HLServerCalls.getSavedWishes(mUser.getUserId());
			}
			else if (type == CallType.DELETE && Utils.isStringValid(wishId))
				result = HLServerCalls.deleteWish(wishId);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		HLRequestTracker.getInstance(((LBSLinkApp) getApplication())).handleCallResult(this, this, result);
	}

	private void setData(JSONArray array) {
		if (array == null || array.length() == 0) {
			mRecView.setVisibility(View.GONE);
			noResult.setVisibility(View.VISIBLE);
			return;
		}

		mRecView.setVisibility(View.VISIBLE);
		noResult.setVisibility(View.GONE);

		if (mList == null)
			mList = new RealmList<>();
		else
			mList.clear();

		for (int i = 0; i < array.length(); i++) {
			HLWish wish = new HLWish().deserializeToClass(array.optJSONObject(i));
			if (wish != null)
				mList.add(wish);
		}

		mAdapter.notifyDataSetChanged();
	}

	//endregion

}

