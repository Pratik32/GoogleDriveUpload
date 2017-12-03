package main.drive;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static main.drive.Constants.*;
/**
 * Created by ps on 29/11/17.
 */
public class Uploader {
    public static void main(String[] args) throws IOException {

        String accesstoken="",refreshtoken="";
        if(!new File(".creds").exists()){
            generatev3token();
        }
        String[] temp=readTokensFromFile();
        accesstoken=temp[0];
        refreshtoken=temp[1];
        System.out.println(accesstoken);
        File file=new File("test.txt");
        String body="{\"name\": \""+file.getName()+"\"}";
        Map<String,String> headers=new HashMap<String, String>();
        headers.put("Authorization","Bearer "+accesstoken);
        headers.put("X-Upload-Content-Type","text/plain");
        headers.put("X-Upload-Content-Length",Integer.toString((int) file.length()));
        headers.put("Content-Type","application/json; charset=UTF-8");
        headers.put("Content-Length",Integer.toString(body.length()));
        HttpsURLConnection conn=buildHttpsConnection(UPLOAD_URI,headers,"POST",body,null);
        conn.connect();
        System.out.println(conn.getResponseCode());
        String uploadurl=conn.getHeaderField("Location");
        System.out.println(uploadurl);
        upload(file,uploadurl,accesstoken);

    }

    public static void upload(File file,String uploadurl,String accesscode) throws IOException {
        HttpsURLConnection conn=null;
        Map<String,String> headers=new HashMap<String, String>();
        headers.put("Content-Type","text/plain");
        headers.put("Content-Length",Integer.toString((int)file.length()));
        headers.put("Authorization","Bearer "+accesscode);
        conn=buildHttpsConnection(uploadurl,headers,"PUT",null,file);
        conn.connect();
        System.out.println(conn.getResponseCode());

    }
    public static void generatev2AccessToken() throws IOException {
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
        saveTokens(tokens[0],tokens[1]);
    }

    public static void generatev3token() throws IOException {
        String url=V3_URL+"redirect_uri="+"https://localhost&"+"response_type=code&"+
                    "client_id="+CLIENT_ID+"&"+"scope="+V3_SCOPE+"&"+"access_type=offline";
        System.out.println("Go the following url:"+url);
        HttpsURLConnection conn;
        String code=new Scanner(System.in).next();
        conn=(HttpsURLConnection)new URL(AUTH_URL).openConnection();
        String params="";
        params="code="+code+"&client_id="+CLIENT_ID+"&client_secret="+CLIENT_SECRET+
                        "&redirect_uri="+REDIRECT_URI+"&grant_type=authorization_code";
        Map<String,String> headers=new HashMap<String, String>();
        headers.put("Content-Type","application/x-www-form-urlencoded");
        conn=buildHttpsConnection(AUTH_URL,headers,"POST",params,null);
        conn.connect();
        System.out.println(conn.getResponseCode());
        BufferedReader reader=new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String[] temp=getValuesForKeys(reader,"access_token","refresh_token");
        saveTokens(temp[0],temp[1]);
    }
    public static String[] getValuesForKeys(BufferedReader reader, String key1,String key2) throws IOException {
        String str = null;
        String value1 = "";
        String value2 = "";
        while ((str = reader.readLine()) != null) {
            if (str.contains("\"" + key1 + "\"")) {
                int temp = str.indexOf(':');
                value1 = str.substring(temp + 3, str.length() - 2);
            }else if(str.contains("\""+key2+"\"")){
                int temp=str.indexOf(':');
                value2=str.substring(temp+3,str.length()-2);
            }
        }
        String[] tokens={value1,value2};
        return tokens;
    }
    public static void printResponse(HttpsURLConnection conn){
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String str;
            while((str=reader.readLine())!=null){
                System.out.println(str);
            }
        }catch (IOException e){
        }
    }

    public static HttpsURLConnection buildHttpsConnection(String url,Map<String,String> headers,String method,String body,File file){
        HttpsURLConnection conn=null;
        try {
            URL url1 = new URL(url);
            conn = (HttpsURLConnection)url1.openConnection();
            conn.setRequestMethod(method);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            if (headers!=null) {
                for (Map.Entry<String, String> e : headers.entrySet()) {
                    conn.setRequestProperty(e.getKey(), e.getValue());
                }
            }
            if (body!=null) {
                OutputStream stream = conn.getOutputStream();
                stream.write(body.getBytes());
                stream.close();
            }
            if (file!=null){
                conn.setRequestProperty("Content-Length",Integer.toString((int) file.length()));
                ByteArrayOutputStream stream=new ByteArrayOutputStream();
                FileInputStream stream1=new FileInputStream(file);
                int temp;
                while((temp=stream1.read())!=-1){
                    stream.write((byte)temp);
                }
                OutputStream stream2=conn.getOutputStream();
                stream2.write(stream.toByteArray());
                stream2.close();
            }
        }catch(IOException e){

        }
        return conn;
    }
    private static void saveTokens(String access,String refresh){
        File file=new File(".creds");
        try {
            FileOutputStream stream=new FileOutputStream(file,true);
            stream.write(access.getBytes());
            stream.write('\n');
            stream.write(refresh.getBytes());
            stream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static String[] readTokensFromFile(){
        String[] tokens=new String[2];
        try {
            BufferedReader reader=new BufferedReader(new FileReader(new File(".creds")));
            tokens[0]=reader.readLine();
            tokens[1]=reader.readLine();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tokens;
    }
}

