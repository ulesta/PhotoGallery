package com.example.photogallery;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;



public class PhotoGalleryActivity extends SingleFragmentActivity {
	
	private static final String TAG = "PhotoGalleryActivity";
	
	private PhotoGalleryFragment fragment;
	
	@Override
	protected Fragment createFragment() {
		fragment = (PhotoGalleryFragment)getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
		return new PhotoGalleryFragment();
	}

	@Override
	/* Called when Search intent is activated since activity is SingleTop at launch */
	public void onNewIntent(Intent intent) {
		fragment = (PhotoGalleryFragment)getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
		
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			Log.i(TAG, "Received a new search query: " + query);
			
			// Calls SharedPreference -- simple persistence
			PreferenceManager.getDefaultSharedPreferences(this)
				.edit()
				.putString(FlickrFetchr.PREF_SEARCH_QUERY, query)
				.commit();
		}
		
		fragment.updateItems();
	}

	@Override
	public void startSearch(String initialQuery, boolean selectInitialQuery,
			Bundle appSearchData, boolean globalSearch) {
		// TODO Auto-generated method stub
		super.startSearch(initialQuery, selectInitialQuery, appSearchData, globalSearch);
	}
	
	
	
}
