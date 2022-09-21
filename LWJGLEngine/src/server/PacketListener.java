package server;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Queue;

public class PacketListener implements Runnable {
	private boolean isRunning = true;
	private Thread thread;
	private String name;
	
	private Socket socket;	//socket on which to listen for packets
	private Queue<byte[]> packetQueue;
	private byte[] packet;
	private int readPtr;
	private boolean isConnected;
	
	private long lastPacketTime;
	private long timeoutMillis = 5000;
	
	public PacketListener(Socket socket, String name) {
		this.socket = socket;
		this.packetQueue = new ArrayDeque<>();
		this.name = name;
		this.isConnected = true;
		this.lastPacketTime = System.currentTimeMillis();
		
		this.start();
	}
	
	private void start() {
		this.thread = new Thread(this);
		this.thread.start();
	}
	
	public void run() {
		while(this.isRunning) {
			this.listenForPackets();
		}
	}
	
	public boolean nextPacket() {
		if(this.packetQueue.size() == 0) {
			return false;
		}
		this.packet = this.packetQueue.poll();
		this.readPtr = 0;
		return true;
	}
	
	public boolean isConnected() {
		long timeFromLastPacket = System.currentTimeMillis() - lastPacketTime;
		return timeFromLastPacket < timeoutMillis && isConnected;
	}
	
	public int readInt() throws ArrayIndexOutOfBoundsException {
		int ans = 0;
		for(int i = 0; i < 4; i++) {
			ans <<= 8;
			ans |= (int) this.readByte() & 0xFF;
		}
		return ans;
	}
	
	public byte readByte() throws ArrayIndexOutOfBoundsException {
		byte ans = this.packet[this.readPtr];
		this.readPtr ++;
		return ans;
	}
	
	public int[] readNInts(int n) throws ArrayIndexOutOfBoundsException {
		int[] ans = new int[n];
		for(int i = 0; i < n; i++) {
			ans[i] = this.readInt();
		}
		return ans;
	}
	
	private void listenForPackets() {
		try {
			DataInputStream dis = new DataInputStream(this.socket.getInputStream());
			int packetSize = dis.readInt();
			this.packetQueue.add(this.readNBytes(packetSize, dis));
			//System.out.println(this.name + " read packet of size " + packetSize);
		} catch(IOException e) {
			//probably closed connection
			e.printStackTrace();
			this.isConnected = false;
		}
		this.lastPacketTime = System.currentTimeMillis();
	}
	
	private byte[] readNBytes(int n, DataInputStream dis) throws IOException {
		byte[] packet = new byte[n];
		for(int i = 0; i < n; i++) {
			packet[i] = dis.readByte();
		}
		return packet;
	}
	
	public void exit() {
		this.isRunning = false;
	}
}
