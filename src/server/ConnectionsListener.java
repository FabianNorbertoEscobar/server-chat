package server;

import com.google.gson.Gson;

import client.Mode;
import client.Usuarios;

public class ConnectionsListener extends Thread {
	
	private final Gson gson = new Gson();

	public ConnectionsListener() {}

	public void run() {
		synchronized(this){
			try {
				while (true) {
					wait();

					for (ClientListener conectado : Server.getClientsConectados()) {
						if(conectado.getUsuario().getEstado()){
							Usuarios pdu = (Usuarios) new Usuarios(Server.getUsuariosConectados()).clone();
							pdu.setMode(Mode.CONNECT);
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