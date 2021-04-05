/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.models.ProfileContactToSend;
import it.keybiz.lbsapp.corporate.utilities.media.GlideApp;

/**
 * @author mbaldrighi on 12/20/2017.
 */
public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactVH> {

	private List<ProfileContactToSend> items;

	private InviteHelper inviteHelper;

	public ContactsAdapter(List<ProfileContactToSend> items, InviteHelper inviteHelper) {
		this.items = items;
		this.inviteHelper = inviteHelper;
	}

	@Override
	public ContactVH onCreateViewHolder(ViewGroup parent, int viewType) {
		return new ContactVH(LayoutInflater.from(parent.getContext())
				.inflate(R.layout.item_profile_contact, parent, false));
	}

	@Override
	public void onBindViewHolder(ContactVH holder, int position) {
		ProfileContactToSend c = items.get(position);
		holder.setContact(c);
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
	 * {@link View} objects of a {@link it.keybiz.lbsapp.corporate.models.ProfileContactToSend}.
	 */
	class ContactVH extends RecyclerView.ViewHolder implements View.OnClickListener {

		private final ImageView profilePic;
		private final TextView contactName;
		private final Button inviteBtn;

		private ProfileContactToSend currentContact;

		ContactVH(View itemView) {
			super(itemView);

			profilePic = itemView.findViewById(R.id.profile_picture);
			contactName = itemView.findViewById(R.id.name);
			inviteBtn = itemView.findViewById(R.id.invite_btn);

			profilePic.setOnClickListener(this);
			inviteBtn.setOnClickListener(this);
		}

		void setContact(ProfileContactToSend contact) {

			if (contact == null)
				return;

			currentContact = contact;

			GlideApp.with(profilePic).asDrawable().load(contact.getPhoto())
					.placeholder(R.drawable.ic_profile_placeholder).into(profilePic);
			contactName.setText(contact.getName());
		}

		@Override
		public void onClick(View view) {
			int id = view.getId();
			switch (id) {
				case R.id.invite_btn:
					inviteHelper.openSelector(view, currentContact);
					break;
			}
		}

	}

}
