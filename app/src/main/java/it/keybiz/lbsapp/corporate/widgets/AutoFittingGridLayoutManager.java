/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.widgets;

import android.content.Context;

import androidx.annotation.Px;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * @author mbaldrighi on 3/2/2018.
 */
public class AutoFittingGridLayoutManager extends GridLayoutManager {

	@Px private int columnWidth;
	private boolean columnWidthChanged = true;

	public AutoFittingGridLayoutManager(Context context, int columnWidth) {
		super(context, 1);

		setColumnWidth(columnWidth);
	}

	public void setColumnWidth(int newColumnWidth) {
		if (newColumnWidth > 0 && newColumnWidth != columnWidth) {
			columnWidth = newColumnWidth;
			columnWidthChanged = true;
		}
	}

	@Override
	public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
		if (columnWidthChanged && columnWidth > 0) {
			int totalSpace;
			if (getOrientation() == RecyclerView.VERTICAL) {
				totalSpace = getWidth() - getPaddingRight() - getPaddingLeft();
			} else {
				totalSpace = getHeight() - getPaddingTop() - getPaddingBottom();
			}
			int spanCount = Math.max(1, totalSpace / columnWidth);
			setSpanCount(spanCount);
			columnWidthChanged = false;
		}
		super.onLayoutChildren(recycler, state);
	}

}
