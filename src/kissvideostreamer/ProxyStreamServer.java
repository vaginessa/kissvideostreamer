package kissvideostreamer;

import com.sun.net.httpserver.HttpServer;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 
 * @author tonikelope
 */
public class ProxyStreamServer {
    
    public static final String VERSION="9.2";
    public static final int CONNECT_TIMEOUT=30000;
    public static final int DEFAULT_PORT=1337;
    public static final int EXP_BACKOFF_BASE=2;
    public static final int EXP_BACKOFF_SECS_RETRY=1;
    public static final int EXP_BACKOFF_MAX_WAIT_TIME=128;
    public static final int ANTI_FLOOD=1000;
    private HttpServer httpserver;
    private MainBox panel;
    private ConcurrentHashMap<String, String[]> link_cache;
    private ContentType ctype;
   
    
    public MainBox getPanel()
    {
        return this.panel;
    }
    
    public ContentType getCtype()
    {
        return this.ctype;
    }
   
    public ProxyStreamServer(MainBox panel) {
        this.panel = panel;
        this.link_cache = new ConcurrentHashMap();
        this.ctype = new ContentType();
    }
    
    public void start(int port, String context) throws IOException
    {
        this.httpserver = HttpServer.create(new InetSocketAddress(port), 0);
        this.printStatusOK("Kissvideostreamer beta "+ProxyStreamServer.VERSION+" loaded! (Waiting for request...)");
        this.panel.debug.append("Kissvideostreamer beta "+ProxyStreamServer.VERSION+" loaded! (Waiting for request...)\n\n");
        this.panel.debug.setCaretPosition( this.panel.debug.getDocument().getLength() );
        this.httpserver.createContext(context, new ProxyStreamServerHandler(this));
        this.httpserver.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        this.httpserver.start();
    }
    
    public void stop()
    {
        this.httpserver.stop(0);
    }
    
    public void printStatusError(String message)
    {
        this.panel.status.setForeground(Color.red);
        this.panel.status.setText(message);
    }
    
    public void printStatusOK(String message)
    {
        this.panel.status.setForeground(new Color(0,128,0));
        this.panel.status.setText(message);
    }
    
    public String[] getFromLinkCache(String link)
    {
        return this.link_cache.containsKey(link)?this.link_cache.get(link):null;
    }
    
    public void updateLinkCache(String link, String[] info) {
        
        this.link_cache.put(link, info);
    }
    
    public void removeFromLinkCache(String link) {
        this.link_cache.remove(link);
    }
    
   public synchronized String[] getMegaFileMetadata(String link, javax.swing.JApplet panel) throws IOException, InterruptedException
   {
        this.getPanel().debug.append("Anti-flood ("+(ANTI_FLOOD/1000)+" secs)...\n\n");
               
        this.getPanel().debug.setCaretPosition( this.getPanel().debug.getDocument().getLength() );
               
        Thread.sleep(ANTI_FLOOD);
       
        String[] file_info=null;
        int retry=0, error_code=0;
        boolean error;

        do
        {
            error=false;

            try
            {
                if( MiscTools.findFirstRegex("://mega\\.co\\.nz/", link, 0) != null)
                {
                    MegaAPI ma = new MegaAPI();
                    
                    file_info = ma.getMegaFileMetadata(link);
                }    
                else
                {
                    file_info = MegaCrypterAPI.getMegaFileMetadata(link, panel); 
 
                } 

            }
            catch(MegaAPIException e)
            {
                error=true;

                error_code = Integer.parseInt(e.getMessage());

                    for(long i=MiscTools.getWaitTimeExpBackOff(retry++, EXP_BACKOFF_BASE, EXP_BACKOFF_SECS_RETRY, EXP_BACKOFF_MAX_WAIT_TIME); i>0; i--)
                    {
                        if(error_code == -18)
                        {
                            this.printStatusError("File temporarily unavailable! (Retrying in "+i+" secs...)");
                        }
                        else
                        {
                            this.printStatusError("MegaAPIException error "+e.getMessage()+" (Retrying in "+i+" secs...)");
                        }

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {}
                    }
            }
            catch(MegaCrypterAPIException e)
            {
                error=true;

                error_code = Integer.parseInt(e.getMessage());

                switch(error_code)
                { 
                    case 22:
                        throw new IOException("MegaCrypter link is not valid!");

                    case 23:
                        throw new IOException("MegaCrypter link is blocked!");

                    case 24:
                        throw new IOException("MegaCrypter link has expired!");
                        
                    default:
                        for(long i=MiscTools.getWaitTimeExpBackOff(retry++, EXP_BACKOFF_BASE, EXP_BACKOFF_SECS_RETRY, EXP_BACKOFF_MAX_WAIT_TIME); i>0; i--)
                        {
                            if(error_code == -18)
                            {
                                this.printStatusError("File temporarily unavailable! (Retrying in "+i+" secs...)");
                            }
                            else
                            {
                                this.printStatusError("MegaCrypterAPIException error "+e.getMessage()+" (Retrying in "+i+" secs...)");
                            }

                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ex) {}
                        }
                }
            }

        }while(error);
        
