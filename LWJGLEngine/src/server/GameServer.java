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

	private static float[][] respawnPoints = new float[][] { { 19.034498f, 4.209578E-6f, -27.220726f }, { 16.666616f, 2.0344742E-6f, -13.573268f }, { 7.693447f, 1.3825484E-6f, -6.869356f }, { 7.530435f, -2.705492E-7f, 3.1779733f }, { -7.62718f, 1.7262031f, 10.893081f },
			{ -24.021294f, 1.7262031f, 9.220424f }, { -23.85061f, 7.8836456E-7f, -4.302293f }, { -19.921665f, 0.43155238f, -14.355063f }, { -23.368916f, 3.1562522E-6f, -25.018263f }, { -20.494223f, 3.7797854E-6f, -31.763893f }, { -12.507785f, 4.310161E-6f, -33.866653f },
			{ -4.116003f, -1.7262013f, -25.567888f }, { 3.98981f, -1.7262013f, -32.315727f }, { 16.353655f, 1.2946576f, -35.787464f }, { 5.4487123f, 1.2946573f, -34.161304f }, { 4.7261915f, 2.5471672E-6f, -20.083405f }, { -8.604298f, 2.3543835E-6f, -6.142592f },
			{ -6.170692f, -1.726202f, -20.582148f }, { -12.950915f, -1.5104262f, -17.905237f }, };

	private HashSet<Integer> connectedClients;
	private HashMap<Integer, Vec3> playerPositions;
	private HashMap<Integer, Integer> playerHealths;
	private HashMap<Integer, Integer> playerLifeIDs;

	private ArrayList<Integer> disconnectedClients;
	private ArrayList<Pair<Integer, Vec3[]>> bulletRays;
	private ArrayList<Pair<Integer, int[]>> damageSources;
	private ArrayList<Pair<Integer, Integer>> killfeed;

	public GameServer(String ip, int port) {
		super(ip, port);

		this.connectedClients = new HashSet<>();
		this.playerPositions = new HashMap<>();
		this.playerHealths = new HashMap<>();
		this.playerLifeIDs = new HashMap<>();

		this.damageSources = new ArrayList<>();
		this.bulletRays = new ArrayList<>();
		this.killfeed = new ArrayList<>();

		this.disconnectedClients = new ArrayList<>();
	}

	@Override
	public void _update() {
		for (Pair<Integer, int[]> p : this.damageSources) {
			int aggressorID = p.first;
			int receiverID = p.second[0];
			int damage = p.second[1];

			if (this.connectedClients.contains(receiverID)) {
				if (!this.connectedClients.contains(aggressorID) || this.playerHealths.get(aggressorID) <= 0) {
					continue;
				}

				this.playerHealths.put(receiverID, this.playerHealths.get(receiverID) - damage);
				if (this.playerHealths.get(receiverID) <= 0 && this.playerHealths.get(receiverID) + damage > 0) { //killing blow
					this.killfeed.add(new Pair<Integer, Integer>(aggressorID, receiverID));
				}
			}
		}
	}

	@Override
	public void writePacket(PacketSender packetSender, int clientID) {
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

		packetSender.writeSectionHeader("player_life_ids", this.playerLifeIDs.size());
		for (int ID : this.playerLifeIDs.keySet()) {
			packetSender.write(ID);
			packetSender.write(this.playerLifeIDs.get(ID));
		}

		if (this.killfeed.size() != 0) {
			packetSender.writeSectionHeader("killfeed", this.killfeed.size());
			for (Pair<Integer, Integer> p : this.killfeed) {
				packetSender.write(p.first);
				packetSender.write(p.second);
			}
		}

		if (playerHealths.get(clientID) <= 0) {
			packetSender.writeSectionHeader("should_respawn", 1);
			packetSender.write(new Vec3(respawnPoints[(int) (Math.random() * respawnPoints.length)]));
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
		killfeed.clear();
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
					int aggressorLifeID = packetListener.readInt();
					int receiverLifeID = packetListener.readInt();
					if (this.playerLifeIDs.get(clientID) != aggressorLifeID || this.playerLifeIDs.get(receiverID) != receiverLifeID) {
						//the aggressor damaged the receivers past life. 
						continue;
					}
					this.damageSources.add(new Pair<Integer, int[]>(playerID, new int[] { receiverID, damage }));
				}
				break;

			case "respawn":
				int health = packetListener.readInt();
				int lifeID = packetListener.readInt();
				this.playerHealths.put(clientID, health);
				this.playerLifeIDs.put(clientID, lifeID);
				break;
			}
		}
	}

	@Override
	public void _clientConnect(int clientID) {
		this.playerPositions.put(clientID, new Vec3(0));
		this.playerHealths.put(clientID, 0);
		this.playerLifeIDs.put(clientID, 0);
		this.connectedClients.add(clientID);
	}

	@Override
	public void _clientDisconnect(int clientID) {
		this.playerPositions.remove(clientID);
		this.playerHealths.remove(clientID);
		this.playerLifeIDs.remove(clientID);
		this.connectedClients.remove(clientID);
		this.disconnectedClients.add(clientID);
	}

	@Override
	public void _exit() {

	}

}
