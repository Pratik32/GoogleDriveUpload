package main.drive;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
/**
 * Created by ps on 29/11/17.
 * A simple uploader class that takes a folder/file as an argument
 * and uploads them to your google drive.Google drive's client library is not
 * used here as I feel they are very heavy and poorly documented.
 * Here we are straight-forward making http/https calls to specified endpoints
 * and getting the job done.
 */
public class Uploader {
    public static final String UPLOAD_URI="https://www.googleapis.com/upload/drive/v3/files/?uploadType=resumable";

    public static void main(String[] args) throws IOException {
        Scanner sc=new Scanner(System.in);
        Authenticator authenticator=new Authenticator();
        Uploader uploader=new Uploader();
        args=new String[2];
        args[0]="-r";
        args[1]="DIR";
        if(args.length==0){
            return;
        }
        if(args[0].equals("-config")) {
            System.out.println("CLIENT ID:");
            String id = sc.next();
            System.out.println("CLIENT SECRET:");
            String secret = sc.next();
            Authenticator.setUserCredentials(id, secret);
            System.out.println("Credentials set successfully.");
        }else if(args[0].equals("-r")){
            File file=new File(args[1]);
            String accesstoken=authenticator.getAccessToken();
            String dirname=file.getName();
            String dirid=uploader.createFolder(accesstoken,"root",dirname);
            for (File f:file.listFiles()){
                String uploadurl=uploader.getLocationUrl(accesstoken,f,dirid);
                uploader.upload(f,uploadurl,accesstoken);
            }
        }else{
            for(String str:args){
                File file=new File(str);
                String accesscode=authenticator.getAccessToken();
                String uploadurl=uploader.getLocationUrl(accesscode,file,"root");
                uploader.upload(file,uploadurl,accesscode);
            }
        }
    }

    /*
         The 'Content-Type' header is optional,drive detects it automatically.
     */
    private  void upload(File file,String uploadurl,String accesscode){
        HttpsURLConnection conn=null;
        Map<String,String> headers=new HashMap<String, String>();
        headers.put("Content-Type",getMIMEType(file));
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
    private String getLocationUrl(String accesstoken,File file,String id){
        String body="{\"name\": \""+file.getName()+"\",\"parents\": [\""+id+"\"]}";
        System.out.println(body);
        Map<String,String> headers=new HashMap<String, String>();
        headers.put("Authorization","Bearer "+accesstoken);
        headers.put("X-Upload-Content-Type",getMIMEType(file));
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

    private String createFolder(String accesstoken,String rootfolder,String foldername) throws IOException {
        String id="";
        String body="{\"name\": \""+foldername+"\",\"mimeType\": \"application/vnd.google-apps.folder\",\"parents\": [\""+rootfolder+"\"]}";
        System.out.println(body);
        Map<String,String> headers=new HashMap<String, String>();
        headers.put("Authorization","Bearer "+accesstoken);
        headers.put("Content-Length",Integer.toString(body.length()));
        headers.put("Content-Type","application/json; charset=UTF-8");
        String url="https://www.googleapis.com/drive/v3/files";
        HttpsURLConnection conn=buildHttpsConnection(url,headers,"POST",body,null);
        conn.connect();
        System.out.println(conn.getResponseCode());
        BufferedReader reader=new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String temp[]=getValuesForKeys(reader,"id",null);
        id=temp[0];
        System.out.println("id: "+id);
        return  id;

    }
    public static String[] getValuesForKeys(BufferedReader reader, String key1,String key2) throws IOException {
        String str = null;
        String value1 = "";
        String value2 = "";
        while ((str = reader.readLine()) != null) {
            System.out.println(str);
            if (key1!=null && str.contains("\"" + key1 + "\"")) {
                int start = str.indexOf(':');
                int end=str.lastIndexOf("\"");
                value1 = str.substring(start + 3,end);
            }else if(key2!=null && str.contains("\""+key2+"\"")){
                int temp=str.indexOf(':');
                value2=str.substring(temp+3,str.length()-2);
            }
        }
        String[] tokens={value1,value2};
        return tokens;
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

    private String getMIMEType(File file){
        String type="";
        try {
            InputStream is = new FileInputStream(file);
            type= URLConnection.guessContentTypeFromStream(is);
            if (type==null){
                type=URLConnection.guessContentTypeFromName(file.getName());
            }
            System.out.println("Type:="+type);
        }catch (FileNotFoundException e){

        } catch (IOException e) {
            e.printStackTrace();
        }
        return type;
    }


}