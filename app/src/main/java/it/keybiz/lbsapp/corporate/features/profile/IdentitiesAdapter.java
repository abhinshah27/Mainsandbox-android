/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.BasicAdapterInteractionsListener;
import it.keybiz.lbsapp.corporate.models.HLIdentity;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * @author mbaldrighi on 1/22/2018.
 */
public class IdentitiesAdapter extends RecyclerView.Adapter<IdentitiesAdapter.IdentityVH> {

	private List<HLIdentity> items;
	private BasicAdapterInteractionsListener listener;

	private String selectedIdentityID;

	public IdentitiesAdapter(List<HLIdentity> items, BasicAdapterInteractionsListener listener) {
		this.items = items;
		this.listener = listener;
	}

	@Override
	public IdentityVH onCreateViewHolder(ViewGroup parent, int viewType) {
		return new IdentityVH(LayoutInflater.from(parent.getContext())
				.inflate(R.layout.item_identity, parent, false));
	}

	@Override
	public void onBindViewHolder(IdentityVH holder, int position) {
		HLIdentity obj = items.get(position);

		if (obj != null) {
			holder.setIdentity(obj);
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	@Override
	public long getItemId(int position) {
		return items.get(position).hashCode();
	}


	class IdentityVH extends RecyclerView.ViewHolder implements View.OnClickListener {

		private final ImageView profilePicture;
		private final TextView name;
		private final TextView type;
		private final View check;

		private HLIdentity currentObject;

		IdentityVH(View itemView) {
			super(itemView);

			profilePicture = itemView.findViewById(R.id.profile_picture);
			name = itemView.findViewById(R.id.name);
			type = itemView.findViewById(R.id.type);
			check = itemView.findViewById(R.id.check);

			itemView.setOnClickListener(this);
		}

		void setIdentity(HLIdentity obj) {
			currentObject = obj;
			if (Utils.isStringValid(obj.getAvatarURL())) {
				MediaHelper.loadProfilePictureWithPlaceholder(profilePicture.getContext(),
						obj.getAvatarURL(), profilePicture);
			}
			else profilePicture.setImageResource(R.drawable.ic_profile_placeholder);

			name.setText(obj.getName());

			type.setText(obj.isInterest() ? R.string.type_interest_page : R.string.interest_follower_name_you);

			check.setVisibility(
					(
							Utils.areStringsValid(obj.getId(), selectedIdentityID) &&
							obj.getId().equals(selectedIdentityID)
					) ? View.VISIBLE : View.GONE
			);
		}


		@Override
		public void onClick(View view) {
			listener.onItemClick(currentObject);
		}
	}


	//region == Getters and setters ==

	public String getSelectedIdentityID() {
		return selectedIdentityID;
	}
	public void setSelectedIdentityID(String selectedIdentityID) {
		this.selectedIdentityID = selectedIdentityID;
	}

	//endregion

}
