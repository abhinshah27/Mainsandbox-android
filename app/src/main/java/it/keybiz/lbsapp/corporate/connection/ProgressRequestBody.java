/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.connection;

import androidx.annotation.NonNull;

import java.io.File;

import it.keybiz.lbsapp.corporate.utilities.LogUtils;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

/**
 * @author mbaldrighi on 5/12/2018.
 */
public class ProgressRequestBody extends RequestBody {

	private static final int SEGMENT_SIZE = 2048; // okio.Segment.SIZE

	private final File file;
	private final ProgressListener listener;
	private final String contentType;

	public ProgressRequestBody(File file, String contentType, ProgressListener listener) {
		this.file = file;
		this.contentType = contentType;
		this.listener = listener;
	}

	@Override
	public long contentLength() {
		return file != null ? file.length() : 0L;
	}

	@Override
	public MediaType contentType() {
		return MediaType.parse(contentType);
	}

	@Override
	public void writeTo(@NonNull BufferedSink sink) {
		Source source = null;
		try {
			source = Okio.source(file);
			double total = 0;
			double read;

			long size = contentLength();
			if (size > 0) {
				while ((read = source.read(sink.buffer(), SEGMENT_SIZE)) != -1) {
					total += read;
					sink.flush();

					double div = total / size;
					double div100 = div * 100;
					int progress = (int) div100;
					LogUtils.v("UPLOAD FILE PROGRESS", "total: " + total + " size: " + size + " -> percent: " + div100 + " > " + progress);
					this.listener.transferred(progress);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			Util.closeQuietly(source);
		}
	}

	public interface ProgressListener {
		void transferred(long num);
	}

}
