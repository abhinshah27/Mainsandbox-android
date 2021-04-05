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
import it.keybiz.lbsapp.corporate.models.MarketPlace;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * @author mbaldrighi on 1/22/2018.
 */
public class MarketPlacesAdapter extends RecyclerView.Adapter<MarketPlacesAdapter.MarketPlaceVH> {

	private List<MarketPlace> items;
	private BasicAdapterInteractionsListener listener;


	public MarketPlacesAdapter(List<MarketPlace> items, BasicAdapterInteractionsListener listener) {
		this.items = items;
		this.listener = listener;
	}

	@NonNull
	@Override
	public MarketPlaceVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return new MarketPlaceVH(LayoutInflater.from(parent.getContext())
				.inflate(R.layout.item_marketplace, parent, false));
	}

	@Override
	public void onBindViewHolder(MarketPlaceVH holder, int position) {
		MarketPlace obj = items.get(position);

		if (obj != null) {
			holder.setMarketPlace(obj);
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


	class MarketPlaceVH extends RecyclerView.ViewHolder implements View.OnClickListener {

		private final View itemView;
		private final ImageView profilePicture;
		private final TextView name;
		private final TextView description;
		private final View overlay;

		private MarketPlace currentObject;

		MarketPlaceVH(View itemView) {
			super(itemView);

			profilePicture = itemView.findViewById(R.id.profile_picture);
			name = itemView.findViewById(R.id.name);
			description = itemView.findViewById(R.id.conversion_and_method);

			overlay = itemView.findViewById(R.id.overlay);

			(this.itemView = itemView).setOnClickListener(this);
		}

		void setMarketPlace(MarketPlace obj) {
			currentObject = obj;
			MediaHelper.loadPictureWithGlide(profilePicture.getContext(), obj.getAvatarURL(), profilePicture);

			name.setText(obj.getName());

			description.setText(
					obj.getDescription().replace("@VALUE", obj.getReadableMoneyConverted())
			);

			overlay.setVisibility(!obj.isConversionValid() ? View.VISIBLE : View.GONE);

			this.itemView.setSelected(obj.isSelected());
		}


		@Override
		public void onClick(View view) {
			if (currentObject.isConversionValid())
				listener.onItemClick(currentObject, view);
		}
	}


	//region == Getters and setters ==

	//endregion

}
