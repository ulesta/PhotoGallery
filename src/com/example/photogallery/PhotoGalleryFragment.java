package com.example.photogallery;

import java.util.ArrayList;

import model.GalleryItem;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

public class PhotoGalleryFragment extends Fragment {
	private static final String TAG = "PhotoGalleryFragment";
	
	GridView mGridView;
	FrameLayout mLoading;
	
	ArrayList<GalleryItem> mItems;
	
	ThumbnailDownloader<ImageView> mThumbnailThread;
	
	ArrayAdapter<GalleryItem> adapter;
	
	// Page Counter
	int pageCount = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
		setHasOptionsMenu(true);
		
		mItems = new ArrayList<GalleryItem>();
		adapter = new ArrayAdapter<GalleryItem>(getActivity(), android.R.layout.simple_gallery_item, mItems);
		
		updateItems();
		
		// Not needed with Picasso implementation
		/*mThumbnailThread = new ThumbnailDownloader<ImageView>(new Handler());
		mThumbnailThread.setListener(new Listener<ImageView>() {

			@Override
			public void onThumbnailDownload(ImageView token, Bitmap thumbnail) {
				// this guard ensures we are not setting an image on a stale ImageView
				if (isVisible()) {
					token.setImageBitmap(thumbnail);
					
					// Preload images after displaying [Ch27 - Challenge 2]
					
				}
			}
		});
		
		mThumbnailThread.start();
		mThumbnailThread.getLooper();
		Log.i(TAG, "Background thread started!");*/
		
		Intent i = new Intent(getActivity(), PollService.class);
		getActivity().startService(i);
	}
	
	public void updateItems() {
		new FetchItemsTask().execute(1);
		showQueryToast(PreferenceManager
				.getDefaultSharedPreferences(getActivity()).getString(FlickrFetchr.PREF_SEARCH_QUERY, null));
	}

	@Override
	@TargetApi(11)
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// TODO Auto-generated method stub
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.fragment_photo_gallery, menu);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			// Pull out the SearchView
			MenuItem searchItem = menu.findItem(R.id.menu_item_search);
			SearchView searchView = (SearchView)searchItem.getActionView();
			
			// Get the data from our searchable.xml as a SearchableInfo
			SearchManager searchManager = (SearchManager)getActivity()
					.getSystemService(Context.SEARCH_SERVICE);
			ComponentName name = getActivity().getComponentName();
			SearchableInfo searchInfo = searchManager.getSearchableInfo(name);
			
			searchView.setSearchableInfo(searchInfo);
			
		}
		
	}
	
	public void showQueryToast(String query) {
		int duration = Toast.LENGTH_LONG;
		String text = "Showing results for:\n";
		
		Toast toast = Toast.makeText(getActivity(), text + query, duration);
		toast.show();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_item_search:
			getActivity().onSearchRequested();
			return true;
		case R.id.menu_item_clear:
			PreferenceManager.getDefaultSharedPreferences(getActivity())
			.edit()
			.putString(FlickrFetchr.PREF_SEARCH_QUERY, null)
			.commit();
			updateItems();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
		
		mGridView = (GridView) v.findViewById(R.id.gridView);
		mLoading = (FrameLayout) v.findViewById(R.id.loadingPanel);

		setupAdapter(adapter);
		
		// Fires up background thread
		
		return v;
	}
	
	
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		// Without Picasso
		// mThumbnailThread.quit();
		Log.i(TAG, "Background thread destroyed!");
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		
		// Not needed with Picasso implementation
		// mThumbnailThread.clearQueue();
	}

	/* Third param in generic is the type of result produced by AsyncTask, it sets the value
	 * returned by AsyncTask. 
	 * 
	 * First generic param: specifies input parameters i.e. doInBackground(String...).
	 * 
	 * Second generic param: specify type for sending progress updates
	 * 
	 *  */
	private class FetchItemsTask extends AsyncTask<Integer, Void, ArrayList<GalleryItem>> {
		
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		/* Note: do not update UI on background thread, memory corruption -> unsafe
		 */
		@Override
		protected ArrayList<GalleryItem> doInBackground(Integer... params) {
			Activity activity = getActivity();
			if (activity == null) {
				return new ArrayList<GalleryItem>();
			}
			
			// get query stashed in SharedPreferences
			String query = PreferenceManager.getDefaultSharedPreferences(activity)
					.getString(FlickrFetchr.PREF_SEARCH_QUERY, null);
			
			if (query != null) {
				return new FlickrFetchr().search(query);
			} else {
				return new FlickrFetchr().fetchItems(1);
			}
		}

		/* Happens after doInBackground is complete, also executed on main thread hence safe
		 * to update UI in it. Accepts the list fetched insdie doInBackground(..)
		 */
		@Override
		protected void onPostExecute(ArrayList<GalleryItem> items) {
			mLoading.setVisibility(View.INVISIBLE);
			mItems = items;
			setupAdapter(adapter);
			// limit to 2 pages
			/*if (pageCount < 2) {
				new FetchItemsTask().execute(++pageCount);
			}*/
		}
		
		
		
	}
	
	private void setupAdapter(ArrayAdapter<GalleryItem> adapter) {
		/* First do checks -- recall: fragments can exist unattached from any activity.
		 * If out fragment is not attached, operations that rely on that activity like creating
		 * our own ArrayAdapter will fail
		 */
		if (getActivity() == null || mGridView == null) {
			return;
		}
		
		if (mItems != null) {
			adapter.notifyDataSetChanged();
			Log.d("Size of mItems", ""+mItems.size());
			mGridView.setAdapter(new GalleryItemAdapter(mItems));
		} else {
			mGridView.setAdapter(null);
		}
		
	}
	
	private class GalleryItemAdapter extends ArrayAdapter<GalleryItem>{
		
		public GalleryItemAdapter(ArrayList<GalleryItem> items) {
			super(getActivity(), 0, items);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = getActivity().getLayoutInflater().inflate(R.layout.gallery_item, parent, false);
			}
			
			ImageView imageView = (ImageView) convertView.findViewById(R.id.gallery_item_imageView);
			imageView.setImageResource(R.drawable.hotair);
			
			GalleryItem item = getItem(position);
			
			// Without Picasso
			// mThumbnailThread.queueThumbnail(imageView, item.getmUrl());
			
			// Picasso implementaion
			// Source: http://www.bignerdranch.com/blog/solving-the-android-image-loading-problem-volley-vs-picasso/
			Picasso.with(getContext()).load(item.getmUrl()).into(imageView);
			
			/* Pre-loading into cache */
			/*ArrayList<String> urls = new ArrayList<String>();
			if (mGridView.getFirstVisiblePosition() == position) {
				// take prev. 10 images
				for (int i = position-1; i >= 0 && position - i <= 10; i--) {
					urls.add(mItems.get(i).getmUrl());
				}	
				mThumbnailThread.preloadCache(urls);
			} else if (mGridView.getLastVisiblePosition() == position); {
				// take next 10 images
				for (int i = position+1; i <= mItems.size() && i - position <= 10; i++) {
					urls.add(mItems.get(i).getmUrl());
				}
				mThumbnailThread.preloadCache(urls);
			}*/
			
			return convertView;
		}
	}

}

