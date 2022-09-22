package server;

import java.util.ArrayList;
import java.util.HashMap;

import util.Vec3;

public class GameServer extends Server {
	
	private HashMap<Integer, Vec3> playerPositions;
	
	private ArrayList<Integer> disconnectedClients;

	public GameServer(String ip, int port) {
		super(ip, port);
		
		this.playerPositions = new HashMap<>();
		this.disconnectedClients = new ArrayList<>();
	}

	@Override
	public void writePacket(PacketSender packetSender, int clientID) {
		packetSender.write(clientID);
		
		packetSender.writeSectionHeader("player_positions", this.playerPositions.size());
		for(int ID : this.playerPositions.keySet()) {
			Vec3 pos = this.playerPositions.get(ID);
			packetSender.write(ID);
			packetSender.write(new float[] {pos.x, pos.y, pos.z});
		}
		
		if(disconnectedClients.size() != 0) {
			packetSender.writeSectionHeader("disconnect", disconnectedClients.size());
			for(int i : disconnectedClients) {
				packetSender.write(i);
			}
			disconnectedClients.clear();
		}
		
	}

	@Override
	public void readPacket(PacketListener packetListener, int clientID) {
		while(packetListener.hasMoreBytes()) {
			String sectionName = packetListener.readSectionHeader();
			int elementAmt = packetListener.getSectionElementAmt();
			
			switch(sectionName) {
			case "pos":
				float[] arr = packetListener.readNFloats(3);
				playerPositions.put(clientID, new Vec3(arr[0], arr[1], arr[2]));
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
