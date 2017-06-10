package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.google.gson.Gson;

import client.Container;
import client.Message;
import client.Mode;
import client.User;
import client.Users;
import client.Mode;
import client.Container;
import client.Users;
import client.Message;
import client.User;

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
			Container container2 = new Container(null, 0);
			user = new User();
			
			String line = (String) input.readObject();
			Container p = gson.fromJson(line, Container.class);

			while (p.mode != Mode.DISCONNECT) {
				switch (p.mode) {
						
					case Mode.LOGIN:
						container2.mode = p.mode;
						user = (User) (gson.fromJson(line, User.class));

						String name = user.name;
						Server.log(name);

						user.users = Server.users;
						user.mode = Mode.LOGIN;
						user.message = "1";
						
						Server.users.add(name);
						Server.map.put(name, Server.sockets.get(Server.users.indexOf(name))); // size - 1
						
						output.writeObject(gson.toJson(user));

						synchronized(Server.usersListener){
							Server.usersListener.notify();
						}

						break;
						
					case Mode.PRIVATE:
						Message message = (Message) (gson.fromJson(line, Message.class));
						message.mode = Mode.PRIVATE;
						
						for (ClientListener client : Server.clients) {
							if(client.socket == Server.map.get(message.receiver))	{
								client.output.writeObject(gson.toJson(message));	
							}
						}

						break;
						
					case Mode.BROADCAST:
						message = (Message) (gson.fromJson(line, Message.class));
						message.mode = p.mode;

						for (ClientListener client : Server.clients) {
							if(client.socket != Server.map.get(message.sender))	{
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
				
				p = gson.fromJson(line, Container.class);
			}
			
			input.close();
			output.close();
			socket.close();
			Server.log(user.name + " disconnected.");

			Server.clients.remove(this);
			Server.sockets.remove(Server.users.indexOf(user.name));
			Server.users.remove(user.name);
			Server.map.remove(user.name);
			
			
			for (ClientListener client : Server.clients) {
				users = new Users(Server.users);
				users.mode = Mode.CONNECTION;
				client.output.writeObject(gson.toJson(users, Users.class));
			}

			Server.log(p.ip + " out");

		}  catch (Exception e) {
			e.printStackTrace();
		} 
	}

}