/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package kissvideostreamer;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import netscape.javascript.JSObject;

/**
 *
 * @author tonikelope
 */
public class ProxyStreamServerHandler implements HttpHandler {
    
    private ProxyStreamServer proxy;
    
    private String file_name;
    
    private long file_size;
    
    private String file_key;
    
    private String pass_hash;
    
    private String noexpire_token;
        
        public ProxyStreamServerHandler(ProxyStreamServer proxy) {
           
            this.proxy = proxy;
        }
    
        @Override
        public void handle(HttpExchange xchg) throws IOException {
           
            long tot_bytes_stream=0;
            
            long clength=0;
            
            OutputStream os=null;
            
            CipherInputStream cis = null;
            
            try{
                this.proxy.printStatusOK("Kissvideostreamer beta "+ProxyStreamServer.VERSION+" (Request received! Dispatching it...)");
                this.proxy.getPanel().debug.append("Kissvideostreamer beta "+ProxyStreamServer.VERSION+" Request received!\n\n");
                
                Headers reqheaders=xchg.getRequestHeaders();
                
                Headers resheaders = xchg.getResponseHeaders();
                
                resheaders.add("Accept-Ranges", "bytes");
            
                for (Map.Entry<String, List<String>> aux : reqheaders.entrySet()) {
                        this.proxy.getPanel().debug.append(aux.getKey()+": "+aux.getValue().get(0)+"\n");
                    }
 

                String url_path = xchg.getRequestURI().getPath();
            
            if(url_path.equals("/video/"))
            {
                xchg.sendResponseHeaders(200, "OK".length());

                os = xchg.getResponseBody();

                os.write("OK".getBytes());
            }
            else
            {  
               String link = url_path.substring(url_path.indexOf("/video/")+7);
               
                if(link.indexOf("mega/") == 0)
                {
                    link = link.replaceAll("mega/", "https://mega.co.nz/#");
                }
                else
                {
                    JSObject win = (JSObject) JSObject.getWindow(this.proxy.getPanel());

                    link = (String)win.eval("window.location.protocol;")+"//"+(String)win.eval("window.location.hostname;")+"/"+link;
                }
               
               this.proxy.getPanel().debug.append("Url: "+link+"\n\n");

               this.proxy.printStatusOK("Kissvideostreamer beta "+ProxyStreamServer.VERSION+" (Retrieving file metadata...)");
               this.proxy.getPanel().debug.append("Kissvideostreamer beta "+ProxyStreamServer.VERSION+" (Retrieving file metadata...)\n\n");
               this.proxy.getPanel().debug.setCaretPosition( this.proxy.getPanel().debug.getDocument().getLength() );
               
               String[] cache_info, file_info;
               
               cache_info = this.proxy.getFromLinkCache(link);
               
               if(cache_info!=null) { 
                   
                    file_info = new String[6];
                   
                    System.arraycopy( cache_info, 0, file_info, 0, cache_info.length );
                    
               } else {
                    
                    file_info = this.proxy.getMegaFileMetadata(link, this.proxy.getPanel());
                   
                    cache_info = new String[6];
                    
                    System.arraycopy( file_info, 0, cache_info, 0, file_info.length );
                    
                    cache_info[5]=null; 
               }
               
               this.file_name = file_info[0];
               
               this.file_size = Long.parseLong(file_info[1]);
               
               this.file_key = file_info[2];
               
               if(file_info.length == 5)
               {
                   this.pass_hash = file_info[3];
                   
                   this.noexpire_token = file_info[4];
                   
               } else {
                   this.pass_hash = null;
                   
                   this.noexpire_token = null;
               }
                   
         
               this.proxy.getPanel().getJSwin().eval("if(typeof update_vlc_file_name == 'function'){update_vlc_file_name('"+this.file_name+"');}");
               
               String file_ext = this.file_name.substring(this.file_name.lastIndexOf(".")+1).toLowerCase();

               resheaders.add("Content-Type", this.proxy.getCtype().getMIME(file_ext));
               
               resheaders.add("Connection", "close");

               URLConnection urlConn;
                
               byte[] buffer = new byte[8*1024];
               int reads;
               

                   this.proxy.printStatusOK("Kissvideostreamer beta "+ProxyStreamServer.VERSION+" (Retrieving file url...)");
                   this.proxy.getPanel().debug.append("Kissvideostreamer beta "+ProxyStreamServer.VERSION+" (Retrieving file url...)\n\n");
                   this.proxy.getPanel().debug.setCaretPosition( this.proxy.getPanel().debug.getDocument().getLength() );

                   String temp_url;
                   
                   if(cache_info[5]!=null) {
                       
                       temp_url = cache_info[5];
                       
                       if(!this.proxy.checkDownloadUrl(temp_url)) {
                           
                           temp_url = this.proxy.getMegaFileDownloadUrl(link, this.pass_hash, this.noexpire_token);
                           
                           cache_info[5] = temp_url;
                           
                           this.proxy.getPanel().debug.append("Using NEW URL\n\n");
                       
                           this.proxy.updateLinkCache(link, cache_info);
                       } else {
                           this.proxy.getPanel().debug.append("Reusing URL from cache\n\n");
                       }
                           
                   } else {
                       temp_url = this.proxy.getMegaFileDownloadUrl(link, this.pass_hash, this.noexpire_token);
                       
                       cache_info[5] = temp_url;
                       
                       this.proxy.updateLinkCache(link, cache_info);
                   }
      
                   this.proxy.printStatusOK("Kissvideostreamer beta "+ProxyStreamServer.VERSION+" (Connecting...)");
                   this.proxy.getPanel().debug.append("Kissvideostreamer beta "+ProxyStreamServer.VERSION+" (Connecting...)\n\n");
                   this.proxy.getPanel().debug.setCaretPosition( this.proxy.getPanel().debug.getDocument().getLength() );
    
                   long[] ranges=new long[2];
                   
                   int sync_bytes=0;
                   
                   String header_range=null;
                   
                   InputStream is;
                   
                   URL url;
                   
                   if(reqheaders.containsKey("Range"))
                   {
                       header_range = "Range";
                       
                   } else if(reqheaders.containsKey("range")) {
                       header_range = "range";
                   }
                   
                   if(header_range != null)
                   {
                       List<String> ranges_raw = reqheaders.get(header_range);
                       
                       String range_header=ranges_raw.get(0);

                       ranges = this.proxy.parseRangeHeader(range_header);
    
                       sync_bytes = (int)ranges[0] % 16;
     
                       if(ranges[1]>=0 && ranges[1]>=ranges[0]) {
                           
                           clength = ranges[1]-ranges[0]+1;
                       
                       } else {
                           
                           clength = this.file_size - ranges[0];
                       
                       }

                       resheaders.add("Content-Range", "bytes "+ranges[0]+"-"+(ranges[1]>=0?ranges[1]:(this.file_size-1))+"/"+this.file_size);
                       
                       xchg.sendResponseHeaders(206, clength);
                       
                       url = new URL(this.proxy.cookRangeUrl(temp_url, ranges, sync_bytes));
                       
                       this.proxy.getPanel().debug.append("Range requested -> "+ranges[0]+"-"+(ranges[1]>=0?ranges[1]:"")+" (sync_bytes: "+sync_bytes+")\n\n");
                   } else {
                      
                       xchg.sendResponseHeaders(200, this.file_size);
                       
                       url = new URL(temp_url);
                   }
                     
                   urlConn = url.openConnection();
                   urlConn.setConnectTimeout(ProxyStreamServer.CONNECT_TIMEOUT);
                   is = urlConn.getInputStream();
                   
                   byte[] iv = CryptTools.initMEGALinkKeyIV(this.file_key);

                   try {

                       cis = new CipherInputStream(is, CryptTools.genDecrypter("AES", "AES/CTR/NoPadding", CryptTools.initMEGALinkKey(this.file_key), (header_range!=null && (ranges[0]-sync_bytes)>0)?CryptTools.forwardMEGALinkKeyIV(iv, ranges[0]-sync_bytes):iv));

                   } catch (    NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException ex) {
                       Logger.getLogger(ProxyStreamServer.class.getName()).log(Level.SEVERE, null, ex);
                   }

                   os = xchg.getResponseBody();

                   this.proxy.printStatusOK("Kissvideostreamer beta "+ProxyStreamServer.VERSION+" (Streaming file...)");
                   this.proxy.getPanel().debug.append("Kissvideostreamer beta "+ProxyStreamServer.VERSION+" (Sending response...)\n\n");
 
                   for (Map.Entry<String, List<String>> aux : resheaders.entrySet()) {
                        this.proxy.getPanel().debug.append(aux.getKey()+": "+aux.getValue().get(0)+"\n");
                    }
                   
                   this.proxy.getPanel().debug.append("\n");
                   
                   this.proxy.getPanel().debug.setCaretPosition( this.proxy.getPanel().debug.getDocument().getLength() );
                   
                   tot_bytes_stream=0;
                    
                    //Skip sync bytes
                    for(int i=0; i<sync_bytes; i++)
                    {
                        cis.read();
                    }
                    
                    boolean exception;

                    do
                    {
                        exception = false;
                        reads=-1;
                        
                        try
                        {
                            if((reads=cis.read(buffer))!=-1)
                            {
                                try
                                {
                                    os.write(buffer, 0, reads);
                                    tot_bytes_stream+=reads;

                                }catch(Exception ex)
                                {
                                    this.proxy.getPanel().debug.append("Output exception -> "+ex.getMessage()+"\n\n");
                                    exception=true;
                                }
                            }
                            
                        }catch(Exception ex)
                        {
                            this.proxy.getPanel().debug.append("Input exception -> "+ex.getMessage()+"\n\n");
                            exception=true;
                        }

                    } while(!exception && reads!=-1);
              }
         
        }
        catch(Exception ex)
        {
            this.proxy.getPanel().debug.append(ex.getClass().getName()+" "+ex.getMessage()+"\n\n");
            
        }
        finally
        {          
            try
            {
                if(cis!=null) {
                    cis.close();
                }
                
            }catch(IOException ex){}
            
            try
            {
                if(os!=null) {
                    os.close();
                }
                
            }catch(IOException ex){}
            
            this.proxy.getPanel().debug.append("Kissvideostreamer beta "+ProxyStreamServer.VERSION+" bye bye (bytes streamed: "+tot_bytes_stream+"/"+clength+")\n\n");
           
            this.proxy.printStatusOK("Kissvideostreamer beta "+ProxyStreamServer.VERSION+" loaded! (Waiting for request...)");
            
            this.proxy.getPanel().debug.append("Kissvideostreamer beta "+ProxyStreamServer.VERSION+" loaded! (Waiting for request...)\n\n");
            
            this.proxy.getPanel().debug.setCaretPosition( this.proxy.getPanel().debug.getDocument().getLength() );

            xchg.close();
        }
   }     
}
