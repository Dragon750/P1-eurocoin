package snippets;


// función de hash criptografica
import eurocoin.util.Digest;
import eurocoin.util.Registro;


public class PruebaRegistro {
   public static void main(String[] var0) {
      try {
         Registro registro = new Registro("Registro.json");

         String hashfunc = registro.getValor("FuncionHash");
         System.out.println("Algoritmo: " + hashfunc);
         Digest digest   = new Digest(hashfunc);

         String passwd_pt = "HolaSoyPedroLuis";
         System.out.println("ClaveUsuario: " + passwd_pt);

         String salt = registro.getValor("PasswordSalt");
         System.out.println("PasswordSalt: " + salt);
         String hash_calc = digest.generateHash(passwd_pt + salt);

         System.out.println("PasswordHash-calculado: " + hash_calc);
         String hash_almac = registro.getValor("PasswordHash");

         System.out.println("PasswordHash: " + hash_almac);
         if (0 == hash_almac.compareTo(hash_calc)) {
            System.out.println("Las claves coinciden");
         } else {
            System.out.println("Las claves no coinciden");
         }

         registro.setValor("FondosEC", "500");

      } catch (java.io.IOException ioe) {
         System.out.println("Hemos tenido problemas para leer Registro.");
      }
   }
}

