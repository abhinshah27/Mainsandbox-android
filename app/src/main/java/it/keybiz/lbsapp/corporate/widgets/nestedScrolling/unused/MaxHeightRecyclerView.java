package it.keybiz.lbsapp.corporate.widgets.nestedScrolling.unused;

import android.content.Context;
import android.util.AttributeSet;

import androidx.recyclerview.widget.RecyclerView;

/**
 * A {@link RecyclerView} with an optional maximum height.
 */
public class MaxHeightRecyclerView extends RecyclerView {
    private int mMaxHeight = -1;

    public MaxHeightRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        final int mode = MeasureSpec.getMode(heightSpec);
        final int height = MeasureSpec.getSize(heightSpec);
        if (mMaxHeight >= 0 && (mode == MeasureSpec.UNSPECIFIED || height > mMaxHeight)) {
            heightSpec = MeasureSpec.makeMeasureSpec(mMaxHeight, MeasureSpec.AT_MOST);
        }
        super.onMeasure(widthSpec, heightSpec);
    }

    /**
     * Sets the maximum height for this recycler view.
     */
    public void setMaxHeight(int maxHeight) {
        if (mMaxHeight != maxHeight) {
            mMaxHeight = maxHeight;
            requestLayout();
        }
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        boolean result = super.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
        return result;
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow, int type) {
        boolean result = super.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type);
        return result;
    }

}