package eurocoin.util;

public class Digest {
 private java.security.MessageDigest md ;

 public Digest(String algorithm) {
   try {
     this.md = java.security.MessageDigest.getInstance(algorithm);
   } catch (java.security.NoSuchAlgorithmException e) {
     throw new RuntimeException("Digest: No disponible: "+algorithm, e);
   }
 }

 public String generateHash(String input) {
      md.update(input.getBytes());
      byte[] digest = md.digest();

      // byte[] a hexadecimal.
      StringBuilder hexString = new StringBuilder();
      for (byte b : digest) {
        hexString.append(String.format("%02x", b));
      }
      return hexString.toString();
  }
}

