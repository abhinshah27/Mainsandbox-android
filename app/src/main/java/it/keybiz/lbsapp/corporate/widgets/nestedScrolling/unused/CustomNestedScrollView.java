package it.keybiz.lbsapp.corporate.widgets.nestedScrolling.unused;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class CustomNestedScrollView extends NestedScrollView {

    public CustomNestedScrollView(@NonNull Context context) {
        super(context);
    }

    public CustomNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }



    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {

        if ( target instanceof RecyclerView ){

            final RecyclerView rv = (RecyclerView) target;

            if ((dy > 0 && isRvScrolledToTop(rv)) /* || (dy > 0 && !isNsvScrolledToBottom(this)) */ ) {
                // The NestedScrollView should steal the scroll event away from the
                // RecyclerView if: (1) the user is scrolling their finger down and the
                // RecyclerView is scrolled to the top of its content, or (2) the user
                // is scrolling their finger up and the NestedScrollView is not scrolled
                // to the bottom of its content.
                scrollBy(0, dy);
                consumed[1] = dy;
                return;
            }
        }

        super.onNestedPreScroll(target, dx, dy, consumed);
    }

    /**
     * Returns true iff the {@link RecyclerView} is scrolled to the
     * top of its content (i.e. its first item is completely visible).
     */
    private static boolean isRvScrolledToTop(RecyclerView rv) {
        final LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
        return lm.findFirstVisibleItemPosition() == 0
                && lm.findViewByPosition(0).getTop() == 0;
    }
}
