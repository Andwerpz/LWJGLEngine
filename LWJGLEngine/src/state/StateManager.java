package state;

import graphics.Framebuffer;
import graphics.Texture;
import player.Player;
import util.Vec3;

public class StateManager {
	
	protected Framebuffer outputBuffer;
	protected Texture outputColorMap;
	
	public State activeState;
	
	public static Player player;
	
	public StateManager() {
		player = new Player(new Vec3(0, 0, 0));
		this.activeState = new GameState();
	}
	
	public void update() {
		this.activeState.update();
	}
	
	public Texture render() {
		return this.activeState.render();
	}
	
}
