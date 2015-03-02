package com.example.photogallery;

import android.app.IntentService;
import android.content.Intent;

public class PollService extends IntentService {

	private static final String TAG = "PollService";
	
	public PollService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.i(TAG, "Received an intent: " + intent);
	}

}
