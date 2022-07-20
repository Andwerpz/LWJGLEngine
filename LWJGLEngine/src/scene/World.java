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
	
	public static final int MAX_LIGHTS = 100;
	
	static Material crystalTex;
	static Material containerTex;
	static Material goldtilesTex;
	static Material goldnuggetTex;
	static Material woodboxTex;
	static Material metalpanelTex;
	static Material woodfloorTex;
	static Material skyboxTex;
	
	public static Cubemap skybox;
	
	static Model dust2;
	static Model rock1;
	
	public static long startTime;
	
	public Player player;
	public static ArrayList<Light> lights;
	
	public static void init() {
		startTime = System.currentTimeMillis();
		woodfloorTex = new Material("/woodbox_diffuse.png", null, null, null);
		goldtilesTex = new Material("/goldtiles_diffuse.jpg", "/goldtiles_specular.jpg", "/goldtiles_normal.jpg", null);
		containerTex = new Material("/container_diffuse.png", "/container_specular.png", null, null);
		crystalTex = new Material("/crystal_diffuse.jpg", "/crystal_specular.jpg", "/crystal_normal.jpg", "/crystal_displacement.png");
		goldnuggetTex = new Material("/goldnugget_diffuse.jpg", "/goldnugget_specular.jpg", "/goldnugget_normal.jpg", "/goldnugget_displacement.png");
		woodboxTex = new Material("/woodbox_diffuse.png", null, "/woodbox_normal.png", "/woodbox_displacement.png");
		metalpanelTex = new Material("/metalpanel_diffuse.jpg", "/metalpanel_specular.jpg", "/metalpanel_normal.jpg", "/metalpanel_displacement.png");
		skyboxTex = new Material("/skybox/right.jpg", null, null, null);
		
		skybox = new Cubemap(
			"/skybox/right.jpg",
			"/skybox/left.jpg",
			"/skybox/top.jpg",
			"/skybox/bottom.jpg",
			"/skybox/back.jpg",
			"/skybox/front.jpg"
		);
		
		dust2 = new Model("/dust2/", "dust2_blend.obj");
		dust2.addInstance(Mat4.rotateX((float) Math.toRadians(90)).mul(Mat4.scale((float) 0.05)));
		
	}

	public World() {
		player = new Player(new Vec3(0, 0, 0));
		
		lights = new ArrayList<>();
		lights.add(new DirLight(new Vec3(0.3f, -1f, -0.5f), new Vec3(0.6f)));
	}

	public void update() {
		Entity.updateEntities();
		Model.updateModels();
	}
	
	public void render(Shader shader, Camera camera) {
		setShaderUniforms(shader, camera);
		shader.enable();
		Model.renderModels();
	}
	
	public void setShaderUniforms(Shader shader, Camera camera) {
		shader.setUniformMat4("pr_matrix", camera.getProjectionMatrix());
		shader.setUniformMat4("vw_matrix", camera.getViewMatrix());
		shader.setUniform3f("view_pos", camera.getPos());
		shader.setUniform1i("enableParallaxMapping", 0);
		shader.setUniform1i("enableTexScaling", 1);
	}
	
}
