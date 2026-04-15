package eurocoin.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class Movimientos {
    private final String nombreArchivo;

    public Movimientos(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public void anotar(String linea) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(nombreArchivo, true))) {
            out.println(linea);
        }
    }

    public void anotarOperacion(String json, String mac) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(nombreArchivo, true))) {
            out.println(json);
            out.println(mac);
        }
    }

    public boolean contieneTexto(String textoBuscado) throws IOException {
        File f = new File(nombreArchivo);
        if (!f.exists()) {
            return false;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                if (linea.contains(textoBuscado)) {
                    return true;
                }
            }
        }
        return false;
    }
}
