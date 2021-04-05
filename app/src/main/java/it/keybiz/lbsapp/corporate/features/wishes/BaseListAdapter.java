/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.wishes;

import android.content.Context;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.request.RequestOptions;

import java.util.List;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.models.WishListElement;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.media.MediaHelper;

/**
 * @author mbaldrighi on 2/16/2018.
 */
public class BaseListAdapter extends ArrayAdapter<WishListElement> {

	private static final int TYPE_TITLE = 0;
	private static final int TYPE_STRING = 1;
	private static final int TYPE_ICON = 2;

	private String date = null;

	private boolean withIcons = false;

	public BaseListAdapter(@NonNull Context context, @NonNull List<WishListElement> objects, boolean withIcons) {
		super(context, 0, objects);

		this.withIcons = withIcons;
	}


	@NonNull
	@Override
	public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		WishListElement object = getItem(position);

		BaseListItemVH viewHolder;
		int type = getItemViewType(position);
		if (convertView == null) {
			@LayoutRes int layout = R.layout.item_wish_base_list;
			if (type == TYPE_STRING)
				layout = R.layout.item_wish_base_list_string;
			convertView = LayoutInflater.from(getContext()).inflate(layout, parent, false);
			viewHolder = new BaseListItemVH(convertView);
			convertView.setTag(viewHolder);
		} else
			viewHolder = (BaseListItemVH) convertView.getTag();


		if (parent instanceof ListView && object != null)
			((ListView) parent).setItemChecked(position, object.isSelected());
		viewHolder.setItem(getContext(), object, date, type);

		return convertView;
	}

	@Override
	public int getItemViewType(int position) {
		if (withIcons) {
			return TYPE_ICON;
		}
		else {
			WishListElement element = getItem(position);
			if (element != null) {
				if (element.hasNextNavigationID() && element.getNextNavigationID().equals(Constants.KEY_NAV_ID_SPECIFIC_DATE))
					return TYPE_STRING;
			}
			return TYPE_TITLE;
		}
	}

	@Override
	public int getViewTypeCount() {
		return 4;
	}

	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}


	//region == ViewHolders ==

	static class BaseListItemVH {

		private View itemView;
		private TextView action;
		private ImageView icon;

		private TextView addedString;

		public BaseListItemVH(View itemView) {

			this.itemView = itemView;

			action = itemView.findViewById(R.id.action_text);
			icon = itemView.findViewById(R.id.icon);

			addedString = itemView.findViewById(R.id.action_added_string);
		}

		void setItem(Context context, WishListElement element, String date, int type) {
			if (element != null) {
				action.setText(element.getName());

				if (addedString != null) {
					if (type == TYPE_STRING && Utils.isStringValid(date)) {
						addedString.setText(date);
						addedString.setVisibility(View.VISIBLE);
					}
					else addedString.setVisibility(View.GONE);
				}

				if (type == TYPE_ICON) {
					if (Utils.hasLollipop())
						icon.setImageTintList(ContextCompat.getColorStateList(context, R.color.color_state_wish_item_list));
					else if (element.isSelected())
						icon.setColorFilter(Utils.getColor(context, R.color.white), PorterDuff.Mode.SRC_ATOP);
					else
						icon.setColorFilter(Utils.getColor(context, R.color.black_87), PorterDuff.Mode.SRC_ATOP);

					MediaHelper.loadPictureWithGlide(context, element.getAvatarURL(), RequestOptions.centerInsideTransform(),
							0, 0, icon);
					icon.setVisibility(View.VISIBLE);
				}
				else
					icon.setVisibility(View.GONE);
			}
		}

	}

	//endregion

}
