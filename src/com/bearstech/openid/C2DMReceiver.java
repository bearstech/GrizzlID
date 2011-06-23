package com.bearstech.openid;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.bearstech.grizzlid.oauth.OAuthDialogActivity;
import com.bearstech.openid.OIDProviderClient.JSONResponse;
import com.google.android.c2dm.C2DMBaseReceiver;

public class C2DMReceiver extends C2DMBaseReceiver {

	private static final int REGISTRATION_RESPONSE_SUCCEED = 0;
	private static final int REGISTRATION_RESPONSE_ERROR_NOT_LOGGED = 1;
	private static final int REGISTRATION_RESPONSE_ERROR_BAD_PROFILE = 2;
	
	public C2DMReceiver() {
		super(Config.C2DM_SENDER);
	}

	@Override
	public void onError(Context context, String errorId) {
		Log.d(Config.TAG, "C2DMReceiver Error: "+errorId);
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
		String intent_type = intent.getStringExtra(Config.C2DM_MESSAGE_TYPE);
		if(intent_type != null){
			if(intent_type.equals("openid")){
				String oidrequest_id = intent.getStringExtra(Config.C2DM_MESSAGE_OIDREQUEST_ID);
				String oidrequest_site = intent.getStringExtra(Config.C2DM_MESSAGE_OIDREQUEST_SITE);
				if(oidrequest_id != null && oidrequest_site != null){
					String required_fields = intent.getStringExtra(Config.C2DM_MESSAGE_OIDREQUEST_REQUIRED_FIELDS);
					if(required_fields != null){
						try{
							JSONArray json_required_fields = new JSONArray(required_fields);
							if(json_required_fields != null){
								int len = json_required_fields.length();
								ArrayList<String> array_required_fields = new ArrayList<String>(len);
								for (int i = 0; i < len; i++)
									array_required_fields.add((String)json_required_fields.get(i));
								intent.putExtra(Config.C2DM_MESSAGE_OIDREQUEST_REQUIRED_FIELDS, array_required_fields);
							}
						}catch(JSONException e){
							Log.d(Config.TAG, "Error JSON in C2DMReceiver::onMessage : " + e.getMessage());
						}
					}
					intent.setClass(context, OIDRequestDialogActivity.class);
				    intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
				    context.startActivity(intent);
				}else
					Log.d(Config.TAG, "C2DMReceiver onMessage error (bad OpenID intent)");
			}else if(intent_type.equals("oauth")){
				String oauth_token_url = intent.getStringExtra(Config.C2DM_MESSAGE_OAUTHREQUEST_URL);
				if(oauth_token_url != null){
					intent.setClass(context, OAuthDialogActivity.class);
				    intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
				    intent.putExtra(Config.C2DM_MESSAGE_OAUTHREQUEST_URL, oauth_token_url);
				    context.startActivity(intent);
				}else
					Log.d(Config.TAG, "C2DMReceiver onMessage error (bad OAuth intent)");
			}else
				Log.d(Config.TAG, "C2DMReceiver onMessage error (bad intent type)");
		}else
			Log.d(Config.TAG, "C2DMReceiver onMessage error (missing intent type");
	}

	@Override
	public void onRegistered(Context context, String registrationId)
			throws IOException {
		Toast.makeText(context, getText(R.string.c2dm_successfully_registered), Toast.LENGTH_SHORT).show();
		GrizzlID.setRemoteRegistrationStatus(context, false);//in case registration_id is renewed
		try {
			OIDProviderClient httpclient = OIDProviderClient.getInstance(getApplicationContext());
			HttpGet httpget = new HttpGet(Config.BEARSTECH_C2DM_REGISTRATION + '/' + registrationId);
			JSONResponse response = httpclient.executeJSON(httpget);
			switch(response.getStatusCode()){
			case REGISTRATION_RESPONSE_SUCCEED:
				GrizzlID.setRemoteRegistrationStatus(context, true);
				break;
			case REGISTRATION_RESPONSE_ERROR_BAD_PROFILE:
			case REGISTRATION_RESPONSE_ERROR_NOT_LOGGED:
			default:
				Log.d(Config.TAG, "Registration failed: "+ response.getErrorMsg() + '('+response.getStatusCode()+')');
				break;
			}
		} catch (JSONException e) {//TODO C2DMessaging.setRegistrationId will be called even if a JSONException is thrown. Is that okay ?
			Log.d(Config.TAG, "Exception: Invalid Login JSON response!");//TODO i18n
		}
	}

	@Override
	public void onUnregistered(Context context) {
		GrizzlID.setRemoteRegistrationStatus(context, false);//TODO are U sure ??
	}

}
