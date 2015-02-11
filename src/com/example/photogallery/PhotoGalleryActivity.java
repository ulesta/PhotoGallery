package com.example.photogallery;

import android.app.SearchManager;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.util.Log;



public class PhotoGalleryActivity extends SingleFragmentActivity {

	private static final String TAG = "PhotoGalleryActivity";
	
	@Override
	protected Fragment createFragment() {
		return new PhotoGalleryFragment();
	}

	@Override
	/* Called when Search intent is activated since activity is SingleTop at launch */
	public void onNewIntent(Intent intent) {
		PhotoGalleryFragment fragment = (PhotoGalleryFragment)getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
		
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			Log.i(TAG, "Received a new search query: " + query);
		}
		
		fragment.updateItems();
	}
	
}
