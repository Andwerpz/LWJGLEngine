package server;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Queue;

import util.Vec3;

public class PacketListener implements Runnable {
	private boolean isRunning = true;
	private Thread thread;
	private String name;

	private Socket socket; // socket on which to listen for packets
	private Queue<byte[]> packetQueue;

	private boolean isConnected;

	private long lastPacketTime;
	private long timeoutMillis = 5000;

	private byte[] packet;
	private int readPtr;

	private int sectionElementAmt;

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

	@Override
	public void run() {
		while (this.isRunning) {
			this.listenForPackets();
		}
	}

	public boolean nextPacket() {
		if (this.packetQueue.size() == 0) {
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

	public boolean hasMoreBytes() {
		return this.readPtr < this.packet.length;
	}

	public byte readByte() throws IOException {
		byte ans = this.packet[this.readPtr];
		this.readPtr++;
		return ans;
	}

	public byte[] readNBytes(int n) throws IOException {
		byte[] ans = new byte[n];
		for (int i = 0; i < n; i++) {
			ans[i] = this.readByte();
		}
		return ans;
	}

	public int readInt() throws IOException {
		int ans = 0;
		for (int i = 0; i < 4; i++) {
			ans <<= 8;
			ans |= this.readByte() & 0xFF;
		}
		return ans;
	}

	public int[] readNInts(int n) throws IOException {
		int[] ans = new int[n];
		for (int i = 0; i < n; i++) {
			ans[i] = this.readInt();
		}
		return ans;
	}

	public float readFloat() throws IOException {
		return Float.intBitsToFloat(this.readInt());
	}

	public float[] readNFloats(int n) throws IOException {
		float[] ans = new float[n];
		for (int i = 0; i < n; i++) {
			ans[i] = this.readFloat();
		}
		return ans;
	}

	public Vec3 readVec3() throws IOException {
		return new Vec3(this.readFloat(), this.readFloat(), this.readFloat());
	}

	public char readChar() throws IOException {
		return (char) this.readByte();
	}

	public char[] readNChars(int n) throws IOException {
		char[] ans = new char[n];
		for (int i = 0; i < n; i++) {
			ans[i] = this.readChar();
		}
		return ans;
	}

	public String readString(int len) throws IOException {
		return new String(this.readNChars(len));
	}

	public String readSectionHeader() throws IOException {
		int len = this.readInt();
		String sectionName = this.readString(len);
		this.sectionElementAmt = this.readInt();
		return sectionName;
	}

	public int getSectionElementAmt() {
		return this.sectionElementAmt;
	}

	private void listenForPackets() {
		try {
			DataInputStream dis = new DataInputStream(this.socket.getInputStream());
			int packetSize = dis.readInt();
			this.packetQueue.add(this.readNBytes(packetSize, dis));
			// System.out.println(this.name + " read packet of size " + packetSize);
		}
		catch (IOException e) {
			// probably closed connection
			System.err.println(this.name + " closed connection");
			e.printStackTrace();
			this.isConnected = false;
			this.exit();
		}
		this.lastPacketTime = System.currentTimeMillis();
	}

	private byte[] readNBytes(int n, DataInputStream dis) throws IOException {
		byte[] packet = new byte[n];
		for (int i = 0; i < n; i++) {
			packet[i] = dis.readByte();
		}
		return packet;
	}

	public void exit() {
		this.isRunning = false;
	}
}
