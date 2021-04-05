/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.globalSearch;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.models.HLUserGeneric;
import it.keybiz.lbsapp.corporate.models.Interest;
import it.keybiz.lbsapp.corporate.models.InterestCategory;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * @author mbaldrighi on 5/22/2019.
 */
public class InterestUsersListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private final int TYPE_PEOPLE = 0;
	private final int TYPE_INTEREST_W_CATEGORIES = 2;

	private List<Object> items;
	private OnItemClickListener listener;

	public InterestUsersListAdapter(List<Object> items, OnItemClickListener listener) {
		this.items = items;
		this.listener = listener;
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		switch (viewType) {
			case TYPE_PEOPLE:
				return new SimpleItemVH(LayoutInflater.from(parent.getContext())
						.inflate(R.layout.item_profile_contact, parent, false));
			case TYPE_INTEREST_W_CATEGORIES:
				return new SimpleInterestWithCategoriesVH(LayoutInflater.from(parent.getContext())
						.inflate(R.layout.item_search_list_interest, parent, false));
		}

		return new SimpleItemVH(LayoutInflater.from(parent.getContext())
				.inflate(R.layout.item_profile_contact, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		Object obj = items.get(position);

		switch (getItemViewType(position)) {
			case TYPE_PEOPLE:
				if (holder instanceof SimpleItemVH) {
					((SimpleItemVH) holder).setSimpleItem(obj);
				}
				break;
			case TYPE_INTEREST_W_CATEGORIES:
				if (holder instanceof SimpleInterestWithCategoriesVH) {
					((SimpleInterestWithCategoriesVH) holder).setSimpleItem(obj);
				}
				break;
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	@Override
	public long getItemId(int position) {
		if (items != null && !items.isEmpty() && items.get(position) != null)
			return items.get(position).hashCode();
		else
			return super.getItemId(position);
	}

	@Override
	public int getItemViewType(int position) {
		Object obj = items.get(position);
		if (obj instanceof HLUserGeneric)
			return TYPE_PEOPLE;
		else if (obj instanceof Interest)
			return TYPE_INTEREST_W_CATEGORIES;

		return super.getItemViewType(position);
	}


	class SimpleItemVH extends RecyclerView.ViewHolder implements View.OnClickListener {

		private final ImageView profilePicture;
		private final TextView name;

		private Object currentObject;

		SimpleItemVH(View itemView) {
			super(itemView);

			profilePicture = itemView.findViewById(R.id.profile_picture);
			name = itemView.findViewById(R.id.name);

			View invite = itemView.findViewById(R.id.invite_btn);
			if (invite != null)
				invite.setVisibility(View.GONE);
			itemView.setOnClickListener(this);
		}

		void setSimpleItem(Object obj) {
			currentObject = obj;

			if (obj instanceof HLUserGeneric) {
				if (Utils.isStringValid(((HLUserGeneric) obj).getAvatarURL())) {
					MediaHelper.loadProfilePictureWithPlaceholder(profilePicture.getContext(),
							((HLUserGeneric) obj).getAvatarURL(), profilePicture);
				}
				else profilePicture.setImageResource(R.drawable.ic_placeholder_profile);

				name.setText(((HLUserGeneric) obj).getCompleteName());
			}
			else if (obj instanceof Interest) {
				if (Utils.isStringValid(((Interest) obj).getAvatarURL())) {
					MediaHelper.loadProfilePictureWithPlaceholder(profilePicture.getContext(),
						((Interest) obj).getAvatarURL(), profilePicture);
				}
				else profilePicture.setImageResource(R.drawable.ic_placeholder_profile);

				name.setText(((Interest) obj).getName());
			}
		}


		@Override
		public void onClick(View view) {
			listener.onItemClick(currentObject, false);
		}
	}

	public interface OnItemClickListener {
		void onItemClick(Object object, boolean isInterest);
	}

	class SimpleInterestWithCategoriesVH extends SimpleItemVH implements View.OnClickListener {

		private final TextView category;

		SimpleInterestWithCategoriesVH(View itemView) {
			super(itemView);

			category = itemView.findViewById(R.id.type);

			itemView.setOnClickListener(this);
		}

		void setSimpleItem(Object obj) {
			super.setSimpleItem(obj);

			if (obj instanceof Interest) {

				StringBuilder stringBuilder = new StringBuilder();
				if (((Interest) obj).getCategories() != null && !((Interest) obj).getCategories().isEmpty()) {
					for (InterestCategory cat :
							((Interest) obj).getCategories()) {
						stringBuilder.append(cat.getName()).append(", ");
					}
				}

				String s;
				if (Utils.isStringValid(s = stringBuilder.toString().trim())) {
					category.setText(s.substring(0, s.length() - 1));
					category.setVisibility(View.VISIBLE);
				}
				else
					category.setVisibility(View.GONE);
			}
		}


		@Override
		public void onClick(View view) {
			listener.onItemClick(super.currentObject, super.currentObject instanceof Interest);
		}
	}

}