package server;

import com.google.gson.Gson;

import client.Comando;
import client.PaqueteDeUsuarios;

public class ConnectionsListener extends Thread {
	
	private final Gson gson = new Gson();

	public ConnectionsListener() {}

	public void run() {
		synchronized(this){
			try {
				while (true) {
					wait();

					for (EscuchaCliente conectado : Servidor.getClientesConectados()) {
						if(conectado.getPaqueteUsuario().getEstado()){
							PaqueteDeUsuarios pdu = (PaqueteDeUsuarios) new PaqueteDeUsuarios(Servidor.getUsuariosConectados()).clone();
							pdu.setComando(Comando.CONNECT);
							conectado.getSalida().writeObject(gson.toJson(pdu));		
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}