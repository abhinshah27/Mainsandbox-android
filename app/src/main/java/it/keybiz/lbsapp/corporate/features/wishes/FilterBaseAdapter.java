/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.wishes;

import android.content.Context;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.models.WishListElement;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 2/16/2018.
 */
public class FilterBaseAdapter extends ArrayAdapter<WishListElement> {

	private Map<WishListElement, List<String>> filtersMap;

	private HLActivity activity;

	private boolean keyboardUp;

	public FilterBaseAdapter(@NonNull Context context, @NonNull List<WishListElement> objects,
	                         @NonNull Map<WishListElement, List<String>> filtersMap) {
		super(context, 0, objects);

		if (context instanceof HLActivity)
			activity = ((HLActivity) context);

		this.filtersMap = filtersMap;
	}


	@NonNull
	@Override
	public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		WishListElement object = getItem(position);

		BaseListItemVH viewHolder;
		if (convertView == null) {
			convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_wish_base_list_filter, parent, false);
			viewHolder = new BaseListItemVH(convertView);
			convertView.setTag(viewHolder);
		} else
			viewHolder = (BaseListItemVH) convertView.getTag();

		if (parent instanceof ListView && object != null)
			((ListView) parent).setItemChecked(position, object.isSelected());
		viewHolder.setItem(getContext(), object, filtersMap);

		return convertView;
	}

	public boolean isKeyboardUp() {
		return keyboardUp;
	}
	public void setKeyboardUp(boolean keyboardUp) {
		this.keyboardUp = keyboardUp;
	}


	//region == ViewHolders ==

	class BaseListItemVH implements View.OnClickListener {

		private TextView action;

		private View childLayout;
		private SwitchCompat filterSwitch;
		private LinearLayout filterWordsContainer, addFilterBtn;

		private EditText newFilterField;

		BaseListItemVH(View itemView) {
			action = itemView.findViewById(R.id.action_text);

			childLayout = itemView.findViewById(R.id.child_layout);
			filterSwitch = itemView.findViewById(R.id.filter_switch);
			filterWordsContainer = itemView.findViewById(R.id.filter_words_container);
			addFilterBtn = itemView.findViewById(R.id.add_filter_btn);
			newFilterField = itemView.findViewById(R.id.add_filter_word_et);
			newFilterField.setOnClickListener(this);
		}

		void setItem(final Context context, final WishListElement element, final Map<WishListElement, List<String>> filtersMap) {
			if (element != null) {
				action.setText(element.getName());

				childLayout.setVisibility(element.isSelected() ? View.VISIBLE : View.GONE);

				filterSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						filterWordsContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
					}
				});
				filterSwitch.setChecked(filterSwitch.isChecked());

				addFilterBtn.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						addFilterBtn.setVisibility(View.GONE);
						newFilterField.setVisibility(View.VISIBLE);
						newFilterField.requestFocus();
						Utils.openKeyboard(newFilterField);
					}
				});

				filterWordsContainer.setVisibility(filterSwitch.isChecked() ? View.VISIBLE : View.GONE);

				newFilterField.setFocusable(true);
				newFilterField.setFocusableInTouchMode(true);
				newFilterField.setClickable(true);
				newFilterField.setImeOptions(EditorInfo.IME_ACTION_NEXT);
				newFilterField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
						if (actionId == EditorInfo.IME_ACTION_NEXT) {
							String filter = newFilterField.getText().toString();
							final View newFilter = LayoutInflater.from(context).inflate(R.layout.item_wish_filter_word_remove, filterWordsContainer, false);
							final TextView tv = newFilter.findViewById(R.id.filter);
							tv.setText(filter);
							newFilter.setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									List<String> strings = filtersMap.get(element);
									if (strings == null || strings.isEmpty()) return;

									strings.remove(tv.getText().toString());

									filterWordsContainer.removeView(newFilter);
								}
							});
							filterWordsContainer.addView(newFilter, filterWordsContainer.getChildCount() - 1);

							newFilterField.setVisibility(View.GONE);
							addFilterBtn.setVisibility(View.VISIBLE);
							newFilterField.setText("");

							List<String> strings = filtersMap.get(element);
							if (strings == null)
								strings = new ArrayList<>();
							strings.add(filter);

							return true;
						}

						return false;
					}
				});
			}
		}

		@Override
		public void onClick(View v) {
			if (v.getId() == R.id.add_filter_word_et && Utils.isContextValid(activity)) {
				if (isKeyboardUp()) {
					Utils.closeKeyboard(activity);
					setKeyboardUp(false);
				}
				else {
					Utils.openKeyboard(activity);
					setKeyboardUp(true);
				}
			}
		}
	}

	//endregion

}
