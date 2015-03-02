package com.example.photogallery;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import model.GalleryItem;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.net.Uri;
import android.util.Log;

public class FlickrFetchr {
	public static final String TAG = "FlickrFetchr";
	
	public static final String PREF_SEARCH_QUERY = "searchQuery";
	public static final String PREF_LAST_RESULT_ID = "lastResultId";
	
	private static final String ENDPOINT = "https://api.flickr.com/services/rest/";
	private static final String API_KEY = "5bb3db600c186f0cce32bcf72b8db715";
	
	private static final String METHOD_GET_RECENT = "flickr.photos.getRecent";
	private static final String METHOD_SEARCH = "flickr.photos.search";
	
	private static final String PARAM_EXTRAS = "extras";
	private static final String EXTRA_SMALL_URL = "url_s";
	private static final String PARAM_TEXT = "text";
	
	// XML constants
	private static final String XML_PHOTO = "photo";
	
	byte[] getUrlBytes(String urlSpec) throws IOException {
		URL url = new URL(urlSpec);
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			// for GET
			InputStream in = connection.getInputStream();
			// POST needs to invoke getOutputStream()
			
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				return null;
			}
			
			int bytesRead = 0;
			byte[] buffer = new byte[1024];
			// if more than 0 bytes are read
			while ((bytesRead = in.read(buffer)) > 0) {
				out.write(buffer, 0, bytesRead);
			}
			out.close();
			return out.toByteArray();
		} finally {
			connection.disconnect();
		}
	}
	
	public String getUrl(String urlSpec) throws IOException {
		return new String(getUrlBytes(urlSpec));
	}
	
	public ArrayList<GalleryItem> downloadGalleryItems(String url) {
		ArrayList<GalleryItem> items = new ArrayList<GalleryItem>();
		try {
			System.out.println("url:\t" + url.toString());
			
			String xmlString = getUrl(url.toString());
			Log.i(TAG, "Received xml: " + xmlString);
			
			// Start parsing the XML received
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			XmlPullParser parser = factory.newPullParser();
			parser.setInput(new StringReader(xmlString));
			
			parseItems(items, parser);
			
		} catch (IOException io) {
			Log.e(TAG, "Failed to fetch items", io);
		} catch (XmlPullParserException xppe) {
			Log.e(TAG, "Failed to parse items", xppe);
		}
		
		return items;
	}
	
	public ArrayList<GalleryItem> fetchItems(Integer page) {
		// Uri.Builder is a convenience class for creating properly escaped paramterized URLs
		String url = Uri.parse(ENDPOINT).buildUpon()
				.appendQueryParameter("method", METHOD_GET_RECENT)
				.appendQueryParameter("api_key", API_KEY)
				.appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
				.appendQueryParameter("page", ""+page)
				.build().toString();
		return downloadGalleryItems(url);
	}
	
	public ArrayList<GalleryItem> search(String query) {
		String url = Uri.parse(ENDPOINT).buildUpon()
				.appendQueryParameter("method", METHOD_SEARCH)
				.appendQueryParameter("api_key", API_KEY)
				.appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
				.appendQueryParameter(PARAM_TEXT, query)
				.build().toString();
		return downloadGalleryItems(url);
	}
	
	void parseItems (ArrayList<GalleryItem> items, XmlPullParser parser) throws XmlPullParserException, IOException {
		int eventType = parser.next();
		
		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_TAG && 
					XML_PHOTO.equals(parser.getName())) {
				String id = parser.getAttributeValue(null, "id");
				String caption = parser.getAttributeValue(null, "title");
				String smallUrl = parser.getAttributeValue(null, EXTRA_SMALL_URL);
				
				GalleryItem item = new GalleryItem();
				item.setmId(id);
				item.setmCaption(caption);
				item.setmUrl(smallUrl);
				items.add(item);
			}
			eventType = parser.next();
		}
	}

}
