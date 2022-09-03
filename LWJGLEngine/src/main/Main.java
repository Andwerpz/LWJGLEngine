package main;

import static org.lwjgl.glfw.GLFW.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.*;

import org.lwjgl.glfw.GLFWVidMode;

import graphics.Shader;
import input.KeyboardInput;
import input.MouseInput;
import model.AssetManager;
import model.ScreenQuad;
import scene.Scene;
import state.StateManager;
import util.Mat4;

import static org.lwjgl.opengl.GL.*;

public class Main implements Runnable{
	
	//seems like the maximum size the viewport can be is equal to the dimensions of the window
	public static int windowWidth = 1280;
	public static int windowHeight = 720;
	
	private Thread thread;
	private boolean running = false;
	
	public static long window;
	private boolean fullscreen = false;
	
	public static final float NEAR = 0.1f;
	public static final float FAR = 200.0f;
	public static final float ASPECT_RATIO = (float) Main.windowWidth / (float) Main.windowHeight;
	public static final float FOV = (float) Math.toRadians(90f);	//vertical FOV
	
	public static final int NORTH = 0;
	public static final int SOUTH = 1;
	public static final int EAST = 2;
	public static final int WEST = 3;
	public static final int UP = 4;
	public static final int DOWN = 5;
	
	public static long selectedEntityID = 0;
	
	private StateManager sm;
	
	public void start() {
		running = true;
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
		AssetManager.init();
		Scene.init();
		this.sm = new StateManager();
		
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
		
		Shader.SPLASH.setUniform1i("tex_color", 0);
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
		
		this.sm.update();
	}
	
	private void render() {
		this.sm.render();
		
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