package main.drive;

/**
 * Created by ps on 29/11/17.
 */
public class Constants {

    public static final String DEVICE_CODE_SCOPE="https://docs.google.com/feeds";
    public static final String CLIENT_ID="685816095141-3gacj3868bd122l8klj6gh5rferjc0it.apps.googleusercontent.com";
    public static final String CLIENT_SECRET="sRxoS4qQTJkYBnKbKZYU6u1c";
    public static final String DEVICE_CODE_URL="https://accounts.google.com/o/oauth2/device/code";
    public static final String AUTH_URL="https://accounts.google.com/o/oauth2/token";
    public static final String GRANT_TYPE="http://oauth.net/grant_type/device/1.0";
    public static final String UPLOAD_URI="https://www.googleapis.com/upload/drive/v3/files/?uploadType=resumable";
    public static final String V3_URL="https://accounts.google.com/o/oauth2/auth?";
    public static final String V3_SCOPE="https://www.googleapis.com/auth/drive";
    public static final String REDIRECT_URI="https://localhost";
    public static final String CREDFILE=System.clearProperty("user.home")+"/.creds";

}
