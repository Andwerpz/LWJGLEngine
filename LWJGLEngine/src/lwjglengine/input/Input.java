package lwjglengine.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import lwjglengine.entity.Entity;
import lwjglengine.model.FilledRectangle;
import lwjglengine.ui.UIElement;

public abstract class Input extends UIElement {
	//the scene in this refers to the selection scene. 

	//children of input will request their own decorative scenes. 

	//TODO 
	// - maybe make it so that we don't have to manually call the pressed released hovered etc functions. 

	private static HashMap<String, Input> inputs = new HashMap<>();
	private static HashMap<Long, String> entityToStringID = new HashMap<>();
	private static HashMap<String, Long> stringToEntityID = new HashMap<>();

	private static HashMap<Integer, HashSet<Input>> sceneToInput = new HashMap<>();

	protected boolean pressed, hovered, clicked;
	protected boolean mouseEntered, mouseExited;

	private String sID;

	public Input(float x, float y, float z, float width, float height, String sID, FilledRectangle baseRect, int scene) {
		super(x, y, z, width, height, baseRect, scene);
		this.init(sID);
	}

	public Input(float x, float y, float z, float width, float height, String sID, int scene) {
		super(x, y, z, width, height, scene);
		this.init(sID);
	}

	private void init(String sID) {
		this.sID = sID;

		this.pressed = false;
		this.hovered = false;
		this.clicked = false;

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
				this.mouseExited = true;
			}
			this.hovered = false;
		}
		else {
			if (!this.hovered) {
				this.mouseEntered = true;
			}
			this.hovered = true;
		}
	}

	private void pressed(long entityID) {
		if (this.getID() != entityID) {
			return;
		}
		this.pressed = true;
	}

	private void released(long entityID) {
		if (this.pressed && entityID == this.getID()) {
			this.clicked = true;
		}
		else {
			this.clicked = false;
		}
		this.pressed = false;
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

	public boolean hasMouseEntered() {
		return this.mouseEntered;
	}

	public boolean hasMouseExited() {
		return this.mouseExited;
	}

	public static void addInput(String id, Input input) {
		inputs.put(id, input);
		entityToStringID.put(input.getID(), id);
		stringToEntityID.put(id, input.getID());

		if (sceneToInput.get(input.getScene()) == null) {
			sceneToInput.put(input.getScene(), new HashSet<>());
		}
		sceneToInput.get(input.getScene()).add(input);
	}

	public static Input getInput(String id) {
		return inputs.get(id);
	}

	public static boolean isClicked(String id) {
		Input b = inputs.get(id);
		return b == null ? false : b.isClicked();
	}

	/**
	 * Returns the sID of the input if something was clicked, empty string if otherwise
	 * @param id
	 * @return
	 */

	public static String getClicked(int scene) {
		if(sceneToInput.get(scene) == null) {
			return "";
		}
		for(Input i : sceneToInput.get(scene)) {
			if(i.isClicked()) {
				return i.sID;
			}
		}
		return "";
	}

	public static String getHovered(int scene) {
		for (String s : inputs.keySet()) {
			Input i = inputs.get(s);
			if (i.isHovered() && i.getScene() == scene) {
				return s;
			}
		}
		return "";
	}

	public static String getText(String id) {
		Input b = inputs.get(id);
		if (!(b instanceof TextField)) {
			return null;
		}
		TextField tf = (TextField) b;
		return tf.getText();
	}

	@Override
	protected void _update() {
		this.mouseEntered = false;
		this.mouseExited = false;

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
