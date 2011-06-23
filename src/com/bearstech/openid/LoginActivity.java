package com.bearstech.openid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;

import com.bearstech.openid.OIDProviderClient.JSONResponse;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity implements OnCancelListener {

	private static final int LOGGING_PROGRESS_DIALOG = 1;
	
	public static final int LOGIN_JSON_SUCCEED_WITH_NOT_REGISTERED_NOTIFICATION = 0;
	public static final int LOGIN_JSON_SUCCEED_WITH_INVALID_REGISTRATION_NOTIFICATION = 1;
	public static final int LOGIN_JSON_SUCCEED_WITH_VALID_NOTIFICATION = 2;
	
	public static final String NO_AUTO_LOGIN = "noAutoLogin";

	private boolean loginCanceled;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);
		
		Intent intent = getIntent();
		boolean noAutoLogin = intent.getBooleanExtra(LoginActivity.NO_AUTO_LOGIN, false);
		
		SharedPreferences settings = getSharedPreferences(Config.PREFS_NAME, 0);
		String username = settings.getString(Config.PREFS_PROVIDER_USERNAME, null);
		String pwd = settings.getString(Config.PREFS_PROVIDER_PASSWORD, null);
		boolean autoLogin = settings.getBoolean(Config.PREFS_PROVIDER_AUTOLOGIN, false);
		
		if(username != null){
			EditText edittext_username = (EditText)findViewById(R.id.EditText_Username);
			edittext_username.setText(username);
		}
		if(pwd != null){
			EditText edittext_pwd = (EditText)findViewById(R.id.EditText_Password);
			edittext_pwd.setText(pwd);
		}
		if(autoLogin){
			CheckBox edittext_autologin = (CheckBox)findViewById(R.id.CheckBox_AutoLogin);
			edittext_autologin.setChecked(true);
			if(!noAutoLogin)
				onLogonClick(null);
		}
	}
	
	@Override
	public void onCancel(DialogInterface dialog) {
		this.loginCanceled = true;
		getIntent().putExtra(NO_AUTO_LOGIN, true);
		removeDialog(LOGGING_PROGRESS_DIALOG);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id){
		case LOGGING_PROGRESS_DIALOG:
			ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressDialog.setMessage(getText(R.string.activity_grizzlid_logging));
			progressDialog.setCancelable(true);
			progressDialog.setOnCancelListener(this);
			progressDialog.show();
			return progressDialog;
		default:
			return super.onCreateDialog(id);
		}
	}

	public void onLogonClick(View v) {
		EditText username = (EditText) findViewById(R.id.EditText_Username);
		EditText pwd = (EditText) findViewById(R.id.EditText_Password);
	    
	    SharedPreferences settings = getSharedPreferences(Config.PREFS_NAME, 0);
	    SharedPreferences.Editor editor = settings.edit();
	    editor.putString(Config.PREFS_PROVIDER_USERNAME, username.getEditableText().toString());
	    editor.putString(Config.PREFS_PROVIDER_PASSWORD, pwd.getEditableText().toString());
	    editor.commit();

	    this.loginCanceled = false;

	    //TODO fix onPause/OnResume broke AsyncTask (ie. orientation change)
		new LoginTask().execute(username.getText().toString(),pwd.getText().toString());
	}

	private class LoginTask extends AsyncTask<String, Void, JSONResponse> {
		
		private static final int LOGIN_JSON_ERROR_INVALID_PROFILE = 3;
		private static final int LOGIN_JSON_ERROR_INACTIVE_USER = 4;
		private static final int LOGIN_JSON_ERROR_INVALID_LOGIN = 5;
		private static final int LOGIN_JSON_ERROR_INVALID_FORM = 6;
		private static final int LOGIN_JSON_ERROR_INVALID_METHOD = 7;
		private static final int LOGIN_JSON_EXCEPTION = 8;
		private static final int LOGIN_JSON_IO_EXCEPTION = 9;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			showDialog(LOGGING_PROGRESS_DIALOG);
		}
		
		@Override
		protected JSONResponse doInBackground(String... params) {
			JSONResponse response;
			try {
				OIDProviderClient httpclient = OIDProviderClient.getInstance(getApplicationContext());
				HttpPost httpost = new HttpPost(Config.BEARSTECH_OPENID_PROVIDER_LOGIN_URI);

				List <NameValuePair> nvps = new ArrayList <NameValuePair>();
				nvps.add(new BasicNameValuePair("email", params[0]));
				nvps.add(new BasicNameValuePair("pw", params[1]));
				//TODO better encoding : fancy pw cause status: failed; message: invalid form
				httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
				return httpclient.executeJSON(httpost);
			} catch (JSONException e) {
				Log.d(Config.TAG, "Exception: Invalid Login JSON response!");
				response = new JSONResponse(LOGIN_JSON_EXCEPTION, e.getMessage());
			} catch (IOException e) {
				response = new JSONResponse(LOGIN_JSON_IO_EXCEPTION, e.getMessage());
			}
			return response;
		}
		
		@Override
		protected void onPostExecute(JSONResponse result) {
			super.onPostExecute(result);
			if(!loginCanceled){
				removeDialog(LOGGING_PROGRESS_DIALOG);
				int statusCode = result.getStatusCode();
				switch(statusCode){
				case LOGIN_JSON_SUCCEED_WITH_NOT_REGISTERED_NOTIFICATION:
				case LOGIN_JSON_SUCCEED_WITH_INVALID_REGISTRATION_NOTIFICATION:
				case LOGIN_JSON_SUCCEED_WITH_VALID_NOTIFICATION:
					Header cookie = result.getHttpResponse().getFirstHeader("Set-Cookie");
					SharedPreferences settings = getSharedPreferences(Config.PREFS_NAME, 0);
					SharedPreferences.Editor editor = settings.edit();
					final CheckBox checkBox_autoLogin = (CheckBox)findViewById(R.id.CheckBox_AutoLogin);
					editor.putBoolean(Config.PREFS_PROVIDER_AUTOLOGIN, checkBox_autoLogin.isChecked());
					editor.putString(Config.PREFS_COOKIE, cookie.getValue());
					editor.commit();
					final Intent intent = new Intent();
					intent.putExtra(Config.C2DM_REMOTE_REGISTRATION_STATE, statusCode);
					setResult(RESULT_OK, intent);
					finish();
					break;
				case LOGIN_JSON_ERROR_INVALID_PROFILE:
				case LOGIN_JSON_ERROR_INACTIVE_USER:
				case LOGIN_JSON_ERROR_INVALID_LOGIN:
				case LOGIN_JSON_ERROR_INVALID_FORM:
				case LOGIN_JSON_ERROR_INVALID_METHOD:
				case LOGIN_JSON_EXCEPTION:
				case LOGIN_JSON_IO_EXCEPTION:
				default:
					Toast.makeText(getApplicationContext(), getString(R.string.activity_oidprovider_login_failed) + result.getErrorMsg() + "("+statusCode+")", Toast.LENGTH_LONG).show();
					break;
				}
			}
		}
	}
}
