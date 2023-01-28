package server;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public abstract class Server implements Runnable {

	public static HashSet<Server> servers = new HashSet<>();

	private boolean isRunning = true;
	private Thread thread;

	private int FPS = 60;
	private long targetTime = 1000 / FPS;

	private String ip;
	private int port;

	private ServerConnectionRequestListener serverConnectionRequestListener;

	private ServerSocket serverSocket;
	private HashSet<Integer> clientIDs;
	private HashMap<Integer, Socket> clientSockets;
	private HashMap<Integer, PacketListener> packetListeners;
	private PacketSender packetSender;

	private HashMap<Integer, Integer> clientCommunicationErrorCounter;

	private long noClientTimeoutMillis = 15000;
	private long firstNoClientTime = 0;
	private boolean prevTickNoClients = false;

	public Server(String ip, int port) {
		this.ip = ip;
		this.port = port;

		this.serverSocket = null;
		try {
			this.serverSocket = new ServerSocket(this.port, 8, InetAddress.getByName(this.ip));
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		this.clientIDs = new HashSet<>();
		this.clientSockets = new HashMap<>();
		this.packetListeners = new HashMap<>();
		this.serverConnectionRequestListener = new ServerConnectionRequestListener(this.serverSocket);
		this.packetSender = new PacketSender();

		this.clientCommunicationErrorCounter = new HashMap<>();

		Server.servers.add(this);

		this.start();
	}

	private void start() {
		this.thread = new Thread(this);
		this.thread.start();
	}

	@Override
	public void run() {
		long start, elapsed, wait;
		while (isRunning) {
			start = System.nanoTime();

			update();

			elapsed = System.nanoTime() - start;
			wait = targetTime - elapsed / 1000000;

			if (wait < 0) {
				wait = 5;
			}

			try {
				this.thread.sleep(wait);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private int generateNewClientID() {
		int ID = 0;
		while (ID == 0 || this.clientIDs.contains(ID)) {
			ID = (int) (Math.random() * 1000000d);
		}
		return ID;
	}

	public void update() {
		if (this.serverConnectionRequestListener.hasNewClients()) {
			ArrayList<Socket> newClients = this.serverConnectionRequestListener.getNewClients();
			for (Socket s : newClients) {
				PacketListener l = new PacketListener(s, "Server");
				int ID = this.generateNewClientID();
				this.clientIDs.add(ID);
				this.clientSockets.put(ID, s);
				this.packetListeners.put(ID, l);
				this._clientConnect(ID);
			}
		}

		// -- READ -- should open for whenever
		ArrayList<Integer> disconnectedClients = new ArrayList<>();
		for (int ID : this.clientIDs) {
			if (!this.packetListeners.get(ID).isConnected()) {
				disconnectedClients.add(ID);
				continue;
			}

			while (this.packetListeners.get(ID).nextPacket()) {
				try {
					this.readPacket(this.packetListeners.get(ID), ID);
					if (this.clientCommunicationErrorCounter.containsKey(ID)) {
						this.clientCommunicationErrorCounter.remove(ID);
					}
				}
				catch (IOException e) {
					//something went wrong with communicating with the client. 
					System.err.println("Error when communicating with client " + ID);
					this.clientCommunicationErrorCounter.put(ID, this.clientCommunicationErrorCounter.getOrDefault(ID, 0) + 1);
					if (this.clientCommunicationErrorCounter.get(ID) >= 3) {
						System.err.println("Force disconnect client " + ID + " due to too many errors during communication");
						disconnectedClients.add(ID);
					}
					e.printStackTrace();
				}
			}
		}

		// -- DEAL WITH DISCONNECTED CLIENTS --
		for (int ID : disconnectedClients) {
			// Client Disconnected
			System.out.println("Client disconnected");
			this.packetListeners.get(ID).exit();
			this.packetListeners.remove(ID);
			try {
				this.clientSockets.get(ID).close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			this.clientSockets.remove(ID);
			this.clientIDs.remove(ID);
			this._clientDisconnect(ID);
		}

		this._update();

		// -- WRITE -- //should run at set tickrate
		for (int ID : this.clientIDs) {
			Socket s = this.clientSockets.get(ID);
			try {
				this.packetSender.write(ID);
				this.writePacket(this.packetSender, ID);
				this.packetSender.flush(s);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		this.writePacketEND();

		if (this.clientSockets.size() == 0) { // no more clients :((
			if (this.prevTickNoClients) {
				if (System.currentTimeMillis() - this.firstNoClientTime > this.noClientTimeoutMillis) {
					System.out.println("No clients, shutting down server");
					this.exit();
				}
			}
			else {
				this.firstNoClientTime = System.currentTimeMillis();
				this.prevTickNoClients = true;
			}
		}
		else {
			this.prevTickNoClients = false;
		}
	}

	//placed between read and write, allows the server to process the information just read in. 
	public abstract void _update();

	// use the packet sender to write a packet. The parent class will flush it for you
	public abstract void writePacket(PacketSender packetSender, int clientID);

	// run once after all packets to clients have been sent. 
	public abstract void writePacketEND();

	// use the packet listener to read in the packet. The parent class has already polled the next packet
	public abstract void readPacket(PacketListener packetListener, int clientID) throws IOException;

	// so that the child class can do whatever they need to do in the case of connection status change
	public abstract void _clientConnect(int clientID);

	public abstract void _clientDisconnect(int clientID);

	public abstract void _exit();

	public boolean isRunning() {
		return this.isRunning;
	}

	public void exit() {
		System.out.println("Closing server at " + ip + ":" + port);
		this.serverConnectionRequestListener.exit();

		for (int ID : this.clientIDs) {
			try {
				if (this.packetListeners.get(ID) != null) {
					this.packetListeners.get(ID).exit();
				}
				if (this.clientSockets.get(ID) != null) {
					this.clientSockets.get(ID).close();
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			this.serverSocket.close();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		this.isRunning = false;
	}

}

class ServerConnectionRequestListener implements Runnable {
	private boolean isRunning = true;
	private Thread thread;

	private ServerSocket serverSocket; // socket on which to listen for connection requests
	private ArrayList<Socket> newClients;

	public ServerConnectionRequestListener(ServerSocket serverSocket) {
		this.serverSocket = serverSocket;
		this.newClients = new ArrayList<>();
		this.start();
	}

	private void start() {
		this.thread = new Thread(this);
		this.thread.start();
	}

	@Override
	public void run() {
		while (this.isRunning) {
			Socket s = listenForServerRequest();
			if (s != null) {
				this.newClients.add(s);
			}
		}
	}

	public boolean hasNewClients() {
		return this.newClients.size() != 0;
	}

	public ArrayList<Socket> getNewClients() {
		ArrayList<Socket> out = new ArrayList<>();
		out.addAll(this.newClients);
		this.newClients = new ArrayList<>();
		return out;
	}

	private Socket listenForServerRequest() {
		try {
			System.out.println("Listening for connection requests");
			Socket socket = this.serverSocket.accept();
			System.out.println("Client has joined");
			return socket;
		}
		catch (IOException e) {
			System.out.println("No connection requests");
		}
		return null;
	}

	public void exit() {
		this.isRunning = false;
	}
}