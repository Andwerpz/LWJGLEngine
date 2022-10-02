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
	private ArrayList<Pair<Integer, Integer>> damageSources;

	private ArrayList<Pair<Integer, Vec3[]>> outBulletRays;

	public GameClient() {
		super();

		this.pos = new Vec3(0);
		this.otherPlayerPositions = new HashMap<>();
		this.disconnectedPlayers = new ArrayList<>();
		this.inBulletRays = new ArrayList<>();

		this.outBulletRays = new ArrayList<>();
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
					int damage = packetListener.readInt();
					this.damageSources.add(new Pair<Integer, Integer>(playerID, damage));
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

	public ArrayList<Pair<Integer, Integer>> getDamageSources() {
		ArrayList<Pair<Integer, Integer>> ans = new ArrayList<>();
		ans.addAll(this.damageSources);
		this.damageSources.clear();
		return ans;
	}

	public int getID() {
		return this.id;
	}

	public void addBulletRay(Vec3 ray_origin, Vec3 ray_dir) {
		this.outBulletRays.add(new Pair<Integer, Vec3[]>(this.id, new Vec3[] { ray_origin, ray_dir }));
	}

	public void setPos(Vec3 pos) {
		this.pos = new Vec3(pos);
	}

}
