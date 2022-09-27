package scene;

import java.util.HashMap;

import graphics.Cubemap;

public abstract class Scene {
	// kinda like a map; holds all the information needed to render a 3D scene.

	public static final int NORTH = 0;
	public static final int SOUTH = 1;
	public static final int EAST = 2;
	public static final int WEST = 3;
	public static final int UP = 4;
	public static final int DOWN = 5;

	public static final int FRAMEBUFFER_SCENE = -1; // reserved for special objects that are involved in the rendering

	public static HashMap<Integer, Cubemap> skyboxes = new HashMap<>();

	public static void init() {

	}
}
