package state;

import graphics.Framebuffer;
import graphics.Texture;

public abstract class State {
	//each state manages it's own logic and rendering. 
	
	//rendering might consist of multiple overlaid screens
	
	protected Framebuffer outputBuffer;
	protected Texture outputColorMap;
	
	protected StateManager sm;
	
	public State(StateManager sm) {
		this.sm = sm;
	}
	
	public abstract void load();	//this is where all the heavy lifting should be done, not in constructor
	
	public abstract void update();
	public abstract void render(Framebuffer outputBuffer);
	
}
