package scene;

import java.util.ArrayList;
import java.util.HashMap;

import graphics.Cubemap;
import model.AssetManager;
import model.Model;
import util.Mat4;
import util.Vec3;

public abstract class Scene {
	//kinda like a map; holds all the information needed to render a 3D scene. 
	
	public static final int NORTH = 0;
	public static final int SOUTH = 1;
	public static final int EAST = 2;
	public static final int WEST = 3;
	public static final int UP = 4;
	public static final int DOWN = 5;
	
	public static final int FRAMEBUFFER_SCENE = 0;	//reserved for special objects that are involved in the rendering pipeline
	public static final int WORLD_SCENE = 1;	//main 3D scene. 
	public static final int UI_SCENE = 2;	//for rendering 2D orthographic projections of the UI. 
	
	public static HashMap<Integer, Cubemap> skyboxes = new HashMap<>();
	
	public static void init() {
		
	}
}
