package eurocoin.app;

import eurocoin.common.APPBancoMensaje;
import eurocoin.common.APPBancoPrimitiva;
import eurocoin.common.MalMensajeProtocoloException;
import eurocoin.util.Digest;
import eurocoin.util.Registro;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class APPCliente {
   
    private static final String HOST_BANCO = "localhost";
    private static final int PUERTO_BANCO = 2000;

    protected final Registro registro;
    protected final Digest digest;
    protected final String nombreRegistro;

    public APPCliente(String nombreRegistro) throws Exception{
	this.nombreRegistro = nombreRegistro;
        this.registro = new Registro(nombreRegistro);

        String algoritmo = registro.getValor("FuncionHash");
        this.digest = new Digest(algoritmo);
    }

    public boolean autenticarUsuarioLocal() {
        try {
            Scanner teclado = new Scanner(System.in);

            String salt = registro.getValor("PasswordSalt");
            String hashAlmacenado = registro.getValor("PasswordHash");

            int intentos = 0;
            while (intentos < 3) {
                System.out.print("Introduce el password de la APP: ");
                String passwordPlano = teclado.nextLine();

                String hashCalculado = digest.generateHash(passwordPlano + salt);

                if (hashCalculado.equals(hashAlmacenado)) {
                    System.out.println("[APP] Password correcto.");
                    return true;
                }

                intentos++;
                System.out.println("[APP] Password incorrecto. Intento " + intentos + " de 3.");
            }

            System.out.println("[APP] APP invalidada tras 3 intentos fallidos.");
            return false;

        } catch (Exception e) {
            System.out.println("[APP] Error en la autenticacion local.");
            e.printStackTrace();
            return false;
        }
    }

    public boolean autenticarConBanco() {
        try (
            Socket socket = new Socket(HOST_BANCO, PUERTO_BANCO);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())
        ) {
            System.out.println("[APP] Conexion con el banco establecida.");

            // 1. Recibir A1 del banco
            APPBancoMensaje msgBanco = (APPBancoMensaje) ois.readObject();
            System.out.println("[Banco -> APP] " + msgBanco);

            if (msgBanco.getPrimitiva() != APPBancoPrimitiva.A1) {
                System.out.println("[APP] Error: se esperaba A1.");
                return false;
            }

            String desafioBanco = msgBanco.getArg1();

            // 2. Construir A2
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

            // 3. Recibir A3 o A4
            APPBancoMensaje respuestaBanco = (APPBancoMensaje) ois.readObject();
            System.out.println("[Banco -> APP] " + respuestaBanco);

            APPBancoMensaje respuestaApp;

            if (respuestaBanco.getPrimitiva() == APPBancoPrimitiva.A3) {
                System.out.println("[APP] El banco ha rechazado la autenticacion.");
                return false;
            }

            if (respuestaBanco.getPrimitiva() != APPBancoPrimitiva.A4) {
                System.out.println("[APP] Error: se esperaba A4.");
                return false;
            }

            // 4. Validar al banco
            String hashBancoRecibido = respuestaBanco.getArg1();
            String hashBancoEsperado = digest.generateHash(desafioCliente + secretoBanco);

            System.out.println("[APP] hash banco recibido = " + hashBancoRecibido);
            System.out.println("[APP] hash banco esperado = " + hashBancoEsperado);

            if (hashBancoEsperado.equals(hashBancoRecibido)) {
                respuestaApp = new APPBancoMensaje(APPBancoPrimitiva.A5);
		System.out.println("[APP] Sesion autentiucada. Esperando antes de cerrar...");
		Thread.sleep(10000);
            } else {
                respuestaApp = new APPBancoMensaje(APPBancoPrimitiva.A6);
            }

            System.out.println("[APP -> Banco] " + respuestaApp);
            oos.writeObject(respuestaApp);
            oos.flush();

            if (respuestaApp.getPrimitiva() == APPBancoPrimitiva.A6) {
                System.out.println("[APP] Banco invalidado.");
                return false;
            }

            // 5. Finalizacion A7
            APPBancoMensaje finApp = new APPBancoMensaje(APPBancoPrimitiva.A7);
            System.out.println("[APP -> Banco] " + finApp);
            oos.writeObject(finApp);
            oos.flush();

            // 6. Esperar A8
            APPBancoMensaje finBanco = (APPBancoMensaje) ois.readObject();
            System.out.println("[Banco -> APP] " + finBanco);

            if (finBanco.getPrimitiva() == APPBancoPrimitiva.A8) {
                System.out.println("[APP] Autenticacion mutua completada correctamente.");
                return true;
            } else {
                System.out.println("[APP] Error en el cierre del protocolo.");
                return false;
            }

        } catch (MalMensajeProtocoloException e) {
            System.out.println("[APP] Error construyendo mensaje del protocolo.");
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.out.println("[APP] No se pudo conectar o autenticar con el banco.");
            e.printStackTrace();
            return false;
        }
    }

    public void iniciar() {
        System.out.println("[APP] Inicio de la aplicacion cliente.");

        boolean usuarioOk = autenticarUsuarioLocal();
        if (!usuarioOk) {
            return;
        }

        boolean bancoOk = autenticarConBanco();

        if (bancoOk) {
            System.out.println("[APP] MODO DE OPERACION DE UI ONLINE");
        } else {
            System.out.println("[APP] MODO DE OPERACION DE UI OFFLINE");
        }
    }

    protected String generarDesafio() {
        int n = 100000 + new java.util.Random().nextInt(900000);
        return Integer.toString(n);
    }


    protected String construirJsonRecarga(String cantidad) throws Exception {
        String usuario = registro.getValor("usuario");

        return "{ \"op\": \"recarga\", \"titular\": \"" + usuario +
                "\", \"cantidad\": \"" + cantidad + "\" }";
    }

    protected String firmarConSecretoPropio(String json) throws Exception {
        String secreto = registro.getValor("SecWallet");
        return digest.generateHash(json + secreto);
    }

    protected String verificarConSecretoBanco(String json) throws Exception {
        String secretoBanco = registro.getValor("SecBanco");
        return digest.generateHash(json + secretoBanco);
    }

    protected String extraerCampoJson(String json, String campo) {
        String patron = "\"" + campo + "\": \"";
        int inicio = json.indexOf(patron);
        if (inicio == -1) return null;

        inicio += patron.length();
        int fin = json.indexOf("\"", inicio);
        if (fin == -1) return null;

        return json.substring(inicio, fin);
    }

    protected String construirJsonDescarga() throws Exception {
        String usuario = registro.getValor("usuario");
        String cantidad = registro.getValor("FondosEC");

        return "{ \"op\": \"descarga\", \"titular\": \"" + usuario +
                "\", \"cantidad\": \"" + cantidad + "\" }";
    }
}
