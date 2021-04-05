/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.tags;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.BasicAdapterInteractionsListener;
import it.keybiz.lbsapp.corporate.features.createPost.InitiativesFragment;
import it.keybiz.lbsapp.corporate.features.createPost.TagFragment;
import it.keybiz.lbsapp.corporate.models.HLUserGeneric;
import it.keybiz.lbsapp.corporate.models.Interest;
import it.keybiz.lbsapp.corporate.models.Tag;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * @author mbaldrighi on 4/3/2018.
 */
public class TagAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	public static final int TYPE_PHOTO = 0;
	public static final int TYPE_TEXT = 1;
	public static final int TYPE_WHOLE = 2;
	public static final int TYPE_DIVIDER = 3;

	private List<Object> mValues;

	private BasicAdapterInteractionsListener mAdapterListener;
	private TagFragment.OnTagFragmentInteractionListener mFragmentListener;

	private boolean fullItem = false;

	private static boolean fromInitiatives = false;


	public TagAdapter(List<Object> values, BasicAdapterInteractionsListener listener) {
		this.mValues = values;
		this.mAdapterListener = listener;

		if (this.mAdapterListener instanceof TagFragment.OnTagFragmentInteractionListener)
			mFragmentListener = (TagFragment.OnTagFragmentInteractionListener) mAdapterListener;
	}

	public TagAdapter(List<Object> values, BasicAdapterInteractionsListener listener, boolean fromInitiatives) {
		this.mValues = values;
		this.mAdapterListener = listener;

		TagAdapter.fromInitiatives = fromInitiatives;

		if (this.mAdapterListener instanceof TagFragment.OnTagFragmentInteractionListener)
			mFragmentListener = (TagFragment.OnTagFragmentInteractionListener) mAdapterListener;
	}

	public TagAdapter(List<Object> values, BasicAdapterInteractionsListener listener, boolean fullItem,
	                  boolean fromInitiatives) {
		this.mValues = values;
		this.mAdapterListener = listener;
		this.mFragmentListener = null;

		this.fullItem = fullItem;

		TagAdapter.fromInitiatives = fromInitiatives;
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		switch (viewType) {
			case TYPE_PHOTO:
				return new TagPhotoItemVH(
						LayoutInflater.from(parent.getContext())
								.inflate(R.layout.item_tag_user_interest, parent, false),
						mAdapterListener,
						mFragmentListener,
						fullItem
				);

			case TYPE_WHOLE:
				return new TagPhotoItemVH(
						LayoutInflater.from(parent.getContext())
								.inflate(fullItem ? R.layout.item_tag_search_full : R.layout.item_tag_search, parent, false),
						mAdapterListener,
						mFragmentListener,
						fullItem
				);

			case TYPE_DIVIDER:
				return new DividerVH(
						LayoutInflater.from(parent.getContext())
								.inflate(R.layout.custom_divider_search_tag_item, parent, false)
				);

			default:
				return new TagPhotoItemVH(
						LayoutInflater.from(parent.getContext())
								.inflate(R.layout.item_tag_user_interest, parent, false),
						mAdapterListener,
						mFragmentListener,
						fullItem
						);
		}
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		Object o = mValues.get(position);

		if (o != null) {
			if (holder instanceof TagPhotoItemVH)
				((TagPhotoItemVH) holder).setItem(o);

			// TODO: 4/4/2018    RETURN HERE WHEN READY !
//			else if () {
//
//			}
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
			if (o instanceof HLUserGeneric || o instanceof Interest)
				return TYPE_PHOTO;

				// TODO: 4/4/2018    RETURN HERE WHEN READY !
//			else if (o instanceof ) {
//				return TYPE_TEXT;
//			}

			else if (o instanceof Tag)
				return TYPE_WHOLE;

			else if (o instanceof View)
				return TYPE_DIVIDER;
		}

		return super.getItemViewType(position);
	}


	static class TagPhotoItemVH extends RecyclerView.ViewHolder implements View.OnClickListener {

		final View itemView;
		final ImageView profilePicture;

		@Nullable final View selectionOverlay;
		@Nullable final TextView tagName;

		Object mItem;

		BasicAdapterInteractionsListener adapterListener;
		TagFragment.OnTagFragmentInteractionListener fragmentListener;


		TagPhotoItemVH(View itemView, BasicAdapterInteractionsListener adapterListener,
		               TagFragment.OnTagFragmentInteractionListener fragmentListener, boolean fullItem) {
			super(itemView);

			this.itemView = itemView;

			selectionOverlay = itemView.findViewById(R.id.selection_overlay);

			tagName = itemView.findViewById(R.id.name);

			profilePicture = itemView.findViewById(R.id.profile_picture);

			this.itemView.setOnClickListener(this);
			this.adapterListener = adapterListener;
			this.fragmentListener = !fullItem ? fragmentListener : null;
		}

		void setItem(@NonNull Object item) {
			mItem = item;

			String id = "", avatar = "";
			if (item instanceof HLUserGeneric) {
				id = ((HLUserGeneric) item).getId();
				avatar = ((HLUserGeneric) item).getAvatarURL();
			}
			else if (item instanceof Interest) {
				id = ((Interest) item).getId();
				avatar = ((Interest) item).getAvatarURL();
			}
			else if (item instanceof Tag) {
				id = ((Tag) item).getId();
				avatar = ((Tag) item).getUserUrl();
				if (tagName != null)
					tagName.setText(((Tag) item).getUserName());
			}

			if (Utils.isStringValid(avatar)) {
				MediaHelper.loadProfilePictureWithPlaceholder(
						profilePicture.getContext(),
						avatar,
						profilePicture
				);
			}
			else profilePicture.setImageResource(R.drawable.ic_profile_placeholder);

			if (fragmentListener != null) {
				Object[] values = fragmentListener.isObjectForTagSelected(id, fromInitiatives);
				boolean activate = false;
				@DrawableRes int bckgRes = R.drawable.shape_circle_orange_alpha;
				if (values != null) {
					activate = (boolean) values[0];
					if (values[1] != null)
						bckgRes = (int) values[1];
				}
				setSelection(activate, bckgRes, itemView);
			}
		}

		@Override
		public void onClick(View v) {
			if (fragmentListener != null) {
				boolean selected;
				if (selectionOverlay != null)
					selected = selectionOverlay.getVisibility() == View.GONE;
				else
					selected = !v.isSelected();

				// multiple selection for initiative not allowed
				if (fragmentListener instanceof InitiativesFragment.OnInitiativesFragmentInteractionListener) {
					InitiativesFragment.OnInitiativesFragmentInteractionListener listener = (InitiativesFragment.OnInitiativesFragmentInteractionListener) fragmentListener;
					if (listener.getHelperObject() != null && selected && listener.getHelperObject().haveTagsInitiativeRecipient() && fromInitiatives) {
						listener.getHelperObject().getActivity().showAlert(R.string.error_cpost_initiative_recipient);
						return;
					}
					else if (!selected && !fromInitiatives && mItem != null) {
						if (listener.getHelperObject() != null && listener.getHelperObject().haveTagsInitiativeRecipient()) {
							String id = null;
							if (mItem instanceof HLUserGeneric)
								id = ((HLUserGeneric) mItem).getId();
							else if (mItem instanceof Interest)
								id = ((Interest) mItem).getId();
							else if (mItem instanceof Tag)
								id = ((Tag) mItem).getId();

							if (id != null && listener.getHelperObject().checkInitiativeRecipientId(id)) {
								listener.getHelperObject().getActivity().showAlert(R.string.error_cpost_initiative_recipient_remove);
								return;
							}
						}
					}
				}

				setSelection(selected, R.drawable.shape_circle_orange_alpha, v);
			}

			if (adapterListener instanceof OnItemClickListener)
				((OnItemClickListener) adapterListener).onItemClick(mItem, fromInitiatives);
			else
				adapterListener.onItemClick(mItem);
		}

		void setSelection(boolean activate, @DrawableRes int bckgRes, View view) {
			if (selectionOverlay != null) {
				if (activate)
					selectionOverlay.setBackgroundResource(bckgRes);
				selectionOverlay.setVisibility(activate ? View.VISIBLE : View.GONE);
			}
			else
				view.setSelected(activate);
		}
	}


	static class DividerVH extends RecyclerView.ViewHolder {

		public DividerVH(View itemView) {
			super(itemView);
		}
	}


	public interface OnItemClickListener extends BasicAdapterInteractionsListener {
		void onItemClick(Object object, boolean fromInitiatives);
	}


	//region == Getters and setters ==

	public static void setFromInitiatives(boolean fromInitiatives) {
		TagAdapter.fromInitiatives = fromInitiatives;
	}

	//endregion

}
