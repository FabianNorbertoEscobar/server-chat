package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.google.gson.Gson;

import client.Comando;
import client.Paquete;
import client.PaqueteDeUsuarios;
import client.PaqueteMensaje;
import client.PaqueteUsuario;

public class EscuchaCliente extends Thread {

	private final Socket socket;
	private final ObjectInputStream entrada;
	private final ObjectOutputStream salida;
	
	private final Gson gson = new Gson();
	
	private PaqueteUsuario paqueteUsuario;
	private PaqueteDeUsuarios paqueteDeUsuarios;
	private PaqueteMensaje paqueteMensaje;

	public EscuchaCliente(String ip, Socket socket, ObjectInputStream entrada, ObjectOutputStream salida) {
		this.socket = socket;
		this.entrada = entrada;
		this.salida = salida;
		paqueteUsuario = new PaqueteUsuario();
	}

	public void run() {
		try {
			Paquete paquete;
			Paquete paqueteSv = new Paquete(null, 0);
			PaqueteUsuario paqueteUsuario = new PaqueteUsuario();
			
			String cadenaLeida = (String) entrada.readObject();
		
			while (!((paquete = gson.fromJson(cadenaLeida, Paquete.class)).getComando() == Comando.DISCONNECT)) {							
				switch (paquete.getComando()) {
						
					case Comando.LOGIN:
						paqueteSv.setComando(Comando.LOGIN);

						paqueteUsuario = (PaqueteUsuario) (gson.fromJson(cadenaLeida, PaqueteUsuario.class));

						if (Servidor.loguearUsuario(paqueteUsuario)) {
							
							paqueteUsuario.setListaDeConectados(Servidor.UsuariosConectados);
							paqueteUsuario.setComando(Comando.LOGIN);
							paqueteUsuario.setMensaje(Paquete.msjExito);
							
							Servidor.UsuariosConectados.add(paqueteUsuario.getUsername());

							int index = Servidor.UsuariosConectados.indexOf(paqueteUsuario.getUsername());
							Servidor.mapConectados.put(paqueteUsuario.getUsername(), Servidor.SocketsConectados.get(index));
							
							salida.writeObject(gson.toJson(paqueteUsuario));

							synchronized(Servidor.connectionsListener){
								Servidor.connectionsListener.notify();
							}
							break;
							
						} else {
							paqueteSv.setMensaje(Paquete.msjFracaso);
							salida.writeObject(gson.toJson(paqueteSv));
							synchronized (this) {
								this.wait(200);
							}
							entrada.close();
							salida.close();
							
							Servidor.SocketsConectados.remove(socket);
							Servidor.getClientesConectados().remove(this);
							
							socket.close();
							this.stop();
							
							return;
						}
						
					case Comando.PRIVATE:
						paqueteMensaje = (PaqueteMensaje) (gson.fromJson(cadenaLeida, PaqueteMensaje.class));
						paqueteMensaje.setComando(Comando.PRIVATE);

						Socket s1 = Servidor.mapConectados.get(paqueteMensaje.getUserReceptor());
						
						for (EscuchaCliente conectado : Servidor.getClientesConectados()) {
							if(conectado.getSocket() == s1)	{
								conectado.getSalida().writeObject(gson.toJson(paqueteMensaje));	
							}
						}

						break;
						
					case Comando.BROADCAST:
						paqueteMensaje = (PaqueteMensaje) (gson.fromJson(cadenaLeida, PaqueteMensaje.class));
						paqueteMensaje.setComando(Comando.BROADCAST);
						
						Socket s2 = Servidor.mapConectados.get(paqueteMensaje.getUserEmisor());
						int count = 0;
						for (EscuchaCliente conectado : Servidor.getClientesConectados()) {
							if(conectado.getSocket() != s2)	{
								conectado.getSalida().writeObject(gson.toJson(paqueteMensaje));
								count++;
							}
						}

						break;
						
					default:
						break;
				}
				
				salida.flush();
				
				synchronized (entrada) {
					cadenaLeida = (String) entrada.readObject();
				}
			}
			Servidor.log.append(paqueteUsuario.getUsername() + " has disconnected." + System.lineSeparator());
			
			entrada.close();
			salida.close();
			socket.close();

			int index = Servidor.UsuariosConectados.indexOf(paqueteUsuario.getUsername());
			Servidor.SocketsConectados.remove(index);
			Servidor.getPersonajesConectados().remove(paqueteUsuario.getUsername());
			Servidor.getUsuariosConectados().remove(paqueteUsuario.getUsername());
			Servidor.getClientesConectados().remove(this);

			for (EscuchaCliente conectado : Servidor.getClientesConectados()) {
				paqueteDeUsuarios = new PaqueteDeUsuarios(Servidor.getUsuariosConectados());
				paqueteDeUsuarios.setComando(Comando.CONNECT);
				conectado.salida.writeObject(gson.toJson(paqueteDeUsuarios, PaqueteDeUsuarios.class));
			}

			Servidor.log.append(paquete.getIp() + " has disconnected." + System.lineSeparator());

		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} 
	}
	
	public Socket getSocket() {
		return socket;
	}
	
	public ObjectInputStream getEntrada() {
		return entrada;
	}
	
	public ObjectOutputStream getSalida() {
		return salida;
	}

	public PaqueteUsuario getPaqueteUsuario() {
		return paqueteUsuario;
	}

	public void setPaqueteUsuario(PaqueteUsuario paqueteUsuario) {
		this.paqueteUsuario = paqueteUsuario;
	}
}