package server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import util.Vec3;

public class GameClient extends Client {

    private Vec3 pos;

    private HashMap<Integer, Vec3> otherPlayerPositions;
    private ArrayList<Integer> disconnectedPlayers;

    private int id;

    public GameClient() {
	super();

	this.pos = new Vec3(0);
	this.otherPlayerPositions = new HashMap<>();
	this.disconnectedPlayers = new ArrayList<>();
    }

    @Override
    public void writePacket(PacketSender packetSender) {
	packetSender.writeSectionHeader("pos", 1);
	packetSender.write(pos.x);
	packetSender.write(pos.y);
	packetSender.write(pos.z);
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

    public void setPos(Vec3 pos) {
	this.pos = new Vec3(pos);
    }

}
