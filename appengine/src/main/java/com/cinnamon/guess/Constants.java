package com.cinnamon.guess;

/**
 * Contains the client IDs for allowed clients consuming the Geometer API.
 */
public class Constants {

    public static final boolean DEVELOPMENT = false;
    //public static final boolean DEVELOPMENT = true;

    public static final String PROJECT_ID = "cinn-guess-number";

    public static final String WEB_CLIENT_ID = "221520974985-urrr9685qitjvdj4c5i7je37t53eqa59.apps.googleusercontent.com";
    public static final String ANDROID_CLIENT_ID = "221520974985-g1aphg98vc71uhq8sgikgv5r92n8kiut.apps.googleusercontent.com";
    public static final String IOS_CLIENT_ID = "3-ios-apps.googleusercontent.com"; // TODO
    public static final String API_EXPLORER_CLIENT_ID = "com.google.api.server.spi.Constant.API_EXPLORER_CLIENT_ID";
    public static final String ANDROID_AUDIENCE = WEB_CLIENT_ID;

    public static final String EMAIL_SCOPE = "https://www.googleapis.com/auth/userinfo.email";

    public static final String KEY_GCM_API = System.getProperty("gcm.api.key");
    public static final String KEY_MSG = "guess-msg";
    public static final String KEY_MATCH = "guess-match-id";

    public static final String MSG_NEW_MATCH = "new match";
    public static final String MSG_MATCH_UPDATE = "match update";
}