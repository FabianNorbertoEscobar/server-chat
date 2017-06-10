package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.google.gson.Gson;

import mensajeria.Comando;
import mensajeria.Paquete;
import mensajeria.PaqueteDeUsuarios;
import mensajeria.PaqueteMensaje;
import mensajeria.PaqueteUsuario;

public class ClientListener extends Thread {

	public User user;
	public Socket socket;
	public ObjectInputStream input;
	public ObjectOutputStream output;

	private Users users;
	private Message message;

	private Gson gson;
	
	public ClientListener(String ip, Socket socket, ObjectInputStream input, ObjectOutputStream output) {
		this.socket = socket;
		this.input = input;
		this.output = output;
		gson = new Gson();
	}

	public void run() {
		try {
			Paquete psv = new Paquete(null, 0);
			user = new User();
			
			String line = (String) input.readObject();
			Paquete p = gson.fromJson(line, Paquete.class);

			while (p.mode != Mode.STOP) {							
				switch (p.getCommand()) {
						
					case Mode.LOGIN:
						psv.setCommand(Command.LOGIN);
						user = (User) (gson.fromJson(line, User.class));

						String name = user.name;
						Server.log(name);

						user.users = Server.users;
						user.mode = Mode.LOGIN;
						user.message = 1;
						
						Server.users.add(name);
						Server.map.put(name, Server.sockets.get(Server.users.indexOf(name))); // size - 1
						
						output.writeObject(gson.toJson(user));

						synchronized(Server.usersListener){
							Server.usersListener.notify();
						}

						break;
						
					case Mode.PRIVATE:
						message = (Message) (gson.fromJson(line, Message.class));
						message.mode = Mode.PRIVATE;
						
						for (ClientListener client : Server.clients) {
							if(client.socket == Server.map.get(message.getReceiver()))	{
								client.output.writeObject(gson.toJson(message));	
							}
						}

						break;
						
					case Mode.BROADCAST:
						message = (Message) (gson.fromJson(line, Message.class));
						message.setCommand(Mode.BROADCAST);

						for (ClientListener client : Server.clients) {
							if(client.socket != Server.map.get(message.getSender()))	{
								client.output.writeObject(gson.toJson(message));
							}
						}

						break;
						
					default:
						break;
				}
				
				output.flush(); // ??
				
				synchronized (input) {
					line = (String) input.readObject();
				}
				
				p = gson.fromJson(line, Paquete.class);
			}
			
			input.close();
			output.close();
			socket.close();
			Server.log(user.name + " disconnected.");

			Server.clients().remove(this);
			Server.sockets.remove(Server.users.indexOf(user.name));
			Server.users().remove(user.name);
			Server.map.remove(user.name);
			
			
			for (ClientListener client : Server.clients) {
				users = new Users(Server.users());
				users.mode = MODE.CONNECTION;
				client.output.writeObject(gson.toJson(users, Users.class));
			}

			Server.log(p.getIp() + " out");

		}  catch (Exception e) {
			e.printStackTrace();
		} 
	}

}