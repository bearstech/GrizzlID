package com.bearstech.grizzlid.oauth;

import java.net.MalformedURLException;
import java.net.URL;

import com.bearstech.openid.Config;
import com.bearstech.openid.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.util.Log;

public class OAuthDialogActivity extends Activity {
	private static final int CONFIRMATION_DIALOG_ID = 1;
	private URL oauth_url;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Intent intent = getIntent();
		if (intent != null) {
			String oauth_token_url = intent.getStringExtra(Config.C2DM_MESSAGE_OAUTHREQUEST_URL);
			if (oauth_token_url != null) {
				try {
					oauth_url = new URL(oauth_token_url);
					if(savedInstanceState == null)
						showDialog(CONFIRMATION_DIALOG_ID);
				} catch (MalformedURLException e) {
					Log.d(Config.TAG,"OAuthDialogActivity: invalid url onCreate!");
					finish();
				}
			} else {
				Log.d(Config.TAG,"OAuthDialogActivity: bad intent extra onCreate!");
				finish();
			}
		} else {
			Log.d(Config.TAG, "OAuthDialogActivity: bad intent onCreate !");
			finish();
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case CONFIRMATION_DIALOG_ID:
			return new OAuthAlertDialog(this, oauth_url);
		default:
			return super.onCreateDialog(id);
		}
	}
	
	private class OAuthAlertDialog extends AlertDialog implements OnCancelListener {
		
		private final String url;
		
		protected OAuthAlertDialog(Context context, URL oauth_url) {
			super(context);
			url = oauth_url.toString();
			String domain = oauth_url.getProtocol() + "://" + oauth_url.getHost();
			setMessage(String.format(getText(R.string.oauth_dialog_confirmation).toString(), domain));
			setCancelable(true);
			setOnCancelListener(this);
			setButton(BUTTON_POSITIVE, getText(R.string.yes), new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent intent = new Intent(OAuthDialogActivity.this, OAuthWebViewActivity.class);
					intent.putExtra(Config.C2DM_MESSAGE_OAUTHREQUEST_URL, url);
					OAuthDialogActivity.this.startActivity(intent);
					finish();
				}
			});
			
			setButton(BUTTON_NEGATIVE, getText(R.string.no), new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
					finish();
				}
			});
		}

		@Override
		public void onCancel(DialogInterface arg0) {
			setResult(RESULT_CANCELED);
			finish();
		}
		
	}

}
