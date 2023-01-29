package state;

import graphics.Framebuffer;
import graphics.Texture;
import model.Model;
import scene.Light;
import ui.UIElement;

public abstract class State {
	// each state manages it's own logic and rendering.

	// rendering might consist of multiple overlaid screens

	protected Framebuffer outputBuffer;
	protected Texture outputColorMap;

	protected StateManager sm;

	public State(StateManager sm) {
		this.sm = sm;
	}

	protected void clearScene(int scene) {
		UIElement.removeAllUIElementsFromScene(scene);
		Model.removeInstancesFromScene(scene);
		Light.removeLightsFromScene(scene);
	}

	// model and scene instance relations
	// before we can add a model instance to a scene, we first must load that model.
	// load state should load all models associated with the current state, unload
	// any others, and reset scene instancing
	public abstract void load(); // this is where all the heavy lifting should be done, not in constructor

	public abstract void kill();

	public abstract void update();

	public abstract void render(Framebuffer outputBuffer);

	public abstract void mousePressed(int button);

	public abstract void mouseReleased(int button);

	public abstract void mouseScrolled(float wheelOffset, float smoothOffset);

	public abstract void keyPressed(int key);

	public abstract void keyReleased(int key);

}
