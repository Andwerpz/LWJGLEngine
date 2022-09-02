package state;

import graphics.Framebuffer;
import graphics.Texture;

public abstract class State {
	//each state manages it's own logic and rendering. 
	
	//rendering might consist of multiple overlaid screens
	
	protected Framebuffer outputBuffer;
	protected Texture outputColorMap;
	
	public State() {
		
	}
	
	public abstract void update();
	public abstract Texture render();
	
}
