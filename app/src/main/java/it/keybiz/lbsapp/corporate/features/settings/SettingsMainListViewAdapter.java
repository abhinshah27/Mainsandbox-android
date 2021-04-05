/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.settings;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.models.HLIdentity;
import it.keybiz.lbsapp.corporate.models.enums.SecurityEntriesEnum;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 3/19/2018.
 */
public class SettingsMainListViewAdapter extends ArrayAdapter<Object> {

	private static final int TYPE_STRING = 0;
	private static final int TYPE_IDENTITY = 1;

	private @LayoutRes int resourceId;
	private SettingsInnerEntriesFragment.ViewType mViewType;

	public SettingsMainListViewAdapter(@NonNull Context context, int resource, @NonNull List<Object> objects,
	                                   @Nullable SettingsInnerEntriesFragment.ViewType viewType) {
		super(context, resource, objects);

		resourceId = resource;
		mViewType = viewType;
	}

	@NonNull
	@Override
	public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		Object entry = getItem(position);

		if (convertView == null) {
			convertView = LayoutInflater.from(getContext()).inflate(resourceId, parent, false);
		}

		TextView text = convertView.findViewById(R.id.action_text);
		String txt = null;
		if (entry != null) {
			if (getItemViewType(position) == TYPE_STRING)
				txt = ((String) entry);
			else if (getItemViewType(position) == TYPE_IDENTITY)
				txt = getContext().getString(R.string.settings_main_manage, ((HLIdentity) entry).getName());

			if (Utils.isStringValid(txt)) {
				text.setText(txt);

				// enables light gray background if row has even position number
				convertView.setActivated((position % 2) == 0);

				if (mViewType != null && mViewType == SettingsInnerEntriesFragment.ViewType.SECURITY) {
					SecurityEntriesEnum entryEnum = SecurityEntriesEnum.toEnum(position);
					if (entryEnum != null && entryEnum == SecurityEntriesEnum.DELETE_ACCOUNT)
						Utils.applyFontToTextView(text, R.string.fontSemiBold);
				}
			}
		}


		return convertView;
	}


	@Override
	public int getItemViewType(int position) {
		Object obj = getItem(position);
		if (obj instanceof String)
			return TYPE_STRING;
		else if (obj instanceof HLIdentity)
			return TYPE_IDENTITY;

		return super.getItemViewType(position);
	}

	@Override
	public int getViewTypeCount() {
		return 2;
	}
}
