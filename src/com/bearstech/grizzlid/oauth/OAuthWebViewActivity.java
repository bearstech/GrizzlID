package com.bearstech.grizzlid.oauth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.bearstech.openid.Config;

public class OAuthWebViewActivity extends Activity {

	private static final int OAUTH_PROGRESS_DIALOG_ID = 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		if(intent != null){
			final String oauth_token_url = intent.getStringExtra(Config.C2DM_MESSAGE_OAUTHREQUEST_URL);
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			WebView webView = new WebView(this);
			webView.setWebViewClient(new WebViewClient(){
				@Override
				public void onLoadResource(WebView view, String url) {
					if(url.contains("oauth_verifier=")){
						view.stopLoading();
						new OAuthWebViewActivity.OAuthTokenVerifierTask().execute(oauth_token_url, url);
						finish();
					}
				}
			});
			webView.loadUrl(oauth_token_url);
			setContentView(webView);
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id){
		case OAUTH_PROGRESS_DIALOG_ID:
			ProgressDialog progressDialog = new ProgressDialog(OAuthWebViewActivity.this);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressDialog.setMessage("sending...");
			progressDialog.setCancelable(false);
			progressDialog.show();
			return progressDialog;
		default:
			return super.onCreateDialog(id);
		}
	}
	
	private class OAuthTokenVerifierTask extends AsyncTask<String, Void, Boolean> {
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			showDialog(OAUTH_PROGRESS_DIALOG_ID);
		}
		
		@Override
		protected Boolean doInBackground(String... params) {
			HttpClient httpClient = new DefaultHttpClient();
			HttpPost post = new HttpPost(Config.BEARSTECH_OAUTH_URI);
			
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
	        nameValuePairs.add(new BasicNameValuePair(Config.C2DM_MESSAGE_OAUTHREQUEST_URL, params[0]));
	        nameValuePairs.add(new BasicNameValuePair("oauth_callback", params[1]));

			try {
				post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				httpClient.execute(post);
				return true;
			} catch (IOException e) {
				return false;
			}
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			removeDialog(OAUTH_PROGRESS_DIALOG_ID);
		}
	}
}
