package eurocoin.common;

/*
 * APPBancoPrimitiva - Enum de Primitivas para protocolo APP-Banco
 *
 * Uso: APPBancoMensaje.java
 *      APPBanco y derivados.
 *      APPCliente y derivados.
 *
 * Practica de laboratorio EUROCOIN.
 * Sistemas Distribuidos 2025-2026 (C) - UVa.
 */

public enum APPBancoPrimitiva implements java.io.Serializable {
  A1,    // saludo(desafio_banco) banco->app
  A2,    // saludo(id_cliente, hash(desafio_banco+secreto_app)) app->banco
  A3,    // saludo_err banco->app
  A4,    // saludo_ok(respuesta) banco->app
  A5,    // saludo_ok  app->banco
  A6,    // saludo_err app->banco
  A7,    // final app->banco
  A8,    // final_ok banco->app
  A9,    // recarga(doc-recarga, MAC_app) wallet->banco
  A10,   // recibo-recarga(doc-recibo-recarga, MAC_banco) banco->wallet
  A11,   // descarga(doc-descarga, MAC_app) app->banco
  A12    // recibo-descarga(doc-recibo-descarga, MAC_banco) banco->app
}
