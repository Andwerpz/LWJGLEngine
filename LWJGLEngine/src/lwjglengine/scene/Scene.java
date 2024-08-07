package lwjglengine.scene;

import java.util.HashMap;
import java.util.HashSet;

import lwjglengine.graphics.Cubemap;
import lwjglengine.model.Model;
import lwjglengine.ui.UIElement;

public abstract class Scene {
	// kinda like a map; holds all the information needed to render a 3D scene.

	//maybe try to keep track of all scenes
	public static HashSet<Integer> scenes = new HashSet<Integer>();

	public static final int NORTH = 0;
	public static final int SOUTH = 1;
	public static final int EAST = 2;
	public static final int WEST = 3;
	public static final int UP = 4;
	public static final int DOWN = 5;

	public static final int FRAMEBUFFER_SCENE = generateScene(); // reserved for special objects that are involved in the rendering
	public static final int TEMP_SCENE = generateScene();	//for things that we don't want rendered. 

	public static HashMap<Integer, Cubemap> skyboxes = new HashMap<>();

	/**
	 * Generates a new unreserved scene id
	 * @return
	 */
	public static int generateScene() {
		int newID = -1;
		while (newID == -1 || scenes.contains(newID)) {
			newID = (int) (Math.random() * 1000000000);
		}
		scenes.add(newID);
		System.out.println("CREATE SCENE " + newID);
		return newID;
	}

	/**
	 * Should deallocate all related info to the scene
	 * 
	 * After you clear a scene, all of the models that were in the scene will be killed, so be careful. 
	 * @param scene
	 */
	public static void clearScene(int scene) {
		UIElement.removeAllUIElementsFromScene(scene);
		Model.removeInstancesFromScene(scene);
		Light.removeLightsFromScene(scene);
		skyboxes.remove(scene);
	}

	/**
	 * Should deallocate all related info to the scene, and unreserve the input scene id
	 * 
	 * After you remove a scene, all of the models that were in the scene will be killed, so be careful. 
	 * @param scene
	 */
	public static void removeScene(int scene) {
		clearScene(scene);
		scenes.remove(scene);
		System.out.println("REMOVE SCENE " + scene);
	}
}
