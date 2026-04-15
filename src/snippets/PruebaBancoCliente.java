package snippets;

import eurocoin.common.APPBancoMensaje;
import eurocoin.common.APPBancoPrimitiva;
import eurocoin.common.MalMensajeProtocoloException;
import eurocoin.util.Digest;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class PruebaBancoCliente {
    public static void main(String[] args) {
        try (
            Socket socket = new Socket("localhost", 2000);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())
        ) {
            // 1. Recibir A1 del banco
            APPBancoMensaje msgBanco = (APPBancoMensaje) ois.readObject();
            System.out.println("[Banco -> Cliente] " + msgBanco);

            String desafioBanco = msgBanco.getArg1();

            // 2. Preparar A2 correcto
            String idCliente = "PedroLuis";
            String desafioCliente = "654321";
            String secretoApp = "Secreto-PedroLuis";
            String secretoBanco = "Secreto-BancoBob";

            Digest digest = new Digest("SHA-256");
            String hashCliente = digest.generateHash(desafioBanco + secretoApp);

            APPBancoMensaje msgCliente =
                    new APPBancoMensaje(APPBancoPrimitiva.A2,
                            idCliente, desafioCliente, hashCliente);

            System.out.println("[Cliente -> Banco] " + msgCliente);
            oos.writeObject(msgCliente);
            oos.flush();

            // 3. Recibir A3 o A4 del banco
            APPBancoMensaje respuestaBanco = (APPBancoMensaje) ois.readObject();
            System.out.println("[Banco -> Cliente] " + respuestaBanco);

            // 4. Validar al banco
            APPBancoMensaje respuestaCliente;
            if (respuestaBanco.getPrimitiva() == APPBancoPrimitiva.A4) {
                String hashBancoRecibido = respuestaBanco.getArg1();
                String hashBancoEsperado =
                        digest.generateHash(desafioCliente + secretoBanco);

                System.out.println("[Cliente] hash banco recibido = " + hashBancoRecibido);
                System.out.println("[Cliente] hash banco esperado = " + hashBancoEsperado);

                if (hashBancoEsperado.equals(hashBancoRecibido)) {
                    respuestaCliente = new APPBancoMensaje(APPBancoPrimitiva.A5);
                } else {
                    respuestaCliente = new APPBancoMensaje(APPBancoPrimitiva.A6);
                }
            } else {
                respuestaCliente = new APPBancoMensaje(APPBancoPrimitiva.A6);
            }

            System.out.println("[Cliente -> Banco] " + respuestaCliente);
            oos.writeObject(respuestaCliente);
            oos.flush();

            // 5. Enviar finalizacion A7
            APPBancoMensaje finCliente =
                    new APPBancoMensaje(APPBancoPrimitiva.A7);
            System.out.println("[Cliente -> Banco] " + finCliente);
            oos.writeObject(finCliente);
            oos.flush();

            // 6. Esperar A8 del banco
            APPBancoMensaje finBanco = (APPBancoMensaje) ois.readObject();
            System.out.println("[Banco -> Cliente] " + finBanco);

        } catch (MalMensajeProtocoloException e) {
            System.out.println("Error construyendo mensaje de protocolo.");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Error en PruebaBancoCliente.");
            e.printStackTrace();
        }
    }
}
