package com.bearstech.openid;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.client.methods.HttpGet;
import org.json.JSONException;

import com.bearstech.openid.JSONOidRequest.OnFaviconFetched;
import com.bearstech.openid.OIDProviderClient.JSONResponse;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class OIDRequestDialogActivity extends Activity {
	
	private static final int DIALOG_NOTIFICATION_ID = 1;
	private static final int OID_LOGIN_ACTIVITY_ID = 2;
	private static final int OIDANSWER_PROGRESS_DIALOG = 3;
	
	private static final int OIDREQUEST_RESPONSE_SUCCEED = 0;
	private static final int OIDREQUEST_RESPONSE_ERROR_BAD_REQUEST = 1;
	private static final int OIDREQUEST_RESPONSE_ERROR_BAD_IDENTITY = 2;
	private static final int OIDREQUEST_RESPONSE_ERROR_BAD_SESSION = 3;
	private static final int OIDREQUEST_RESPONSE_ERROR_BAD_ACTION = 4;
	private static final int OIDREQUEST_JSON_ERROR = 5;
	private static final int OIDREQUEST_IO_ERROR = 6;

	private JSONOidRequest oidRequest;
	private String action;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		if(intent != null){
			String id = intent.getStringExtra(Config.C2DM_MESSAGE_OIDREQUEST_ID);
			String trust_root = intent.getStringExtra(Config.C2DM_MESSAGE_OIDREQUEST_SITE);
			ArrayList<String> required_fields = intent.getStringArrayListExtra(Config.C2DM_MESSAGE_OIDREQUEST_REQUIRED_FIELDS);
			this.oidRequest = new JSONOidRequest(id, trust_root, required_fields);
			if(savedInstanceState == null)//if not savedInstanceState (because managed dialogs will show it automagicaly)
				showDialog(DIALOG_NOTIFICATION_ID);
		}else
			Log.d(Config.TAG, "OIDRequestDialogActivity: bad intent onCreate !");
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch(requestCode){
		case OID_LOGIN_ACTIVITY_ID:
			if(resultCode == RESULT_OK){
				new OIDRequestDialogTask().execute();
			}
			break;
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id){
		case DIALOG_NOTIFICATION_ID:
			return new OIDRequestDialog(this);
		case OIDANSWER_PROGRESS_DIALOG:
			ProgressDialog progressDialog = new ProgressDialog(OIDRequestDialogActivity.this);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressDialog.setMessage(getText(R.string.activity_oidrequest_response));
			progressDialog.setCancelable(true);
			progressDialog.show();
			return progressDialog;
		default:
			return super.onCreateDialog(id);
		}
	}

	private class OIDRequestDialog extends Dialog implements View.OnClickListener, OnFaviconFetched, OnCancelListener{

		public OIDRequestDialog(Context ctx){
			super(ctx);
			setOnCancelListener(this);
			requestWindowFeature(Window.FEATURE_LEFT_ICON);
			setContentView(R.layout.notification_dialog);
			setTitle(oidRequest.getSite());

			String required_fields = oidRequest.getFormatedRequiredFields();
			if(required_fields.length() > 0){
				TextView notification_view = (TextView) findViewById(R.id.TextView_OidRequest_Notification);
				if( notification_view != null)
					notification_view.setText(getText(R.string.activity_oidrequest_notification_required_fields));

				TextView required_fields_view = (TextView) findViewById(R.id.TextView_OidRequest_Required_Fields);
				if( required_fields_view != null)
					required_fields_view.setText(required_fields);
			}

			Bitmap bmp = oidRequest.getFavicon(this, null);
			if(bmp != null)
				setFeatureDrawable(Window.FEATURE_LEFT_ICON, new BitmapDrawable(bmp));
			else
				setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.bt_logo);

			Button acceptButton = (Button) findViewById(R.id.Button_OidRequest_Notification_Accept);
			if(acceptButton != null)
				acceptButton.setOnClickListener(this);

			Button refuseButton = (Button) findViewById(R.id.Button_OidRequest_Notification_Refuse);
			if(refuseButton != null)
				refuseButton.setOnClickListener(this);
		}

		@Override
		public void onClick(View v) {
			if(v.getId() == R.id.Button_OidRequest_Notification_Accept)
				action = "accept";
			else
				action = "refuse";
			new OIDRequestDialogTask().execute();
		}

		@Override
		public void onCancel(DialogInterface dialog) {
			setResult(RESULT_CANCELED);
			finish();
		}

		@Override
		public void onFaviconFetched(Bitmap bmp, Object src) {
			setFeatureDrawable(Window.FEATURE_LEFT_ICON, new BitmapDrawable(bmp));
		}
	}

	private class OIDRequestDialogTask extends AsyncTask<String,Void,JSONResponse>{
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			showDialog(OIDANSWER_PROGRESS_DIALOG);
		}

		@Override
		protected JSONResponse doInBackground(String... params) {
			JSONResponse response;
			try {
				OIDProviderClient httpclient = OIDProviderClient.getInstance(getApplicationContext());
				HttpGet httpget = new HttpGet(Config.BEARSTECH_OPENID_PROVIDER_OIDREQUEST_URI + '/' + oidRequest.getId() + '/' + action);
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
			removeDialog(OIDANSWER_PROGRESS_DIALOG);//TODO verify
			String error;
			Intent intent;
			switch(result.getStatusCode()){
			case OIDREQUEST_RESPONSE_SUCCEED:
				removeDialog(DIALOG_NOTIFICATION_ID);
				intent = new Intent();
				intent.putExtra(Config.C2DM_MESSAGE_OIDREQUEST_ID, oidRequest.getId());
				setResult(RESULT_OK, intent);
				finish();
				break;
			case OIDREQUEST_RESPONSE_ERROR_BAD_SESSION:
				intent = new Intent(ctx, LoginActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
				startActivityForResult(intent, OID_LOGIN_ACTIVITY_ID);
				break;
			case OIDREQUEST_JSON_ERROR:
				error = getText(R.string.oidrequest_json_error) + result.getErrorMsg();
				Log.d(Config.TAG, error);
				Toast.makeText(ctx, error, Toast.LENGTH_LONG).show();
				break;
			case OIDREQUEST_IO_ERROR:
				error = getText(R.string.oidrequest_io_error) + result.getErrorMsg();
				Log.d(Config.TAG, error);
				Toast.makeText(ctx, error, Toast.LENGTH_LONG).show();
				break;
			case OIDREQUEST_RESPONSE_ERROR_BAD_IDENTITY:
			case OIDREQUEST_RESPONSE_ERROR_BAD_REQUEST:
			case OIDREQUEST_RESPONSE_ERROR_BAD_ACTION:
			default:
				removeDialog(DIALOG_NOTIFICATION_ID);
				error = getText(R.string.oidrequest_request_error) + result.getErrorMsg();
				Log.d(Config.TAG, error);
		    	Toast.makeText(ctx, error, Toast.LENGTH_LONG).show();
				break;
			}
		}
	}
}
