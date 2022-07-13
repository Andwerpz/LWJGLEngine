package main;

import static org.lwjgl.glfw.GLFW.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL21.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.util.ArrayList;

import org.lwjgl.glfw.GLFWVidMode;

import graphics.Framebuffer;
import graphics.Shader;
import graphics.Texture;
import input.KeyboardInput;
import input.MouseInput;
import model.Model;
import model.ScreenQuad;
import scene.Light;
import scene.World;
import util.Mat4;
import util.SystemUtils;

import static org.lwjgl.opengl.GL.*;


public class Main implements Runnable{
	
	public static int windowWidth = 1280;
	public static int windowHeight = 720;
	
	private Thread thread;
	private boolean running = false;
	private long startTime;
	
	public static long window;
	
	private World world;
	private Framebuffer geometryBuffer;
	private Framebuffer lightingBuffer;
	private Model screenQuad;
	
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
		window = glfwCreateWindow(windowWidth, windowHeight, "LWJGL", NULL, NULL);
		
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
		glClearColor(0.1f, 0.1f, 0.1f, 0.0f);
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_CULL_FACE);  
		glCullFace(GL_BACK);  
		System.out.println("OpenGL : " + glGetString(GL_VERSION));
		Shader.loadAll();
		
		Mat4 pr_matrix = Mat4.perspective((float) Math.toRadians(90f), (float) windowWidth, (float) windowHeight, 0.1f, 1000f);
		
		Shader.GEOMETRY.setUniformMat4("pr_matrix", pr_matrix);
		Shader.GEOMETRY.setUniform1i("tex_diffuse", 0);
		Shader.GEOMETRY.setUniform1i("tex_specular", 1);
		Shader.GEOMETRY.setUniform1i("tex_normal", 2);
		Shader.GEOMETRY.setUniform1i("tex_displacement", 3);
		
		Shader.LIGHTING.setUniform1i("tex_position", 0);
		Shader.LIGHTING.setUniform1i("tex_normal", 1);
		Shader.LIGHTING.setUniform1i("tex_diffuse", 2);
		
		Shader.POST_PROCESS.setUniform1i("tex_color", 0);
		
		//INIT
		World.init();
		this.world = new World();
		
		this.screenQuad = new ScreenQuad();
		this.screenQuad.modelMats.add(Mat4.identity());
		this.screenQuad.updateModelMats();
		
		this.geometryBuffer = new Framebuffer(Main.windowWidth, Main.windowHeight);
		this.geometryBuffer.addColorBuffer(GL_RGBA16F, GL_RGBA, GL_FLOAT, GL_COLOR_ATTACHMENT0);	//position
		this.geometryBuffer.addColorBuffer(GL_RGBA16F, GL_RGBA, GL_FLOAT, GL_COLOR_ATTACHMENT1);	//normal
		this.geometryBuffer.addColorBuffer(GL_RGBA, GL_RGBA, GL_FLOAT, GL_COLOR_ATTACHMENT2);	//RGB: color, A: specular
		this.geometryBuffer.addRenderBuffer();
		this.geometryBuffer.setDrawBuffers(new int[] {GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1, GL_COLOR_ATTACHMENT2});
		this.geometryBuffer.isComplete();
		
		this.lightingBuffer = new Framebuffer(Main.windowWidth, Main.windowHeight);
		this.lightingBuffer.addColorBuffer(GL_RGB, GL_RGB, GL_UNSIGNED_BYTE, GL_COLOR_ATTACHMENT0);
		this.lightingBuffer.addRenderBuffer();
		this.lightingBuffer.setDrawBuffers(new int[] {GL_COLOR_ATTACHMENT0});
		this.lightingBuffer.isComplete();
		
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
		//render 3d perspective to geometry buffer
		geometryBuffer.bind();
		glEnable(GL_DEPTH_TEST);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer
		Shader.GEOMETRY.enable();
		
		world.render();

		
		//using information from the geometryBuffer, calculate lighting.
		lightingBuffer.bind();
		glClear(GL_COLOR_BUFFER_BIT);
		glDisable(GL_DEPTH_TEST);
		Shader.LIGHTING.enable();
		
		ArrayList<Light> lights = World.lights;
		Shader.LIGHTING.setUniform1i("nrLights", lights.size());
		for(int i = 0; i < lights.size(); i++) {
			lights.get(i).bind(Shader.LIGHTING, i);
		}
		Shader.LIGHTING.setUniform3f("view_pos", this.world.player.camera.pos);
		
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, geometryBuffer.getColorBuffer(GL_COLOR_ATTACHMENT0));
		glActiveTexture(GL_TEXTURE1);
		glBindTexture(GL_TEXTURE_2D, geometryBuffer.getColorBuffer(GL_COLOR_ATTACHMENT1));
		glActiveTexture(GL_TEXTURE2);
		glBindTexture(GL_TEXTURE_2D, geometryBuffer.getColorBuffer(GL_COLOR_ATTACHMENT2));
		screenQuad.render();
				
		//render contents of lighting buffer onto screen sized quad
		glBindFramebuffer(GL_FRAMEBUFFER, 0);	//back to default framebuffer
		glClear(GL_COLOR_BUFFER_BIT);
		glDisable(GL_DEPTH_TEST);
		Shader.POST_PROCESS.enable();
		
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, lightingBuffer.getColorBuffer(GL_COLOR_ATTACHMENT0));
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