package scene;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL21.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL41.*;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.opengl.GL44.*;
import static org.lwjgl.opengl.GL45.*;

import java.nio.IntBuffer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import entity.Entity;
import graphics.Cubemap;
import graphics.Material;
import graphics.Shader;
import graphics.Material;
import model.Model;
import model.ScreenQuad;
import player.Camera;
import player.Player;
import util.Mat4;
import util.SystemUtils;
import util.Vec3;

public class World {
	//I think the cardinals are consistent
	//north = -z
	//south = +z
	//east = -x
	//west = +x
	
	public static final int NORTH = 0;
	public static final int SOUTH = 1;
	public static final int EAST = 2;
	public static final int WEST = 3;
	public static final int UP = 4;
	public static final int DOWN = 5;
	
	public static Cubemap skybox;
	
	static Model dust2;
	
	public static long startTime;
	
	public Player player;
	public static ArrayList<Light> lights;
	
	public static void init() {
		startTime = System.currentTimeMillis();
		
		skybox = new Cubemap(
			"/skybox/right.jpg",
			"/skybox/left.jpg",
			"/skybox/top.jpg",
			"/skybox/bottom.jpg",
			"/skybox/back.jpg",
			"/skybox/front.jpg"
		);
		
		dust2 = new Model("/dust2/", "dust2_blend.obj");
		dust2.addInstance(Mat4.rotateX((float) Math.toRadians(90)).mul(Mat4.scale((float) 0.05)), Scene.WORLD_SCENE);
		
		Model.updateModels();
	}

	public World() {
		player = new Player(new Vec3(0, 0, 0));
		
		lights = new ArrayList<>();
		lights.add(new DirLight(new Vec3(0.3f, -1f, -0.5f), new Vec3(0.8f), 0.3f));
	}

	public void update() {
		Entity.updateEntities();
		Model.updateModels();
	}
	
}
