/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.profile;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.gridlayout.widget.GridLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.models.HLCircle;
import it.keybiz.lbsapp.corporate.models.HLUser;
import it.keybiz.lbsapp.corporate.models.HLUserGeneric;
import it.keybiz.lbsapp.corporate.models.InteractionShare;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * @author mbaldrighi on 12/24/2017.
 */
public class CirclesAdapter extends RecyclerView.Adapter<CirclesAdapter.CircleVH> {

	private List<HLCircle> items;

	private OnInnerCircleActionListener mListener;

	public CirclesAdapter(List<HLCircle> items, OnInnerCircleActionListener listener) {
		this.items = items;
		this.mListener = listener;
	}

	@NonNull
	@Override
	public CircleVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return new CircleVH(LayoutInflater.from(parent.getContext())
				.inflate(R.layout.item_profile_circle, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull CircleVH holder, int position) {
		HLCircle circle = items.get(position);
		holder.setCircle(circle);
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	@Override
	public long getItemId(int position) {
		return items.get(position).hashCode();
	}


	/**
	 * The {@link RecyclerView.ViewHolder} responsible to retain the
	 * {@link View} objects of a {@link InteractionShare}.
	 */
	class CircleVH extends RecyclerView.ViewHolder implements View.OnClickListener {

		private final TextView circleName;
		private final GridLayout membersGridView;
		private final TextView noResult;
		private final TextView viewMore;

		private HLCircle currentCircle;
		//		private CircleMembersAdapter membersAdapter;
		private List<HLUserGeneric> members = new ArrayList<>();

//		private OnInnerCircleActionListener mListener;

		CircleVH(View itemView) {
			super(itemView);

			circleName = itemView.findViewById(R.id.circle_name);
			membersGridView = itemView.findViewById(R.id.grid_view);
			membersGridView.setColumnCount(4);
			membersGridView.setOrientation(GridLayout.HORIZONTAL);
			/*
			membersGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
					HLUserGeneric user = members.get(i);
					ProfileHelper.ProfileType type = ProfileHelper.ProfileType.NOT_FRIEND;
					if (mListener.getUser().getId().equals(user.getId()))
						type = ProfileHelper.ProfileType.ME;
					mListener.goToProfile(type, user.getId());
				}
			});
			*/
			noResult = itemView.findViewById(R.id.no_result);
			viewMore = itemView.findViewById(R.id.view_more);
			viewMore.setOnClickListener(this);

//			membersAdapter = new CircleMembersAdapter(itemView.getContext(), members, mListener);
//			membersGridView.setAdapter(membersAdapter);

			if (Utils.isContextValid(itemView.getContext()) &&
					itemView.getContext() instanceof CirclesAdapter.OnInnerCircleActionListener) {
				mListener = ((CirclesAdapter.OnInnerCircleActionListener) itemView.getContext());
			}
		}

		protected void setCircle(HLCircle circle) {
			if (circle == null)
				return;

			currentCircle = circle;

			circleName.setText(circle.getNameToDisplay());

			if (circle.hasMoreData()) {
				viewMore.setVisibility(View.VISIBLE);
				viewMore.setText(Utils.getFormattedHtml(itemView.getResources(), R.string.view_more));
			}
			else
				viewMore.setVisibility(View.GONE);

			List<HLUserGeneric> members = circle.getUsers();
			if (members.isEmpty()) {
				membersGridView.setVisibility(View.GONE);
				noResult.setText(R.string.no_member_in_circle);
				noResult.setVisibility(View.VISIBLE);
			}
			else {
				membersGridView.setVisibility(View.VISIBLE);
				noResult.setVisibility(View.GONE);

				this.members.addAll(members);

				int rows = this.members.size() % membersGridView.getColumnCount();
				membersGridView.setRowCount(
						rows > 0 ?
								(members.size() / membersGridView.getColumnCount()) + 1 :
								members.size() / membersGridView.getColumnCount()
				);

//				if (members.size() > 21) {
//					this.members.addAll(members.subList(0, 21));
//				}
//				else {
//					this.members.addAll(members);
//				}

				setMembers(rows);
			}
		}

		@Override
		public void onClick(View view) {
			if (mListener != null) {
				int id = view.getId();
				switch (id) {
					case R.id.view_more:
						mListener.goToViewMore(currentCircle.getName());
						break;
				}
			}
		}

		private void setMembers(int rows) {
			if (this.members != null && !this.members.isEmpty()) {

				membersGridView.removeAllViews();

				for (HLUserGeneric user : this.members) {
					final View view = LayoutInflater.from(membersGridView.getContext())
							.inflate(R.layout.item_profile_circle_member_grid, membersGridView, false);

					if (view != null) {
						GridLayout.LayoutParams lp = (GridLayout.LayoutParams) view.getLayoutParams();
						lp.setGravity(Gravity.FILL_HORIZONTAL);
						lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
						view.setLayoutParams(lp);

						final ImageView iv = view.findViewById(R.id.profile_picture);
						if (user != null) {
							MediaHelper.loadProfilePictureWithPlaceholder(membersGridView.getContext(), user.getAvatarURL(), iv);

							((TextView) view.findViewById(R.id.member_name)).setText(user.getNameForCircleMember());
						}

						view.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								if (view.getTag() instanceof HLUserGeneric) {
									HLUserGeneric us = (HLUserGeneric) view.getTag();
									ProfileHelper.ProfileType type = ProfileHelper.ProfileType.NOT_FRIEND;
									if (mListener.getUser().getId().equals(us.getId()))
										type = ProfileHelper.ProfileType.ME;
									mListener.goToProfile(type, us.getId());
								}
							}
						});

						view.setTag(user);

						membersGridView.addView(view);
					}
				}

				if (rows > 0) {
					for (int j = 0; j < membersGridView.getColumnCount() - rows; j++) {
						setInvisibleItem(membersGridView.getContext());
					}
				}
			}
		}

