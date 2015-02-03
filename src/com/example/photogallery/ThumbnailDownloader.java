package com.example.photogallery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;

public class ThumbnailDownloader<Token> extends HandlerThread {
	private static final String TAG = "ThumbnailDownloader";
	private static final int MESSAGE_DOWNLOAD = 0;
	private static final int MESSAGE_PRELOAD = 1;
	
	Handler mHandler;
	Handler mResponseHandler;
	Listener<Token> mListener;
	
	Map<Token, String> requestMap = Collections.synchronizedMap(new HashMap<Token, String>());
	
	// Get max available VM memory (in kilobytes), exceeding this amount will throw an
    // OutOfMemory exception. Stored in kilobytes as LruCache takes an
    // int in its constructor.
    final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

    // Use 1/8th of the available memory for this memory cache.
    final int cacheSize = maxMemory / 4;

    private LruCache<String, Bitmap> mMemoryCache;
	
	public interface Listener<Token> {
		void onThumbnailDownload(Token token, Bitmap thumbnail);
	}
	public void setListener(Listener<Token> listener) {
		mListener = listener;
	}

	public ThumbnailDownloader(Handler handler) {
		super(TAG);
		mResponseHandler = handler;
	}
	
	/* This method is called before the looper does its first check, any intiliaztion should
	 * be performed here (non-Javadoc)
	 * @see android.os.HandlerThread#onLooperPrepared()
	 */
	@SuppressLint("HandlerLeak")
	@Override
	protected void onLooperPrepared() {
		mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
	        @Override
	        protected int sizeOf(String key, Bitmap bitmap) {
	            // The cache size will be measured in kilobytes rather than
	            // number of items.
	            return bitmap.getByteCount() / 1024;
	        }
	    };
		
		mHandler = new Handler() {

			@SuppressWarnings("unchecked")
			@Override
			public void handleMessage(Message msg) {
				if(msg.what == MESSAGE_DOWNLOAD) {
					@SuppressWarnings("unchecked")
					Token token = (Token)msg.obj;
					Log.i(TAG, "Got a request for url: " + requestMap.get(token));
					
					handleRequest(token);
				} else if (msg.what == MESSAGE_PRELOAD) {
					Log.i("REACHED", "MESSAGE_PRELOAD");
					handlePreload((ArrayList<String>)msg.obj);
				}
			}

		};
	}

	public void queueThumbnail(Token token, String url) {
		Log.i(TAG, "Got an URL: "  + url);
		requestMap.put(token, url);
		// obtainMessage(..) gets target based on what and obj params
		/* each message has 3 params:
		 * what - a user-defined int that describes the message.
		 * obj - a user-specified object to be sent with the message.
		 * target- the Handler that will handle the message.
		 */
		
		mHandler.obtainMessage(MESSAGE_DOWNLOAD, token).sendToTarget();
	}
	
	/* where downloading happens */
	private void handleRequest(final Token token) {
		try {
			final String url = requestMap.get(token);
			if (url == null) {
				return;
			}
			final Bitmap cached = getBitmapFromMemCache(url);
			final Bitmap bitmap;
			
			// For When Cache is enabled
			/*
			if (cached == null) {
				byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
				bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
				
				addBitmapToMemoryCache(url, bitmap);
				Log.i(TAG, "Created new bitmap");
			} else {
				Log.i("Cache", "Retrieved item from cache: " + url);
				bitmap = cached;
			}*/
			

			// No Cache
			byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
			bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
			
			addBitmapToMemoryCache(url, bitmap);
			Log.i(TAG, "Created new bitmap");
			
			// posts task to main thread (UI)
			mResponseHandler.post(new Runnable() {
				// runs this in UI thread
				@Override
				public void run() {
					// necessary because the GridView recycles its views
					// ensures Token gets correct image even if another request has been made
					if (requestMap.get(token) != url) {
						return;
					}
					requestMap.remove(token);
					mListener.onThumbnailDownload(token, bitmap);
				}
			});
		} catch (IOException e) {
			Log.e(TAG, "Error downloading image", e);
		}
	}
	
	public void clearQueue() {
		mHandler.removeMessages(MESSAGE_DOWNLOAD);
		requestMap.clear();
	}
	
	/* ------- Cache methods ------- */
	public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
	    if (getBitmapFromMemCache(key) == null) {
	        mMemoryCache.put(key, bitmap);
	    }
	}
	
	public Bitmap getBitmapFromMemCache(String url) {
	    return mMemoryCache.get(url);
	}

	public void preloadCache(ArrayList<String> urls) {
		
		mHandler.obtainMessage(MESSAGE_PRELOAD, urls).sendToTarget();
	}
	
	private void handlePreload(ArrayList<String> urls) {
		Log.i("REACHED", "handlePreload");
		for (int i = 0; i < urls.size(); i++ ) {
			final Bitmap cached = getBitmapFromMemCache(urls.get(i));
			if (cached == null) {
				try {
					final byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(urls.get(i));
					final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
					
					addBitmapToMemoryCache(urls.get(i), bitmap);
					Log.i("PRELOAD", "preloaded img to cache");
				} catch (IOException e) {
					Log.i(TAG, "Error loading file!", e);
				}
			}			
		}
	}
	
	
}
