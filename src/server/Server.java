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

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

import client.PaqueteMensaje;
import client.Usuario;
import java.awt.TextArea;
import java.awt.Color;
import java.awt.Font;

public class Server extends Thread {
	
	private static ArrayList<ClientListener> clientesConectados = new ArrayList<>();
	public static Map<String, Socket> mapConectados = new HashMap<>();
	
	public static ArrayList<Socket> SocketsConectados = new ArrayList<Socket>();
	public static ArrayList<String> UsuariosConectados = new ArrayList<String>();
	
	private static ServerSocket serverSocket;
	private final int puerto = 3000;
	
	private static Thread server;
	
	static TextArea log = new TextArea();
	static boolean estadoServer;
	
	public static ConnectionsListener connectionsListener;
	
	public static ArrayList<ClientListener> getClientesConectados() {
		return clientesConectados;
	}

	public static void setClientesConectados(ArrayList<ClientListener> clientesConectados) {
		Server.clientesConectados = clientesConectados;
	}

	public static ArrayList<String> getUsuariosConectados() {
		return UsuariosConectados;
	}
	
	public static ArrayList<Socket> getSocketsConectados() {
		return SocketsConectados;
	}

	public static void setSocketsConectados(ArrayList<Socket> socketsConectados) {
		SocketsConectados = socketsConectados;
	}

	public static boolean loguearUsuario(Usuario user) {
		boolean result = true;
		if(UsuariosConectados.contains(user.getUsername())) {
			result = false;
		}
		if (result) {
			Server.log.append(user.getUsername() + " logged in" + System.lineSeparator());
			return true;
		} else {
			Server.log.append(user.getUsername() + " is already logged in" + System.lineSeparator());
			return false;
		}
	}
	
	public static Map<String, Socket> getPersonajesConectados() {
		return mapConectados;
	}

	public static void setPersonajesConectados(Map<String, Socket> personajesConectados) {
		Server.mapConectados = personajesConectados;
	}
	
	@Override
	public void run() {
		try {
			estadoServer = true;
			log.append("Server started correctly." + System.lineSeparator());
			serverSocket = new ServerSocket(puerto);
			log.append("Waiting for connections on port " + puerto + " ..." + System.lineSeparator());
			String ipRemota;
			
			connectionsListener = new ConnectionsListener();
			connectionsListener.start();
		
			while (estadoServer) {
				Socket cliente = serverSocket.accept();
				SocketsConectados.add(cliente);
				
				ipRemota = cliente.getInetAddress().getHostAddress();
				log.append(ipRemota + " connected" + System.lineSeparator());

				ObjectOutputStream salida = new ObjectOutputStream(cliente.getOutputStream());
				ObjectInputStream entrada = new ObjectInputStream(cliente.getInputStream());
				
				ClientListener atencion = new ClientListener(ipRemota, cliente, entrada, salida);
				atencion.start();
				clientesConectados.add(atencion);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		JFrame ventana = new JFrame("Chat Server");
		ventana.getContentPane().setBackground(Color.PINK);
		ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		ventana.setSize(542, 538);
		ventana.setResizable(false);
		ventana.setLocationRelativeTo(null);
		ventana.getContentPane().setLayout(null);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(12, 13, 512, 434);
		ventana.getContentPane().add(scrollPane);
		log.setFont(new Font("Comic Sans MS", Font.PLAIN, 13));
		log.setEditable(false);
		
		scrollPane.setViewportView(log);
		
		final JButton botonIniciar = new JButton();
		final JButton botonDetener = new JButton();
		botonIniciar.setText("Start");
		botonIniciar.setBounds(354, 459, 170, 50);
		botonIniciar.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				server = new Thread(new Server());
				server.start();
				botonIniciar.setEnabled(false);
				botonDetener.setEnabled(true);
			}
		});

		ventana.getContentPane().add(botonIniciar);

		botonDetener.setText("Stop");
		botonDetener.setBounds(12, 459, 184, 50);
		botonDetener.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					estadoServer = false;
					UsuariosConectados = new ArrayList<String>();
					server.stop();
					connectionsListener.stop();
					for (ClientListener cliente : clientesConectados) {
						cliente.getSalida().close();
						cliente.getEntrada().close();
						cliente.getSocket().close();
					}
					serverSocket.close();
					log.append("Server stopped." + System.lineSeparator());
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				botonDetener.setEnabled(false);
				botonIniciar.setEnabled(true);
			}
		});
		botonDetener.setEnabled(false);
		ventana.getContentPane().add(botonDetener);
		ventana.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		ventana.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				if (serverSocket != null) {
					try {
						estadoServer = false;
						UsuariosConectados = new ArrayList<String>();
						server.stop();
						connectionsListener.stop();
						for (ClientListener cliente : clientesConectados) {
							cliente.getSalida().close();
							cliente.getEntrada().close();
							cliente.getSocket().close();
						}
						serverSocket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				System.exit(0);
			}
		});
		ventana.setVisible(true);
	}
}