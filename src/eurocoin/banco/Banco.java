package eurocoin.banco;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Banco {
    public static final int PUERTO = 2000;

    // Usuarios con sesion activa en este momento
    private final Set<String> usuariosActivos = ConcurrentHashMap.newKeySet();
    
    private final Set<String> usuariosComprometidos = ConcurrentHashMap.newKeySet();

    public void iniciar() {
        try (ServerSocket servidor = new ServerSocket(PUERTO)) {
            System.out.println("Banco iniciado en puerto " + PUERTO);

            while (true) {
                System.out.println("----Server Waiting For Client----");

                Socket socketCliente = servidor.accept();

                System.out.println("Nueva conexion desde: "
                        + socketCliente.getInetAddress().getHostAddress()
                        + ":" + socketCliente.getPort());

                try {
                    Sirviente sirviente = new Sirviente(socketCliente, usuariosActivos, usuariosComprometidos);
		    Thread hilo = new Thread(sirviente);
                    hilo.start();
                } catch (IOException e) {
                    System.out.println("[Banco] Error creando streams del cliente.");
                    e.printStackTrace();
                    socketCliente.close();
                }
            }

        } catch (IOException e) {
            System.out.println("[Banco] Error de E/S en el servidor.");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("[Banco] Error inesperado en el servidor.");
            e.printStackTrace();
        }
    }
}
