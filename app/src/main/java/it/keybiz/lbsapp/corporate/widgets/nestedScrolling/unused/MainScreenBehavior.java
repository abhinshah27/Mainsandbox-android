package it.keybiz.lbsapp.corporate.widgets.nestedScrolling.unused;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;

// INFO: 2019-05-22    I couldn't check, but we might need this
public class MainScreenBehavior extends CoordinatorLayout.Behavior {

    private View recyclerView;
    private View appBarLayoutView;

    public MainScreenBehavior(){}

    public MainScreenBehavior(Context context){}

    /**
     * Default constructor for inflating a CommentListBehavior from layout.
     */
    public MainScreenBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Extract any custom attributes out
        // preferably prefixed with behavior_ to denote they
        // belong to a behavior
    }

    @Override
    public boolean layoutDependsOn(
            CoordinatorLayout parent, View child, View dependency) {
        // List the toolbar container as a dependency to ensure that it will
        // always be laid out before the child (which depends on the toolbar
        // container's height in onLayoutChild() below).

        // INFO: 2019-05-22
//        return dependency.getId() == R.id.app_bar_layout;
        return false;
    }

    @Override
    public boolean onLayoutChild(@NonNull CoordinatorLayout parent, @NonNull View child, int layoutDirection) {


        // First layout the child as normal.
        parent.onLayoutChild(child, layoutDirection);

        // INFO: 2019-05-22
//        if ( appBarLayoutView == null ){
//            appBarLayoutView = parent.findViewById(R.id.app_bar_layout);
//        }
//
//        if ( recyclerView == null ){
//            recyclerView = parent.findViewById(R.id.posts_recycler_view);
//        }

        ViewCompat.offsetTopAndBottom(child, appBarLayoutView.getHeight());

        return true;
    }

}
