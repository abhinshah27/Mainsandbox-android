/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.timeline.interactions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.models.HLCircle;
import it.keybiz.lbsapp.corporate.models.HLUserGeneric;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * @author mbaldrighi on 11/10/2017.
 */
public class SharePeopleAdapter extends RecyclerView.Adapter<SharePeopleAdapter.ShareItemVH> {

	public static final int TYPE_CIRCLE = 0;
	public static final int TYPE_USER = 1;

	private List<Object> mValues;

	private OnShareElementClickedListener mListener;

	private boolean actingAsInterest;

	public SharePeopleAdapter(List<Object> values, OnShareElementClickedListener listener,
	                          boolean actingAsInterest) {
		super();
		this.mValues = values;
		this.mListener = listener;
		this.actingAsInterest = actingAsInterest;
	}

	@NonNull
	@Override
	public ShareItemVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		switch (viewType) {
			case TYPE_CIRCLE:
				return new ShareItemVH(LayoutInflater.from(parent.getContext())
						.inflate(R.layout.item_share_circle_new, parent, false));
		}
		return new ShareItemVH(LayoutInflater.from(parent.getContext())
				.inflate(R.layout.item_share_user, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull ShareItemVH holder, int position) {
		Object o = mValues.get(position);

		if (o != null) {
			holder.setItem(o, actingAsInterest);
			holder.setOnShareElementClickedListener(new ShareItemVH.OnShareElementClickedListener() {
				@Override
				public void onShareElementClick(View v, Object o, int position) {
					mListener.onShareElementClick(o, position);
				}
			});
			if (actingAsInterest)
				holder.itemView.performClick();
		}
	}

	@Override
	public int getItemCount() {
		return mValues.size();
	}

	@Override
	public long getItemId(int position) {
		Object o = mValues.get(position);
		if (o != null)
			return o.hashCode();

		return super.getItemId(position);
	}

	@Override
	public int getItemViewType(int position) {
		Object o = mValues.get(position);
		if (o != null) {
			if (o instanceof HLCircle)
				return TYPE_CIRCLE;
			else if (o instanceof HLUserGeneric)
				return TYPE_USER;
		}

		return super.getItemViewType(position);
	}


	public interface OnShareElementClickedListener {
		void onShareElementClick(Object o, int position);
	}



	static class ShareItemVH extends RecyclerView.ViewHolder implements View.OnClickListener {

		final View itemView;
		final View singleContainer;
		final ImageView singlePicture;
		final TextView name;

		final View selectionOverlay;

		Object mItem;

		OnShareElementClickedListener onShareElementClickedListener;

		ShareItemVH(View itemView) {
			super(itemView);

			this.itemView = itemView;

			singleContainer = itemView.findViewById(R.id.single_picture_container);

			selectionOverlay = itemView.findViewById(R.id.selection_overlay);
			selectionOverlay.setBackgroundResource(R.drawable.shape_circle_orange_alpha);

			singlePicture = itemView.findViewById(R.id.single_picture);
			name = itemView.findViewById(R.id.name);

			itemView.setOnClickListener(this);
		}

		void setItem(@NonNull Object item, boolean actingAsInterest) {
			mItem = item;

			if (item instanceof HLUserGeneric) {
				if (Utils.isStringValid(((HLUserGeneric) item).getAvatarURL())) {
					MediaHelper.loadProfilePictureWithPlaceholder(
							singlePicture.getContext(),
							((HLUserGeneric) item).getAvatarURL(),
							singlePicture
					);
				}
				else singlePicture.setImageResource(R.drawable.ic_profile_placeholder);

				name.setText(((HLUserGeneric) item).getName());
			}
			else {
				if (actingAsInterest)
					name.setText(name.getResources().getString(R.string.all_followers));
				else
					name.setText(((HLCircle) item).getNameToDisplay());
			}

			boolean selected = false;
			if (mItem instanceof HLUserGeneric)
				selected = ((HLUserGeneric) mItem).isSelected();
			else if (mItem instanceof HLCircle)
				selected = ((HLCircle) mItem).isSelected();
			setSelection(selected);
		}

		@Override
		public void onClick(View v) {
			onShareElementClickedListener.onShareElementClick(v, mItem, getAdapterPosition());
		}

		void setSelection(boolean activate) {
			if (selectionOverlay != null)
				selectionOverlay.setVisibility(activate ? View.VISIBLE : View.GONE);
		}

		public interface OnShareElementClickedListener {
			void onShareElementClick(View v, Object o, int position);
		}

		public void setOnShareElementClickedListener(OnShareElementClickedListener onShareElementClickedListener) {
			this.onShareElementClickedListener = onShareElementClickedListener;
		}
	}

}
