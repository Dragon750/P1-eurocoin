package eurocoin.app;

import eurocoin.common.APPOfflineMensaje;
import eurocoin.common.APPOfflinePrimitiva;
import eurocoin.common.MalMensajeProtocoloException;
import eurocoin.util.Movimientos;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class TPVOffline extends APPClienteOffline {
    private static final String HOST_SECUNDARIO = "localhost";
    private static final int PUERTO_OFFLINE = 3000;
    private static final String ARCHIVO_MOVS = "MovimientosTPV.txt";

    public TPVOffline() throws Exception {
        super("RegistroTPV.json");
    }

    public void iniciarPrimario() {
        System.out.println("[TPVOffline] Iniciando modo primario.");
        System.out.println("[TPVOffline] Conectando con secundario en "
                + HOST_SECUNDARIO + ":" + PUERTO_OFFLINE);

        try (
            Socket socket = new Socket(HOST_SECUNDARIO, PUERTO_OFFLINE);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())
        ) {

            Movimientos movimientos = new Movimientos(ARCHIVO_MOVS);

            // O1
            String desafioPrim = generarDesafio();
            APPOfflineMensaje o1 =
                    new APPOfflineMensaje(APPOfflinePrimitiva.O1, desafioPrim);

            System.out.println("[Primario -> Secundario] " + o1);
            oos.writeObject(o1);
            oos.flush();

            // O2
            APPOfflineMensaje o2 = (APPOfflineMensaje) ois.readObject();
            System.out.println("[Secundario -> Primario] " + o2);

            if (o2.getPrimitiva() != APPOfflinePrimitiva.O2) {
                System.out.println("[TPVOffline] Error grave de autenticacion: se esperaba O2.");
                return;
            }

            String desafioSec = o2.getArg1();
            String hashRecibido = o2.getArg2();
            String hashEsperado = hashConMasterKey(desafioPrim);

            System.out.println("[TPVOffline] hash recibido del secundario = " + hashRecibido);
            System.out.println("[TPVOffline] hash esperado del secundario = " + hashEsperado);

            if (!hashEsperado.equals(hashRecibido)) {
                System.out.println("[TPVOffline] Error grave de autenticacion.");
                return;
            }

            // O3
            String nombrePrim = getUsuario();
            String respuestaSec = hashConMasterKey(desafioSec);

            APPOfflineMensaje o3 =
                    new APPOfflineMensaje(APPOfflinePrimitiva.O3, nombrePrim, respuestaSec);

            System.out.println("[Primario -> Secundario] " + o3);
            oos.writeObject(o3);
            oos.flush();

            // O4
            APPOfflineMensaje o4 = (APPOfflineMensaje) ois.readObject();
            System.out.println("[Secundario -> Primario] " + o4);

            if (o4.getPrimitiva() != APPOfflinePrimitiva.O4) {
                System.out.println("[TPVOffline] Error grave de autenticacion: se esperaba O4.");
                return;
            }

            String nombreSec = o4.getArg1();

            System.out.println("[TPVOffline] Autenticacion offline completada correctamente.");
            System.out.println("[TPVOffline] Secundario autenticado como: " + nombreSec);

            // O7
            String facturaJson = "{ \"op\": \"factura\", \"de\": \"" + nombrePrim +
                    "\", \"a\": \"" + nombreSec +
                    "\", \"cantidad\": \"12,50\", \"idFactura\": \"A2026-192288\" }";

            String facturaMac = hashConSecretoPropio(facturaJson);

            movimientos.anotarOperacion(facturaJson, facturaMac);

            APPOfflineMensaje o7 =
                    new APPOfflineMensaje(APPOfflinePrimitiva.O7, facturaJson, facturaMac);

            System.out.println("[Primario -> Secundario] " + o7);
            oos.writeObject(o7);
            oos.flush();

            // O8
            APPOfflineMensaje o8 = (APPOfflineMensaje) ois.readObject();
            System.out.println("[Secundario -> Primario] " + o8);

            if (o8.getPrimitiva() != APPOfflinePrimitiva.O8) {
                System.out.println("[TPVOffline] Se esperaba O8.");
                return;
            }

            String pagoJson = o8.getArg1();
            String pagoMac = o8.getArg2();

            String usuarioPagador = extraerCampoJson(pagoJson, "de");
            String secretoSec = getSecretoPorUsuario(usuarioPagador);
            String pagoMacEsperado = hashConSecretoAjeno(pagoJson, secretoSec);

            System.out.println("[TPVOffline] MAC pago recibido = " + pagoMac);
            System.out.println("[TPVOffline] MAC pago esperado = " + pagoMacEsperado);

            if (secretoSec == null || !pagoMac.equals(pagoMacEsperado)) {
                System.out.println("[TPVOffline] MAC de pago incorrecto. Operacion abortada.");
                return;
            }

            movimientos.anotarOperacion(pagoJson, pagoMac);

            double ingresosActuales = getIngresosECAsDouble();
            String cantidadPago = extraerCampoJson(pagoJson, "cantidad");
            double cantidad = parseCantidad(cantidadPago);
            double nuevosIngresos = ingresosActuales + cantidad;

            System.out.println("[TPVOffline] IngresosEC antes del cobro = " + ingresosActuales);

            setIngresosECFromDouble(nuevosIngresos);

            System.out.println("[TPVOffline] IngresosEC despues del cobro = " + nuevosIngresos);

            // O9
            APPOfflineMensaje o9 =
                    new APPOfflineMensaje(APPOfflinePrimitiva.O9);

            System.out.println("[Primario -> Secundario] " + o9);
            oos.writeObject(o9);
            oos.flush();

            // O10
            APPOfflineMensaje o10 = (APPOfflineMensaje) ois.readObject();
            System.out.println("[Secundario -> Primario] " + o10);

            if (o10.getPrimitiva() != APPOfflinePrimitiva.O10) {
                System.out.println("[TPVOffline] Se esperaba O10.");
                return;
            }

            System.out.println("[TPVOffline] Interaccion offline completada correctamente.");

        } catch (EOFException e) {
            System.out.println("[TPVOffline] Conexion cerrada por el secundario.");
        } catch (IOException e) {
            System.out.println("[TPVOffline] Error de E/S.");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.out.println("[TPVOffline] Clase recibida no reconocida.");
            e.printStackTrace();
        } catch (MalMensajeProtocoloException e) {
            System.out.println("[TPVOffline] Error construyendo mensaje de protocolo.");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("[TPVOffline] Error inesperado.");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            TPVOffline tpvOffline = new TPVOffline();
            tpvOffline.iniciarPrimario();
        } catch (Exception e) {
            System.out.println("[TPVOffline] Error al iniciar TPVOffline.");
            e.printStackTrace();
        }
    }
}
