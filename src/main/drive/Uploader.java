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
 * A simple uploader class that takes a folder/file as an argument
 * and uploads them to your google drive.Google drive's client library is not
 * used here as I feel they are very heavy and poorly documented.
 * Here we are straight-forward making http/https calls to specified endpoints
 * and getting the job done.
 */
public class Uploader {
    public static void main(String[] args) throws IOException {

        String str=new Scanner(System.in).next();
        Authenticator authenticator=new Authenticator();
        Uploader uploader=new Uploader();
        File file=new File(str);
        authenticator.setUserCredentials(CLIENT_ID,CLIENT_SECRET);
        String accesstoken=authenticator.getAccessToken();
        String url=uploader.getLocationUrl(accesstoken,file);
        uploader.upload(file,url,accesstoken);

    }

    /*
         The 'Content-Type' header is optional,drive detects it automatically.
     */
    private  void upload(File file,String uploadurl,String accesscode){
        HttpsURLConnection conn=null;
        Map<String,String> headers=new HashMap<String, String>();
        //headers.put("Content-Type","text/plain");
        headers.put("Content-Length",Integer.toString((int)file.length()));
        headers.put("Authorization","Bearer "+accesscode);
        conn=buildHttpsConnection(uploadurl,headers,"PUT",null,file);
        try {
            conn.connect();
            System.out.println(conn.getResponseCode());
        }catch (IOException e){

        }
    }

    /*
        Get the location url of given file,for uploading the data.
     */
    private String getLocationUrl(String accesstoken,File file){
        String body="{\"name\": \""+file.getName()+"\"}";
        Map<String,String> headers=new HashMap<String, String>();
        headers.put("Authorization","Bearer "+accesstoken);
        headers.put("X-Upload-Content-Type","text/plain");
        headers.put("X-Upload-Content-Length",Integer.toString((int) file.length()));
        headers.put("Content-Type","application/json; charset=UTF-8");
        headers.put("Content-Length",Integer.toString(body.length()));
        HttpsURLConnection conn=buildHttpsConnection(UPLOAD_URI,headers,"POST",body,null);
        try {
            conn.connect();
            System.out.println(conn.getResponseCode());
        }catch (IOException e){

        }
        String uploadurl=conn.getHeaderField("Location");
        System.out.println(uploadurl);
        return uploadurl;
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

    /*
        Build a Http/Https request with given 'url'.and headers in 'headers' map.
        'body' and 'file' are optional arguments may not be present all the time.
     */
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