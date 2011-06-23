package com.bearstech.openid;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;


public class OIDProviderClient extends DefaultHttpClient{
	
	private static final String OIDPROVIDER_REQUEST_STATUS = "status";
	private static final String OIDPROVIDER_REQUEST_MESSAGE = "message";
	private static final String OIDPROVIDER_REQUEST_DATA = "data";
	
	private static OIDProviderClient oidProviderClient;
	
	private OIDProviderClient(){
		super();
	}
	
	public static synchronized OIDProviderClient getInstance(Context ctx){
		if(oidProviderClient == null){
			oidProviderClient = new OIDProviderClient();
		    SharedPreferences settings = ctx.getSharedPreferences(Config.PREFS_NAME, 0);
		    String cookie = settings.getString(Config.PREFS_COOKIE, null);
		    //if we have a cookie; add it to the newly instantiated DefaultHttpClient 
		    if(cookie != null){
				CookieStore cs = oidProviderClient.getCookieStore();
				List<BasicClientCookie> cookies = new CookieParser(cookie).parse();
				for(int i = 0; i < cookies.size(); i++){
					BasicClientCookie bcc = cookies.get(i);
					if(bcc != null && bcc.getValue() != null){
						bcc.setDomain(Config.BEARSTECH_OPENID_PROVIDER_DOMAIN);
						cs.addCookie(bcc);
						Log.d(Config.TAG, "cookie added:"+bcc.getValue()+','+bcc.getDomain());//TODO remove
					}
				}
		    }
		}
		return oidProviderClient;
	}
	
	public static void clearInstance(){
		oidProviderClient = null;
	}
	
	public synchronized JSONResponse executeJSON (HttpUriRequest request) throws IOException, JSONException{
		HttpResponse response = execute(request);
		if(response.getStatusLine() != null && response.getStatusLine().getStatusCode() == 200)
		{
			DataInputStream is = new DataInputStream( response.getEntity().getContent() );
			JSONObject jo = new JSONObject(is.readLine());
			return new JSONResponse(response, jo.getInt(OIDPROVIDER_REQUEST_STATUS), jo.optString(OIDPROVIDER_REQUEST_MESSAGE), jo.optJSONArray(OIDPROVIDER_REQUEST_DATA));
		}else
			throw new JSONException("Invalid status line!");
	}
	
	public static class JSONResponse{
		private HttpResponse httpResponse;
		private int statusCode;
		private String errorMsg;
		private JSONArray data;
		
		public JSONResponse(int statusCode, String errorMsg) {
			this.statusCode = statusCode;
			this.errorMsg = errorMsg;
		}
		
		public JSONResponse(HttpResponse response, int statusCode, String errorMsg, JSONArray data) {
			this.httpResponse = response;
			this.statusCode = statusCode;
			this.errorMsg = errorMsg;
			this.data = data;
		}
		
		public JSONArray getData(){
			return this.data;
		}
		
		public String getErrorMsg() {
			return errorMsg;
		}
		
		public HttpResponse getHttpResponse(){
			return httpResponse;
		}
		
		public int getStatusCode() {
			return statusCode;
		}
	}
}
