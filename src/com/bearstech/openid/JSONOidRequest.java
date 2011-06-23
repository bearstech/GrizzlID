package com.bearstech.openid;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;

	public class JSONOidRequest{

		public static final int OIDREQUEST_STATUS_PENDING = 0;
		public static final int OIDREQUEST_STATUS_REFUSED = 1;
		public static final int OIDREQUEST_STATUS_ACCEPTED = 2;
		
		private String id;
		private String trust_root;
		private String identity;
		private String date_created;
		public int validation;
		private Bitmap favicon;
		private ArrayList<String> required_fields;
		private OnFaviconFetched favicon_callback;
		private Object favicon_callback_src;

		protected JSONOidRequest(JSONObject src) throws JSONException {
			this.id = src.getString(Config.C2DM_MESSAGE_OIDREQUEST_ID);
			this.trust_root = src.getString(Config.C2DM_MESSAGE_OIDREQUEST_SITE);
			this.identity = src.getString("identity");
			this.date_created = src.getString("date_created");
			this.validation = src.getInt("validation");
			this.setRequiredFields(src.optJSONArray(Config.C2DM_MESSAGE_OIDREQUEST_REQUIRED_FIELDS));
			this.favicon = FaviconFetcher.getInstance().fetchFavicon(this);
		}
		
		protected JSONOidRequest(String id, String trust_root, ArrayList<String> required_fields){
			this.id = id;
			this.trust_root = trust_root;
			this.required_fields = required_fields;
			this.favicon = FaviconFetcher.getInstance().fetchFavicon(this);
		}

		protected Bitmap getFavicon(){
			return this.favicon;
		}

		protected Bitmap getFavicon(OnFaviconFetched cb, Object src){
			this.favicon_callback = cb;
			this.favicon_callback_src = src;
			return this.getFavicon();
		}

		protected String getId() {
			return this.id;
		}
		
		protected String getFormatedRequiredFields(){
		    StringBuffer buf = new StringBuffer();
		    if(this.required_fields != null) {
			    Iterator<String> it = this.required_fields.iterator();
			    while (it.hasNext()) {
			        buf.append(it.next());
			        if (it.hasNext())
			            buf.append(", ");
			    }
		    }
		    return buf.toString();
		}

		protected ArrayList<String> getRequiredFields(){
			return this.required_fields;
		}

		protected String getSite() {
			return this.trust_root;
		}

		protected void setFavicon(Bitmap bmp){
			this.favicon = bmp;
			if(this.favicon_callback!= null){
				this.favicon_callback.onFaviconFetched(bmp, this.favicon_callback_src);
			}
		}

		private void setRequiredFields(JSONArray json_required_fields) throws JSONException {
			if(json_required_fields != null){
				int len = json_required_fields.length();
				this.required_fields = new ArrayList<String>(len);
				for (int i = 0; i < len; i++)
					this.required_fields.add((String)json_required_fields.get(i));
			}
		}

		@Override
		public String toString() {
			return date_created + ": " + trust_root + "(" + identity + ")";
		}

		protected static interface OnFaviconFetched{

			public void onFaviconFetched(Bitmap bmp, Object src);
		}
	}