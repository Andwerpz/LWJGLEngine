package server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

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

	private static final int MAX_HEALTH = 100;

	private HashSet<Integer> connectedClients;
	private HashMap<Integer, Vec3> playerPositions;
	private HashMap<Integer, Integer> playerHealths;

	private ArrayList<Integer> disconnectedClients;
	private ArrayList<Pair<Integer, Vec3[]>> bulletRays;
	private ArrayList<Pair<Integer, int[]>> damageSources;
	private ArrayList<Pair<Integer, Integer>> killfeed;

	public GameServer(String ip, int port) {
		super(ip, port);

		this.connectedClients = new HashSet<>();
		this.playerPositions = new HashMap<>();
		this.playerHealths = new HashMap<>();

		this.damageSources = new ArrayList<>();
		this.bulletRays = new ArrayList<>();
		this.killfeed = new ArrayList<>();

		this.disconnectedClients = new ArrayList<>();
	}

	@Override
	public void _update() {
		for (Pair<Integer, int[]> p : this.damageSources) {
			int agressorID = p.first;
			int receiverID = p.second[0];
			int damage = p.second[1];

			if (this.connectedClients.contains(receiverID)) {
				if (!this.connectedClients.contains(agressorID) || this.playerHealths.get(agressorID) == 0) {
					continue;
				}
				this.playerHealths.put(receiverID, this.playerHealths.get(receiverID) - damage);
			}
		}
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

		packetSender.writeSectionHeader("player_healths", this.playerHealths.size());
		for (int ID : this.playerHealths.keySet()) {
			packetSender.write(ID);
			packetSender.write(this.playerHealths.get(ID));
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
		this.playerHealths.put(clientID, MAX_HEALTH);
		this.connectedClients.add(clientID);
	}

	@Override
	public void _clientDisconnect(int clientID) {
		this.playerPositions.remove(clientID);
		this.playerHealths.remove(clientID);
		this.connectedClients.remove(clientID);
		this.disconnectedClients.add(clientID);
	}

	@Override
	public void _exit() {

	}

}
