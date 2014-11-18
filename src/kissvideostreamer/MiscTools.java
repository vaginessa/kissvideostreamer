package kissvideostreamer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * 
 * @author tonikelope
 */
public class MiscTools {
    
    public static int[] bin2i32a(byte[] bin)
    {
        ByteBuffer bin_buffer = ByteBuffer.wrap(bin);
        IntBuffer int_buffer = bin_buffer.asIntBuffer();
        
        if(int_buffer.hasArray()) {
            return int_buffer.array();
        }
        else
        {
            ArrayList<Integer> list = new ArrayList();
        
            while(int_buffer.hasRemaining()) {
                list.add(int_buffer.get());
            }

            int[] aux = new int[list.size()];

            for(int i=0; i<aux.length; i++) {
                aux[i] = list.get(i);
            }

            return aux;
        }
    }
    
    public static byte[] i32a2bin(int[] i32a)
    {
        ByteBuffer bin_buffer  = ByteBuffer.allocate(i32a.length * 4);        
        IntBuffer int_buffer = bin_buffer.asIntBuffer();
        int_buffer.put(i32a);
        
        if(bin_buffer.hasArray()) {
            return bin_buffer.array();
        }
        else
        {
            ArrayList<Byte> list = new ArrayList();
        
            while(int_buffer.hasRemaining()) {
                list.add(bin_buffer.get());
            }

            byte[] aux = new byte[list.size()];

            for(int i=0; i<aux.length; i++) {
                aux[i] = list.get(i);
            }

            return aux;
        }
    }
    
    public static byte[] long2bytearray(long val) {
    
        byte [] b = new byte[8];
        
        for (int i = 7; i >= 0; i--) {
          b[i] = (byte) val;
          val >>>= 8;
        }
        
        return b;
    }
    
    public static long bytearray2long(byte[] val) {
        
        long l=0;
        
        for (int i = 0; i <=7; i++) {
            l+=val[i];
            l<<=8;
        }
        
        return l;
    }
    
    public static String findFirstRegex(String regex, String data, int group)
    {
        Pattern pattern = Pattern.compile(regex);
        
        Matcher matcher = pattern.matcher(data);
        
        return matcher.find()?matcher.group(group):null;   
    }
    
    public static String HashString(String algo, String data) throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        MessageDigest md = MessageDigest.getInstance(algo);
        
        byte[] thedigest = md.digest(data.getBytes("UTF-8"));
        
        BigInteger bi = new BigInteger(1, thedigest);
    
        return String.format("%0" + (thedigest.length << 1) + "x", bi);
    }
    
    public static String HashString(String algo, byte[] data) throws NoSuchAlgorithmException
    {
        MessageDigest md = MessageDigest.getInstance(algo);
        
        byte[] thedigest = md.digest(data);
        
        BigInteger bi = new BigInteger(1, thedigest);
    
        return String.format("%0" + (thedigest.length << 1) + "x", bi);
    }
    
    public static byte[] HashBin(String algo, String data) throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        MessageDigest md = MessageDigest.getInstance(algo);
        
        return md.digest(data.getBytes("UTF-8"));
    }
    
    public static byte[] HashBin(String algo, byte[] data) throws NoSuchAlgorithmException
    {
        MessageDigest md = MessageDigest.getInstance(algo);
        
        return md.digest(data);
    }
    
    public static byte[] BASE642Bin(String data) throws IOException
    {
        BASE64Decoder decoder = new BASE64Decoder();
       
        int count_padding = (4 - data.length()%4)%4;
        
        for(int i=0; i<count_padding; i++)
            data+='=';
        
        return decoder.decodeBuffer(data);
    }
    
    public static String Bin2BASE64(byte[] data) throws IOException
    {
        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(data);
    }
    
    public static byte[] UrlBASE642Bin(String data) throws IOException
    {
        return MiscTools.BASE642Bin(data.replace('_', '/').replace('-', '+').replace(",", ""));
    }
    
    public static String Bin2UrlBASE64(byte[] data) throws IOException
    {
        return MiscTools.Bin2BASE64(data).replace('/', '_').replace('+', '-');
    }
    
    public static long getWaitTimeExpBackOff(int retryCount, int pow_base, int secs_by_retry, long max_time) {

        long waitTime = ((long) Math.pow(pow_base, retryCount) * secs_by_retry);

        return Math.min(waitTime, max_time);
    }
    
    public static String bin2hex(byte[] b){
        
        BigInteger bi = new BigInteger(1, b);

        return String.format("%0" + (b.length << 1) + "x", bi);
    }
}
