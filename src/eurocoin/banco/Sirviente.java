package eurocoin.banco;

import eurocoin.common.APPBancoMensaje;
import eurocoin.common.APPBancoPrimitiva;
import eurocoin.common.MalMensajeProtocoloException;
import eurocoin.util.Digest;
import eurocoin.util.RegistroBanco;

import java.io.*;
import java.net.Socket;
import java.util.Random;
import java.util.Set;

public class Sirviente implements Runnable {
    private final Socket socketCliente;
    private final ObjectOutputStream oos;
    private final ObjectInputStream ois;
    private final Set<String> usuariosActivos;
    private final Set<String> usuariosComprometidos;

    public Sirviente(Socket socketCliente, Set<String> usuariosActivos, Set<String> usuariosComprometidos)
		    throws IOException {
        this.socketCliente = socketCliente;
        this.usuariosActivos = usuariosActivos;
	this.usuariosComprometidos = usuariosComprometidos;
        this.oos = new ObjectOutputStream(socketCliente.getOutputStream());
        this.ois = new ObjectInputStream(socketCliente.getInputStream());
    }

    public void run() {
        String ipCliente = socketCliente.getInetAddress().getHostAddress();
        int puertoCliente = socketCliente.getPort();
        String idClienteAutenticado = null;

        try {
            System.out.println("[Sirviente] Atendiendo cliente " +
                    ipCliente + ":" + puertoCliente);

            // A1
            String desafioBanco = generarDesafio();
            APPBancoMensaje saludoBanco =
                    new APPBancoMensaje(APPBancoPrimitiva.A1, desafioBanco);

            System.out.println("[Banco -> APP] " + saludoBanco);
            oos.writeObject(saludoBanco);
            oos.flush();

            // A2
            APPBancoMensaje respuesta =
                    (APPBancoMensaje) ois.readObject();

            System.out.println("[APP -> Banco] " + respuesta);

            if (respuesta.getPrimitiva() != APPBancoPrimitiva.A2) {
                APPBancoMensaje error =
                        new APPBancoMensaje(APPBancoPrimitiva.A3);
                System.out.println("[Banco -> APP] " + error);
                oos.writeObject(error);
                oos.flush();
                return;
            }

            String idCliente = respuesta.getArg1();
            String desafioCliente = respuesta.getArg2();
            String hashRecibido = respuesta.getArg3();

	    synchronized (usuariosComprometidos) {
	        if (usuariosComprometidos.contains(idCliente)) {
	            System.out.println("[Banco] Usuario comprometido: " + idCliente);
	            APPBancoMensaje error = new APPBancoMensaje(APPBancoPrimitiva.A3);
        	    System.out.println("[Banco -> APP] " + error);
       	            oos.writeObject(error);
        	    oos.flush();
        	    return;
    	    	}
	    }
            synchronized (usuariosActivos) {
                if (usuariosActivos.contains(idCliente)) {
                    System.out.println("[Banco] Usuario ya conectado: " + idCliente);
                    APPBancoMensaje error = new APPBancoMensaje(APPBancoPrimitiva.A3);
                    System.out.println("[Banco -> APP] " + error);
                    oos.writeObject(error);
                    oos.flush();
                    return;
                }
                usuariosActivos.add(idCliente);
                idClienteAutenticado = idCliente;
            }

            RegistroBanco registroBanco = new RegistroBanco("RegistroBanco.json");
            String secretoApp = registroBanco.getValor(idCliente, "SecWallet");
            String secretoBanco = registroBanco.getValor(idCliente, "SecBanco");

            if (secretoApp == null || secretoBanco == null) {
                APPBancoMensaje error =
                        new APPBancoMensaje(APPBancoPrimitiva.A3);
                System.out.println("[Banco -> APP] " + error);
                oos.writeObject(error);
                oos.flush();
                return;
            }

            Digest digest = new Digest("SHA-256");
            String hashEsperado = digest.generateHash(desafioBanco + secretoApp);

            System.out.println("[Banco] hash recibido = " + hashRecibido);
            System.out.println("[Banco] hash esperado = " + hashEsperado);

            if (!hashEsperado.equals(hashRecibido)) {
               synchronized (usuariosComprometidos) {
	            usuariosComprometidos.add(idCliente);
            }

	    System.out.println("[Banco] Cliente comprometido por fallo de autenticacion: " + idCliente);

	    APPBancoMensaje error =
                        new APPBancoMensaje(APPBancoPrimitiva.A3);
                System.out.println("[Banco -> APP] " + error);
                oos.writeObject(error);
                oos.flush();
                return;
            }

            // A4
            String hashBanco = digest.generateHash(desafioCliente + secretoBanco);
            APPBancoMensaje okBanco =
                    new APPBancoMensaje(APPBancoPrimitiva.A4, hashBanco);

            System.out.println("[Banco -> APP] " + okBanco);
            oos.writeObject(okBanco);
            oos.flush();

            // A5 / A6
            APPBancoMensaje respuestaCliente = (APPBancoMensaje) ois.readObject();
            System.out.println("[APP -> Banco] " + respuestaCliente);

            if (respuestaCliente.getPrimitiva() == APPBancoPrimitiva.A6) {
                System.out.println("[Banco] La APP rechazo la autenticacion del banco.");
                return;
            }

            if (respuestaCliente.getPrimitiva() != APPBancoPrimitiva.A5) {
                System.out.println("[Banco] Se esperaba A5 o A6.");
                return;
            }

            // A7 / A8
            APPBancoMensaje finCliente = (APPBancoMensaje) ois.readObject();
            System.out.println("[APP -> Banco] " + finCliente);

            if (finCliente.getPrimitiva() == APPBancoPrimitiva.A7) {
                APPBancoMensaje finBanco =
                        new APPBancoMensaje(APPBancoPrimitiva.A8);
                System.out.println("[Banco -> APP] " + finBanco);
                oos.writeObject(finBanco);
                oos.flush();
            }

            // A9 o A11
            APPBancoMensaje msgOperacion = (APPBancoMensaje) ois.readObject();
            System.out.println("[APP -> Banco] " + msgOperacion);

            if (msgOperacion.getPrimitiva() == APPBancoPrimitiva.A9) {

                String jsonRecarga = msgOperacion.getArg1();
                String macRecarga = msgOperacion.getArg2();

                String usuario = extraerCampoJson(jsonRecarga, "titular");
                String cantidadStr = extraerCampoJson(jsonRecarga, "cantidad");

                String secretoWallet = registroBanco.getValor(usuario, "SecWallet");

                String macEsperado = digest.generateHash(jsonRecarga + secretoWallet);

                System.out.println("[Banco] MAC recibido = " + macRecarga);
                System.out.println("[Banco] MAC esperado = " + macEsperado);

                if (!macRecarga.equals(macEsperado)) {
                    System.out.println("[Banco] MAC incorrecto. Operacion rechazada.");
                    return;
                }

		double cantidad = Double.parseDouble(cantidadStr.replace(",", "."));

		String saldoStr = registroBanco.getValor(usuario, "Saldo");
		String fondosStr = registroBanco.getValor(usuario, "FondosEC");

		double saldo = Double.parseDouble(saldoStr.replace(",", "."));
		double fondos = Double.parseDouble(fondosStr.replace(",", "."));

		double nuevoSaldo = saldo - cantidad;
		double nuevosFondos = fondos + cantidad;

		registroBanco.setValor(usuario, "Saldo", Double.toString(nuevoSaldo));
		registroBanco.setValor(usuario, "FondosEC", Double.toString(nuevosFondos));

		System.out.println("[Banco] Saldo antes = " + saldo);
		System.out.println("[Banco] Saldo despues = " + nuevoSaldo);
		System.out.println("[Banco] FondosEC antes = " + fondos);
		System.out.println("[Banco] FondosEC despues = " + nuevosFondos);

                String fecha = new java.util.Date().toString();

                String reciboJson = "{ \"op\": \"recibo-recarga\", \"titular\": \"" + usuario +
                        "\", \"cantidad\": \"" + cantidadStr +
                        "\", \"fecha\": \"" + fecha + "\" }";

                String macRecibo = digest.generateHash(reciboJson + secretoBanco);

                APPBancoMensaje respuestaA10 =
                        new APPBancoMensaje(APPBancoPrimitiva.A10, reciboJson, macRecibo);

                System.out.println("[Banco -> APP] " + respuestaA10);
                oos.writeObject(respuestaA10);
                oos.flush();

            } else if (msgOperacion.getPrimitiva() == APPBancoPrimitiva.A11) {

                String jsonDescarga = msgOperacion.getArg1();
                String macDescarga = msgOperacion.getArg2();

                String usuarioDescarga = extraerCampoJson(jsonDescarga, "titular");
                String cantidadDescarga = extraerCampoJson(jsonDescarga, "cantidad");

                String secretoWalletDescarga = registroBanco.getValor(usuarioDescarga, "SecWallet");

                String macEsperadoDescarga = digest.generateHash(jsonDescarga + secretoWalletDescarga);

                System.out.println("[Banco] MAC recibido = " + macDescarga);
                System.out.println("[Banco] MAC esperado = " + macEsperadoDescarga);

                if (!macDescarga.equals(macEsperadoDescarga)) {
                    System.out.println("[Banco] MAC incorrecto. Operacion rechazada.");
                    return;
                }

		double cantidad = Double.parseDouble(cantidadDescarga.replace(",", "."));

		String saldoStr = registroBanco.getValor(usuarioDescarga, "Saldo");
		String fondosStr = registroBanco.getValor(usuarioDescarga, "FondosEC");

		double saldo = Double.parseDouble(saldoStr.replace(",", "."));
		double fondos = Double.parseDouble(fondosStr.replace(",", "."));

		double nuevoSaldo = saldo + cantidad;

		registroBanco.setValor(usuarioDescarga, "Saldo", Double.toString(nuevoSaldo));
		registroBanco.setValor(usuarioDescarga, "FondosEC", "0.0");

		System.out.println("[Banco] Saldo antes = " + saldo);
		System.out.println("[Banco] Saldo despues = " + nuevoSaldo);
		System.out.println("[Banco] FondosEC antes = " + fondos);
		System.out.println("[Banco] FondosEC despues = 0.0");

                String fechaDescarga = new java.util.Date().toString();

                String reciboJsonDescarga = "{ \"op\": \"recibo-descarga\", \"titular\": \"" + usuarioDescarga +
                        "\", \"cantidad\": \"" + cantidadDescarga +
                        "\", \"fecha\": \"" + fechaDescarga + "\" }";

                String macReciboDescarga = digest.generateHash(reciboJsonDescarga + secretoBanco);

                APPBancoMensaje respuestaA12 =
                        new APPBancoMensaje(APPBancoPrimitiva.A12, reciboJsonDescarga, macReciboDescarga);

                System.out.println("[Banco -> APP] " + respuestaA12);
                oos.writeObject(respuestaA12);
                oos.flush();
            }

        } catch (EOFException e) {
            System.out.println("[Sirviente] Fin de conexion con " +
                    ipCliente + ":" + puertoCliente);
        } catch (IOException e) {
            System.out.println("[Sirviente] Error de E/S con " +
                    ipCliente + ":" + puertoCliente);
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.out.println("[Sirviente] Clase no reconocida recibida de " +
                    ipCliente + ":" + puertoCliente);
            e.printStackTrace();
        } catch (MalMensajeProtocoloException e) {
            System.out.println("[Sirviente] Error de mensaje de protocolo con " +
                    ipCliente + ":" + puertoCliente);
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("[Sirviente] Error inesperado con " +
                    ipCliente + ":" + puertoCliente);
            e.printStackTrace();
        } finally {
            if (idClienteAutenticado != null) {
                synchronized (usuariosActivos) {
                    usuariosActivos.remove(idClienteAutenticado);
                    System.out.println("[Banco] Sesion liberada para " + idClienteAutenticado);
                }
            }

            try { ois.close(); } catch (Exception ignored) {}
            try { oos.close(); } catch (Exception ignored) {}

            try {
                socketCliente.close();
                System.out.println("[Sirviente] Conexion cerrada con " +
                        ipCliente + ":" + puertoCliente);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String generarDesafio() {
        Random r = new Random();
        int n = 100000 + r.nextInt(900000);
        return Integer.toString(n);
    }

    private String extraerCampoJson(String json, String campo) {
        String patron = "\"" + campo + "\": \"";
        int inicio = json.indexOf(patron);
        if (inicio == -1) return null;

        inicio += patron.length();
        int fin = json.indexOf("\"", inicio);
        if (fin == -1) return null;

        return json.substring(inicio, fin);
    }
}
