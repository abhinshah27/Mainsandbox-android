/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.base;


import android.view.View;

import it.keybiz.lbsapp.corporate.models.HLUser;

/**
 * @author mbaldrighi on 1/24/2018.
 */
public interface BasicAdapterInteractionsListener {
	void onItemClick(Object object);
	void onItemClick(Object object, View view);
	HLUser getUser();
}
