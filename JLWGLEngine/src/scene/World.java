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
	
	public static final int MAX_LIGHTS = 256;
	
	Player player;
	static Texture container;
	
	public static ArrayList<Light> lights;
	
	public static void init() {
		container = new Texture("/container_diffuse.png", "/container_specular.png");
		Cube.create();
		lights = new ArrayList<>();
		//lights.add(new DirLight(new Vec3(0.1f, -1, 0.5f), new Vec3(1)));
		lights.add(new PointLight(new Vec3(1.2f, 0.9f, -1.5f), new Vec3(1), 1f, 0.09f, 0.032f));
	}

	public World() {
		player = new Player(new Vec3(0, 0, 0));
	}

	public void update() {
		player.update();
	}
	
	//assume that the perspective shader is enabled
	public void render() {
		Shader.PERS.enable();
		
		//lights
		Shader.PERS.setUniform1i("nrLights", lights.size());
		for(int i = 0; i < lights.size(); i++) {
			lights.get(i).bind(Shader.PERS, i);
		}
		
		Mat4 vw_matrix = player.camera.getViewMatrix();
		Shader.PERS.setUniformMat4("vw_matrix", vw_matrix);
		Shader.PERS.setUniform3f("view_pos", player.camera.pos);
		
		Mat4 md_matrix = Mat4.translate(new Vec3(0, 0, -3));
		Shader.PERS.setUniformMat4("md_matrix", md_matrix);
		Cube.render(container);
		
		md_matrix = Mat4.translate(new Vec3(0, 1, -2));
		Shader.PERS.setUniformMat4("md_matrix", md_matrix);
		Cube.render(container);
		
		md_matrix = Mat4.translate(new Vec3(4, -1, 0));
		Shader.PERS.setUniformMat4("md_matrix", md_matrix);
		Cube.render(container);
		
		Shader.PERS.disable();
	}
}
