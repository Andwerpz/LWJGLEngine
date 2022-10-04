package server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import util.Pair;
import util.Vec3;

public class GameClient extends Client {
	
	//each time you respawn, you get a new life id, this prevents bullets being shot against a 
	//previous instance of you to harm the respawned instance.
	private int lifeID;	
	
	private Vec3 pos;

	private HashMap<Integer, Vec3> playerPositions;
	private HashMap<Integer, Integer> playerHealths;
	private HashMap<Integer, Integer> playerLifeIDs;

	private ArrayList<Integer> disconnectedPlayers;
	private ArrayList<Pair<Integer, Vec3[]>> inBulletRays; //player id, bullet ray. 
	private ArrayList<Pair<Integer, Vec3[]>> outBulletRays;

	private ArrayList<Pair<Integer, int[]>> inDamageSources;
	private ArrayList<Pair<Integer, int[]>> outDamageSources; //player id, reciever id, damage amt
	
	private boolean shouldRespawn = false;
	private Vec3 respawnPos;
	
	private boolean writeRespawn = false;
	private int writeRespawnHealth;

	public GameClient() {
		super();

		this.pos = new Vec3(0);
		this.playerPositions = new HashMap<>();
		this.playerHealths = new HashMap<>();
		this.playerLifeIDs = new HashMap<>();
		this.disconnectedPlayers = new ArrayList<>();

		this.inBulletRays = new ArrayList<>();
		this.outBulletRays = new ArrayList<>();

		this.inDamageSources = new ArrayList<>();
		this.outDamageSources = new ArrayList<>();
		
		this.respawnPos = new Vec3(0);
		this.writeRespawnHealth = 0;
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
				packetSender.write(this.ID);
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
		
		if(this.writeRespawn) {
			packetSender.writeSectionHeader("respawn", 1);
			packetSender.write(this.writeRespawnHealth);
			packetSender.write(this.lifeID);
			this.writeRespawn = false;
		}
	}

	@Override
	public void readPacket(PacketListener packetListener) {
		while (packetListener.hasMoreBytes()) {
			String sectionName = packetListener.readSectionHeader();
			int elementAmt = packetListener.getSectionElementAmt();

			switch (sectionName) {
			case "player_positions":
				for (int i = 0; i < elementAmt; i++) {
					int playerID = packetListener.readInt();
					float[] arr = packetListener.readNFloats(3);
					this.playerPositions.put(playerID, new Vec3(arr[0], arr[1], arr[2]));
				}
				break;

			case "player_healths":
				for (int i = 0; i < elementAmt; i++) {
					int playerID = packetListener.readInt();
					int playerHealth = packetListener.readInt();
					this.playerHealths.put(playerID, playerHealth);
				}
				break;
				
			case "player_life_ids":
				for(int i = 0; i < elementAmt; i++) {
					int playerID = packetListener.readInt();
					int lifeID = packetListener.readInt();
					this.playerLifeIDs.put(playerID, lifeID);
				}
				break;

			case "disconnect":
				for (int i = 0; i < elementAmt; i++) {
					int playerID = packetListener.readInt();
					this.playerPositions.remove(playerID);
					this.playerHealths.remove(playerID);
					this.playerLifeIDs.remove(playerID);
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
				
			case "should_respawn":
				this.respawnPos = packetListener.readVec3();
				this.shouldRespawn = true;
				break;
			}
		}
	}

	public HashMap<Integer, Vec3> getPlayerPositions() {
		return this.playerPositions;
	}
	
	public HashMap<Integer, Integer> getPlayerHealths() {
		return this.playerHealths;
	}
	
	public HashMap<Integer, Integer> getPlayerLifeIDs(){
		return this.playerLifeIDs;
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
	
	public boolean shouldRespawn() {
		if(this.shouldRespawn) {
			this.shouldRespawn = false;
			return true;
		}
		return false;
	}
	
	public Vec3 getRespawnPos() {
		return this.respawnPos;
	}
	
	public void respawn(int health) {
		this.writeRespawn = true;
		this.writeRespawnHealth = health;
		int oldLifeID = this.lifeID;
		while(this.lifeID != oldLifeID) {
			this.lifeID = (int) (Math.random() * 1000000);
		}
	}

	public int getID() {
		return this.ID;
	}

	public void addBulletRay(Vec3 ray_origin, Vec3 ray_dir) {
		this.outBulletRays.add(new Pair<Integer, Vec3[]>(this.ID, new Vec3[] { ray_origin, ray_dir }));
	}

	public void addDamageSource(int receiverID, int damage) {
		if(!this.playerLifeIDs.containsKey(receiverID) || !this.playerLifeIDs.containsKey(this.ID)) {
			return;
		}
		int aggressorLifeID = this.playerLifeIDs.get(this.ID);
		int receiverLifeID = this.playerLifeIDs.get(receiverID);
		this.outDamageSources.add(new Pair<Integer, int[]>(this.getID(), new int[] { receiverID, damage, aggressorLifeID, receiverLifeID }));
	}

	public void setPos(Vec3 pos) {
		this.pos = new Vec3(pos);
	}

}
