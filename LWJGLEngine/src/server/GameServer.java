package server;

import java.util.ArrayList;
import java.util.HashMap;

import entity.Capsule;
import graphics.Material;
import model.Model;
import state.GameState;
import util.Mat4;
import util.MathUtils;
import util.Pair;
import util.Vec3;
import util.Vec4;

public class GameServer extends Server {

	private HashMap<Integer, Vec3> playerPositions;

	private ArrayList<Integer> disconnectedClients;
	private ArrayList<Pair<Integer, Vec3[]>> bulletRays;
	private ArrayList<Pair<Integer, int[]>> damageSources;

	public GameServer(String ip, int port) {
		super(ip, port);

		this.playerPositions = new HashMap<>();
		this.damageSources = new ArrayList<>();
		this.disconnectedClients = new ArrayList<>();
		this.bulletRays = new ArrayList<>();
	}

	@Override
	public void _update() {
		//TODO move hit detection to server
	}

	@Override
	public void writePacket(PacketSender packetSender, int clientID) {
		packetSender.write(clientID);

		packetSender.writeSectionHeader("player_positions", this.playerPositions.size());
		for (int ID : this.playerPositions.keySet()) {
			Vec3 pos = this.playerPositions.get(ID);
			packetSender.write(ID);
			packetSender.write(new float[] { pos.x, pos.y, pos.z });
		}

		if (disconnectedClients.size() != 0) {
			packetSender.writeSectionHeader("disconnect", disconnectedClients.size());
			for (int i : disconnectedClients) {
				packetSender.write(i);
			}
		}

		if (this.bulletRays.size() != 0) {
			packetSender.writeSectionHeader("bullet_rays", this.bulletRays.size());
			for (Pair<Integer, Vec3[]> p : this.bulletRays) {
				packetSender.write(p.first);
				packetSender.write(p.second[0]);
				packetSender.write(p.second[1]);
			}
		}

		if (this.damageSources.size() != 0) {
			packetSender.writeSectionHeader("damage_sources", this.damageSources.size());
			for (Pair<Integer, int[]> p : this.damageSources) {
				packetSender.write(p.first);
				packetSender.write(p.second);
			}
		}

	}

	@Override
	public void writePacketEND() {
		disconnectedClients.clear();
		damageSources.clear();
		bulletRays.clear();
	}

	@Override
	public void readPacket(PacketListener packetListener, int clientID) {
		while (packetListener.hasMoreBytes()) {
			String sectionName = packetListener.readSectionHeader();
			int elementAmt = packetListener.getSectionElementAmt();

			switch (sectionName) {
			case "pos":
				float[] arr = packetListener.readNFloats(3);
				playerPositions.put(clientID, new Vec3(arr[0], arr[1], arr[2]));
				break;

			case "bullet_rays":
				for (int i = 0; i < elementAmt; i++) {
					int playerID = packetListener.readInt();
					Vec3 ray_origin = packetListener.readVec3();
					Vec3 ray_dir = packetListener.readVec3();
					this.bulletRays.add(new Pair<Integer, Vec3[]>(playerID, new Vec3[] { ray_origin, ray_dir }));
				}
				break;

			case "damage_sources":
				for (int i = 0; i < elementAmt; i++) {
					int playerID = packetListener.readInt();
					int receiverID = packetListener.readInt();
					int damage = packetListener.readInt();
					this.damageSources.add(new Pair<Integer, int[]>(playerID, new int[] { receiverID, damage }));
				}
				break;
			}
		}
	}

	@Override
	public void _clientConnect(int clientID) {
		this.playerPositions.put(clientID, new Vec3(0));
	}

	@Override
	public void _clientDisconnect(int clientID) {
		this.playerPositions.remove(clientID);
		this.disconnectedClients.add(clientID);
	}

	@Override
	public void _exit() {

	}

}
