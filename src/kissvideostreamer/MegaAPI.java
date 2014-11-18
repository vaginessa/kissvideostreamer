package kissvideostreamer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

/**
 * TODO: add methods to get metadata info without require MegaCrypter API
 * @author tonikelope
 */
public class MegaAPI {
 
    public static final String API_URL = "https://g.api.mega.co.nz";
    
    public static int seqno;
    
    public static String api_key=null;
    
    public MegaAPI()
    {
        Random randomno = new Random();
            
        seqno=randomno.nextInt();
    }
    
    public MegaAPI(String ak)
    {
        Random randomno = new Random();
            
        seqno=randomno.nextInt();
        
        api_key=ak;
    }
     
    public String getMegaFileDownloadUrl(String link) throws IOException, MegaAPIException
    {
        seqno++;

        String file_id = MiscTools.findFirstRegex("#!([^!]+)", link, 1);
        
        String request = "[{\"a\":\"g\", \"g\":\"1\", \"p\":\""+file_id+"\"}]";
        
        URL url_api = new URL(API_URL+"/cs?id="+seqno+(api_key!=null?"&ak="+api_key:""));
        HttpURLConnection conn = (HttpURLConnection) url_api.openConnection();
        conn.setConnectTimeout(ProxyStreamServer.CONNECT_TIMEOUT);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        
        OutputStream out;
        out = conn.getOutputStream();
	out.write(request.getBytes());
        out.close();
        
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK)
        {    
            throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
        }
	
        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
  
        String response, response_guay="";

        while ((response = br.readLine()) != null)
        {
                response_guay+=response;
        }

        br.close();
        
        conn.disconnect();
        
        String data = response_guay.replaceAll("\\\\", "");
        
        int mega_error;
        
        if((mega_error=checkMEGAError(data))!=0)
        {
            throw new MegaAPIException(String.valueOf(mega_error));
        }
        
        return MiscTools.findFirstRegex("\"g\" *: *\"([^\"]+)\"", response_guay.replaceAll("\\\\", ""), 1);
    }
    
    private int checkMEGAError(String data)
    {
        String error = MiscTools.findFirstRegex("\\[(\\-[0-9]+)\\]", data, 1);

        return error != null?Integer.parseInt(error):0;
    }
}
