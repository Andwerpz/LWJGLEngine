package server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class PacketSender {
	//packets always start with an int denoting the length of the packet
	
	private ArrayList<Byte> packet;
	
	public PacketSender() {
		this.packet = new ArrayList<>();
	}
	
	public void flush(Socket socket) throws IOException {
		int packetSize = this.packet.size();
		byte[] packetArr = new byte[packetSize];
		for(int i = 0; i < packetSize; i++) {
			packetArr[i] = packet.get(i);
		}
		this.packet.clear();
		DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
		dos.writeInt(packetSize);
		dos.write(packetArr);
		dos.flush();
	}
	
	public void write(byte a) {
		this.packet.add(a);
	}
	
	public void write(byte[] a) {
		for(byte b : a) {
			this.packet.add(b);
		}
	}
	
	public void write(float a) {
		int bits = Float.floatToIntBits(a);
		this.write(bits);
	}
	
	public void write(float[] a) {
		for(float b : a) {
			this.write(b);
		}
	}
	
	public void write(int a) {
		this.packet.add((byte) (0xFF & (a >> 24)));
		this.packet.add((byte) (0xFF & (a >> 16)));
		this.packet.add((byte) (0xFF & (a >> 8)));
		this.packet.add((byte) (0xFF & (a >> 0)));
	}
	
	public void write(int[] a) {
		for(int i : a) {
			this.write(i);
		}
	}
}