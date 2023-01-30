package input;

import java.util.ArrayList;
import java.util.HashMap;

import entity.Entity;
import ui.UIElement;

public abstract class Input extends UIElement {

	private static HashMap<String, Input> inputs = new HashMap<>();
	private static HashMap<Long, String> entityToStringID = new HashMap<>();
	private static HashMap<String, Long> stringToEntityID = new HashMap<>();

	protected boolean pressed, hovered, clicked;
	protected boolean mouseEntered, mouseExited;

	private String sID;

	public Input(float x, float y, float z, float width, float height, String sID, int scene) {
		super(x, y, z, width, height, scene);

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
		}
		this.___kill();
	}

	protected abstract void ___kill();

	private void hovered(long entityID, int scene) {
		if (scene != this.scene) {
			return;
		}
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

	private void released(long entityID, int scene) {
		if (scene != this.scene) {
			return;
		}
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
	}

	public static Input getInput(String id) {
		return inputs.get(id);
	}

	public static boolean isClicked(String id) {
		Input b = inputs.get(id);
		return b == null ? false : b.isClicked();
	}

	public static String getClicked() {
		for (String s : inputs.keySet()) {
			Input i = inputs.get(s);
			if (i.isClicked()) {
				return s;
			}
		}
		return "";
	}

	public static String getHovered() {
		for (String s : inputs.keySet()) {
			Input i = inputs.get(s);
			if (i.isHovered()) {
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
		for (Input b : inputs.values()) {
			b.hovered(entityID, scene);
		}
	}

	public static void inputsPressed(long entityID) {
		String id = entityToStringID.get(entityID);
		if (id == null) {
			return;
		}
		inputs.get(id).pressed(entityID);
	}

	public static void inputsReleased(long entityID, int scene) {
		for (Input b : inputs.values()) {
			b.released(entityID, scene);
		}
	}

	public static void inputsKeyPressed(int key) {
		for (Input b : inputs.values()) {
			if (b instanceof TextField) {
				((TextField) b).keyPressed(key);
			}
		}
	}

	public static void inputsKeyReleased(int key) {
		for (Input b : inputs.values()) {
			if (b instanceof TextField) {
				((TextField) b).keyReleased(key);
			}
		}
	}

}
