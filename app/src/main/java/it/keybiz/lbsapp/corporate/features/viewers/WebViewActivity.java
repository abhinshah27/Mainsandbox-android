/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.features.viewers;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.base.HLActivity;
import it.keybiz.lbsapp.corporate.utilities.AnalyticsUtils;
import it.keybiz.lbsapp.corporate.utilities.Constants;
import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import it.keybiz.lbsapp.corporate.utilities.Utils;
import it.keybiz.lbsapp.corporate.utilities.helpers.ShareHelper;

/**
 * A screen dedicated to the visualization of web content.
 */
public class WebViewActivity extends HLActivity implements ShareHelper.ShareableProvider {

	public static final String LOG_TAG = WebViewActivity.class.getCanonicalName();

	private TextView toolbarTitle;
	private WebView webView;
	private String intentUrl, title;
	private String postOrMessageId;
	private boolean fromChat;

	private ShareHelper mShareHelper;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_webview);
		setRootContent(R.id.root_content);
		setProgressIndicator(R.id.generic_progress_indicator);

		manageIntent();

		toolbarTitle = findViewById(R.id.toolbar_title);

		findViewById(R.id.back_arrow).setOnClickListener(v -> {
			finish();
			overridePendingTransition(R.anim.no_animation, R.anim.slide_out_down);
		});

		if (Utils.hasKitKat()) {
			if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE))
				WebView.setWebContentsDebuggingEnabled(true);
		}

		webView = findViewById(R.id.web_view);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.setWebViewClient(new WebViewClient() {

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				super.onPageStarted(view, url, favicon);

				setProgressMessage(R.string.webview_page_loading);
				showProgressIndicator(true);
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);

				showProgressIndicator(false);
			}

			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
				Toast.makeText(view.getContext(), R.string.error_generic_operation, Toast.LENGTH_SHORT).show();

				LogUtils.e(LOG_TAG, "Error loading url: " + intentUrl + "with error: " + description);
			}
		});

		webView.loadUrl(intentUrl);

		findViewById(R.id.share_btn).setOnClickListener(v -> {
			if (Utils.areStringsValid(mUser.getId(), intentUrl))
				mShareHelper.initOps(fromChat);
		});

		mShareHelper = new ShareHelper(this, this);
	}

	@Override
	protected void onResume() {
		super.onResume();

		AnalyticsUtils.trackScreen(this, AnalyticsUtils.FEED_WEBLINK);

		mShareHelper.onResume();

		toolbarTitle.setText(title);
	}

	@Override
	protected void onStop() {
		mShareHelper.onStop();

		super.onStop();
	}

	@Override
	protected void configureResponseReceiver() {}

	@Override
	protected void manageIntent() {
		Intent intent = getIntent();
		if (intent != null && intent.hasExtra(Constants.EXTRA_PARAM_1))
			intentUrl = intent.getStringExtra(Constants.EXTRA_PARAM_1);
		if (intent != null && intent.hasExtra(Constants.EXTRA_PARAM_2))
			title = intent.getStringExtra(Constants.EXTRA_PARAM_2);
		if (intent != null && intent.hasExtra(Constants.EXTRA_PARAM_3))
			postOrMessageId = intent.getStringExtra(Constants.EXTRA_PARAM_3);
		if (intent != null && intent.hasExtra(Constants.EXTRA_PARAM_4))
			fromChat = intent.getBooleanExtra(Constants.EXTRA_PARAM_4, false);
	}

	@Override
	public void onBackPressed() {

		if (webView.canGoBack())
			webView.goBack();
		else {
			finish();
			overridePendingTransition(R.anim.no_animation, R.anim.slide_out_down);
		}
	}


	//region == SHARE ==

	@Nullable
	@Override
	public View getProgressView() {
		return null;
	}

	@Override
	public void afterOps() {}

	@NotNull
	@Override
	public String getUserID() {
		return mUser.getId();
	}

	@NotNull
	@Override
	public String getPostOrMessageID() {
		return postOrMessageId;
	}

	//endregion

}

