package main;

import static org.lwjgl.glfw.GLFW.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.system.MemoryUtil.*;

import org.lwjgl.glfw.GLFWVidMode;

import graphics.Shader;
import graphics.Texture;
import graphics.VertexArray;
import input.KeyboardInput;
import input.MouseInput;
import model.Cube;
import model.Model;
import scene.World;
import util.Mat4;
import util.Vec3;

import static org.lwjgl.opengl.GL.*;


public class Main implements Runnable{
	
	public static int width = 1280;
	public static int height = 720;
	
	private Thread thread;
	private boolean running = false;
	private long startTime;
	
	public static long window;
	
	private World world;
	
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
		window = glfwCreateWindow(width, height, "JLWGL", NULL, NULL);
		
		if(window == NULL) {
			return;
		}
		
		GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
		glfwSetWindowPos(window, (vidmode.width() - width) / 2, (vidmode.height() - height) / 2);
		glfwMakeContextCurrent(window);
		glfwShowWindow(window);
		
		glfwSetKeyCallback(window, new KeyboardInput());
		glfwSetMouseButtonCallback(window, new MouseInput());
		glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);	//lock mouse to center
		
		createCapabilities();
		glClearColor(0.1f, 0.1f, 0.1f, 0.0f);
		glEnable(GL_DEPTH_TEST);
		System.out.println("OpenGL : " + glGetString(GL_VERSION));
		Shader.loadAll();
		
		Mat4 pr_matrix = Mat4.perspective((float) Math.toRadians(90f), (float) width, (float) height, 0.1f, 1000f);
		Mat4 vw_matrix = Mat4.translate(new Vec3(0));
		Mat4 md_matrix = Mat4.identity();
		
		System.out.println(pr_matrix);
		System.out.println(vw_matrix);
		System.out.println(md_matrix);
		
		Shader.PERS.setUniformMat4("pr_matrix", pr_matrix);
		Shader.PERS.setUniform1i("tex_diffuse", 0);
		Shader.PERS.setUniform1i("tex_specular", 1);
		Shader.PERS.setUniform1i("tex_normal", 2);
		
		//INIT
		World.init();
		this.world = new World();
		
		//wireframe
		//glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

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
	}
	
	Texture triTex;
	
	private void render() {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer
		
		world.render();
		
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