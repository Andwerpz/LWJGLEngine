package main;

import static org.lwjgl.glfw.GLFW.*;

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
import static org.lwjgl.system.MemoryUtil.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import org.lwjgl.glfw.GLFWVidMode;

import graphics.Cubemap;
import graphics.Framebuffer;
import graphics.Shader;
import graphics.Texture;
import graphics.VertexArray;
import input.KeyboardInput;
import input.MouseInput;
import model.Model;
import model.ScreenQuad;
import model.SkyboxCube;
import player.Camera;
import player.Player;
import scene.Light;
import scene.Scene;
import scene.World;
import screen.PerspectiveScreen;
import util.BufferUtils;
import util.Mat4;
import util.SystemUtils;
import util.Vec2;
import util.Vec3;

import static org.lwjgl.opengl.GL.*;


public class Main implements Runnable{
	
	//seems like the maximum size the viewport can be is equal to the dimensions of the window
	public static int windowWidth = 1280;
	public static int windowHeight = 720;
	
	private Thread thread;
	private boolean running = false;
	private long startTime;
	
	public static long window;
	private boolean fullscreen = false;
	
	public static final float NEAR = 0.1f;
	public static final float FAR = 200.0f;
	public static final float ASPECT_RATIO = (float) Main.windowWidth / (float) Main.windowHeight;
	public static final float FOV = (float) Math.toRadians(90f);	//vertical FOV
	
	public static Camera camera;
	
	public static long selectedEntityID = 0;
	
	private World world;
	private ScreenQuad screenQuad;	//used to render the final product onto the screen
	
	private PerspectiveScreen screen;
	
	public void start() {
		running = true;
		startTime = System.currentTimeMillis();
		thread = new Thread(this, "Game");
		thread.start();
	}
	
	private void init() {
		if(!glfwInit()) {
			//window failed to init
			return;
		}
		
		glfwWindowHint(GLFW_RESIZABLE, GL_FALSE);
		long primaryMonitor = glfwGetPrimaryMonitor();
		window = glfwCreateWindow(windowWidth, windowHeight, "LWJGL", fullscreen? primaryMonitor : NULL, NULL);
		
		if(window == NULL) {
			return;
		}
		
		GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
		glfwSetWindowPos(window, (vidmode.width() - windowWidth) / 2, (vidmode.height() - windowHeight) / 2);
		glfwMakeContextCurrent(window);
		glfwShowWindow(window);
		
		glfwSetKeyCallback(window, new KeyboardInput());
		glfwSetMouseButtonCallback(window, new MouseInput());
		glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);	//lock mouse to center
		
		createCapabilities();
		glClearColor(0f, 0f, 0f, 0f);
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_CULL_FACE);  
		glCullFace(GL_BACK);  
		System.out.println("OpenGL : " + glGetString(GL_VERSION));
		
		//INIT
		World.init();
		this.world = new World();
		
		this.screenQuad = new ScreenQuad();
		
		//init shaders
		Shader.loadAll();
		
		Shader.GEOMETRY.setUniform1i("tex_diffuse", 0);
		Shader.GEOMETRY.setUniform1i("tex_specular", 1);
		Shader.GEOMETRY.setUniform1i("tex_normal", 2);
		Shader.GEOMETRY.setUniform1i("tex_displacement", 3);
		Shader.GEOMETRY.setUniform1i("enableParallaxMapping", 0);
		Shader.GEOMETRY.setUniform1i("enableTexScaling", 1);
		
		Mat4 pr_matrix = Mat4.perspective(FOV, (float) windowWidth, (float) windowHeight, NEAR, FAR);
		Shader.SKYBOX.setUniformMat4("pr_matrix", pr_matrix);
		Shader.SKYBOX.setUniform1i("skybox", 0);
		
		Shader.LIGHTING.setUniform1i("tex_position", 0);
		Shader.LIGHTING.setUniform1i("tex_normal", 1);
		Shader.LIGHTING.setUniform1i("tex_diffuse", 2);
		Shader.LIGHTING.setUniform1i("shadowMap", 3);
		Shader.LIGHTING.setUniform1i("shadowBackfaceMap", 4);
		Shader.LIGHTING.setUniform1i("shadowCubemap", 5);
		
		Shader.GEOM_POST_PROCESS.setUniform1i("tex_color", 0);
		Shader.GEOM_POST_PROCESS.setUniform1i("tex_position", 1);
		Shader.GEOM_POST_PROCESS.setUniform1i("skybox", 2);
		
		Shader.IMG_POST_PROCESS.setUniform1i("tex_color", 0);
		
		Main.camera = new Camera(FOV, (float) windowWidth, (float) windowHeight, NEAR, FAR);
		
		//wireframe
		//glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
		
		this.screen = new PerspectiveScreen();
		this.screen.setCamera(Main.camera);
	}
	
	public void run() {
		init();	
		
		long lastTime = System.nanoTime();
		double delta = 0.0;
		double ns = 1000000000.0 / 60;
		long timer = System.currentTimeMillis();
		
		int updates = 0;
		int frames = 0;
		while(running) {
			long now = System.nanoTime();
			delta += (now - lastTime) / ns;
			lastTime = now;
			if(delta >= 1.0) {
				update();
				delta --;
				updates ++;
			}
			
			render();
			frames ++;
			
			if(System.currentTimeMillis() - timer > 1000) {
				timer += 1000;
				System.out.println(frames + " fps \\ " + updates + " ups");
				updates = 0;
				frames = 0;
			}			
			
			if(glfwWindowShouldClose(window)) {
				running = false;
			}
		}
		
		glfwDestroyWindow(window);
		glfwTerminate();
	}
	
	private void update() {
		glfwPollEvents();
		
		//esc for exit
		if(KeyboardInput.isKeyPressed(GLFW_KEY_ESCAPE)) {
			running = false;
		}
		
		world.update();
		updateCamera();
	}
	
	private void updateCamera() {
		Player p = world.player;
		
		Main.camera.setPos(p.pos.add(Player.cameraVec));
		Main.camera.setFacing(p.camXRot, p.camYRot);
		Main.camera.setUp(new Vec3(0, 1, 0));
	}
	
	private void render() {
		this.screen.render(Scene.WORLD_SCENE);
		
		// -- POST PROCESSING -- : render contents of color buffer onto screen sized quad
		glBindFramebuffer(GL_FRAMEBUFFER, 0);	//back to default framebuffer
		glClear(GL_COLOR_BUFFER_BIT);
		glDisable(GL_DEPTH_TEST);
		glDisable(GL_BLEND);
		Shader.IMG_POST_PROCESS.enable();
		this.screen.getOutputTexture().bind(GL_TEXTURE0);
		
		screenQuad.render();
		
		glfwSwapBuffers(window);
		
		int error = glGetError();
		if(error != GL_NO_ERROR) {
			System.out.println(error);
		}
	}

	public static void main(String[] args) {
		new Main().start();
	}

}