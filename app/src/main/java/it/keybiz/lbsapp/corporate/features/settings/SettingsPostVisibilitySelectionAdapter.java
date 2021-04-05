/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.settings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.BasicAdapterInteractionsListener;
import it.keybiz.lbsapp.corporate.models.PostList;
import it.keybiz.lbsapp.corporate.models.enums.PrivacyPostVisibilityEnum;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * @author mbaldrighi on 3/1/2018.
 */
public class SettingsPostVisibilitySelectionAdapter extends RecyclerView.Adapter<SettingsPostVisibilitySelectionAdapter.ElementVH> {

	private List<SettingsPrivacyPostVisibilitySelectionFragment.ObjectWithSelection> items;

	private PrivacyPostVisibilityEnum mViewType;
	private BasicAdapterInteractionsListener mListener;

	public SettingsPostVisibilitySelectionAdapter(List<SettingsPrivacyPostVisibilitySelectionFragment.ObjectWithSelection> items,
	                                              BasicAdapterInteractionsListener listener,
	                                              PrivacyPostVisibilityEnum type) {
		this.items = items;
		this.mListener = listener;
		this.mViewType = type;
	}

	@NonNull
	@Override
	public ElementVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return new ElementVH(
				LayoutInflater
						.from(parent.getContext())
						.inflate(R.layout.item_wish_recipient, parent, false),
				mListener
		);
	}

	@Override
	public void onBindViewHolder(@NonNull ElementVH holder, int position) {
		SettingsPrivacyPostVisibilitySelectionFragment.ObjectWithSelection post = items.get(position);
		holder.setElement(post);
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
	 * {@link View} objects of a {@link PostList}.
	 */
	class ElementVH extends RecyclerView.ViewHolder implements View.OnClickListener {

		private final View itemView;

		private final TextView name;
		private final ImageView picture;

		private SettingsPrivacyPostVisibilitySelectionFragment.ObjectWithSelection listElement;

		private BasicAdapterInteractionsListener mListener;


		ElementVH(View itemView, BasicAdapterInteractionsListener listener) {
			super(itemView);

			this.itemView = itemView;
			this.itemView.setOnClickListener(this);

			this.mListener = listener;

			name = itemView.findViewById(R.id.name);
			picture = itemView.findViewById(R.id.profile_picture);
		}

		void setElement(final SettingsPrivacyPostVisibilitySelectionFragment.ObjectWithSelection ows) {
			if (ows == null)
				return;

			listElement = ows;

			itemView.setActivated(ows.isSelected());

			if (mViewType == PrivacyPostVisibilityEnum.ONLY_SELECTED_CIRCLES)
				name.setText(ows.getNameToDisplay());
			else if (mViewType == PrivacyPostVisibilityEnum.ONLY_SELECTED_USERS)
				name.setText(ows.getName());

			picture.setVisibility(
					mViewType == PrivacyPostVisibilityEnum.ONLY_SELECTED_CIRCLES ?
							View.GONE : View.VISIBLE
			);
			if (Utils.isStringValid(ows.getAvatarURL()))
				MediaHelper.loadProfilePictureWithPlaceholder(picture.getContext(), ows.getAvatarURL(), picture);
			else
				picture.setImageResource(R.drawable.ic_profile_placeholder);
		}

		@Override
		public void onClick(View v) {
			mListener.onItemClick(listElement);
		}
	}

}
