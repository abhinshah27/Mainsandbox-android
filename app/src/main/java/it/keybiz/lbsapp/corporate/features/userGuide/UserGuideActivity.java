/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.userGuide;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.features.HomeActivity;
import it.keybiz.lbsapp.corporate.models.HLUser;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.SharedPrefsUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * A screen showing introductory guide to Highlanders app.
 *
 * Consider relating to <a href="https://guides.codepath.com/android/Working-with-the-ImageView">this link</a> and
 * <a href="https://androidsnippets.wordpress.com/2012/10/25/how-to-scale-a-bitmap-as-per-device-width-and-height">this link</a>
 * to improve UX with scaled bitmaps.
 */
public class UserGuideActivity extends HLActivity implements View.OnClickListener {

	public static final String LOG_TAG = UserGuideActivity.class.getCanonicalName();

	private static final int PAGES_COUNT = 5;
	private static final int MAX_INDEX = PAGES_COUNT - 1;

	public enum ViewType { FIRST_OPEN, SETTINGS }
	private ViewType mType;

	private View bottomBar;
	private View backLayout, nextLayout;
	private TextView nextBtn;

	int currentItem = 0;

	private ViewPager mPager;

	private Drawable drawBack, drawNext;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_user_guide_pager);
		setRootContent(R.id.root_content);

		View decorView = getWindow().getDecorView();
		// Hide both the navigation bar and the status bar.
		// SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher
		int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE;
		decorView.setSystemUiVisibility(uiOptions);
		setImmersiveValue(true);

		manageIntent();

		bottomBar = findViewById(R.id.bottom_bar);

		backLayout = findViewById(R.id.btn_back);
		nextLayout = findViewById(R.id.btn_next);
		TextView backBtn = findViewById(R.id.btn_back_text);
		drawBack = backBtn.getCompoundDrawablesRelative()[0];
		nextBtn = findViewById(R.id.btn_next_text);
		drawNext = nextBtn.getCompoundDrawablesRelative()[2];

		mPager = findViewById(R.id.pager);
		mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

			@Override
			public void onPageSelected(int position) {
				currentItem = position;

				backLayout.setEnabled(position != 0);
				backLayout.setVisibility(position == 0 ? View.INVISIBLE : View.VISIBLE);
				nextLayout.setActivated(position == MAX_INDEX);
				nextBtn.setText(position == MAX_INDEX ? R.string.action_go_to_app : R.string.action_next);


				if (!Utils.hasMarshmallow())
					drawNext.setColorFilter(
							Utils.getColor(
									UserGuideActivity.this,
									position == MAX_INDEX ? R.color.white : R.color.colorAccent
							),
							PorterDuff.Mode.SRC_ATOP
					);
				else
					nextBtn.setCompoundDrawableTintList(getResources().getColorStateList(R.color.state_list_guide_button, null));
			}

			@Override
			public void onPageScrollStateChanged(int state) {}
		});
		mPager.setAdapter(new FragmentStatePagerAdapter(getSupportFragmentManager()) {

			@Override
			public Fragment getItem(int position) {
				switch (position) {
					case 0:
						return PhotoGuideFragment.newInstance(R.drawable.user_guide_0);
					case 1:
						return PhotoGuideFragment.newInstance(R.drawable.user_guide_1);
					case 2:
						return PhotoGuideFragment.newInstance(R.drawable.user_guide_2);
					case 3:
						return PhotoGuideFragment.newInstance(R.drawable.user_guide_3);
					case 4:
						return PhotoGuideFragment.newInstance(R.drawable.user_guide_4);
				}
				return null;
			}

			@Override
			public int getCount() {
				return PAGES_COUNT;
			}
		});

		mPager.setCurrentItem(0);
	}

	@Override
	protected void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(this, AnalyticsUtils.SETTINGS_PRIVACY_USER_GUIDE);

		bottomBar.setVisibility(mType == ViewType.SETTINGS ? View.GONE : View.VISIBLE);

		backLayout.setVisibility(currentItem == 0 ? View.INVISIBLE : View.VISIBLE);
		backLayout.setEnabled(currentItem != 0);
		nextLayout.setActivated(currentItem == MAX_INDEX);
		nextBtn.setText(currentItem == MAX_INDEX ? R.string.action_go_to_app : R.string.action_next);


		if (!Utils.hasMarshmallow()) {
			drawNext.setColorFilter(
					Utils.getColor(
							UserGuideActivity.this,
							currentItem == MAX_INDEX ? R.color.white : R.color.colorAccent
					),
					PorterDuff.Mode.SRC_ATOP
			);
			drawBack.setColorFilter(Utils.getColor(this, R.color.colorAccent), PorterDuff.Mode.SRC_ATOP);
		} else
			nextBtn.setCompoundDrawableTintList(getResources().getColorStateList(R.color.state_list_guide_button, null));

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btn_back:
				mPager.setCurrentItem(--currentItem);
				break;

			case R.id.btn_next:
				if (currentItem == MAX_INDEX && v.isActivated()) {

					SharedPrefsUtils.setGuideSeen(this, true);
					new HLUser().write(realm);

					startActivity(new Intent(this, HomeActivity.class));
					finish();
					overridePendingTransition(0, R.anim.alpha_out);
				}
				else {
					mPager.setCurrentItem(++currentItem);
				}
				break;
		}
	}

	@Override
	protected void configureResponseReceiver() {}

	@Override
	protected void manageIntent() {
		Intent intent = getIntent();
		if (intent != null) {
			if (intent.hasExtra(Constants.EXTRA_PARAM_1))
				mType = (ViewType) intent.getSerializableExtra(Constants.EXTRA_PARAM_1);
		}
	}





	public Bitmap scaleToActualAspectRatio(Bitmap bitmap) {
		if (bitmap != null) {
			boolean flag = true;

			Point newPoint = new Point();
			getWindowManager().getDefaultDisplay()
					.getSize(newPoint);

			int deviceWidth = newPoint.x;
			int deviceHeight = newPoint.y;
			int bitmapHeight = bitmap.getHeight(); // 563
			int bitmapWidth = bitmap.getWidth(); // 900

			// aSCPECT rATIO IS Always WIDTH x HEIGHT rEMEMMBER 1024 x 768

			if (bitmapWidth > deviceWidth) {
				flag = false;

				// scale According to WIDTH
				int scaledWidth = deviceWidth;
				int scaledHeight = (scaledWidth * bitmapHeight) / bitmapWidth;

				try {
					if (scaledHeight > deviceHeight)
						scaledHeight = deviceHeight;

					bitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth,
							scaledHeight, true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			if (flag) {
				if (bitmapHeight > deviceHeight) {
					// scale According to HEIGHT
					int scaledHeight = deviceHeight;
					int scaledWidth = (scaledHeight * bitmapWidth)
							/ bitmapHeight;

					try {
						if (scaledWidth > deviceWidth)
							scaledWidth = deviceWidth;

						bitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth,
								scaledHeight, true);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return bitmap;
	}



}

