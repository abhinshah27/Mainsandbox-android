/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.models.enums;

import android.content.Context;

import java.io.Serializable;

import it.keybiz.lbsapp.corporate.R;
import it.keybiz.lbsapp.corporate.utilities.Utils;

/**
 * @author mbaldrighi on 12/28/2017.
 */
public enum RequestsStatusEnum implements Serializable {

	NOT_AVAILABLE("notAvailable"),
	PENDING("pending"),
	AUTHORIZED("authorized"),
	DECLINED("declined");

	private String value;

	RequestsStatusEnum(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return this.getValue();
	}

	public static RequestsStatusEnum toEnum(String value) {
		if (!Utils.isStringValid(value))
			return null;

        RequestsStatusEnum[] statuses = RequestsStatusEnum.values();
		for (RequestsStatusEnum status : statuses)
			if (status.getValue().equalsIgnoreCase(value))
				return status;
		return null;
	}

	public static String getReadableForm(Context context, String value) {
		RequestsStatusEnum status = toEnum(value);
		if (status != null) {
			switch (status) {
				case AUTHORIZED:
					return context.getString(R.string.status_authorized);
				case DECLINED:
					return context.getString(R.string.status_declined);
				case PENDING:
					return context.getString(R.string.status_pending);
				case NOT_AVAILABLE:
					return context.getString(R.string.status_notavailable);
			}
		}

		return null;
	}

}
