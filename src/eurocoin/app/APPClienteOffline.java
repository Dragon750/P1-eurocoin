package eurocoin.app;

import eurocoin.util.Digest;
import eurocoin.util.Registro;

public class APPClienteOffline {
    protected final Registro registro;
    protected final Digest digest;
    protected final String nombreRegistro;

    public APPClienteOffline(String nombreRegistro) throws Exception {
        this.nombreRegistro = nombreRegistro;
        this.registro = new Registro(nombreRegistro);

        String algoritmo = registro.getValor("FuncionHash");
        this.digest = new Digest(algoritmo);
    }

    protected String getUsuario() throws Exception {
        return registro.getValor("usuario");
    }

    protected String getMasterKey() throws Exception {
        return registro.getValor("MasterKey");
    }

    protected String getSecretoPropio() throws Exception {
        return registro.getValor("SecWallet");
    }

    protected String getFondosEC() throws Exception {
        return registro.getValor("FondosEC");
    }

    protected String getIngresosEC() throws Exception {
        return registro.getValor("IngresosEC");
    }

    protected void setFondosEC(String valor) throws Exception {
        registro.setValor("FondosEC", valor);
    }

    protected void setIngresosEC(String valor) throws Exception {
        registro.setValor("IngresosEC", valor);
    }

    protected String generarDesafio() {
        int n = 100000 + new java.util.Random().nextInt(900000);
        return Integer.toString(n);
    }

    protected String hashConMasterKey(String desafio) throws Exception {
        return digest.generateHash(desafio + getMasterKey());
    }

    protected String hashConSecretoPropio(String texto) throws Exception {
        return digest.generateHash(texto + getSecretoPropio());
    }

    protected double getFondosECAsDouble() throws Exception {
        return Double.parseDouble(getFondosEC().replace(",", "."));
    }

    protected double getIngresosECAsDouble() throws Exception {
        return Double.parseDouble(getIngresosEC().replace(",", "."));
    }

    protected void setFondosECFromDouble(double valor) throws Exception {
        setFondosEC(Double.toString(valor));
    }

    protected void setIngresosECFromDouble(double valor) throws Exception {
        setIngresosEC(Double.toString(valor));
    }

    protected double parseCantidad(String cantidad) {
        return Double.parseDouble(cantidad.replace(",", "."));
    }

    protected String hashConSecretoAjeno(String texto, String secretoAjeno) {
        return digest.generateHash(texto + secretoAjeno);
    }

    protected String extraerCampoJson(String json, String campo) {
        String patron = "\"" + campo + "\": \"";
        int inicio = json.indexOf(patron);
        if (inicio == -1) {
            return null;
        }

        inicio += patron.length();
        int fin = json.indexOf("\"", inicio);
        if (fin == -1) {
            return null;
        }

        return json.substring(inicio, fin);
    }

    protected String getSecretoPorUsuario(String usuario) {
        if ("PedroLuis".equals(usuario)) {
            return "Secreto-PedroLuis";
        }
        if ("AngelAntonio".equals(usuario)) {
            return "Secreto-AngelAntonio";
        }
        return null;
    }
}
