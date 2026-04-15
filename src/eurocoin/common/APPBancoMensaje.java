package eurocoin.common;

/*
 * APPBancoMensaje - Mensaje del Protocolo APP-Banco online
 *
 * Depende de: APPBancoPrimitiva.java
 *
 * Uso: Banco y derivados
 *      APPCliente y derivados: Wallet y TPV
 *
 * Practica de laboratorio EUROCOIN.
 * Sistemas Distribuidos 2025-2026 (C) - UVa.
 */

public class APPBancoMensaje implements java.io.Serializable {
  private final APPBancoPrimitiva primitiva;
  private final String arg1;
  private final String arg2;
  private final String arg3;

  /* Constructor 0 args para A3,A5,A6,A7,A8 */
  public APPBancoMensaje(APPBancoPrimitiva p)
      throws MalMensajeProtocoloException {
    if (p == APPBancoPrimitiva.A3 || p == APPBancoPrimitiva.A5 ||
        p == APPBancoPrimitiva.A6 || p == APPBancoPrimitiva.A7 ||
        p == APPBancoPrimitiva.A8) {
       this.primitiva = p;
       this.arg1 = this.arg2 = this.arg3 = null;
    } else
       throw new MalMensajeProtocoloException();
  }

  /* Constructor 1 args para A1,A4 */
  public APPBancoMensaje(APPBancoPrimitiva p, String arg1)
      throws MalMensajeProtocoloException {
    if (p == APPBancoPrimitiva.A1 || p == APPBancoPrimitiva.A4) {
      this.arg1 = new String(arg1);
      this.arg2 = this.arg3 = null;
    } else
      throw new MalMensajeProtocoloException();
    this.primitiva = p;
  }

  /* Constructor 2 args para A9, A10, A11 y A12 */
  public APPBancoMensaje(APPBancoPrimitiva p, String arg1, String arg2)
      throws MalMensajeProtocoloException {
    if (p == APPBancoPrimitiva.A9  || p == APPBancoPrimitiva.A10 ||
        p == APPBancoPrimitiva.A11 || p == APPBancoPrimitiva.A12) {
      this.arg1 = new String(arg1);
      this.arg2 = new String(arg2);
      this.arg3 = null;
      this.primitiva = p;
    } else
      throw new MalMensajeProtocoloException();
  }

  /* Constructor 3 args para A2 */
  public APPBancoMensaje(APPBancoPrimitiva p, String arg1, String arg2, String arg3)
      throws MalMensajeProtocoloException {
    if (p == APPBancoPrimitiva.A2) {
      this.arg1 = new String(arg1);
      this.arg2 = new String(arg2);
      this.arg3 = new String(arg3);
    } else
      throw new MalMensajeProtocoloException();
    this.primitiva = p;
  }

  public APPBancoPrimitiva getPrimitiva() { return this.primitiva; }
  public String getArg1() { return this.arg1; }
  public String getArg2() { return this.arg2; }
  public String getArg3() { return this.arg3; }

  public String toString() { /* prettyPrinter de la clase */
    switch (this.primitiva) {
      case A1:
        return "saludo (B->A) "+this.arg1;
      case A2:
        return "saludo (A->B) "+this.arg1+":"+this.arg2+":"+this.arg3;
      case A3:
        return "saludo_err (B->A)";
      case A4:
        return "saludo_ok (B->A) "+this.arg1;
      case A5:
        return "ok_err (A->B)";
      case A6:
        return "saludo_err (A->B)";
      case A7:
        return "final (A->B)";
      case A8:
        return "final_ok (B->A)";
      case A9:
        return "recarga (A->B) "+this.arg1+"\n\tMAC "+this.arg2;
      case A10:
        return "recibo-recarga (B->A) "+this.arg1+"\n\tMAC "+this.arg2;
      case A11:
        return "descarga (A->B) "+this.arg1+"\n\tMAC "+this.arg2;
      case A12:
        return "recibo-descarga (B->A) "+this.arg1+"\n\tMAC "+this.arg2;
      default :
        return "Codigo de primitiva no encontrado." ;
    }
  }
}
