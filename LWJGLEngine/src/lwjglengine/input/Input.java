package lwjglengine.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import lwjglengine.entity.Entity;
import lwjglengine.model.FilledRectangle;
import lwjglengine.ui.UIElement;
import myutils.math.Vec2;

public abstract class Input extends UIElement {
	//the scene in this refers to the selection scene. 

	//children of input will request their own decorative scenes. 

	//TODO 
	// - maybe make it so that we don't have to manually call the pressed released hovered etc functions. 
	// - it might be the case that between two windows, two inputs will share the same id. 
	//   - perhaps we also have to specify which selection scene an input is from?

	private static HashMap<Integer, HashMap<String, Input>> inputs = new HashMap<>();

	private int selectionScene;

	protected boolean pressed, hovered;

	//if this is true, then it means that this is the last input to be clicked. 
	private boolean clicked;

	//if true, this input will be disabled, and it won't interact with the user. 
	//TODO implement this
	private boolean isDisabled;

	private String sID;

	protected InputCallback callback = null;

	public Input(float x, float y, float z, float width, float height, String sID, InputCallback callback, int scene) {
		super(x, y, z, width, height, scene);
		this.callback = callback;
		this.init(sID, scene);
	}

	private void init(String sID, int selectionScene) {
		this.sID = sID;

		this.selectionScene = selectionScene;

		this.pressed = false;
		this.hovered = false;
		this.clicked = false;

		Input.addInput(this);
	}

	@Override
	protected void __kill() {
		Input.removeInput(this);
		this.___kill();
	}

	protected abstract void ___kill();

	private void hovered(long entityID) {
		if (this.getID() != entityID) {
			if (this.hovered) {
				this._mouseExited();
			}
			this.hovered = false;
		}
		else {
			if (!this.hovered) {
				this._mouseEntered();
			}
			this.hovered = true;
		}
	}

	protected void _mouseEntered() {
		/* Keeping it optional to implement */
	}

	protected void _mouseExited() {
		/* Keeping it optional to implement */
	}

	private void pressed(long entityID) {
		if (this.getID() != entityID) {
			return;
		}
		this.pressed = true;
		this._pressed();
	}

	protected void _pressed() {
		/* Keeping it optional to implement */
	}

	private void released(long entityID) {
		if (this.pressed && entityID == this.getID()) {
			this.clicked = true;
			this.notifyInputClicked();
			this._clicked();
		}
		else {
			this.clicked = false;
			this._released();
		}
		this.pressed = false;
	}

	/**
	 * User has pressed on this input, but has released outside of the bounds of this input
	 */
	protected void _released() {
		/* Keeping it optional to implement */
	}

	/**
	 * User has clicked on this input. 
	 */
	protected void _clicked() {
		/* Keeping it optional to implement */
	}

	public boolean isClicked() {
		return this.clicked;
	}

	public boolean isHovered() {
		return this.hovered;
	}

	public boolean isPressed() {
		return this.pressed;
	}

	public static void addInput(Input input) {
		String id = input.sID;
		int selectionScene = input.selectionScene;

		if (inputs.get(selectionScene) == null) {
			inputs.put(selectionScene, new HashMap<String, Input>());
		}
		assert !inputs.get(selectionScene).containsKey(id) : "Input : sID \"" + id + "\" already exists";
		inputs.get(selectionScene).put(id, input);
	}

	public static void removeInput(Input input) {
		String id = input.sID;
		int selectionScene = input.selectionScene;

		assert inputs.containsKey(selectionScene) : "Input : scene " + selectionScene + " doesn't exist";
		assert inputs.get(selectionScene).containsKey(id) : "Input : sID \"" + id + "\" doesn't exist";
		assert inputs.get(selectionScene).get(id) == input : "Input : entry in input map doesn't match up";

		inputs.get(selectionScene).remove(id);
		if (inputs.get(selectionScene).size() == 0) {
			inputs.remove(selectionScene);
		}
	}

	public static Input getInput(String id, int scene) {
		if (inputs.get(scene) == null) {
			return null;
		}
		return inputs.get(scene).get(id);
	}

	public static boolean isClicked(String id, int scene) {
		Input b = Input.getInput(id, scene);
		return b == null ? false : b.isClicked();
	}

	public static String getText(String id, int scene) {
		Input b = Input.getInput(id, scene);
		if (!(b instanceof TextField)) {
			return null;
		}
		TextField tf = (TextField) b;
		return tf.getText();
	}

	@Override
	protected void _update() {
		this.__update();
	}

	protected abstract void __update();

	public static void inputsHovered(long entityID, int scene) {
		if (inputs.get(scene) == null) {
			return;
		}
		for (Input b : inputs.get(scene).values()) {
			b.hovered(entityID);
		}
	}

	public static void inputsPressed(long entityID, int scene) {
		if (inputs.get(scene) == null) {
			return;
		}
		for (Input b : inputs.get(scene).values()) {
			b.pressed(entityID);
		}
	}

	public static void inputsReleased(long entityID, int scene) {
		if (inputs.get(scene) == null) {
			return;
		}
		for (Input b : inputs.get(scene).values()) {
			b.released(entityID);
		}
	}

	public static void inputsKeyPressed(int key, int scene) {
		if (inputs.get(scene) == null) {
			return;
		}
		for (Input b : inputs.get(scene).values()) {
			b.keyPressed(key);
		}
	}

	public abstract void keyPressed(int key);

	public static void inputsKeyReleased(int key, int scene) {
		if (inputs.get(scene) == null) {
			return;
		}
		for (Input b : inputs.get(scene).values()) {
			b.keyReleased(key);
		}
	}

	public abstract void keyReleased(int key);

	//	public void disable() {
	//
	//	}
	//
	//	protected abstract void _disable();
	//
	//	public void enable() {
	//
	//	}
	//
	//	protected abstract void _enable();

	protected void notifyInputClicked() {
		if (this.callback != null) {
			this.callback.inputClicked(this.sID);
		}
	}

	protected void notifyInputChanged() {
		if (this.callback != null) {
			this.callback.inputChanged(this.sID);
		}
	}

	public interface InputCallback {
		void inputClicked(String sID);

		void inputChanged(String sID);
	}

}
