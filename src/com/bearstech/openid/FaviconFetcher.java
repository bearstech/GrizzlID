package com.bearstech.openid;

import java.io.IOException;
import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

public class FaviconFetcher {
	
	private static FaviconFetcher faviconFetcher;
	
	private HashMap<String, Bitmap> favicons;
	
	private FaviconFetcher(){
		this.favicons = new HashMap<String, Bitmap>();
	}
	
	public static FaviconFetcher getInstance(){
		if(faviconFetcher == null){
			faviconFetcher = new FaviconFetcher();
		}
		return faviconFetcher;
	}
	
	public Bitmap fetchFavicon(JSONOidRequest jsonOidRequest){
		Uri uri = Uri.parse(jsonOidRequest.getSite());
		if(uri != null){
			String domain = uri.getHost();
			if (domain != null && domain.length() > 0){
				if(this.favicons.containsKey(domain))
					return this.favicons.get(domain);
				else{
					new FaviconFetcherTask(jsonOidRequest).execute(domain);
				}
			}
		}
		return null;
	}
	
	private class FaviconFetcherTask extends AsyncTask<String,Void,Bitmap>{
		
		private JSONOidRequest jsonOidRequest;
		
		public FaviconFetcherTask(JSONOidRequest obj){
			super();
			this.jsonOidRequest = obj;
		}

		@Override
		protected Bitmap doInBackground(String... params) {
			HttpClient httpClient = new DefaultHttpClient();
			String domain = params[0];
			String url = "http://" + domain + "/" + "favicon.ico";//TODO get real scheme
			Log.d(Config.TAG, "Fetching favicon: " + url);
			HttpGet get = new HttpGet(url);
			HttpResponse response;
			try {
				response = httpClient.execute(get);
				if(response.getStatusLine().getStatusCode() == 200){
					BitmapFactory.Options bfo = new BitmapFactory.Options();
					bfo.outHeight = bfo.outWidth = 64;
					Bitmap bmp = BitmapFactory.decodeStream(response.getEntity().getContent());
					favicons.put(domain, bmp);
					return bmp;
				}
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Bitmap result) {
			super.onPostExecute(result);
			if(result != null){
				this.jsonOidRequest.setFavicon(result);
			}
		}
	}
}
