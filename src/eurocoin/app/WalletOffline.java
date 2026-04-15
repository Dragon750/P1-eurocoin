package eurocoin.app;

import eurocoin.common.APPOfflineMensaje;
import eurocoin.common.APPOfflinePrimitiva;
import eurocoin.common.MalMensajeProtocoloException;
import eurocoin.util.Movimientos;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class WalletOffline extends APPClienteOffline {
    private static final int PUERTO_OFFLINE = 3000;
    private static final String ARCHIVO_MOVS = "MovimientosWallet.txt";

    public WalletOffline() throws Exception {
        super("RegistroWallet.json");
    }

    public WalletOffline(String nombreRegistro) throws Exception {
        super(nombreRegistro);
    }

    public void iniciarSecundario() {
        System.out.println("[WalletOffline] Iniciando modo secundario en puerto " + PUERTO_OFFLINE);

        try (ServerSocket servidor = new ServerSocket(PUERTO_OFFLINE)) {
            System.out.println("[WalletOffline] Esperando conexion de un primario...");

            Socket socket = servidor.accept();
            System.out.println("[WalletOffline] Conexion recibida desde "
                    + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());

            try (
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())
            ) {

                Movimientos movimientos = new Movimientos(ARCHIVO_MOVS);

                // O1
                APPOfflineMensaje o1 = (APPOfflineMensaje) ois.readObject();
                System.out.println("[Primario -> Secundario] " + o1);

                if (o1.getPrimitiva() != APPOfflinePrimitiva.O1) {
                    System.out.println("[WalletOffline] Error grave de autenticacion: se esperaba O1.");
                    return;
                }

                String desafioPrim = o1.getArg1();

                // O2
                String desafioSec = generarDesafio();
                String respuestaPrim = hashConMasterKey(desafioPrim);

                APPOfflineMensaje o2 =
                        new APPOfflineMensaje(APPOfflinePrimitiva.O2, desafioSec, respuestaPrim);

                System.out.println("[Secundario -> Primario] " + o2);
                oos.writeObject(o2);
                oos.flush();

                // O3
                APPOfflineMensaje o3 = (APPOfflineMensaje) ois.readObject();
                System.out.println("[Primario -> Secundario] " + o3);

                if (o3.getPrimitiva() != APPOfflinePrimitiva.O3) {
                    System.out.println("[WalletOffline] Error grave de autenticacion: se esperaba O3.");
                    return;
                }

                String nombrePrim = o3.getArg1();
                String hashRecibido = o3.getArg2();
                String hashEsperado = hashConMasterKey(desafioSec);

                System.out.println("[WalletOffline] hash recibido del primario = " + hashRecibido);
                System.out.println("[WalletOffline] hash esperado del primario = " + hashEsperado);

                if (!hashEsperado.equals(hashRecibido)) {
                    System.out.println("[WalletOffline] Error grave de autenticacion.");
                    return;
                }

                // O4
                String nombreSec = getUsuario();
                APPOfflineMensaje o4 =
                        new APPOfflineMensaje(APPOfflinePrimitiva.O4, nombreSec);

                System.out.println("[Secundario -> Primario] " + o4);
                oos.writeObject(o4);
                oos.flush();

                System.out.println("[WalletOffline] Autenticacion offline completada correctamente.");
                System.out.println("[WalletOffline] Primario autenticado como: " + nombrePrim);

                // Operacion tras O4: puede ser O7 (TPV->Wallet) u O5 (Wallet->Wallet)
                APPOfflineMensaje operacion = (APPOfflineMensaje) ois.readObject();
                System.out.println("[Primario -> Secundario] " + operacion);

                if (operacion.getPrimitiva() == APPOfflinePrimitiva.O7) {
                    String facturaJson = operacion.getArg1();
                    String facturaMac = operacion.getArg2();

                    String secretoPrim = getSecretoPorUsuario(nombrePrim);
                    String facturaMacEsperado = hashConSecretoAjeno(facturaJson, secretoPrim);

                    System.out.println("[WalletOffline] MAC factura recibido = " + facturaMac);
                    System.out.println("[WalletOffline] MAC factura esperado = " + facturaMacEsperado);

                    if (secretoPrim == null || !facturaMac.equals(facturaMacEsperado)) {
                        System.out.println("[WalletOffline] MAC de factura incorrecto. Operacion abortada.");
                        return;
                    }

                    String idFactura = extraerCampoJson(facturaJson, "idFactura");
                    String cantidadFactura = extraerCampoJson(facturaJson, "cantidad");

                    System.out.println("[WalletOffline] idFactura recibida = " + idFactura);
                    System.out.println("[WalletOffline] cantidad recibida = " + cantidadFactura);

                    if (movimientos.contieneTexto(idFactura)) {
                        System.out.println("[WalletOffline] Factura duplicada detectada: " + idFactura);
                        System.out.println("[WalletOffline] Operacion abortada.");
                        return;
                    }

                    movimientos.anotarOperacion(facturaJson, facturaMac);

                    double cantidad = parseCantidad(cantidadFactura);
                    double fondosActuales = getFondosECAsDouble();

                    System.out.println("[WalletOffline] FondosEC antes del pago = " + fondosActuales);

                    if (fondosActuales < cantidad) {
                        System.out.println("[WalletOffline] No hay fondos suficientes para pagar offline.");
                        return;
                    }

                    double nuevosFondos = fondosActuales - cantidad;
                    setFondosECFromDouble(nuevosFondos);

                    System.out.println("[WalletOffline] FondosEC despues del pago = " + nuevosFondos);

                    String pagoJson = "{ \"op\": \"pago\", \"de\": \"" + nombreSec +
                            "\", \"a\": \"" + nombrePrim +
                            "\", \"cantidad\": \"" + cantidadFactura +
                            "\", \"idFactura\": \"" + idFactura + "\" }";

                    String pagoMac = hashConSecretoPropio(pagoJson);

                    movimientos.anotarOperacion(pagoJson, pagoMac);

                    APPOfflineMensaje o8 =
                            new APPOfflineMensaje(APPOfflinePrimitiva.O8, pagoJson, pagoMac);

                    System.out.println("[Secundario -> Primario] " + o8);
                    oos.writeObject(o8);
                    oos.flush();

                } else if (operacion.getPrimitiva() == APPOfflinePrimitiva.O5) {
                    String ingresoJson = operacion.getArg1();
                    String ingresoMac = operacion.getArg2();

                    String secretoPrim = getSecretoPorUsuario(nombrePrim);
                    String ingresoMacEsperado = hashConSecretoAjeno(ingresoJson, secretoPrim);

                    System.out.println("[WalletOffline] MAC ingreso recibido = " + ingresoMac);
                    System.out.println("[WalletOffline] MAC ingreso esperado = " + ingresoMacEsperado);

                    if (secretoPrim == null || !ingresoMac.equals(ingresoMacEsperado)) {
                        System.out.println("[WalletOffline] MAC de ingreso incorrecto.");
                        return;
                    }

                    movimientos.anotarOperacion(ingresoJson, ingresoMac);

                    String cantidadIngreso = extraerCampoJson(ingresoJson, "cantidad");
                    double cantidad = parseCantidad(cantidadIngreso);

                    double ingresosActuales = getIngresosECAsDouble();
                    double nuevosIngresos = ingresosActuales + cantidad;
                    setIngresosECFromDouble(nuevosIngresos);

                    System.out.println("[WalletOffline] IngresosEC despues del ingreso = " + nuevosIngresos);

                    String nombrePrimIngreso = extraerCampoJson(ingresoJson, "de");

                    String reciboJson = "{ \"op\": \"recibo\", \"de\": \"" + nombrePrimIngreso +
                            "\", \"a\": \"" + nombreSec +
                            "\", \"cantidad\": \"" + cantidadIngreso + "\" }";

                    String reciboMac = hashConSecretoPropio(reciboJson);

                    movimientos.anotarOperacion(reciboJson, reciboMac);

                    APPOfflineMensaje o6 =
                            new APPOfflineMensaje(APPOfflinePrimitiva.O6, reciboJson, reciboMac);

                    System.out.println("[Secundario -> Primario] " + o6);
                    oos.writeObject(o6);
                    oos.flush();

                } else {
                    System.out.println("[WalletOffline] Operacion inesperada tras autenticacion.");
                    return;
                }

                // O9
                APPOfflineMensaje o9 = (APPOfflineMensaje) ois.readObject();
                System.out.println("[Primario -> Secundario] " + o9);

                if (o9.getPrimitiva() != APPOfflinePrimitiva.O9) {
                    System.out.println("[WalletOffline] Se esperaba O9.");
                    return;
                }

                // O10
                APPOfflineMensaje o10 =
                        new APPOfflineMensaje(APPOfflinePrimitiva.O10);

                System.out.println("[Secundario -> Primario] " + o10);
                oos.writeObject(o10);
                oos.flush();

                System.out.println("[WalletOffline] Interaccion offline completada correctamente.");

            } catch (EOFException e) {
                System.out.println("[WalletOffline] Conexion cerrada por el primario.");
            } catch (IOException e) {
                System.out.println("[WalletOffline] Error de E/S.");
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                System.out.println("[WalletOffline] Clase recibida no reconocida.");
                e.printStackTrace();
            } catch (MalMensajeProtocoloException e) {
                System.out.println("[WalletOffline] Error construyendo mensaje de protocolo.");
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }

        } catch (IOException e) {
            System.out.println("[WalletOffline] Error al abrir ServerSocket.");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("[WalletOffline] Error inesperado.");
            e.printStackTrace();
        }
    }

    public void iniciarPrimario() {
        final String hostSecundario = "localhost";
        final int puertoOffline = 3000;
        final String archivoMovs = "MovimientosWalletPrim.txt";

        System.out.println("[WalletOffline] Iniciando modo primario.");
        System.out.println("[WalletOffline] Conectando con secundario en "
                + hostSecundario + ":" + puertoOffline);

        try (
            Socket socket = new Socket(hostSecundario, puertoOffline);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())
        ) {
            Movimientos movimientos = new Movimientos(archivoMovs);

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
                System.out.println("[WalletOffline] Se esperaba O2.");
                return;
            }

            String desafioSec = o2.getArg1();
            String hashRecibido = o2.getArg2();
            String hashEsperado = hashConMasterKey(desafioPrim);

            System.out.println("[WalletOffline] hash recibido del secundario = " + hashRecibido);
            System.out.println("[WalletOffline] hash esperado del secundario = " + hashEsperado);

            if (!hashEsperado.equals(hashRecibido)) {
                System.out.println("[WalletOffline] Error grave de autenticacion.");
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
                System.out.println("[WalletOffline] Se esperaba O4.");
                return;
            }

            String nombreSec = o4.getArg1();

            System.out.println("[WalletOffline] Autenticacion offline completada correctamente.");
            System.out.println("[WalletOffline] Secundario autenticado como: " + nombreSec);

            // O5
            String cantidadIngreso = "20";
            double fondosActuales = getFondosECAsDouble();
            double cantidad = parseCantidad(cantidadIngreso);

            System.out.println("[WalletOffline] FondosEC antes del envio = " + fondosActuales);

            if (fondosActuales < cantidad) {
                System.out.println("[WalletOffline] No hay fondos suficientes.");
                return;
            }

            double nuevosFondos = fondosActuales - cantidad;
            setFondosECFromDouble(nuevosFondos);

            System.out.println("[WalletOffline] FondosEC despues del envio = " + nuevosFondos);

            String ingresoJson = "{ \"op\": \"ingreso\", \"de\": \"" + nombrePrim +
                    "\", \"a\": \"" + nombreSec +
                    "\", \"cantidad\": \"" + cantidadIngreso + "\" }";

            String ingresoMac = hashConSecretoPropio(ingresoJson);

            movimientos.anotarOperacion(ingresoJson, ingresoMac);

            APPOfflineMensaje o5 =
                    new APPOfflineMensaje(APPOfflinePrimitiva.O5, ingresoJson, ingresoMac);

            System.out.println("[Primario -> Secundario] " + o5);
            oos.writeObject(o5);
            oos.flush();

            // O6
            APPOfflineMensaje o6 = (APPOfflineMensaje) ois.readObject();
            System.out.println("[Secundario -> Primario] " + o6);

            if (o6.getPrimitiva() != APPOfflinePrimitiva.O6) {
                System.out.println("[WalletOffline] Se esperaba O6.");
                return;
            }

            String reciboJson = o6.getArg1();
            String reciboMac = o6.getArg2();

            String usuarioSec = extraerCampoJson(reciboJson, "a");
            String secretoSec = getSecretoPorUsuario(usuarioSec.equals(nombreSec) ? nombreSec : nombreSec);
            String reciboMacEsperado = hashConSecretoAjeno(reciboJson, secretoSec);

            System.out.println("[WalletOffline] MAC recibo recibido = " + reciboMac);
            System.out.println("[WalletOffline] MAC recibo esperado = " + reciboMacEsperado);

            if (secretoSec == null || !reciboMac.equals(reciboMacEsperado)) {
                System.out.println("[WalletOffline] MAC de recibo incorrecto.");
                return;
            }

            movimientos.anotarOperacion(reciboJson, reciboMac);

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
                System.out.println("[WalletOffline] Se esperaba O10.");
                return;
            }

            System.out.println("[WalletOffline] Interaccion offline completada correctamente.");

        } catch (EOFException e) {
            System.out.println("[WalletOffline] Conexion cerrada por el secundario.");
        } catch (IOException e) {
            System.out.println("[WalletOffline] Error de E/S.");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.out.println("[WalletOffline] Clase no reconocida.");
            e.printStackTrace();
        } catch (MalMensajeProtocoloException e) {
            System.out.println("[WalletOffline] Error construyendo mensaje.");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("[WalletOffline] Error inesperado.");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            WalletOffline walletOffline;

            if (args.length > 1 && args[1].equalsIgnoreCase("segundo")) {
                walletOffline = new WalletOffline("RegistroWallet2.json");
            } else {
                walletOffline = new WalletOffline("RegistroWallet.json");
            }

            if (args.length > 0 && args[0].equalsIgnoreCase("primario")) {
                walletOffline.iniciarPrimario();
            } else {
                walletOffline.iniciarSecundario();
            }
        } catch (Exception e) {
            System.out.println("[WalletOffline] Error al iniciar WalletOffline.");
            e.printStackTrace();
        }
    }
}
