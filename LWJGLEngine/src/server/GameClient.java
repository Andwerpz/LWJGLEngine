package server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import util.Pair;
import util.Vec3;

public class GameClient extends Client {

	private int id;

	private Vec3 pos;

	private HashMap<Integer, Vec3> otherPlayerPositions;
	private ArrayList<Integer> disconnectedPlayers;
	private ArrayList<Pair<Integer, Vec3[]>> inBulletRays; //player id, bullet ray. 
	private ArrayList<Pair<Integer, Vec3[]>> outBulletRays;

	private ArrayList<Pair<Integer, int[]>> inDamageSources;
	private ArrayList<Pair<Integer, int[]>> outDamageSources; //player id, reciever id, damage amt

	public GameClient() {
		super();

		this.pos = new Vec3(0);
		this.otherPlayerPositions = new HashMap<>();
		this.disconnectedPlayers = new ArrayList<>();

		this.inBulletRays = new ArrayList<>();
		this.outBulletRays = new ArrayList<>();

		this.inDamageSources = new ArrayList<>();
		this.outDamageSources = new ArrayList<>();
	}

	@Override
	public void _update() {

	}

	@Override
	public void writePacket(PacketSender packetSender) {
		packetSender.writeSectionHeader("pos", 1);
		packetSender.write(pos.x);
		packetSender.write(pos.y);
		packetSender.write(pos.z);

		if (this.outBulletRays.size() != 0) {
			packetSender.writeSectionHeader("bullet_rays", this.outBulletRays.size());
			for (Pair<Integer, Vec3[]> p : this.outBulletRays) {
				packetSender.write(this.id);
				packetSender.write(p.second[0]);
				packetSender.write(p.second[1]);
			}
			this.outBulletRays.clear();
		}

		if (this.outDamageSources.size() != 0) {
			packetSender.writeSectionHeader("damage_sources", this.outDamageSources.size());
			for (Pair<Integer, int[]> p : this.outDamageSources) {
				packetSender.write(p.first);
				packetSender.write(p.second);
			}
			this.outDamageSources.clear();
		}
	}

	@Override
	public void readPacket(PacketListener packetListener) {
		this.id = packetListener.readInt();

		while (packetListener.hasMoreBytes()) {
			String sectionName = packetListener.readSectionHeader();
			int elementAmt = packetListener.getSectionElementAmt();

			switch (sectionName) {
			case "player_positions":
				for (int i = 0; i < elementAmt; i++) {
					int playerID = packetListener.readInt();
					float[] arr = packetListener.readNFloats(3);
					if (playerID == this.id) {
						continue;
					}
					this.otherPlayerPositions.put(playerID, new Vec3(arr[0], arr[1], arr[2]));
				}
				break;

			case "disconnect":
				for (int i = 0; i < elementAmt; i++) {
					int playerID = packetListener.readInt();
					this.otherPlayerPositions.remove(playerID);
					this.disconnectedPlayers.add(playerID);
				}
				break;

			case "bullet_rays":
				for (int i = 0; i < elementAmt; i++) {
					int playerID = packetListener.readInt();
					Vec3 ray_origin = packetListener.readVec3();
					Vec3 ray_dir = packetListener.readVec3();
					this.inBulletRays.add(new Pair<Integer, Vec3[]>(playerID, new Vec3[] { ray_origin, ray_dir }));
				}
				break;

			case "damage_sources":
				for (int i = 0; i < elementAmt; i++) {
					int playerID = packetListener.readInt();
					int recieverID = packetListener.readInt();
					int damage = packetListener.readInt();
					this.inDamageSources.add(new Pair<Integer, int[]>(playerID, new int[] { recieverID, damage }));
				}
				break;
			}
		}
	}

	public HashMap<Integer, Vec3> getOtherPlayerPositions() {
		return this.otherPlayerPositions;
	}

	public ArrayList<Integer> getDisconnectedPlayers() {
		ArrayList<Integer> ans = new ArrayList<>();
		ans.addAll(this.disconnectedPlayers);
		this.disconnectedPlayers.clear();
		return ans;
	}

	public ArrayList<Pair<Integer, Vec3[]>> getBulletRays() {
		ArrayList<Pair<Integer, Vec3[]>> ans = new ArrayList<>();
		ans.addAll(this.inBulletRays);
		this.inBulletRays.clear();
		return ans;
	}

	public ArrayList<Pair<Integer, int[]>> getDamageSources() {
		ArrayList<Pair<Integer, int[]>> ans = new ArrayList<>();
		ans.addAll(this.inDamageSources);
		this.inDamageSources.clear();
		return ans;
	}

	public int getID() {
		return this.id;
	}

	public void addBulletRay(Vec3 ray_origin, Vec3 ray_dir) {
		this.outBulletRays.add(new Pair<Integer, Vec3[]>(this.id, new Vec3[] { ray_origin, ray_dir }));
	}

	public void addDamageSource(int receiverID, int damage) {
		this.outDamageSources.add(new Pair<Integer, int[]>(this.getID(), new int[] { receiverID, damage }));
	}

	public void setPos(Vec3 pos) {
		this.pos = new Vec3(pos);
	}

}
