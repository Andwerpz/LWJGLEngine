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

public abstract class Server implements Runnable {
	private boolean isRunning = true;
	private Thread thread;
	
	private int FPS = 60;
	private long targetTime = 1000 / FPS;
	
	private String ip;
	private int port;
	
	private ServerConnectionRequestListener serverConnectionRequestListener;
	
	private ServerSocket serverSocket;
	private ArrayList<Socket> clientSockets;
	private ArrayList<PacketListener> packetListeners;
	private PacketSender packetSender;
	
	private long noClientTimeoutMillis = 15000;
	private long firstNoClientTime = 0;
	private boolean prevTickNoClients = false;
	
	public Server(String ip, int port) {
		this.ip = ip;
		this.port = port;
		
		this.serverSocket = null;
		try {
			this.serverSocket = new ServerSocket(this.port, 8, InetAddress.getByName(this.ip));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		this.clientSockets = new ArrayList<>();
		this.packetListeners = new ArrayList<>();
		this.serverConnectionRequestListener = new ServerConnectionRequestListener(this.serverSocket);
		this.packetSender = new PacketSender();
		
		this.start();
	}
	
	private void start() {
		this.thread = new Thread(this);
		this.thread.start();
	}

	public void run() {
		long start, elapsed, wait;
		while(isRunning) {
			start = System.nanoTime();
			
			tick();
			
			elapsed = System.nanoTime() - start;
			wait = targetTime - elapsed / 1000000;
			
			if(wait < 0) {
				wait = 5;
			}
			
			try {
				this.thread.sleep(wait);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void tick() {
		if(this.serverConnectionRequestListener.hasNewClients()) {
			ArrayList<Socket> newClients = this.serverConnectionRequestListener.getNewClients();
			for(Socket s : newClients) {
				PacketListener l = new PacketListener(s, "Server");
				this.clientSockets.add(s);
				this.packetListeners.add(l);
			}
		}
		
		// -- READ --	//should open for whenever
		for(int i = this.clientSockets.size() - 1; i >= 0; i--) {
			if(!this.packetListeners.get(i).isConnected()) {
				//client disconnected
				System.out.println("Client disconnected");
				this.packetListeners.get(i).exit();
				this.packetListeners.remove(i);
				try {
					this.clientSockets.get(i).close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				this.clientSockets.remove(i);
				continue;
			}
			
			while(this.packetListeners.get(i).nextPacket()) {
				this.readPacket(this.packetListeners.get(i));
			}
			
		}
		
		// -- WRITE --	//should run at set tickrate
		for(int i = this.clientSockets.size() - 1; i >= 0; i--) {
			Socket s = this.clientSockets.get(i);
			try {
				this.writePacket(this.packetSender);
				this.packetSender.flush(s);
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		
		if(this.clientSockets.size() == 0) {	//no more clients :((
			if(this.prevTickNoClients) {
				if(System.currentTimeMillis() - this.firstNoClientTime > this.noClientTimeoutMillis) {
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
	
	//use the packet sender to write a packet. The parent class will flush it for you
	public abstract void writePacket(PacketSender packetSender);
	
	//use the packet listener to read in the packet. The parent class has already polled the next packet
	public abstract void readPacket(PacketListener packetListener);
	
	public void exit() {
		System.out.println("Closing server at " + ip + ":" + port);
		this.serverConnectionRequestListener.exit();
		
		for(int i = 0; i < this.clientSockets.size(); i++) {
			try {
				this.packetListeners.get(i).exit();
				this.clientSockets.get(i).close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		
		try {
			this.serverSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		this.isRunning = false;
	}
	
}

class ServerConnectionRequestListener implements Runnable {
	private boolean isRunning = true;
	private Thread thread;
	
	private ServerSocket serverSocket;	//socket on which to listen for connection requests
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
	
	public void run() {
		while(this.isRunning) {
			Socket s = listenForServerRequest();
			if(s != null) {
				this.newClients.add(s);
			}
		}
	}
	
	public boolean hasNewClients() {
		return this.newClients.size() != 0;
	}
	
	public ArrayList<Socket> getNewClients(){
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
		} catch (IOException e) {
			System.out.println("No connection requests");
		}
		return null;
	}
	
	public void exit() {
		this.isRunning = false;
	}
}