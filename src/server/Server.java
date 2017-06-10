package server;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

import client.Message;
import client.User;
import java.awt.TextArea;

public class Server extends Thread {

	public static Thread server;
	public static TextArea output;
	
	public static ArrayList<Socket> sockets = new ArrayList<Socket>();
	public static ArrayList<String> users = new ArrayList<String>();
	public static ArrayList<ClientListener> clients = new ArrayList<>();

	public static Map<String, Socket> map = new HashMap<>();
	public static UsersListener usersListener;
	

	private static ServerSocket serverSocket;
	private final int port = 3000;
	
	public static void main(String[] args) {
		JFrame serverFrame = new JFrame("Server");
		serverFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		serverFrame.setSize(800, 800);
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(12, 13, 512, 434);
		serverFrame.getContentPane().add(scrollPane);
		scrollPane.setViewportView(output);

		server = new Thread(new Server());
		server.start();

		JButton stopButton = new JButton("Stop !");
		stopButton.setBounds(300, 400, 100, 30);
		serverFrame.getContentPane().add(stopButton);
		serverFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		serverFrame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				server.stop();
				System.exit(0);
			}
		});
		
		serverFrame.setVisible(true);
	}
	
	@Override
	public void run() {
		try {
			output.append("Server started, waiting for connections on " + port + " . . .");
			serverSocket = new ServerSocket(port);
			usersListener = new UsersListener();
			usersListener.start();

			while (true) {
				Socket socket = serverSocket.accept();
				output.append(socket.getInetAddress().getHostAddress() + " in");
				sockets.add(socket);
			
				String ip = socket.getInetAddress().getHostAddress();
				
				ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
				ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
				
				ClientListener listener = new ClientListener(ip, socket, in, out);
				listener.start();
				clients.add(listener);
			}
		} catch (Exception e) {
			output.append(e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void log(String text) {
		Server.output.append(text);
	}
}