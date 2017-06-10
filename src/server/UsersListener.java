package server;

import com.google.gson.Gson;

import client.Mode;
import client.Users;

public class UsersListener extends Thread {
	
	private final Gson gson = new Gson();

	public void run() {
		synchronized(this){
			try {
				while (true) {
					wait();
					for (ClientListener client: Server.clients) {
						if(client.user.state){
							Users users = (Users) new Users(Server.users).clone();
							users.mode = Mode.CONNECTION;
							client.output.writeObject(gson.toJson(users));		
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
