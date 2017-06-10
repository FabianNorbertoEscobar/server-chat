package server;

import com.google.gson.Gson;

import mensajeria.Comando;
import mensajeria.PaqueteDeUsuarios;

public class UsersListener extends Thread {
	
	private final Gson gson = new Gson();

	public void run() {
		synchronized(this){
			//try {
				while (true) {
					wait();
					for (ClientListener client: Server.getClients()) {
						if(client.getUser().getState()){
							User user = (User) new User(Server.getUsers()).clone();
							user.setCommand(Command.CONNECTION);
							client.getOutput().writeObject(gson.toJson(user));		
						}
					}
				}
			//} catch (Exception e) {
			//	e.printStackTrace();
			//}
		}
	}
}