package lwjglengine.v10.state;

import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.graphics.Texture;
import lwjglengine.v10.model.Model;
import lwjglengine.v10.scene.Light;
import lwjglengine.v10.ui.UIElement;
import myutils.v10.math.Vec2;

public abstract class State {
	// each state manages its own logic and rendering.
	// rendering might consist of multiple overlaid screens

	// should keep track of its own models, allocating when loading, and deallocating them when done. 
	// perhaps can also keep track of its own scenes in a similar manner

	// states are rendered directly onto the state manager window layer, so if there are any windows
	// attached to the state manager, they will always render above the state. 

	protected int bufferWidth, bufferHeight;

	protected StateManagerWindow sm;

	public State() {
		this.sm = null;
	}

	public void setStateManager(StateManagerWindow sm) {
		this.sm = sm;
	}

	/**
	 * use Scene.clearScene() instead
	 * @param scene
	 */
	@Deprecated
	protected void clearScene(int scene) {
		UIElement.removeAllUIElementsFromScene(scene);
		Model.removeInstancesFromScene(scene);
		Light.removeLightsFromScene(scene);
	}

	public void setBufferWidth(int w) {
		this.setBufferDimensions(w, this.bufferHeight);
	}

	public void setBufferHeight(int h) {
		this.setBufferDimensions(this.bufferWidth, h);
	}

	public void setBufferDimensions(int w, int h) {
		this.bufferWidth = w;
		this.bufferHeight = h;
		this.buildBuffers();
	}

	public Vec2 getMousePos() {
		return this.sm.getWindowMousePos();
	}

	// model and scene instance relations
	// before we can add a model instance to a scene, we first must load that model.
	// load state should load all models associated with the current state, unload
	// any others, and reset scene instancing
	public abstract void load(); // this is where all the heavy lifting should be done, not in constructor

	public abstract void buildBuffers();

	//should unload any models related to this state specifically. 
	public abstract void kill();

	public abstract void update();

	public abstract void render(Framebuffer outputBuffer);

	public abstract void mousePressed(int button);

	public abstract void mouseReleased(int button);

	public abstract void mouseScrolled(float wheelOffset, float smoothOffset);

	public abstract void keyPressed(int key);

	public abstract void keyReleased(int key);

}
