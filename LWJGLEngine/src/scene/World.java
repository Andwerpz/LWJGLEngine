package scene;

import java.text.NumberFormat;
import java.util.ArrayList;

import graphics.Shader;
import graphics.Texture;
import model.Cube;
import model.ScreenQuad;
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
	
	static Texture crystalTex;
	static Texture containerTex;
	static Texture goldtilesTex;
	static Texture goldnuggetTex;
	static Texture woodboxTex;
	static Texture metalpanelTex;
	static Texture woodfloorTex;
	static Cube boxModel;
	static ScreenQuad quadModel, floorModel;
	
	public static long startTime;
	
	public Player player;
	public static ArrayList<Light> lights;
	
	public static void init() {
		startTime = System.currentTimeMillis();
		woodfloorTex = new Texture("/woodbox_diffuse.png", null, null, null);
		goldtilesTex = new Texture("/goldtiles_diffuse.jpg", "/goldtiles_specular.jpg", "/goldtiles_normal.jpg", null);
		containerTex = new Texture("/container_diffuse.png", "/container_specular.png", null, null);
		crystalTex = new Texture("/crystal_diffuse.jpg", "/crystal_specular.jpg", "/crystal_normal.jpg", "/crystal_displacement.png");
		goldnuggetTex = new Texture("/goldnugget_diffuse.jpg", "/goldnugget_specular.jpg", "/goldnugget_normal.jpg", "/goldnugget_displacement.png");
		woodboxTex = new Texture("/woodbox_diffuse.png", null, "/woodbox_normal.png", "/woodbox_displacement.png");
		metalpanelTex = new Texture("/metalpanel_diffuse.jpg", "/metalpanel_specular.jpg", "/metalpanel_normal.jpg", "/metalpanel_displacement.png");
		boxModel = new Cube();
		floorModel = new ScreenQuad();
		quadModel = new ScreenQuad();
		
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
		boxModel.updateModelMats();
		
		int floorSize = 10;
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
		//lights.add(new PointLight(new Vec3(0, 10, 0), new Vec3(1), 1f, 0.0014f, 0.000007f));
		lights.add(new DirLight(new Vec3(-0.2f, -1f, 0.4f), new Vec3(1)));
	}

	public void update() {
		player.update();
		
		//lights.get(0).pos = new Vec3(player.camera.pos);
		
//		float rads = (float) (System.currentTimeMillis() - startTime) / 1000f;
//		
//		boxModel.modelMats.set(0, Mat4.rotateY(rads));
//		boxModel.updateModelMats();
	}
	
	//assume that the geometry shader is enabled
	public void renderGeometry(Camera camera) {
		//bind view matrix
		Shader.GEOMETRY.setUniformMat4("vw_matrix", camera.getViewMatrix());
		Shader.GEOMETRY.setUniform3f("view_pos", camera.pos);
		
		//render world
		Shader.GEOMETRY.setUniform1i("enableParallaxMapping", 0);
		Shader.GEOMETRY.setUniform1i("enableTexScaling", 1);
		woodfloorTex.bind();
		floorModel.render();
		
		Shader.GEOMETRY.setUniform1i("enableParallaxMapping", 1);
		containerTex.bind();
		boxModel.render();
	}
	
	//assume depth shader is enabled
	public void renderDepth(Mat4 projectionMat, Mat4 viewMat) {
		Shader.DEPTH.setUniformMat4("pr_matrix", projectionMat);
		Shader.DEPTH.setUniformMat4("vw_matrix", viewMat);
		
		floorModel.render();
		boxModel.render();
	}
}
