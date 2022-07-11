package scene;

import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.StringTokenizer;

import org.lwjgl.BufferUtils;

import static org.lwjgl.glfw.GLFW.*;

import graphics.Shader;
import graphics.Texture;
import input.MouseInput;
import main.Main;
import model.Cube;
import model.Model;
import player.Camera;
import player.Player;
import util.Mat4;
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
	
	Player player;
	static Texture crystalTex;
	static Texture containerTex;
	static Texture goldtilesTex;
	static Texture goldnuggetTex;
	static Texture woodboxTex;
	static Texture metalpanelTex;
	static Cube boxModel;
	
	public static ArrayList<Light> lights;
	
	public static long startTime;
	
	public static void init() {
		startTime = System.currentTimeMillis();
		goldtilesTex = new Texture("/goldtiles_diffuse.jpg", "/goldtiles_specular.jpg", "/goldtiles_normal.jpg", null);
		containerTex = new Texture("/container_diffuse.png", "/container_specular.png", null, null);
		crystalTex = new Texture("/crystal_diffuse.jpg", "/crystal_specular.jpg", "/crystal_normal.jpg", "/crystal_displacement.png");
		goldnuggetTex = new Texture("/goldnugget_diffuse.jpg", "/goldnugget_specular.jpg", "/goldnugget_normal.jpg", "/goldnugget_displacement.png");
		woodboxTex = new Texture("/woodbox_diffuse.png", null, "/woodbox_normal.png", "/woodbox_displacement.png");
		metalpanelTex = new Texture("/metalpanel_diffuse.jpg", "/metalpanel_specular.jpg", "/metalpanel_normal.jpg", "/metalpanel_displacement.png");
		boxModel = new Cube();
		
		lights = new ArrayList<>();
		lights.add(new PointLight(new Vec3(0), new Vec3(1), 1f, 0.0014f, 0.000007f));
		//lights.add(new DirLight(new Vec3(0.1f, 1f, 0.5f), new Vec3(1)));
		
		int amt = 100;
		float radius = 10f;
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
		//boxModel.modelMats.add(Mat4.identity());
		boxModel.updateModelMats();
	}

	public World() {
		player = new Player(new Vec3(0, 0, 0));
	}

	public void update() {
		player.update();
		
//		float rads = (float) (System.currentTimeMillis() - startTime) / 1000f;
//		
//		boxModel.modelMats.set(0, Mat4.rotateY(rads));
//		boxModel.updateModelMats();
	}
	
	//assume that the perspective shader is enabled
	public void render() {
		Shader.PERS.enable();
		
		//bind lights
		Shader.PERS.setUniform1i("nrLights", lights.size());
		for(int i = 0; i < lights.size(); i++) {
			lights.get(i).bind(Shader.PERS, i);
		}
		
		//bind view matrix
		Mat4 vw_matrix = player.camera.getViewMatrix();
		Shader.PERS.setUniformMat4("vw_matrix", vw_matrix);
		Shader.PERS.setUniform3f("view_pos", player.camera.pos);
		
		//render world
		boxModel.render(metalpanelTex);
		
		Shader.PERS.disable();
	}
}
