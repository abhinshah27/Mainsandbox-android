/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;

import it.keybiz.lbsapp.corporate.R;

/**
 * @author mbaldrighi on 10/5/2017.
 */
public class OSBTextView extends androidx.appcompat.widget.AppCompatTextView {

    public OSBTextView(Context context) {
        super(context);
        init(null);
    }

    public OSBTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public OSBTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        if (attrs!=null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.OSBTextView);
            String fontName = a.getString(R.styleable.OSBTextView_osBold);
            if (fontName!=null) {
                Typeface myTypeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/"+fontName);
                setTypeface(myTypeface);
            }
            a.recycle();
        }
    }

}
