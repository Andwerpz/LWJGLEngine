package lwjglengine.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import lwjglengine.entity.Entity;
import lwjglengine.model.FilledRectangle;
import lwjglengine.ui.UIElement;
import myutils.v10.math.Vec2;

public abstract class Input extends UIElement {
	//the scene in this refers to the selection scene. 

	//children of input will request their own decorative scenes. 

	//TODO 
	// - maybe make it so that we don't have to manually call the pressed released hovered etc functions. 
	// - it might be the case that between two windows, two inputs will share the same id. 
	//   - perhaps we also have to specify which selection scene an input is from?

	private static HashMap<Integer, HashMap<String, Input>> inputs = new HashMap<>();
	private static HashMap<Long, String> entityToStringID = new HashMap<>();
	private static HashMap<String, Long> stringToEntityID = new HashMap<>();

	//this does kind of the same thing as 'inputs'
	private static HashMap<Integer, HashSet<Input>> sceneToInput = new HashMap<>();

	private int selectionScene;

	protected boolean pressed, hovered;

	//if this is true, then it means that this is the last input to be clicked. 
	private boolean clicked;

	protected Vec2 mousePos, mouseDiff; //TODO move this to MouseInput

	private String sID;

	public Input(float x, float y, float z, float width, float height, String sID, FilledRectangle baseRect, int scene) {
		super(x, y, z, width, height, baseRect, scene);
		this.init(sID, scene);
	}

	public Input(float x, float y, float z, float width, float height, String sID, int scene) {
		super(x, y, z, width, height, scene);
		this.init(sID, scene);
	}

	private void init(String sID, int selectionScene) {
		this.sID = sID;

		this.selectionScene = selectionScene;

		this.pressed = false;
		this.hovered = false;
		this.clicked = false;

		this.mousePos = MouseInput.getMousePos();
		this.mouseDiff = new Vec2(0);

		Input.addInput(sID, this);
	}

	@Override
	protected void __kill() {
		inputs.remove(this.sID);
		if (stringToEntityID.get(this.sID) == null) {
			System.err.println("Can't find input : " + this.sID);
		}
		else {
			long entityID = stringToEntityID.get(this.sID);
			entityToStringID.remove(entityID);
			stringToEntityID.remove(this.sID);
			sceneToInput.get(this.getScene()).remove(this);
			if (sceneToInput.get(this.getScene()).size() == 0) {
				sceneToInput.remove(this.getScene());
			}
		}
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

	public static void addInput(String id, Input input) {
		int selectionScene = input.selectionScene;

		if (inputs.get(selectionScene) == null) {
			inputs.put(selectionScene, new HashMap<String, Input>());
		}
		inputs.get(selectionScene).put(id, input);

		entityToStringID.put(input.getID(), id);
		stringToEntityID.put(id, input.getID());

		if (sceneToInput.get(input.getScene()) == null) {
			sceneToInput.put(input.getScene(), new HashSet<>());
		}
		sceneToInput.get(input.getScene()).add(input);
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

	/**
	 * Returns the sID of the input if something was clicked, empty string if otherwise
	 * @param id
	 * @return
	 */

	public static String getClicked(int scene) {
		if (sceneToInput.get(scene) == null) {
			return "";
		}
		for (Input i : sceneToInput.get(scene)) {
			if (i.isClicked()) {
				return i.sID;
			}
		}
		return "";
	}

	public static String getHovered(int scene) {
		if (inputs.get(scene) == null) {
			return "";
		}
		for (String s : inputs.get(scene).keySet()) {
			Input i = inputs.get(scene).get(s);
			if (i.isHovered() && i.getScene() == scene) {
				return s;
			}
		}
		return "";
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
		Vec2 nextMousePos = MouseInput.getMousePos();
		this.mouseDiff.set(new Vec2(this.mousePos, nextMousePos));
		this.mousePos.set(nextMousePos);

		this.__update();
	}

	protected abstract void __update();

	public static void inputsHovered(long entityID, int scene) {
		if (sceneToInput.get(scene) == null) {
			return;
		}
		for (Input b : sceneToInput.get(scene)) {
			b.hovered(entityID);
		}
	}

	public static void inputsPressed(long entityID, int scene) {
		if (sceneToInput.get(scene) == null) {
			return;
		}
		for (Input b : sceneToInput.get(scene)) {
			b.pressed(entityID);
		}
	}

	public static void inputsReleased(long entityID, int scene) {
		if (sceneToInput.get(scene) == null) {
			return;
		}
		for (Input b : sceneToInput.get(scene)) {
			b.released(entityID);
		}
	}

	public static void inputsKeyPressed(int key, int scene) {
		if (sceneToInput.get(scene) == null) {
			return;
		}
		for (Input b : sceneToInput.get(scene)) {
			b.keyPressed(key);
		}
	}

	public abstract void keyPressed(int key);

	public static void inputsKeyReleased(int key, int scene) {
		if (sceneToInput.get(scene) == null) {
			return;
		}
		for (Input b : sceneToInput.get(scene)) {
			b.keyReleased(key);
		}
	}

	public abstract void keyReleased(int key);

}
