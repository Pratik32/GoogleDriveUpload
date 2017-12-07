package main.drive;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static main.drive.Constants.*;
import static main.drive.Constants.AUTH_URL;
import static main.drive.Constants.GRANT_TYPE;
import static main.drive.Uploader.buildHttpsConnection;
import static main.drive.Uploader.getValuesForKeys;

/**
 * Created by ps on 5/12/17.
 */
public class Authenticator {

    public static final String CREDS_FILE=System.getProperty("user.home")+"/"+".creds";
    public static final String AUTH_URL="https://accounts.google.com/o/oauth2/token";
    public static final String TOKEN_FILE=System.getProperty("user.home")+"/"+".tokens";
    public static final String AUTHORIZATION_CODE="authorization_code";
    public static final String REFRESH_TOKEN="refresh_token";
    public static final long ACCESS_TOKEN_VALIDITY=3000*1000;
    private String refreshtoken="";
    private String accesstoken="";
    private long lasttokentime=0;

    /*
        Load tokens from tokens file.
     */
    protected Authenticator(){
        if(new File(TOKEN_FILE).exists()) {
            String temp[] = readTokensFromFile();
            accesstoken = temp[0];
            refreshtoken = temp[1];
            lasttokentime = Long.parseLong(temp[2]);
        }
    }
    protected String getAccessToken() throws IOException {
       if(new File(TOKEN_FILE).exists()){
            if (!isTokenValid()){
                System.out.println("Refreshing access token ...");
                refreshAccessToken(refreshtoken,REFRESH_TOKEN);;
            }
           }else{
            generatev3token();
        }
        return accesstoken;
    }

    /*
       For v3 APIs access token can be generated as follows.
       1)A POST request @https://accounts.google.com/o/oauth2/auth returns
       a 'code' at specified redirection url.This is where we allow our program
       to manage(Upload in this case) the user's drive data.This is done only once.
       2)'code' is then used to obtain access_token by a POST request
       @https://accounts.google.com/o/oauth2/token.This returns a json array that
       contains access and refresh tokens.
    */
    private void generatev3token() throws IOException {
        String url=V3_URL+"redirect_uri="+REDIRECT_URI+"&response_type=code&"+
                "client_id="+CLIENT_ID+"&"+"scope="+V3_SCOPE+"&"+"access_type=offline";
        System.out.println("Go the following url:"+url);
        HttpsURLConnection conn;
        String code=new Scanner(System.in).next();
        conn=(HttpsURLConnection)new URL(AUTH_URL).openConnection();
        String[] temp=exchangeTokens("code",code,AUTHORIZATION_CODE);
        lasttokentime=System.currentTimeMillis();
        saveToFile(TOKEN_FILE,temp[0],temp[1],Long.toString(lasttokentime));
        accesstoken=temp[0];
        refreshtoken=temp[1];
    }

    private String[] exchangeTokens(String codetype,String code ,String granttype){
        String params=codetype+"="+code+"&client_id="+CLIENT_ID+"&client_secret="+CLIENT_SECRET+
                "&redirect_uri="+REDIRECT_URI+"&grant_type="+granttype;
        Map<String,String> headers=new HashMap<String, String>();
        headers.put("Content-Type","application/x-www-form-urlencoded");
        HttpsURLConnection conn=buildHttpsConnection(AUTH_URL,headers,"POST",params,null);
        String tokens[]=null;
        try{
            conn.connect();
            System.out.println(conn.getResponseCode());
            BufferedReader reader=new BufferedReader(new InputStreamReader(conn.getInputStream()));
            tokens=getValuesForKeys(reader,"access_token","refresh_token");
        }catch (IOException e){
        }

        return tokens;

    }
    /*
      For v2 APIs,access token can be generated as following:
      1)A POST request at https://docs.google.com/feeds returns
      you 'device_code' which is used  for sending a access_token.This
      API call is for device authentication and it is done only once.
      2)'device_code' is use for sending access_token request at
      https://accounts.google.com/o/oauth2/device/code. Output of this
      request is a json array that contains access_token and a refresh_token.
      3)refresh_token can be used to obtain access_token after it expires.
   */
    private String generatev2AccessToken() throws IOException {
        String params = "client_id=" + CLIENT_ID + "&" + "scope=" + DEVICE_CODE_SCOPE;
        Map<String,String> header=new HashMap<String, String>();
        header.put("Content-Type", "application/x-www-form-urlencoded");
        header.put("Content-Length", Integer.toString(params.length()));
        HttpsURLConnection conn=buildHttpsConnection(DEVICE_CODE_URL,null,"POST",params,null);
        System.out.println(conn.getResponseCode());
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String str = null;
        String devicecode = "";
        String usercode = "";
        while ((str = reader.readLine()) != null) {
            if (str.contains("\"user_code\"")) {
                int temp = str.indexOf(':');
                usercode = str.substring(temp + 3, str.length() - 1);
            } else if (str.contains("\"device_code\"")) {
                int temp = str.indexOf(':');
                devicecode = str.substring(temp + 3, str.length() - 2);
            }
        }
        System.out.println("User code is :" + usercode);
        System.out.println("Device code is :" + devicecode);
        conn.disconnect();
        Scanner scanner = new Scanner(System.in);
        String temp = scanner.next();
        params = "client_id=" + CLIENT_ID + "&" + "client_secret=" + CLIENT_SECRET + "&" +
                "code=" + devicecode + "&" + "grant_type=" + GRANT_TYPE;
        header=new HashMap<String, String>();
        header.put("Content-Type", "application/x-www-form-urlencoded");
        header.put("Content-Length", Integer.toString(params.length()));
        conn=buildHttpsConnection(AUTH_URL,header,"POST",params,null);
        conn.connect();
        System.out.println(conn.getResponseCode());
        reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String tokens[]=getValuesForKeys(reader,"access_token","refresh_token");
        saveToFile(TOKEN_FILE,tokens[0],tokens[1]);
        return tokens[0];
    }
    protected void setUserCredentials(String id,String secret){
        saveToFile(CREDS_FILE,id,secret);
    }

    private void saveToFile(String filename,String... data){
        File file=new File(filename);
        try{
            BufferedWriter writer=new BufferedWriter(new FileWriter(file,true));
            writer.write("");
            for(String str:data){
                writer.write(str+"\n");
            }
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private void refreshAccessToken(String code,String granttype){
        String tokens[]=exchangeTokens("refresh_token",refreshtoken,REFRESH_TOKEN);
        lasttokentime=System.currentTimeMillis();
        accesstoken=tokens[0];
        refreshtoken=tokens[1];
        saveToFile(TOKEN_FILE,accesstoken,refreshtoken,Long.toString(lasttokentime));
    }
    private static String[] readTokensFromFile(){
        String[] tokens=new String[3];
        try {
            BufferedReader reader=new BufferedReader(new FileReader(new File(TOKEN_FILE)));
            tokens[0]=reader.readLine();
            tokens[1]=reader.readLine();
            tokens[2]=reader.readLine();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tokens;
    }

    private boolean isTokenValid(){
        return (System.currentTimeMillis()-lasttokentime)<ACCESS_TOKEN_VALIDITY;

    }
}
