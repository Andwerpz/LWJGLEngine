package server;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Queue;

import util.Pair;
import util.Vec2;
import util.Vec3;

public class PacketListener implements Runnable {
	//each packet starts with an int denoting it's length

	//then, each section starts with a string header, followed by it's length. 
	//we should read each section in, and the server or client should process each section one by one. 

	private boolean isRunning = true;
	private Thread thread;
	private String name;

	private Socket socket; // socket on which to listen for packets
	private Queue<Pair<String, byte[]>> sectionQueue;

	private boolean isConnected;

	private long lastPacketTime;
	private long timeoutMillis = 5000;

	private byte[] section;
	private String sectionName;
	private int readPtr;

	public PacketListener(Socket socket, String name) {
		this.socket = socket;
		this.sectionQueue = new ArrayDeque<>();
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

	public boolean nextSection() {
		if (this.sectionQueue.size() == 0) {
			return false;
		}
		this.section = this.sectionQueue.peek().second;
		this.sectionName = this.sectionQueue.peek().first;
		this.sectionQueue.poll();
		this.readPtr = 0;
		return true;
	}

	public String getSectionName() {
		return this.sectionName;
	}

	public boolean isConnected() {
		long timeFromLastPacket = System.currentTimeMillis() - lastPacketTime;
		return timeFromLastPacket < timeoutMillis && isConnected;
	}

	public boolean hasMoreBytes() {
		return this.readPtr < this.section.length;
	}

	public byte readByte() throws IOException {
		byte ans = this.section[this.readPtr];
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

	public long readLong() throws IOException {
		long ans = 0;
		for (int i = 0; i < 8; i++) {
			ans <<= 8;
			ans |= this.readByte() & 0xFF;
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

	public Vec2 readVec2() throws IOException {
		return new Vec2(this.readFloat(), this.readFloat());
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

	/**
	 * First reads an int, len, then reads a string of size len. 
	 * @param len
	 * @return
	 * @throws IOException
	 */
	public String readString() throws IOException {
		int len = this.readInt();
		return new String(this.readNChars(len));
	}

	private void listenForPackets() {
		try {
			DataInputStream dis = new DataInputStream(this.socket.getInputStream());
			int packetSize = dis.readInt();

			//parse the packet into sections
			int ptr = 0;
			while (ptr < packetSize) {
				int sectionNameLength = this.readInt(dis);
				ptr += 4;

				char[] cstr = new char[sectionNameLength];
				for (int i = 0; i < sectionNameLength; i++) {
					cstr[i] = (char) dis.readByte();
					ptr += 1;
				}
				String sectionName = new String(cstr);

				int sectionContentsLength = this.readInt(dis);
				ptr += 4;

				byte[] sectionContents = new byte[sectionContentsLength];
				for (int i = 0; i < sectionContentsLength; i++) {
					sectionContents[i] = dis.readByte();
					ptr += 1;
				}

				this.sectionQueue.add(new Pair<String, byte[]>(sectionName, sectionContents));
			}

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

	private int readInt(DataInputStream dis) throws IOException {
		int ans = 0;
		for (int i = 0; i < 4; i++) {
			ans <<= 8;
			ans |= dis.readByte() & 0xFF;
		}
		return ans;
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
