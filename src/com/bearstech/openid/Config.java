package com.bearstech.openid;

public class Config {
	
	protected static final String BEARSTECH_OPENID_PROVIDER_DOMAIN = "openid.bearstech.com";
	protected static final String BEARSTECH_OPENID_PROVIDER_LOGIN_URI = "https://"+BEARSTECH_OPENID_PROVIDER_DOMAIN+"/account/login_json";
	protected static final String BEARSTECH_OPENID_PROVIDER_OIDREQUEST_URI = "http://"+BEARSTECH_OPENID_PROVIDER_DOMAIN+"/server/oidrequest";
	public static final String BEARSTECH_OAUTH_URI = "http://"+BEARSTECH_OPENID_PROVIDER_DOMAIN+"/oauth/finalize";
	protected static final String BEARSTECH_C2DM_REGISTRATION = "http://"+BEARSTECH_OPENID_PROVIDER_DOMAIN+"/server/registerC2DM";

	protected static final String C2DM_SENDER = "grizzlid@gmail.com";
	public static final String C2DM_MESSAGE_TYPE = "type";
	protected static final String C2DM_MESSAGE_OIDREQUEST_ID = "id";
	protected static final String C2DM_MESSAGE_OIDREQUEST_SITE = "trust_root";
	protected static final String C2DM_MESSAGE_OIDREQUEST_REQUIRED_FIELDS = "required_fields";
	public static final String C2DM_MESSAGE_OAUTHREQUEST_URL = "oauth_url";

	protected static final String C2DM_REMOTE_REGISTRATION_STATE = "remote_registration_status";
    
	public static final String TAG = "GrizzlID";
	protected static final String PREFS_NAME = "GrizzlIDPrefs";
    
	protected static final String PREFS_COOKIE = "prefs_cookie";
    
	protected static final String PREFS_PROVIDER_USERNAME = "prefs_provider_username";
	protected static final String PREFS_PROVIDER_PASSWORD = "prefs_provider_pwd";
	protected static final String PREFS_PROVIDER_AUTOLOGIN = "prefs_provider_autologin";
	protected static final String PREFS_REGISTRATON_ID = "prefs_registration_id";
}
