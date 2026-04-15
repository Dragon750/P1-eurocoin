package eurocoin.util;

// trabajo con Json en Gson
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;


public class Registro {
   private final String nombreArchivo;

   public Registro(String nArch) throws java.io.IOException {
      this.nombreArchivo = new String(nArch);

      java.io.File f = new java.io.File(nombreArchivo);
      if (!f.exists()) throw new java.io.IOException("No existe el archivo: "+nArch);
   }

   public String getValor(String propiedad) throws java.io.IOException {
      java.io.InputStreamReader archivo = new java.io.InputStreamReader(
                      new java.io.FileInputStream(nombreArchivo));
      java.util.HashMap<String,String> registro =
         (new Gson()).fromJson(archivo,
            new TypeToken<java.util.HashMap<String, String>>() {}.getType());
      archivo.close();
      return (String)registro.get(propiedad);
   }

   public void setValor(String propiedad, String valor)
                   throws java.io.IOException {
      // consigue el HashMap con el registro completo.
      java.io.InputStreamReader archivo = new java.io.InputStreamReader(
                      new java.io.FileInputStream(nombreArchivo));
      java.util.HashMap<String,String> registro =
         (new Gson()).fromJson(archivo,
            new TypeToken<java.util.HashMap<String, String>>() {}.getType());
      archivo.close();

      // actualiza el valor, localmente
      registro.put(propiedad, valor);

      // guarda el registro completo
      java.io.PrintWriter archivo2 =
         new java.io.PrintWriter(new java.io.File(nombreArchivo));

      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      String prettyRegistro = gson.toJson(registro);

      archivo2.print(prettyRegistro);
      archivo2.close();
      return;
   }
}

