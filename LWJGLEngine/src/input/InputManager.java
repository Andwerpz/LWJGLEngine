package input;

import java.util.HashMap;
import java.util.HashSet;

import entity.Entity;

public class InputManager {
	
	private static HashMap<String, Button> inputs = new HashMap<>();
	private static HashMap<Long, String> entityToStringID = new HashMap<>();
	private static HashMap<String, Long> stringToEntityID = new HashMap<>();
	
	public static void addInput(String id, Button input) {
		inputs.put(id, input);
		entityToStringID.put(input.getID(), id);
		stringToEntityID.put(id, input.getID());
	}
	
	public static Button getInput(String id) {
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
		for(Button b : inputs.values()) {
			b.kill();
		}
		inputs.clear();
	}
	
	public static void pressed(long entityID) {
		String id = entityToStringID.get(entityID);
		if(id == null) {
			return;
		}
		inputs.get(id).pressed();
	}
	
	public static void released() {
		for(Button b : inputs.values()) {
			b.released();
		}
	}
	
	public static boolean isClicked(String id) {
		Button b = inputs.get(id);
		return b == null? false : b.isClicked();
	}
	
}
