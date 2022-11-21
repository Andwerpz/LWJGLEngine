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

	private ArrayList<Pair<String, String>> killfeed;

	private ArrayList<Pair<Integer, Pair<Integer, Vec3[]>>> inBulletRays; //player id, (weapon id, bullet ray)
	private ArrayList<Pair<Integer, Pair<Integer, Vec3[]>>> outBulletRays;

	private ArrayList<Pair<Integer, int[]>> inDamageSources; //player id, reciever id, damage amt
	private ArrayList<Pair<Integer, int[]>> outDamageSources;

	private ArrayList<Pair<Integer, Pair<Integer, float[]>>> inFootsteps; //footstep type, x, y, z
	private ArrayList<Pair<Integer, Pair<Integer, float[]>>> outFootsteps;

	private ArrayList<String> serverMessages;

	private boolean shouldRespawn = false;
	private Vec3 respawnPos;

	private boolean writeRespawn = false;
	private int writeRespawnHealth;

	private boolean writeNickname = false;
	private String nickname;

	public GameClient() {
		super();

		this.pos = new Vec3(0);
		this.playerPositions = new HashMap<>();
		this.playerHealths = new HashMap<>();
		this.playerLifeIDs = new HashMap<>();

		this.disconnectedPlayers = new ArrayList<>();

		this.killfeed = new ArrayList<>();

		this.inBulletRays = new ArrayList<>();
		this.outBulletRays = new ArrayList<>();

		this.inDamageSources = new ArrayList<>();
		this.outDamageSources = new ArrayList<>();

		this.inFootsteps = new ArrayList<>();
		this.outFootsteps = new ArrayList<>();

		this.serverMessages = new ArrayList<>();

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
			for (Pair<Integer, Pair<Integer, Vec3[]>> p : this.outBulletRays) {
				packetSender.write(this.ID);
				packetSender.write(p.second.first);
				packetSender.write(p.second.second[0]);
				packetSender.write(p.second.second[1]);
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

		if (this.outFootsteps.size() != 0) {
			packetSender.writeSectionHeader("footsteps", this.outFootsteps.size());
			for (Pair<Integer, Pair<Integer, float[]>> p : this.outFootsteps) {
				packetSender.write(p.first);
				packetSender.write(p.second.first);
				packetSender.write(p.second.second);
			}
			this.outFootsteps.clear();
		}

		if (this.writeRespawn) {
			packetSender.writeSectionHeader("respawn", 1);
			packetSender.write(this.writeRespawnHealth);
			packetSender.write(this.lifeID);
			this.writeRespawn = false;
		}

		if (this.writeNickname) {
			packetSender.writeSectionHeader("set_nickname", 1);
			packetSender.write(this.nickname.length());
			packetSender.write(this.nickname);
			this.writeNickname = false;
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
				for (int i = 0; i < elementAmt; i++) {
					int playerID = packetListener.readInt();
					int lifeID = packetListener.readInt();
					this.playerLifeIDs.put(playerID, lifeID);
				}
				break;

			case "killfeed":
				for (int i = 0; i < elementAmt; i++) {
					int aggressorNickLength = packetListener.readInt();
					String aggressorNick = packetListener.readString(aggressorNickLength);
					int receiverNickLength = packetListener.readInt();
					String receiverNick = packetListener.readString(receiverNickLength);
					this.killfeed.add(new Pair<String, String>(aggressorNick, receiverNick));
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
					int weaponID = packetListener.readInt();
					Vec3 ray_origin = packetListener.readVec3();
					Vec3 ray_dir = packetListener.readVec3();
					this.inBulletRays.add(new Pair<Integer, Pair<Integer, Vec3[]>>(playerID, new Pair<Integer, Vec3[]>(weaponID, new Vec3[] { ray_origin, ray_dir })));
				}
				break;

			case "should_respawn":
				this.respawnPos = packetListener.readVec3();
				this.shouldRespawn = true;
				break;

			case "server_messages":
				for (int i = 0; i < elementAmt; i++) {
					int sLength = packetListener.readInt();
					String s = packetListener.readString(sLength);
					this.serverMessages.add(s);
				}
				break;

			case "footsteps":
				for (int i = 0; i < elementAmt; i++) {
					int sourceClientID = packetListener.readInt();
					int footstepType = packetListener.readInt();
					float[] footstepPos = packetListener.readNFloats(3);
					this.inFootsteps.add(new Pair<Integer, Pair<Integer, float[]>>(sourceClientID, new Pair<Integer, float[]>(footstepType, footstepPos)));
				}
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

	public HashMap<Integer, Integer> getPlayerLifeIDs() {
		return this.playerLifeIDs;
	}

	public ArrayList<Pair<String, String>> getKillfeed() {
		ArrayList<Pair<String, String>> ans = new ArrayList<>();
		ans.addAll(this.killfeed);
		this.killfeed.clear();
		return ans;
	}

	public ArrayList<Pair<Integer, Pair<Integer, float[]>>> getFootsteps() {
		ArrayList<Pair<Integer, Pair<Integer, float[]>>> ans = new ArrayList<>();
		ans.addAll(this.inFootsteps);
		this.inFootsteps.clear();
		return ans;
	}

	public ArrayList<Integer> getDisconnectedPlayers() {
		ArrayList<Integer> ans = new ArrayList<>();
		ans.addAll(this.disconnectedPlayers);
		this.disconnectedPlayers.clear();
		return ans;
	}

	public ArrayList<Pair<Integer, Pair<Integer, Vec3[]>>> getBulletRays() {
		ArrayList<Pair<Integer, Pair<Integer, Vec3[]>>> ans = new ArrayList<>();
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

	public ArrayList<String> getServerMessages() {
		ArrayList<String> ans = new ArrayList<>();
		ans.addAll(this.serverMessages);
		this.serverMessages.clear();
		return ans;
	}

	public boolean shouldRespawn() {
		if (this.shouldRespawn) {
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
		while (this.lifeID == oldLifeID || this.lifeID == 0) {
			this.lifeID = (int) (Math.random() * 1000000);
		}
	}

	public int getID() {
		return this.ID;
	}

	public void addDamageSource(int receiverID, int damage) {
		if (!this.playerLifeIDs.containsKey(receiverID) || !this.playerLifeIDs.containsKey(this.ID)) {
			return;
		}
		int aggressorLifeID = this.playerLifeIDs.get(this.ID);
		int receiverLifeID = this.playerLifeIDs.get(receiverID);
		this.outDamageSources.add(new Pair<Integer, int[]>(this.getID(), new int[] { receiverID, damage, aggressorLifeID, receiverLifeID }));
	}

	public void addFootstep(int footstepType, Vec3 footstepPos) {
		this.outFootsteps.add(new Pair<Integer, Pair<Integer, float[]>>(this.getID(), new Pair<Integer, float[]>(footstepType, new float[] { footstepPos.x, footstepPos.y, footstepPos.z })));
	}

	public void setPos(Vec3 pos) {
		this.pos = new Vec3(pos);
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
		this.writeNickname = true;
	}

}
