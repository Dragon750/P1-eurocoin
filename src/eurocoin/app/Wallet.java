package eurocoin.app;

import eurocoin.common.APPBancoMensaje;
import eurocoin.common.APPBancoPrimitiva;
import eurocoin.common.MalMensajeProtocoloException;
import eurocoin.util.Movimientos;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Wallet extends APPCliente {

    private static final String ARCHIVO_MOVS = "MovimientosWallet.txt";

    public Wallet() throws Exception {
        super("RegistroWallet.json");
    }

    public void recargarFondosOnline(String cantidad) {
        try (
            Socket socket = new Socket("localhost", 2000);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())
        ) {

            Movimientos movimientos = new Movimientos(ARCHIVO_MOVS);

            System.out.println("[Wallet] Conexion con el banco establecida.");

            // AUTENTICACIÓN (A1-A8)

            APPBancoMensaje msgBanco = (APPBancoMensaje) ois.readObject();
            System.out.println("[Banco -> APP] " + msgBanco);

            String desafioBanco = msgBanco.getArg1();

            String idCliente = registro.getValor("usuario");
            String secretoApp = registro.getValor("SecWallet");
            String secretoBanco = registro.getValor("SecBanco");
            String desafioCliente = generarDesafio();

            String hashCliente = digest.generateHash(desafioBanco + secretoApp);

            APPBancoMensaje a2 =
                    new APPBancoMensaje(APPBancoPrimitiva.A2,
                            idCliente, desafioCliente, hashCliente);

            System.out.println("[APP -> Banco] " + a2);
            oos.writeObject(a2);
            oos.flush();

            APPBancoMensaje respuestaBanco = (APPBancoMensaje) ois.readObject();
            System.out.println("[Banco -> APP] " + respuestaBanco);

            String hashBancoRecibido = respuestaBanco.getArg1();
            String hashBancoEsperado = digest.generateHash(desafioCliente + secretoBanco);

            if (!hashBancoEsperado.equals(hashBancoRecibido)) {
                System.out.println("[Wallet] Banco no autenticado.");
                return;
            }

            APPBancoMensaje a5 = new APPBancoMensaje(APPBancoPrimitiva.A5);
            oos.writeObject(a5);
            oos.flush();

            APPBancoMensaje a7 = new APPBancoMensaje(APPBancoPrimitiva.A7);
            oos.writeObject(a7);
            oos.flush();

            ois.readObject(); 

            System.out.println("[Wallet] Autenticacion completada.");

            // A9 - RECARGA

            String jsonRecarga = construirJsonRecarga(cantidad);
            String macRecarga = firmarConSecretoPropio(jsonRecarga);

            movimientos.anotarOperacion(jsonRecarga, macRecarga);

            APPBancoMensaje a9 =
                    new APPBancoMensaje(APPBancoPrimitiva.A9, jsonRecarga, macRecarga);

            System.out.println("[APP -> Banco] " + a9);
            oos.writeObject(a9);
            oos.flush();

            // A10 - RECIBO

            APPBancoMensaje a10 = (APPBancoMensaje) ois.readObject();
            System.out.println("[Banco -> APP] " + a10);

            String reciboJson = a10.getArg1();
            String reciboMac = a10.getArg2();

            String macEsperado = verificarConSecretoBanco(reciboJson);

            System.out.println("[Wallet] MAC recibido = " + reciboMac);
            System.out.println("[Wallet] MAC esperado = " + macEsperado);

            if (!reciboMac.equals(macEsperado)) {
                System.out.println("[Wallet] MAC del banco incorrecto.");
                return;
            }

            movimientos.anotarOperacion(reciboJson, reciboMac);

            // ACTUALIZAR FONDOS

            String cantidadRecibida = extraerCampoJson(reciboJson, "cantidad");

            double fondosActuales = Double.parseDouble(registro.getValor("FondosEC"));
            double cantidadDouble = Double.parseDouble(cantidadRecibida);

            double nuevosFondos = fondosActuales + cantidadDouble;

            registro.setValor("FondosEC", Double.toString(nuevosFondos));

            System.out.println("[Wallet] FondosEC antes = " + fondosActuales);
            System.out.println("[Wallet] FondosEC despues = " + nuevosFondos);

        } catch (Exception e) {
            System.out.println("[Wallet] Error en recarga.");
            e.printStackTrace();
        }
    }

    public void descargarOnline() {
    	try (
            Socket socket = new Socket("localhost", 2000);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())
        ) {

            Movimientos movimientos = new Movimientos(ARCHIVO_MOVS);

            System.out.println("[Wallet] Conexion con el banco para descarga.");

            // A1-A8

            APPBancoMensaje msgBanco = (APPBancoMensaje) ois.readObject();
            System.out.println("[Banco -> APP] " + msgBanco);

            String desafioBanco = msgBanco.getArg1();

            String idCliente = registro.getValor("usuario");
            String secretoApp = registro.getValor("SecWallet");
            String secretoBanco = registro.getValor("SecBanco");
            String desafioCliente = generarDesafio();

            String hashCliente = digest.generateHash(desafioBanco + secretoApp);

            APPBancoMensaje a2 =
                    new APPBancoMensaje(APPBancoPrimitiva.A2,
                            idCliente, desafioCliente, hashCliente);

            System.out.println("[APP -> Banco] " + a2);
            oos.writeObject(a2);
            oos.flush();

            APPBancoMensaje respuestaBanco = (APPBancoMensaje) ois.readObject();
            System.out.println("[Banco -> APP] " + respuestaBanco);

            String hashBancoRecibido = respuestaBanco.getArg1();
            String hashBancoEsperado = digest.generateHash(desafioCliente + secretoBanco);

            if (!hashBancoEsperado.equals(hashBancoRecibido)) {
                System.out.println("[Wallet] Banco no autenticado.");
                return;
            }

            APPBancoMensaje a5 = new APPBancoMensaje(APPBancoPrimitiva.A5);
            oos.writeObject(a5);
            oos.flush();

            APPBancoMensaje a7 = new APPBancoMensaje(APPBancoPrimitiva.A7);
            oos.writeObject(a7);
            oos.flush();

            ois.readObject(); 

            System.out.println("[Wallet] Autenticacion completada.");

            // A11

            String jsonDescarga = construirJsonDescarga();
            String macDescarga = firmarConSecretoPropio(jsonDescarga);

            movimientos.anotarOperacion(jsonDescarga, macDescarga);

            APPBancoMensaje a11 =
                    new APPBancoMensaje(APPBancoPrimitiva.A11, jsonDescarga, macDescarga);

            System.out.println("[APP -> Banco] " + a11);
            oos.writeObject(a11);
            oos.flush();

            // A12

            APPBancoMensaje a12 = (APPBancoMensaje) ois.readObject();
            System.out.println("[Banco -> APP] " + a12);

            String reciboJson = a12.getArg1();
            String reciboMac = a12.getArg2();

            String macEsperado = verificarConSecretoBanco(reciboJson);

            System.out.println("[Wallet] MAC recibido = " + reciboMac);
            System.out.println("[Wallet] MAC esperado = " + macEsperado);

            if (!reciboMac.equals(macEsperado)) {
                System.out.println("[Wallet] MAC del banco incorrecto.");
                return;
            }

            movimientos.anotarOperacion(reciboJson, reciboMac);

            double fondosActuales = Double.parseDouble(registro.getValor("FondosEC"));
            registro.setValor("FondosEC", "0.0");

            System.out.println("[Wallet] FondosEC antes = " + fondosActuales);
            System.out.println("[Wallet] FondosEC despues = 0.0");

        } catch (Exception e) {
            System.out.println("[Wallet] Error en descarga online.");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            Wallet wallet = new Wallet();

            if (!wallet.autenticarUsuarioLocal()) return;

            wallet.recargarFondosOnline("50");
	    wallet.descargarOnline();

        } catch (Exception e) {
            System.out.println("[Wallet] Error al iniciar Wallet.");
            e.printStackTrace();
        }
    }
}