		private void setInvisibleItem(Context context) {
			View dummyMember = LayoutInflater.from(context).inflate(R.layout.item_profile_circle_member_grid, membersGridView, false);
			if (dummyMember != null) {
				GridLayout.LayoutParams lp = (GridLayout.LayoutParams) dummyMember.getLayoutParams();
				lp.setGravity(Gravity.FILL_HORIZONTAL);
				lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
				dummyMember.setLayoutParams(lp);

				dummyMember.setVisibility(View.INVISIBLE);
				membersGridView.addView(dummyMember);
			}
		}

	}

	public interface OnInnerCircleActionListener {
		void goToViewMore(@NonNull String circleName);
		void goToProfile(@Nullable ProfileHelper.ProfileType type, @NonNull String userId);
		HLUser getUser();
	}


	/*
	class CircleMembersAdapter extends ArrayAdapter<HLUserGeneric> {

		private List<HLUserGeneric> items;

		private HLUserGeneric currentUser;

		private OnInnerCircleActionListener mListener;

		CircleMembersAdapter(Context context, List<HLUserGeneric> items, OnInnerCircleActionListener listener) {
			super(context, 0, items);
			this.items = items;
			this.mListener = listener;
		}

		@Override
		public int getCount() {
			return items.size();
		}

		@Override
		public HLUserGeneric getItem(int i) {
			return items.get(i);
		}

		@Override
		public long getItemId(int i) {
			return items.get(i).hashCode();
		}

		@NonNull
		@Override
		public View getView(int i, View view, @NonNull ViewGroup viewGroup) {
			HLUserGeneric user = getItem(i);
			MemberViewHolder viewHolder;
			if (view == null) {
				// if it's not recycled, initialize some attributes
				view  = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_profile_circle_member_grid, viewGroup, false);
				viewHolder = new MemberViewHolder(view);
				view.setTag(viewHolder);
			} else {
				viewHolder = (MemberViewHolder) view.getTag();
			}

			viewHolder.setMember(user);

			return view;
		}


		private class MemberViewHolder *//*implements View.OnClickListener*//* {
			View itemView;
			ImageView profilePicture;

			MemberViewHolder(View itemView) {
				this.itemView = itemView;
				this.profilePicture = itemView.findViewById(R.id.profile_picture);
//				this.profilePicture.setOnClickListener(this);
			}

			private void setMember(HLUserGeneric user) {
				currentUser = user;

				if (user != null) {
					MediaHelper.loadProfilePictureWithPlaceholder(getContext(), user.getAvatarURL(), profilePicture);
				}
			}

//			@Override
//			public void onClick(View view) {
//				switch (view.getId()) {
//					case R.id.profile_picture:
//						ProfileHelper.ProfileType type = null;
//						if (mListener.getUser().getId().equals(currentUser.getId()))
//							type = ProfileHelper.ProfileType.ME;
//						mListener.goToProfile(type, currentUser.getId());
//						break;
//				}
//			}
		}
	}
	*/

}
