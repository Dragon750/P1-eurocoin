package eurocoin.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RegistroBanco {

    private final String nombreArchivo;

    public RegistroBanco(String nombreArchivo) throws IOException {
        this.nombreArchivo = nombreArchivo;
        File f = new File(nombreArchivo);
        if (!f.exists()) {
            throw new IOException("No existe el archivo: " + nombreArchivo);
        }
    }

    public String getValor(String entidad, String propiedad) throws IOException {
        InputStreamReader archivo =
                new InputStreamReader(new FileInputStream(nombreArchivo));

        Map<String, HashMap<String, String>> registro =
                (new Gson()).fromJson(
                        archivo,
                        new TypeToken<Map<String, HashMap<String, String>>>() {}.getType()
                );

        archivo.close();

        HashMap<String, String> bloque = registro.get(entidad);
        if (bloque == null) {
            return null;
        }
        return bloque.get(propiedad);
    }

    public void setValor(String entidad, String propiedad, String valor) throws IOException {
        InputStreamReader archivo =
                new InputStreamReader(new FileInputStream(nombreArchivo));

        Map<String, HashMap<String, String>> registro =
                (new Gson()).fromJson(
                        archivo,
                        new TypeToken<Map<String, HashMap<String, String>>>() {}.getType()
                );

        archivo.close();

        HashMap<String, String> bloque = registro.get(entidad);
        if (bloque == null) {
            bloque = new HashMap<>();
            registro.put(entidad, bloque);
        }

        bloque.put(propiedad, valor);

        PrintWriter salida = new PrintWriter(new File(nombreArchivo));
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        salida.print(gson.toJson(registro));
        salida.close();
    }
}
