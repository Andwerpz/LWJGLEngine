package server;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;

public abstract class Client implements Runnable {

	public static HashSet<Client> clients = new HashSet<>();

	private boolean isRunning = true;
	private Thread thread;

	private int FPS = 60;
	private long targetTime = 1000 / FPS;

	private String ip;
	private int port;

	private boolean connectedToServer = false;
	private boolean connectionAttemptFailed = false;
	private Socket socket;
	private PacketListener packetListener;
	private PacketSender packetSender;

	protected int ID; //your client id assigned by the server

	public Client() {
		this.packetSender = new PacketSender();

		Client.clients.add(this);

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

	public abstract void _update();

	public void update() {
		if (this.connectedToServer) {
			// -- READ --
			if (this.packetListener == null || !this.packetListener.isConnected()) { // lost connection to server
				this.disconnect();
			}

			while (this.packetListener.nextPacket()) {
				this.ID = this.packetListener.readInt();
				this.readPacket(this.packetListener);
			}

			_update();

			// -- WRITE --
			try {
				this.writePacket(this.packetSender);
				this.packetSender.flush(this.socket);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// use the packet sender to write a packet. The parent class will flush it for
	// you
	public abstract void writePacket(PacketSender packetSender);

	// use the packet listener to read in the packet. The parent class has already
	// polled the next packet
	public abstract void readPacket(PacketListener packetListener);

	public boolean connect(String ip, int port) {
		this.ip = ip;
		this.port = port;
		this.connectionAttemptFailed = false;
		try {
			this.socket = new Socket(this.ip, this.port);
		}
		catch (IOException e) {
			this.connectionAttemptFailed = true;
			System.out.println("Unable to connect to the address: " + ip + ":" + port);
			return false;
		}
		System.out.println("Successfully connected to the address: " + ip + ":" + port);
		this.connectedToServer = true;
		this.packetListener = new PacketListener(this.socket, "Client");
		return true;
	}

	public void disconnect() {
		this.connectedToServer = false;
		this.connectionAttemptFailed = false;

		try {
			if (this.socket != null) {
				this.socket.close();
			}
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (this.packetListener != null) {
			this.packetListener.exit();
		}
	}

	public boolean isConnected() {
		return this.connectedToServer;
	}

	public boolean isRunning() {
		return this.isRunning;
	}

	public void exit() {
		if (this.packetListener != null) {
			this.packetListener.exit();
		}
		this.disconnect();
		this.isRunning = false;
	}

}