        return file_info;
    }
        
   public synchronized String getMegaFileDownloadUrl(String link, String pass_hash, String noexpire_token) throws IOException, InterruptedException
   {
        this.getPanel().debug.append("Anti-flood ("+(ANTI_FLOOD/1000)+" secs)...\n\n");
               
        this.getPanel().debug.setCaretPosition( this.getPanel().debug.getDocument().getLength() );
               
        Thread.sleep(ANTI_FLOOD);
       
        String dl_url=null;
        int retry=0, error_code;
        boolean error;
                
        do
        {
            error=false;
            
            try
            {
                if( MiscTools.findFirstRegex("://mega\\.co\\.nz/", link, 0) != null)
                {
                    MegaAPI ma = new MegaAPI();

                    dl_url = ma.getMegaFileDownloadUrl(link);
                }    
                else
                {
                    dl_url = MegaCrypterAPI.getMegaFileDownloadUrl(link, pass_hash, noexpire_token);
                }
            }
            catch(MegaAPIException e)
            {
                error=true;

                error_code = Integer.parseInt(e.getMessage());

                    for(long i=MiscTools.getWaitTimeExpBackOff(retry++, EXP_BACKOFF_BASE, EXP_BACKOFF_SECS_RETRY, EXP_BACKOFF_MAX_WAIT_TIME); i>0; i--)
                    {
                        if(error_code == -18)
                        {
                            this.printStatusError("File temporarily unavailable! (Retrying in "+i+" secs...)");
                        }
                        else
                        {
                            this.printStatusError("MegaAPIException error "+e.getMessage()+" (Retrying in "+i+" secs...)");
                        }

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {}
                    }
            }
            catch(MegaCrypterAPIException e)
            {
                error=true;

                error_code = Integer.parseInt(e.getMessage());
                
                switch(error_code)
                { 
                    case 22:
                        throw new IOException("MegaCrypter link is not valid!");

                    case 23:
                        throw new IOException("MegaCrypter link is blocked!");

                    case 24:
                        throw new IOException("MegaCrypter link has expired!");
                        
                    default:
                        for(long i=MiscTools.getWaitTimeExpBackOff(retry++, EXP_BACKOFF_BASE, EXP_BACKOFF_SECS_RETRY, EXP_BACKOFF_MAX_WAIT_TIME); i>0; i--)
                        {
                            if(error_code == -18)
                            {
                                this.printStatusError("File temporarily unavailable! (Retrying in "+i+" secs...)");
                            }
                            else
                            {
                                this.printStatusError("MegaCrypterAPIException error "+e.getMessage()+" (Retrying in "+i+" secs...)");
                            }

                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ex) {}
                        }
                }
            }

        }while(error);
        
        return dl_url;
    }
   
    public boolean checkDownloadUrl(String string_url)
    {
        try {
            URL url = new URL(string_url+"/0-0");
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            InputStream is = connection.getInputStream();
            
            while(is.read()!=-1);
            
            is.close();
             
            return true;
            
        }catch (Exception ex) {
            
            return false;
        }        
    }
    
    public long[] parseRangeHeader(String header)
    {
        Pattern pattern = Pattern.compile("bytes\\=([0-9]+)\\-([0-9]+)?");
        
        Matcher matcher = pattern.matcher(header);
        
        long[] ranges=new long[2];
        
        if(matcher.find())
        {
            ranges[0] = Long.valueOf(matcher.group(1));
        
            if(matcher.group(2)!=null) {
                ranges[1] = Long.valueOf(matcher.group(2));
            } else
            {
                ranges[1]=-1;
            }
        }

        return ranges;
    }
    
    public String cookRangeUrl(String url, long[] ranges, int sync_bytes)
    {
        return url+"/"+String.valueOf(ranges[0]-sync_bytes)+(ranges[1]>=0?"-"+String.valueOf(ranges[1]):"");
    }

}

