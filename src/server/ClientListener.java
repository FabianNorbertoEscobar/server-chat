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
import client.Usuario;

public class ClientListener extends Thread {

	private final Socket socket;
	private final ObjectInputStream entrada;
	private final ObjectOutputStream salida;
	
	private final Gson gson = new Gson();
	
	private Usuario usuario;
	private PaqueteDeUsuarios paqueteDeUsuarios;
	private PaqueteMensaje paqueteMensaje;

	
	public Socket getSocket() {
		return socket;
	}
	
	public ObjectInputStream getEntrada() {
		return entrada;
	}
	
	public ObjectOutputStream getSalida() {
		return salida;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}
	
	public ClientListener(String ip, Socket socket, ObjectInputStream entrada, ObjectOutputStream salida) {
		usuario = new Usuario();
		this.socket = socket;
		this.entrada = entrada;
		this.salida = salida;
	}

	public void run() {
		try {
			Paquete paquete;
			Paquete paqueteSv = new Paquete(null, 0);
			Usuario usuario = new Usuario();
			
			String cadenaLeida = (String) entrada.readObject();
		
			while (!((paquete = gson.fromJson(cadenaLeida, Paquete.class)).getComando() == Comando.DISCONNECT)) {							
				switch (paquete.getComando()) {
						
					case Comando.LOGIN:
						paqueteSv.setComando(Comando.LOGIN);

						usuario = (Usuario) (gson.fromJson(cadenaLeida, Usuario.class));

						if (Server.loguearUsuario(usuario)) {
							
							usuario.setListaDeConectados(Server.UsuariosConectados);
							usuario.setComando(Comando.LOGIN);
							usuario.setMensaje(Paquete.SUCCESS);
							
							Server.UsuariosConectados.add(usuario.getUsername());

							int index = Server.UsuariosConectados.indexOf(usuario.getUsername());
							Server.mapConectados.put(usuario.getUsername(), Server.SocketsConectados.get(index));
							
							salida.writeObject(gson.toJson(usuario));

							synchronized(Server.connectionsListener){
								Server.connectionsListener.notify();
							}
							break;
							
						} else {
							paqueteSv.setMensaje(Paquete.FAILURE);
							salida.writeObject(gson.toJson(paqueteSv));
							synchronized (this) {
								this.wait(200);
							}
							entrada.close();
							salida.close();
							
							Server.SocketsConectados.remove(socket);
							Server.getClientesConectados().remove(this);
							
							socket.close();
							this.stop();
							
							return;
						}
						
					case Comando.PRIVATE:
						paqueteMensaje = (PaqueteMensaje) (gson.fromJson(cadenaLeida, PaqueteMensaje.class));
						paqueteMensaje.setComando(Comando.PRIVATE);

						Socket s1 = Server.mapConectados.get(paqueteMensaje.getUserReceptor());
						
						for (ClientListener conectado : Server.getClientesConectados()) {
							if(conectado.getSocket() == s1)	{
								conectado.getSalida().writeObject(gson.toJson(paqueteMensaje));	
							}
						}

						break;
						
					case Comando.BROADCAST:
						paqueteMensaje = (PaqueteMensaje) (gson.fromJson(cadenaLeida, PaqueteMensaje.class));
						paqueteMensaje.setComando(Comando.BROADCAST);
						
						Socket s2 = Server.mapConectados.get(paqueteMensaje.getUserEmisor());

						for (ClientListener conectado : Server.getClientesConectados()) {
							if(conectado.getSocket() != s2)	{
								conectado.getSalida().writeObject(gson.toJson(paqueteMensaje));
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

			Server.log.append(usuario.getUsername() + " has disconnected." + System.lineSeparator());
			
			entrada.close();
			salida.close();
			socket.close();

			int index = Server.UsuariosConectados.indexOf(usuario.getUsername());
			Server.SocketsConectados.remove(index);
			Server.getPersonajesConectados().remove(usuario.getUsername());
			Server.getUsuariosConectados().remove(usuario.getUsername());
			Server.getClientesConectados().remove(this);

			for (ClientListener conectado : Server.getClientesConectados()) {
				paqueteDeUsuarios = new PaqueteDeUsuarios(Server.getUsuariosConectados());
				paqueteDeUsuarios.setComando(Comando.CONNECT);
				conectado.salida.writeObject(gson.toJson(paqueteDeUsuarios, PaqueteDeUsuarios.class));
			}

			Server.log.append(paquete.getIp() + " has disconnected." + System.lineSeparator());

		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} 
	}
}