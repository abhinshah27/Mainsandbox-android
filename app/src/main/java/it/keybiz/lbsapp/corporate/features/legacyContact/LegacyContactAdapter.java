/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.legacyContact;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.BasicAdapterInteractionsListener;
import it.keybiz.lbsapp.corporate.models.HLUserGeneric;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * @author mbaldrighi on 3/13/2018.
 */
public class LegacyContactAdapter extends RecyclerView.Adapter<LegacyContactAdapter.IdentityVH> {

	private List<HLUserGeneric> items;
	private BasicAdapterInteractionsListener listener;

	private String selectedIdentityID;

	public LegacyContactAdapter(List<HLUserGeneric> items, BasicAdapterInteractionsListener listener) {
		this.items = items;
		this.listener = listener;
	}

	@Override
	public IdentityVH onCreateViewHolder(ViewGroup parent, int viewType) {
		return new IdentityVH(LayoutInflater.from(parent.getContext())
				.inflate(R.layout.item_legacy_contact, parent, false));
	}

	@Override
	public void onBindViewHolder(IdentityVH holder, int position) {
		HLUserGeneric obj = items.get(position);

		if (obj != null) {
			holder.setContact(obj);
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
		private final TextView reqSent;

		private HLUserGeneric currentObject;

		IdentityVH(View itemView) {
			super(itemView);

			profilePicture = itemView.findViewById(R.id.profile_picture);
			name = itemView.findViewById(R.id.name);
			reqSent = itemView.findViewById(R.id.request_sent);

			itemView.setOnClickListener(this);
		}

		void setContact(HLUserGeneric obj) {
			currentObject = obj;
			if (Utils.isStringValid(obj.getAvatarURL())) {
				MediaHelper.loadProfilePictureWithPlaceholder(profilePicture.getContext(),
						obj.getAvatarURL(), profilePicture);
			}
			else profilePicture.setImageResource(R.drawable.ic_profile_placeholder);

			name.setText(obj.getName());
		}


		@Override
		public void onClick(View view) {
			listener.onItemClick(currentObject, view);
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
