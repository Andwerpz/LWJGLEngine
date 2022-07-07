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
	
	Player player;
	static Texture astolfo;
	
	public static void init() {
		astolfo = new Texture("/astolfo 11.jpg");
		Cube.create();
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
		
		Mat4 vw_matrix = player.camera.getViewMatrix();
		Shader.PERS.setUniformMat4("vw_matrix", vw_matrix);
		Shader.PERS.setUniform3f("view_pos", player.camera.pos);
		
		Mat4 md_matrix = Mat4.translate(new Vec3(0, 0, -3));
		Shader.PERS.setUniformMat4("md_matrix", md_matrix);
		Cube.render(astolfo);
		
		Shader.PERS.disable();
	}
}
