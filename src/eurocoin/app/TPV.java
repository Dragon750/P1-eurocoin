package eurocoin.app;

import eurocoin.common.APPBancoMensaje;
import eurocoin.common.APPBancoPrimitiva;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class TPV extends APPCliente {

    public TPV() throws Exception {
        super("RegistroTPV.json");    
    }
    
public void descargarOnline() {
    try (
        Socket socket = new Socket("localhost", 2000);
        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())
    ) {
	
	eurocoin.util.Movimientos movimientos = new eurocoin.util.Movimientos("MovimientosTPV.txt");
	
        System.out.println("[TPV] Conexion con el banco para descarga.");

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
            System.out.println("[TPV] Banco no autenticado.");
            return;
        }

        APPBancoMensaje a5 = new APPBancoMensaje(APPBancoPrimitiva.A5);
        oos.writeObject(a5);
        oos.flush();

        APPBancoMensaje a7 = new APPBancoMensaje(APPBancoPrimitiva.A7);
        oos.writeObject(a7);
        oos.flush();

        ois.readObject(); 

        System.out.println("[TPV] Autenticacion completada.");

        String jsonDescarga = construirJsonDescarga();
        String macDescarga = firmarConSecretoPropio(jsonDescarga);
	movimientos.anotarOperacion(jsonDescarga, macDescarga);

        APPBancoMensaje a11 =
                new APPBancoMensaje(APPBancoPrimitiva.A11, jsonDescarga, macDescarga);

        System.out.println("[APP -> Banco] " + a11);
        oos.writeObject(a11);
        oos.flush();

        APPBancoMensaje a12 = (APPBancoMensaje) ois.readObject();
        System.out.println("[Banco -> APP] " + a12);

        String reciboJson = a12.getArg1();
        String reciboMac = a12.getArg2();

        String macEsperado = verificarConSecretoBanco(reciboJson);

        System.out.println("[TPV] MAC recibido = " + reciboMac);
        System.out.println("[TPV] MAC esperado = " + macEsperado);

        if (!reciboMac.equals(macEsperado)) {
            System.out.println("[TPV] MAC del banco incorrecto.");
            return;
        }
	movimientos.anotarOperacion(reciboJson, reciboMac);

        double fondosActuales = Double.parseDouble(registro.getValor("FondosEC"));
        registro.setValor("FondosEC", "0.0");

        System.out.println("[TPV] FondosEC antes = " + fondosActuales);
        System.out.println("[TPV] FondosEC despues = 0.0");

    } catch (Exception e) {
        System.out.println("[TPV] Error en descarga online.");
        e.printStackTrace();
    }
}

    public static void main(String[] args) {
        try {
            TPV tpv = new TPV();

            if (!tpv.autenticarUsuarioLocal()) return;

            tpv.descargarOnline();

        } catch (Exception e) {
            System.out.println("[TPV] Error al iniciar TPV.");
            e.printStackTrace();
        }
    }

}

