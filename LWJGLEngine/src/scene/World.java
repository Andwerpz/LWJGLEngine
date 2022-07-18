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

import graphics.Cubemap;
import graphics.Material;
import graphics.Shader;
import graphics.Material;
import model.Cube;
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
	
	static Cube boxModel;
	static ScreenQuad quadModel, floorModel;
	static Model dust2;
	
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
		
		boxModel = new Cube();
		floorModel = new ScreenQuad();
		quadModel = new ScreenQuad();
		
		dust2 = new Model("/dust2/", "dust2_blend.obj");
		dust2.modelMats.add(Mat4.rotateX((float) Math.toRadians(90)).mul(Mat4.scale((float) 0.05)));
		dust2.updateModelMats();
		
		int amt = 100;
		float radius = 6f;
		float offset = 2.5f;
		for(int i = 0; i < amt; i++) {
			Mat4 md_matrix = Mat4.identity();
			
			//scale 
			md_matrix.muli(Mat4.scale((float) (Math.random() * 1f + 0.05f)));
			
			//rotate
			md_matrix.muli(Mat4.rotateX((float) (Math.random() * Math.PI))).muli(Mat4.rotateY((float) (Math.random() * Math.PI))).muli(Mat4.rotateZ((float) (Math.random() * Math.PI)));
			
			//translate
			float angle = (float) (Math.random() * Math.PI * 2f);
		    float displacement = (float) (Math.random() * (int)(2 * offset * 100)) / 100.0f - offset;
		    float x = (float) Math.sin(angle) * radius + displacement;
		    displacement = (float) (Math.random() * (int)(2 * offset * 100)) / 100.0f - offset;
		    float y = displacement * 0.4f; // keep height of field smaller compared to width of x and z
		    displacement = (float) (Math.random() * (int)(2 * offset * 100)) / 100.0f - offset;
		    float z = (float) Math.cos(angle) * radius + displacement;
			md_matrix.muli(Mat4.translate(new Vec3(x, y, z)));
			boxModel.modelMats.add(md_matrix);
		}
		
		boxModel.modelMats.add(Mat4.translate(new Vec3(0, -3, 0)));
		
		boxModel.updateModelMats();
		
		int floorSize = 20;
		float floorYOffset = -3;
		for(int i = -floorSize; i <= floorSize; i += 2) {
			for(int j = -floorSize; j <= floorSize; j += 2) {
				floorModel.modelMats.add(Mat4.rotateX((float) Math.toRadians(90)).mul(Mat4.translate(new Vec3(i, floorYOffset, j))));
			}
		}
		floorModel.updateModelMats();
		
		quadModel.modelMats.add(Mat4.identity());
		quadModel.updateModelMats();
	}

	public World() {
		player = new Player(new Vec3(0, 0, 0));
		
		lights = new ArrayList<>();
		lights.add(new DirLight(new Vec3(0.2f, -1f, 0.3f), new Vec3(0.6f)));
		//lights.add(new DirLight(new Vec3(0f, -1f, -1f), new Vec3(1f)));
		//lights.add(new DirLight(new Vec3(-0.2f, -1f, 0.4f), new Vec3(0.5f)));
		//lights.add(new DirLight(new Vec3(0.3f, -1f, 1f), new Vec3(0.6f)));
		
	}

	public void update() {
		player.update();
		
		player.camera.setFacing(player.camera.getFacing());
		
		//lights.get(0).pos = new Vec3(player.camera.pos);
		
//		float rads = (float) (System.currentTimeMillis() - startTime) / 1000f;
//		
//		boxModel.modelMats.set(0, Mat4.rotateY(rads));
//		boxModel.updateModelMats();
	}
	
	public void render(Shader shader, Camera camera) {
		setShaderUniforms(shader, camera);
		shader.enable();
		
		//floorModel.render(new ArrayList<Material>(Arrays.asList(woodfloorTex)));
		
		dust2.render();
	}
	
	public void setShaderUniforms(Shader shader, Camera camera) {
		shader.setUniformMat4("pr_matrix", camera.getProjectionMatrix());
		shader.setUniformMat4("vw_matrix", camera.getViewMatrix());
		shader.setUniform3f("view_pos", camera.getPos());
		shader.setUniform1i("enableParallaxMapping", 0);
		shader.setUniform1i("enableTexScaling", 1);
	}
	
}
