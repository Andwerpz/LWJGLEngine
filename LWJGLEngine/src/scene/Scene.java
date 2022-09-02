package scene;

import java.util.ArrayList;
import java.util.HashMap;

import graphics.Cubemap;
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
	
	public static HashMap<Integer, Cubemap> skyboxes = new HashMap<>();
	
	public static Model dust2;
	public static Cubemap lakeSkybox;
	
	public static void init() {
		dust2 = new Model("/dust2/", "dust2_blend.obj");
		lakeSkybox = new Cubemap(
			"/skybox/right.jpg",
			"/skybox/left.jpg",
			"/skybox/top.jpg",
			"/skybox/bottom.jpg",
			"/skybox/back.jpg",
			"/skybox/front.jpg"
		);
		
		// -- WORLD SCENE --
		Scene.dust2.addInstance(Mat4.rotateX((float) Math.toRadians(90)).mul(Mat4.scale((float) 0.05)), Scene.WORLD_SCENE);
		Light.lights.put(Scene.WORLD_SCENE, new ArrayList<>());
		Light.lights.get(Scene.WORLD_SCENE).add(new DirLight(new Vec3(0.3f, -1f, -0.5f), new Vec3(0.8f), 0.3f));
		skyboxes.put(Scene.WORLD_SCENE, lakeSkybox);
	}
}
