package main;

import static org.lwjgl.glfw.GLFW.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.system.MemoryUtil.*;

import org.lwjgl.glfw.GLFWVidMode;

import graphics.Shader;
import graphics.Texture;
import graphics.VertexArray;
import input.Input;
import util.Mat4;
import util.Vec3;

import static org.lwjgl.opengl.GL.*;


public class Main implements Runnable{
	
	public static int width = 1280;
	public static int height = 720;
	
	private Thread thread;
	private boolean running = false;
	private long startTime;
	
	private long window;
	
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
		
		glfwSetKeyCallback(window, new Input());
		
		createCapabilities();
		glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		glEnable(GL_DEPTH_TEST);
		glActiveTexture(GL_TEXTURE1);
		System.out.println("OpenGL : " + glGetString(GL_VERSION));
		Shader.loadAll();
		
		Mat4 pr_matrix = Mat4.perspective((float) Math.toRadians(90f), (float) width, (float) height, 0.1f, 1000f);
		Mat4 vw_matrix = Mat4.translate(new Vec3(0, 0, -3));
		Mat4 md_matrix = Mat4.identity();
		
		System.out.println(pr_matrix);
		System.out.println(vw_matrix);
		System.out.println(md_matrix);
		
		Shader.HTRI.setUniform1i("tex", 1);
		
		Shader.PERS.setUniformMat4("pr_matrix", pr_matrix);
		Shader.PERS.setUniformMat4("vw_matrix", vw_matrix);
		Shader.PERS.setUniformMat4("md_matrix", md_matrix);
		Shader.PERS.setUniform1i("tex", 1);
		
		tri = new VertexArray(vertices, indices, tex);
		triTex = new Texture("res/astolfo 11.jpg");
		
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
		
		//upd matrices
		Mat4 pr_matrix = Mat4.perspective((float) Math.toRadians(90f), (float) width, (float) height, 0.1f, 1000f);
		Mat4 vw_matrix = Mat4.translate(new Vec3(0, 0, -3));
		Mat4 md_matrix = Mat4.rotateX((float) Math.toRadians(System.currentTimeMillis() - startTime) * 0.04f);
		md_matrix = md_matrix.mul(Mat4.rotateY((float) Math.toRadians(System.currentTimeMillis() - startTime) * 0.1f));
		
		Shader.PERS.setUniformMat4("pr_matrix", pr_matrix);
		Shader.PERS.setUniformMat4("vw_matrix", vw_matrix);
		Shader.PERS.setUniformMat4("md_matrix", md_matrix);
	}
	
	float[] vertices = new float[] {
		0f, 0f, 0f,
		1f, 0f, 0f,
		1f, 1f, 0f,
		0f, 1f, 0f,
		0f, 0f, 1f,
		1f, 0f, 1f,
		1f, 1f, 1f,
		0f, 1f, 1f,
	};
	
	float[] tex = new float[] {
		0f, 0f,
		0f, 1f,
		1f, 1f,
		1f, 0f,
		0f, 0f,
		0f, 1f,
		1f, 1f,
		1f, 0f,
	};
	
	byte[] indices = new byte[] {
		0, 1, 2,
		0, 3, 2,
		4, 5, 6,
		4, 7, 6,
		0, 1, 5,
		0, 4, 5,
		1, 2, 6,
		1, 5, 6,
		2, 3, 7,
		2, 6, 7,
		3, 0, 4,
		3, 7, 4
	};
	
//	float vertices[] = {
//        // positions      
//         0.5f,  0.5f, 0.0f,
//         0.5f, -0.5f, 0.0f,
//        -0.5f, -0.5f, 0.0f,
//        -0.5f,  0.5f, 0.0f, 
//    };
//	
//	float[] tex = new float[] {
//		1f, 1f,
//		1f, 0f,
//		0f, 0f,
//		0f, 1f
//	};
//    byte[] indices = new byte[]{
//        0, 1, 3, // first triangle
//        1, 2, 3  // second triangle
//    };
	
	VertexArray tri;
	Texture triTex;
	
	private void render() {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer
		
		triTex.bind();
		Shader.PERS.enable();
		tri.render();
		triTex.unbind();
		Shader.PERS.disable();
		
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