/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.profile;

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
import it.keybiz.lbsapp.corporate.models.Tag;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * @author mbaldrighi on 12/26/2017.
 */
public class SimilarForInterestAdapter extends RecyclerView.Adapter<SimilarForInterestAdapter.SimpleInterestWithCategoriesVH> {

	private List<Interest> items;
	private OnItemClickListener listener;

	public SimilarForInterestAdapter(List<Interest> items, OnItemClickListener listener) {
		this.items = items;
		this.listener = listener;
	}

	@NonNull
	@Override
	public SimilarForInterestAdapter.SimpleInterestWithCategoriesVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return new SimpleInterestWithCategoriesVH(LayoutInflater.from(parent.getContext())
				.inflate(R.layout.item_search_list_interest, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull SimilarForInterestAdapter.SimpleInterestWithCategoriesVH holder, int position) {
		Object obj = items.get(position);
		holder.setSimpleItem(obj);
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
			else if (obj instanceof Tag) {
				if (Utils.isStringValid(((Tag) obj).getUserUrl())) {
					MediaHelper.loadProfilePictureWithPlaceholder(profilePicture.getContext(),
						((Tag) obj).getUserUrl(), profilePicture);
				}
				else profilePicture.setImageResource(R.drawable.ic_placeholder_profile);

				name.setText(((Tag) obj).getUserName());
			}
		}


		@Override
		public void onClick(View view) {
			listener.onItemClick(currentObject);
		}
	}

	public interface OnItemClickListener {
		void onItemClick(Object object);
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
			listener.onItemClick(super.currentObject);
		}
	}

}