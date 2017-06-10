package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.google.gson.Gson;

import client.Mode;
import client.Conjunto;
import client.Usuarios;
import client.ConjuntoMensaje;
import client.Usuario;

public class ClientListener extends Thread {

	private final Socket socket;
	private final ObjectInputStream entrada;
	private final ObjectOutputStream salida;
	
	private final Gson gson = new Gson();
	
	private Usuario usuario;
	private Usuarios usuarios;
	private ConjuntoMensaje conjuntoMensaje;

	
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
			Conjunto conjunto;
			Conjunto conjuntoSv = new Conjunto(null, 0);
			Usuario usuario = new Usuario();
			
			String cadenaLeida = (String) entrada.readObject();
		
			while (!((conjunto = gson.fromJson(cadenaLeida, Conjunto.class)).getMode() == Mode.DISCONNECT)) {							
				switch (conjunto.getMode()) {
						
					case Mode.LOGIN:
						conjuntoSv.setMode(Mode.LOGIN);

						usuario = (Usuario) (gson.fromJson(cadenaLeida, Usuario.class));

						if (Server.loguearUsuario(usuario)) {
							
							usuario.setListaDeConectados(Server.UsuariosConectados);
							usuario.setMode(Mode.LOGIN);
							usuario.setMensaje(Conjunto.SUCCESS);
							
							Server.UsuariosConectados.add(usuario.getUsername());

							int index = Server.UsuariosConectados.indexOf(usuario.getUsername());
							Server.mapConectados.put(usuario.getUsername(), Server.SocketsConectados.get(index));
							
							salida.writeObject(gson.toJson(usuario));

							synchronized(Server.connectionsListener){
								Server.connectionsListener.notify();
							}
							break;
							
						} else {
							conjuntoSv.setMensaje(Conjunto.FAILURE);
							salida.writeObject(gson.toJson(conjuntoSv));
							synchronized (this) {
								this.wait(200);
							}
							entrada.close();
							salida.close();
							
							Server.SocketsConectados.remove(socket);
							Server.getClientsConectados().remove(this);
							
							socket.close();
							this.stop();
							
							return;
						}
						
					case Mode.PRIVATE:
						conjuntoMensaje = (ConjuntoMensaje) (gson.fromJson(cadenaLeida, ConjuntoMensaje.class));
						conjuntoMensaje.setMode(Mode.PRIVATE);

						Socket s1 = Server.mapConectados.get(conjuntoMensaje.getUserReceptor());
						
						for (ClientListener conectado : Server.getClientsConectados()) {
							if(conectado.getSocket() == s1)	{
								conectado.getSalida().writeObject(gson.toJson(conjuntoMensaje));	
							}
						}

						break;
						
					case Mode.BROADCAST:
						conjuntoMensaje = (ConjuntoMensaje) (gson.fromJson(cadenaLeida, ConjuntoMensaje.class));
						conjuntoMensaje.setMode(Mode.BROADCAST);
						
						Socket s2 = Server.mapConectados.get(conjuntoMensaje.getUserEmisor());

						for (ClientListener conectado : Server.getClientsConectados()) {
							if(conectado.getSocket() != s2)	{
								conectado.getSalida().writeObject(gson.toJson(conjuntoMensaje));
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
			Server.getClientsConectados().remove(this);

			for (ClientListener conectado : Server.getClientsConectados()) {
				usuarios = new Usuarios(Server.getUsuariosConectados());
				usuarios.setMode(Mode.CONNECT);
				conectado.salida.writeObject(gson.toJson(usuarios, Usuarios.class));
			}

			Server.log.append(conjunto.getIp() + " has disconnected." + System.lineSeparator());

		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} 
	}
}