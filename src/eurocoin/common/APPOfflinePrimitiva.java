package eurocoin.common;

/*
 * APPOfflinePrimitiva - Enum de Primitivas para protocolo APP-APP offline
 *
 * Uso: APPOfflineMensaje.java
 *      APPCliente y derivados: Wallet y TPV
 *
 * Practica de laboratorio EUROCOIN.
 * Sistemas Distribuidos 2025-2026 (C) - UVa.
 */

public enum APPOfflinePrimitiva implements java.io.Serializable {
  O1,    // saludo(desafio_primario) prim->sec
  O2,    // saludo(desafio_sec, hash(desafio_prim+MasterKey)) sec->prim
  O3,    // saludo_ok(id-user, hash(desafio_sec+MasterKey)) prim->sec
  O4,    // saludo_ok(id-user) sec->prim
  O5,    // ingreso(doc-ingreso, MAC_prim) prim->sec
  O6,    // recibo(doc-recibo, MAC_sec) sec->prim
  O7,    // factura(doc-factura, MAC_prim) prim->sec
  O8,    // pago(doc-pago-factura, MAC_sec) sec->prim
  O9,    // final_ok prim->sec
  O10    // final_ok sec->prim
}
