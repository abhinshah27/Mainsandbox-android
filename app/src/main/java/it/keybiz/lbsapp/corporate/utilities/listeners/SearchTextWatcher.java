/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.utilities.listeners;

import android.text.Editable;
import android.text.TextWatcher;

import it.keybiz.lbsapp.corporate.features.globalSearch.GlobalSearchFragment;

/**
 * @author mbaldrighi on 11/12/2017.
 */
public class SearchTextWatcher implements TextWatcher {

	public enum SearchType { SINGLE_CHAR, CHARS_3 }
	private SearchType searchType;

	private OnQuerySubmitted mListener;

	private boolean textChanged;

	public SearchTextWatcher(OnQuerySubmitted listener) {
		searchType = SearchType.CHARS_3;
		mListener = listener;
	}

	public SearchTextWatcher(OnQuerySubmitted listener, SearchType type) {
		searchType = type;
		mListener = listener;
	}


	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		textChanged = true;
	}

	@Override
	public void afterTextChanged(Editable s) {
		if (textChanged) {
			if (searchType == SearchType.CHARS_3) {
				if (s.length() >= 3)
					mListener.onQueryReceived(s.toString());
				else if (mListener instanceof GlobalSearchFragment)
					mListener.onQueryReceived("");
			} else
				mListener.onQueryReceived(s.toString());

			textChanged = false;
		}
	}


	public void setListener(OnQuerySubmitted listener) {
		this.mListener = listener;
	}

	public void setSearchType(SearchType searchType) {
		this.searchType = searchType;
	}

	public interface OnQuerySubmitted {
		void onQueryReceived(String query);
	}
}
