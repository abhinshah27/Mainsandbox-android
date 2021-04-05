/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.widgets;

import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorRes;

import com.google.android.material.snackbar.Snackbar;

/**
 * @author mbaldrighi on 10/5/2017.
 */
public class ColoredSnackBar {

    private static final int red = android.R.color.holo_red_dark;
    private static final int green = android.R.color.holo_green_dark;
    private static final int blue = android.R.color.holo_blue_dark;
    private static final int orange = android.R.color.holo_orange_light;

    private static View getSnackBarLayout(Snackbar snackbar) {
        if (snackbar != null) {
            return snackbar.getView();
        }
        return null;
    }

    private static Snackbar colorSnackBar(Snackbar snackbar, @ColorRes int colorId,
                                          int textColor, int actionColor) {
        View snackBarView = getSnackBarLayout(snackbar);
        if (snackBarView != null) {
            snackBarView.setBackgroundResource(colorId);
            TextView tv = snackBarView.findViewById(com.google.android.material.R.id.snackbar_text);
            tv.setTextColor(textColor);
            snackbar.setActionTextColor(actionColor);
        }

        return snackbar;
    }

    public static Snackbar info(Snackbar snackbar) {
        return colorSnackBar(snackbar, blue, Color.WHITE, Color.WHITE);
    }

    public static Snackbar warning(Snackbar snackbar) {
        return colorSnackBar(snackbar, orange, Color.WHITE, Color.WHITE);
    }

    public static Snackbar alert(Snackbar snackbar) {
        return colorSnackBar(snackbar, red, Color.WHITE, Color.WHITE);
    }

    public static Snackbar confirm(Snackbar snackbar) {
        return colorSnackBar(snackbar, green, Color.WHITE, Color.WHITE);
    }
}
