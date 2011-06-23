package com.bearstech.openid;

import java.io.IOException;

import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bearstech.openid.JSONOidRequest.OnFaviconFetched;
import com.bearstech.openid.OIDProviderClient.JSONResponse;
import com.google.android.c2dm.C2DMBaseReceiver;
import com.google.android.c2dm.C2DMessaging;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class GrizzlID extends ListActivity {

	private static final int OID_LOGIN_ACTIVITY_ID = 1;
	private static final int OID_REQUEST_DIALOG_ID = 2;
	private static final int OIDLIST_PROGRESS_DIALOG = 3;

	private static final int OIDREQUEST_LIST_SUCCEED = 0;
	private static final int OIDREQUEST_JSON_ERROR = 1;
	private static final int OIDREQUEST_IO_ERROR = 2;

	private OIDRequestArrayAdapter arrayAdapter;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.oidrequest_list_activity_view);

		if(savedInstanceState == null){
			arrayAdapter = new OIDRequestArrayAdapter(this);
			Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
			startActivityForResult(intent, OID_LOGIN_ACTIVITY_ID);
		}else{
			Object obj = getLastNonConfigurationInstance();
			if (obj instanceof ArrayAdapter<?>) {
				arrayAdapter = (OIDRequestArrayAdapter) obj;
			}else
				arrayAdapter = new OIDRequestArrayAdapter(this);
		}
		setListAdapter(arrayAdapter);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_menu, menu);
	    return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Context ctx = getApplicationContext();
		switch (requestCode) {
		case OID_LOGIN_ACTIVITY_ID:
			if (resultCode == RESULT_OK) {
				boolean reRegister = false;
				// store remote registration state returned by LoginActivity.
				int remoteNotificationState = data.getIntExtra(
						Config.C2DM_REMOTE_REGISTRATION_STATE, -1);
				// check if remote registration is okay
				if (remoteNotificationState != LoginActivity.LOGIN_JSON_SUCCEED_WITH_VALID_NOTIFICATION) {
					setRemoteRegistrationStatus(ctx, false);
					// force C2DM renewing if remote registration was
					// invalidated
					if (remoteNotificationState == LoginActivity.LOGIN_JSON_SUCCEED_WITH_INVALID_REGISTRATION_NOTIFICATION)
						reRegister = true;
				}
				String registrationId = C2DMessaging.getRegistrationId(ctx);
				// TODO registration pas automatique! seulement si le user le
				// veut!
				// if C2DM registration is needed
				if (reRegister || registrationId.length() == 0)
					C2DMessaging.register(getApplicationContext(),
							Config.C2DM_SENDER);
				// else, already registered on C2DM. but if not yet remotely
				// registered
				else if (!getRemoteRegistrationStatus()) {
					// simulate new registration with intent callback
					Intent intent = new Intent();
					intent
					.setAction(C2DMBaseReceiver.REGISTRATION_CALLBACK_INTENT);
					intent.putExtra(C2DMBaseReceiver.EXTRA_REGISTRATION_ID,
							registrationId);
					C2DMBaseReceiver.runIntentInService(ctx, intent);
				}
				new OIDListTask().execute();
			}else
				finish();
			break;
		case OID_REQUEST_DIALOG_ID:
			if (resultCode == RESULT_OK) {
				String oidrequest_id = data.getStringExtra(Config.C2DM_MESSAGE_OIDREQUEST_ID);
				if (oidrequest_id != null) {
					for (int i = 0; i < arrayAdapter.getCount(); i++) {
						JSONOidRequest item = arrayAdapter.getItem(i);
						if (item.getId().equals(oidrequest_id)) {
							arrayAdapter.remove(item);
							break;
						}
					}
				}
			}
			break;
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id){
		case OIDLIST_PROGRESS_DIALOG:
			ProgressDialog progressDialog = new ProgressDialog(this);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressDialog.setMessage(getText(R.string.activity_oidprovider_list_loading));
			progressDialog.setCancelable(false);
			progressDialog.show();
			return progressDialog;
		default:
			return null;
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Object obj = getListView().getItemAtPosition(position);
		if (obj instanceof JSONOidRequest) {
			JSONOidRequest oidRequest = (JSONOidRequest) obj;
			Intent intent = new Intent();
			intent.setClass(getApplicationContext(), OIDRequestDialogActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
			intent.putExtra(Config.C2DM_MESSAGE_OIDREQUEST_ID, oidRequest.getId());
			intent.putExtra(Config.C2DM_MESSAGE_OIDREQUEST_SITE, oidRequest.getSite());
			intent.putExtra(Config.C2DM_MESSAGE_OIDREQUEST_REQUIRED_FIELDS, oidRequest.getRequiredFields());
			startActivityForResult(intent, OID_REQUEST_DIALOG_ID);
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case R.id.option_menu_logout:
			if(arrayAdapter != null)
				arrayAdapter.clear();
			Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
			intent.putExtra(LoginActivity.NO_AUTO_LOGIN, true);
			startActivityForResult(intent, OID_LOGIN_ACTIVITY_ID);
			return true;
		case R.id.option_menu_refresh:
			new OIDListTask().execute();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return arrayAdapter;
	}

	private boolean getRemoteRegistrationStatus() {
		SharedPreferences settings = getSharedPreferences(Config.PREFS_NAME, 0);
		return settings.getBoolean(Config.C2DM_REMOTE_REGISTRATION_STATE, false);
	}

	protected static void setRemoteRegistrationStatus(Context ctx, boolean mRemoteRegistrationStatus) {
		SharedPreferences settings = ctx.getSharedPreferences(Config.PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(Config.C2DM_REMOTE_REGISTRATION_STATE, mRemoteRegistrationStatus);
		editor.commit();
	}

	private class OIDListTask extends AsyncTask<Void, Void, JSONResponse> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			showDialog(OIDLIST_PROGRESS_DIALOG);
		}

		@Override
		protected JSONResponse doInBackground(Void... arg0) {
			JSONResponse response;
			try {
				OIDProviderClient httpclient = OIDProviderClient.getInstance(getApplicationContext());
				HttpGet httpget = new HttpGet(Config.BEARSTECH_OPENID_PROVIDER_OIDREQUEST_URI);
				response = httpclient.executeJSON(httpget);
			} catch (JSONException e) {
				response = new JSONResponse(OIDREQUEST_JSON_ERROR, e.getMessage());

			} catch (IOException e) {
				response = new JSONResponse(OIDREQUEST_IO_ERROR, e.getMessage());
			}
			return response;
		}

		@Override
		protected void onPostExecute(JSONResponse result) {
			super.onPostExecute(result);
			Context ctx = getApplicationContext();
			removeDialog(OIDLIST_PROGRESS_DIALOG);
			String error;
			switch (result.getStatusCode()) {
			case OIDREQUEST_LIST_SUCCEED:
				JSONArray data = result.getData();
				if (data != null) {
					arrayAdapter.clear();
					for (int i = 0; i < data.length(); i++) {
						try {
							arrayAdapter.add(new JSONOidRequest((JSONObject) data.get(i)));
						} catch (JSONException e) {
							Log.d(Config.TAG, "JSON exception: " + e.getMessage());
						}
					}
				}
				break;
			case OIDREQUEST_JSON_ERROR:
				error = getText(R.string.oidrequest_list_json_error) + result.getErrorMsg();
				Log.d(Config.TAG, error);
				Toast.makeText(ctx, error, Toast.LENGTH_LONG).show();
				break;
			case OIDREQUEST_IO_ERROR:
				error = getText(R.string.oidrequest_list_io_error) + result.getErrorMsg();
				Log.d(Config.TAG, error);
				Toast.makeText(ctx, error, Toast.LENGTH_LONG).show();
				break;
			default:
				error = getText(R.string.oidrequest_list_error) + result.getErrorMsg();
				Log.d(Config.TAG, error);
				Toast.makeText(ctx, error, Toast.LENGTH_LONG).show();
				break;
			}
		}
	}

	private class OIDRequestArrayAdapter extends ArrayAdapter<JSONOidRequest> implements OnFaviconFetched{

		public OIDRequestArrayAdapter(Context context) {
			super(context, R.layout.oidrequest_list_row);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.oidrequest_list_row, null);
			}
			JSONOidRequest o = getItem(position);
			if (o != null) {
				ImageView iv = (ImageView) v.findViewById(R.id.oidrequest_list_row_icon);
				TextView siteFieldView = (TextView) v.findViewById(R.id.oidrequest_list_row_site);
				TextView stateFieldView = (TextView) v.findViewById(R.id.oidrequest_list_row_state);
				if (siteFieldView != null) {
					siteFieldView.setText(getText(R.string.oidrequest_row_label_site) + o.getSite());
				}
				if (stateFieldView != null) {
					switch(o.validation){
					case JSONOidRequest.OIDREQUEST_STATUS_PENDING:
						stateFieldView.setText(getText(R.string.oidrequest_row_label_status_pending));
						break;
					case JSONOidRequest.OIDREQUEST_STATUS_ACCEPTED:
						stateFieldView.setText(getText(R.string.oidrequest_row_label_status_accepted));
						break;
					case JSONOidRequest.OIDREQUEST_STATUS_REFUSED:
						stateFieldView.setText(getText(R.string.oidrequest_row_label_status_refused));
						break;
					}
				}
				if (iv != null){
					Bitmap favicon = o.getFavicon(this, iv);
					if(favicon != null)
						iv.setImageBitmap(favicon);
					else
						iv.setImageResource(R.drawable.icon);
				}
			}
			return v;
		}

		@Override
		public void onFaviconFetched(Bitmap bmp, Object src) {
			if(src instanceof ImageView){
				ImageView iv = (ImageView) src;
				iv.setImageBitmap(bmp);
			}
		}
	}

}