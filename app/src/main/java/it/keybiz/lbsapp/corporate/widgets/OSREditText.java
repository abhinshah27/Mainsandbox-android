/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;

import it.keybiz.lbsapp.corporate.R;

/**
 * @author mbaldrighi on 10/5/2017.
 */
public class OSREditText extends androidx.appcompat.widget.AppCompatEditText {

    private Drawable dStart, dTop, dEnd, dBottom;
    private boolean wantsSpellChecker;

    public OSREditText(Context context) {
        super(context);
        init(null);
    }

    public OSREditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public OSREditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        if (attrs!=null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.OSREditText);
            String fontName = a.getString(R.styleable.OSREditText_osRegEdit);
            if (fontName!=null) {
                Typeface myTypeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/"+fontName);
                setTypeface(myTypeface);
            }
            a.recycle();
        }
    }


    @Override
    public void setCompoundDrawablesRelative(Drawable start, Drawable top, Drawable end, Drawable bottom) {

        if (start != null)
            dStart = start;
        if (top != null)
            dTop = top;
        if (end != null)
            dEnd = end;
        if (bottom != null)
            dBottom = bottom;

        super.setCompoundDrawablesRelative(start, top, end, bottom);
    }


    @NonNull
    @Override
    public Drawable[] getCompoundDrawablesRelative() {
        return super.getCompoundDrawablesRelative();
    }


    @Override
    protected void finalize() throws Throwable {
        dStart  = null;
        dTop    = null;
        dEnd    = null;
        dBottom = null;

        super.finalize();
    }


    @Override
    public boolean isSuggestionsEnabled() {
        return wantsSpellChecker && super.isSuggestionsEnabled();

    }

    public void setWantsSpellChecker(boolean wantsSpellChecker) {
        this.wantsSpellChecker = wantsSpellChecker;
    }
}
