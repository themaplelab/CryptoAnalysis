
package pkm.constantKey;

import org.alexmbraga.utils.U;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

public final class ConstantKeyforMAC {

  public static void main(String args[]) throws NoSuchAlgorithmException,
          NoSuchPaddingException, InvalidKeyException, BadPaddingException,
          IllegalBlockSizeException, NoSuchProviderException, 
          InvalidAlgorithmParameterException {

      
    byte[] ck = {(byte)0x01,(byte)0x23,(byte)0x45,(byte)0x67,
                   (byte)0x89,(byte)0xAB,(byte)0xCD,(byte)0xEF,
                   (byte)0x01,(byte)0x23,(byte)0x45,(byte)0x67,
                   (byte)0x89,(byte)0xAB,(byte)0xCD,(byte)0xEF,
                   (byte)0x01,(byte)0x23,(byte)0x45,(byte)0x67,
                   (byte)0x89,(byte)0xAB,(byte)0xCD,(byte)0xEF,
                   (byte)0x01,(byte)0x23,(byte)0x45,(byte)0x67,
                   (byte)0x89,(byte)0xAB,(byte)0xCD,(byte)0xEF};  
    
    SecretKeySpec sks1 = new SecretKeySpec(ck, "HMACSHA256");
    
    Mac m = Mac.getInstance("HMACSHA256","SunJCE");
    m.init(sks1);
    
    byte[] msgAna = "This is a test for MAC".getBytes();
    byte[] tag = m.doFinal(msgAna);
    
    //This is for verification 
    SecretKeySpec sks = new SecretKeySpec(ck, "HMACSHA256");
    m.init(sks);
    byte[] tag2 = m.doFinal(msgAna);
    boolean ok = MessageDigest.isEqual(tag2,tag); 
    if (ok) {
      U.println("Do match!");
    } else {
      U.println("Do not match!");}
  }
  
}
