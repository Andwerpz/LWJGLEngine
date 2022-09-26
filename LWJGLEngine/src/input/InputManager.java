package input;

import java.util.HashMap;
import java.util.HashSet;

import entity.Entity;

public class InputManager {

    private static HashMap<String, Input> inputs = new HashMap<>();
    private static HashMap<Long, String> entityToStringID = new HashMap<>();
    private static HashMap<String, Long> stringToEntityID = new HashMap<>();

    public static void addInput(String id, Input input) {
	inputs.put(id, input);
	entityToStringID.put(input.getID(), id);
	stringToEntityID.put(id, input.getID());
    }

    public static Input getInput(String id) {
	return inputs.get(id);
    }

    public static void removeInput(String id) {
	inputs.remove(id);
	long entityID = stringToEntityID.get(id);
	entityToStringID.remove(entityID);
	stringToEntityID.remove(id);
    }

    public static void update() {

    }

    public static void removeAllInputs() {
	for (Input b : inputs.values()) {
	    b.kill();
	}
	inputs.clear();
    }

    public static boolean isClicked(String id) {
	Input b = inputs.get(id);
	return b == null ? false : b.isClicked();
    }
    
    public static String getText(String id) {
	Input b = inputs.get(id);
	if(!(b instanceof TextField)) {
	    return null;
	}
	TextField tf = (TextField) b;
	return tf.getText();
    }

    public static void hovered(long entityID) {
	for (Input b : inputs.values()) {
	    b.hovered(entityID);
	}
    }

    public static void pressed(long entityID) {
	String id = entityToStringID.get(entityID);
	if (id == null) {
	    return;
	}
	inputs.get(id).pressed(entityID);
    }

    public static void released(long entityID) {
	for (Input b : inputs.values()) {
	    b.released(entityID);
	}
    }

    public static void keyPressed(int key) {
	for (Input b : inputs.values()) {
	    if (b instanceof TextField) {
		((TextField) b).keyPressed(key);
	    }
	}
    }

    public static void keyReleased(int key) {
	for (Input b : inputs.values()) {
	    if (b instanceof TextField) {
		((TextField) b).keyReleased(key);
	    }
	}
    }
}
