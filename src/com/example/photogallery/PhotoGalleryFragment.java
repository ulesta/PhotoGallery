package com.example.photogallery;

import java.util.ArrayList;

import model.GalleryItem;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;

public class PhotoGalleryFragment extends Fragment {
	private static final String TAG = "PhotoGalleryFragment";
	
	GridView mGridView;
	FrameLayout mLoading;
	
	ArrayList<GalleryItem> mItems;
	
	ArrayAdapter<GalleryItem> adapter;
	
	// Page Counter
	int pageCount = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		
		mItems = new ArrayList<GalleryItem>();
		adapter = new ArrayAdapter<GalleryItem>(getActivity(), android.R.layout.simple_gallery_item, mItems);
	}

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
		
		mGridView = (GridView) v.findViewById(R.id.gridView);
		mLoading = (FrameLayout) v.findViewById(R.id.loadingPanel);

		setupAdapter(adapter);
		
		// Fires up background thread
		new FetchItemsTask().execute(pageCount);
		
		return v;
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
			mLoading.setVisibility(View.VISIBLE);
		}

		/* Note: do not update UI on background thread, memory corruption -> unsafe
		 */
		@Override
		protected ArrayList<GalleryItem> doInBackground(Integer... params) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return new FlickrFetchr().fetchItems(params[0]);
		}

		/* Happens after doInBackground is complete, also executed on main thread hence safe
		 * to update UI in it. Accepts the list fetched insdie doInBackground(..)
		 */
		@Override
		protected void onPostExecute(ArrayList<GalleryItem> items) {
			mLoading.setVisibility(View.INVISIBLE);
			mItems.addAll(items);
			setupAdapter(adapter);
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
			// Limit to add 20 pages
			if (pageCount <= 20) {
				new FetchItemsTask().execute(++pageCount);
			}
			mGridView.setAdapter(new ArrayAdapter<GalleryItem>(getActivity(), android.R.layout.simple_gallery_item, mItems));
		} else {
			mGridView.setAdapter(null);
		}
		
	}

}

