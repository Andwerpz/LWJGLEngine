package server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import util.Vec2;
import util.Vec3;

public class PacketSender {
	// packets always start with an int denoting the length of the packet

	//each section starts with a section header, and the length of the section in bytes. 

	private ArrayList<Byte> packet;

	private ArrayList<Byte> curSection;
	private String curSectionName;

	public PacketSender() {
		this.packet = new ArrayList<>();
	}

	public void flush(Socket socket) throws IOException {
		if (this.curSection != null) { //make sure to include last section before flushing. 
			this.endSection();
		}

		int packetSize = this.packet.size();
		byte[] packetArr = new byte[packetSize];
		for (int i = 0; i < packetSize; i++) {
			packetArr[i] = packet.get(i);
		}
		this.packet.clear();

		DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
		dos.writeInt(packetSize);
		dos.write(packetArr);
		dos.flush();
	}

	public void startSection(String sectionName) {
		if (this.curSection != null) {
			this.endSection();
		}
		this.curSection = new ArrayList<Byte>();
		this.curSectionName = sectionName;
	}

	public void endSection() {
		if (this.curSection == null) {
			System.err.println("Packet Sender, tried to end null section");
			return;
		}

		this.write(this.curSectionName, this.packet);
		this.write(this.curSection.size(), this.packet);
		this.packet.addAll(this.curSection);

		this.curSection = null;
	}

	private void write(byte a, ArrayList<Byte> packet) {
		if (packet == null) {
			System.err.println("Can't write to a null packet");
		}
		packet.add(a);
	}

	public void write(byte a) {
		this.write(a, this.curSection);
	}

	private void write(byte[] a, ArrayList<Byte> packet) {
		for (byte b : a) {
			write(b);
		}
	}

	public void write(byte[] a) {
		this.write(a, this.curSection);
	}

	private void write(float a, ArrayList<Byte> packet) {
		int bits = Float.floatToIntBits(a);
		this.write(bits, packet);
	}

	public void write(float a) {
		this.write(a, this.curSection);
	}

	private void write(float[] a, ArrayList<Byte> packet) {
		for (float b : a) {
			this.write(b, packet);
		}
	}

	public void write(float[] a) {
		this.write(a, this.curSection);
	}

	private void write(Vec3 a, ArrayList<Byte> packet) {
		this.write(a.x, packet);
		this.write(a.y, packet);
		this.write(a.z, packet);
	}

	public void write(Vec3 a) {
		this.write(a, this.curSection);
	}

	private void write(Vec2 a, ArrayList<Byte> packet) {
		this.write(a.x, packet);
		this.write(a.y, packet);
	}

	public void write(Vec2 a) {
		this.write(a, this.curSection);
	}

	private void write(long a, ArrayList<Byte> packet) {
		this.write((byte) (0xFF & (a >> 56)), packet);
		this.write((byte) (0xFF & (a >> 48)), packet);
		this.write((byte) (0xFF & (a >> 40)), packet);
		this.write((byte) (0xFF & (a >> 32)), packet);
		this.write((byte) (0xFF & (a >> 24)), packet);
		this.write((byte) (0xFF & (a >> 16)), packet);
		this.write((byte) (0xFF & (a >> 8)), packet);
		this.write((byte) (0xFF & (a >> 0)), packet);
	}

	public void write(long a) {
		this.write(a, this.curSection);
	}

	private void write(int a, ArrayList<Byte> packet) {
		this.write((byte) (0xFF & (a >> 24)), packet);
		this.write((byte) (0xFF & (a >> 16)), packet);
		this.write((byte) (0xFF & (a >> 8)), packet);
		this.write((byte) (0xFF & (a >> 0)), packet);
	}

	public void write(int a) {
		this.write(a, this.curSection);
	}

	private void write(int[] a, ArrayList<Byte> packet) {
		for (int i : a) {
			this.write(i, packet);
		}
	}

	public void write(int[] a) {
		this.write(a, this.curSection);
	}

	private void write(String a, ArrayList<Byte> packet) {
		this.write(a.length(), packet);
		char[] arr = a.toCharArray();
		for (char c : arr) {
			this.write((byte) c, packet);
		}
	}

	/**
	 * Writes the length of the string, then writes the string.
	 * @param a
	 */
	public void write(String a) {
		this.write(a, this.curSection);
	}
}