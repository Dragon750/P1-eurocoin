# Proyecto 1 - EuroCoin

| Asignatura    | Sistemas Distribuidos |
|---------------|-----------------------|
| Curso         | 2025-2026             |
| Equipo        | E5.2 |
| Integrante 1  | Gonzalo Arias Hernando |
| Integrante 2  | Javier Arranz Pérez |
| Integrante 3  | Manuel Pérez Domínguez |
| Integrante 4  | Enrique Quiñones Ureta |

---

## Descripción

Este proyecto forma parte de la asignatura de Sistemas Distribuidos y consiste en el desarrollo de un sistema cliente-servidor en Java que simula un banco digital llamado EuroCoin.

El sistema permite la comunicación entre diferentes aplicaciones (Wallet, TPV y Banco) utilizando sockets TCP e implementando un protocolo propio basado en intercambio de mensajes.

---

## Estructura del proyecto

```
P1-Eurocoin/
├── lib/
│ └── gson-2.13.2.jar
├── src/
│ └── eurocoin/
│ ├── app/
│ │ ├── APPCliente.java
│ │ ├── APPClienteOffline.java
│ │ ├── Wallet.java
│ │ ├── WalletOffline.java
│ │ ├── TPV.java
│ │ └── TPVOffline.java
│ ├── banco/
│ │ ├── Banco.java
│ │ ├── Launcher.java
│ │ └── Sirviente.java
│ ├── common/
│ │ ├── APPBancoMensaje.java
│ │ ├── APPBancoPrimitiva.java
│ │ ├── APPOfflineMensaje.java
│ │ ├── APPOfflinePrimitiva.java
│ │ └── MalMensajeProtocoloException.java
│ ├── util/
│ │ ├── Digest.java
│ │ ├── Movimientos.java
│ │ ├── Registro.java
│ │ └── RegistroBanco.java
│ └── snippets/
│ ├── PruebaBancoCliente.java
│ └── PruebaRegistro.java
├── RegistroBanco.json
├── RegistroTPV.json
├── RegistroWallet.json
├── RegistroWallet2.json
├── MovimientosTPV.txt
├── MovimientosWallet.txt
├── MovimientosWalletPrim.txt
└── README.md
```
