package eurocoin.common;

/*
 * APPOfflineMensaje - Mensaje del Protocolo APP-APP offline
 *
 * Depende de: APPOfflinePrimitiva.java
 *
 * Uso:  APPCliente y derivados: Wallet y TPV
 *
 * Practica de laboratorio EUROCOIN.
 * Sistemas Distribuidos 2025-2026 (C) - UVa.
 */

public class APPOfflineMensaje implements java.io.Serializable {
  private final APPOfflinePrimitiva primitiva;
  private final String arg1;
  private final String arg2;

  /* Constructor 0 args para O9 y O10 */
  public APPOfflineMensaje(APPOfflinePrimitiva p)
      throws MalMensajeProtocoloException {
    if (p == APPOfflinePrimitiva.O9 || p == APPOfflinePrimitiva.O10) {
       this.primitiva = p;
       this.arg1 = this.arg2 = null;
    } else
       throw new MalMensajeProtocoloException();
  }

  /* Constructor 1 args para O1,O4 */
  public APPOfflineMensaje(APPOfflinePrimitiva p, String arg1)
      throws MalMensajeProtocoloException {
    if (p == APPOfflinePrimitiva.O1 || p == APPOfflinePrimitiva.O4) {
      this.arg1 = new String(arg1);
      this.arg2 = null;
    } else
      throw new MalMensajeProtocoloException();
    this.primitiva = p;
  }

  /* Constructor 2 args para O2, O3, O5, O6, O7, O8 */
  public APPOfflineMensaje(APPOfflinePrimitiva p, String arg1, String arg2)
      throws MalMensajeProtocoloException {
    if (p == APPOfflinePrimitiva.O2 || p == APPOfflinePrimitiva.O3 ||
        p == APPOfflinePrimitiva.O5 || p == APPOfflinePrimitiva.O6 ||
        p == APPOfflinePrimitiva.O7 || p == APPOfflinePrimitiva.O8) {
      this.arg1 = new String(arg1);
      this.arg2 = new String(arg2);
    } else
      throw new MalMensajeProtocoloException();
    this.primitiva = p;
  }

  public APPOfflinePrimitiva getPrimitiva() { return this.primitiva; }
  public String getArg1() { return this.arg1; }
  public String getArg2() { return this.arg2; }


  public String toString() { /* prettyPrinter de la clase */
    switch (this.primitiva) {
      case O1:
        return "saludo (P->S) "+this.arg1;
      case O2:
        return "saludo (S->P) "+this.arg1+":"+this.arg2;
      case O3:
        return "saludo_ok (P->S) "+this.arg1+":"+this.arg2;
      case O4:
        return "saludo_ok (S->P) "+this.arg1;
      case O5:
        return "ingreso (P->S) "+this.arg1+"\n"+this.arg2;
      case O6:
        return "recibo (P->S) "+this.arg1+"\n"+this.arg2;
      case O7:
        return "factura (P->S) "+this.arg1+"\n"+this.arg2;
      case O8:
        return "pago (P->S) "+this.arg1+"\n"+this.arg2;
      case O9:
        return "final (P->S)";
      case O10:
        return "final_ok (S->P)";
      default :
        return "Codigo de primitiva no encontrado." ;
    }
  }
}
