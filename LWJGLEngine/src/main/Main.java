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
import model.Cube;
import model.Model;
import model.ScreenQuad;
import player.Camera;
import scene.Light;
import scene.World;
import util.Mat4;
import util.SystemUtils;
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
	public static final float ASPECT_RATIO = (float) windowWidth / (float) windowHeight;
	public static final float FOV = (float) Math.toRadians(90f);	//vertical FOV
	
	private static final int SHADOW_MAP_NR_CASCADES = 6;
	private static float[] shadowCascades = new float[] {NEAR, 2, 5, 10, 20, 50, FAR};
	
	private World world;
	private Framebuffer geometryBuffer;
	private Framebuffer lightingBuffer;
	private Framebuffer shadowBuffer;
	private Framebuffer skyboxBuffer;
	
	private int shadowDepthMapID;
	private Cubemap shadowCubemap;
	
	private int testTextureID;
	
	private Model skyboxCube;
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
		glClearColor(0.1f, 0.1f, 0.1f, 0.0f);
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_CULL_FACE);  
		glCullFace(GL_BACK);  
		System.out.println("OpenGL : " + glGetString(GL_VERSION));
		
		//INIT
		World.init();
		this.world = new World();
		
		this.screenQuad = new ScreenQuad();
		this.screenQuad.modelMats.add(Mat4.identity());
		this.screenQuad.updateModelMats();
		
		this.skyboxCube = new Cube();
		this.skyboxCube.modelMats.add(Mat4.scale(2).mul(Mat4.translate(new Vec3(-1f))));
		this.skyboxCube.updateModelMats();
		
		this.geometryBuffer = new Framebuffer(Main.windowWidth, Main.windowHeight);
		this.geometryBuffer.addColorBuffer(GL_RGBA16F, GL_RGBA, GL_FLOAT, GL_COLOR_ATTACHMENT0);	//RGB: position, A: depth
		this.geometryBuffer.addColorBuffer(GL_RGBA16F, GL_RGBA, GL_FLOAT, GL_COLOR_ATTACHMENT1);	//RGB: normal
		this.geometryBuffer.addColorBuffer(GL_RGBA, GL_RGBA, GL_FLOAT, GL_COLOR_ATTACHMENT2);	//RGB: color, A: specular
		this.geometryBuffer.addRenderBuffer();
		this.geometryBuffer.setDrawBuffers(new int[] {GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1, GL_COLOR_ATTACHMENT2});
		this.geometryBuffer.isComplete();
		
		this.lightingBuffer = new Framebuffer(Main.windowWidth, Main.windowHeight);
		this.lightingBuffer.addColorBuffer(GL_RGB, GL_RGB, GL_UNSIGNED_BYTE, GL_COLOR_ATTACHMENT0);	//RGB: color
		//this.lightingBuffer.addRenderBuffer();	//on big pc, works without renderbuffer
		this.lightingBuffer.setDrawBuffers(new int[] {GL_COLOR_ATTACHMENT0});
		this.lightingBuffer.isComplete();
		
		this.shadowBuffer = new Framebuffer(Main.windowWidth, Main.windowHeight);
		//this.shadowBuffer.addRenderBuffer();	//on big pc, works without renderbuffer
		this.shadowBuffer.addDepthBuffer();
		this.shadowBuffer.isComplete();
		
		this.shadowDepthMapID = shadowBuffer.getDepthBuffer();
		this.shadowCubemap = new Cubemap();
		
		this.testTextureID = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, testTextureID);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);	//magnification filter
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT); 
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, Main.windowWidth, Main.windowHeight, 0, GL_RGBA, GL_FLOAT, (FloatBuffer) null);
		glBindTexture(GL_TEXTURE_2D, 0);
		
		this.skyboxBuffer = new Framebuffer(Main.windowWidth, Main.windowHeight);
		this.skyboxBuffer.addColorBuffer(GL_RGBA, GL_RGBA, GL_FLOAT, GL_COLOR_ATTACHMENT0);	//RGB: color
		this.skyboxBuffer.addRenderBuffer();
		this.skyboxBuffer.isComplete();
		
		//init shaders
		Shader.loadAll();
		Mat4 pr_matrix = Mat4.perspective(FOV, (float) windowWidth, (float) windowHeight, NEAR, FAR);
		
		Shader.GEOMETRY.setUniform1i("tex_diffuse", 0);
		Shader.GEOMETRY.setUniform1i("tex_specular", 1);
		Shader.GEOMETRY.setUniform1i("tex_normal", 2);
		Shader.GEOMETRY.setUniform1i("tex_displacement", 3);
		
		Shader.SKYBOX.setUniformMat4("pr_matrix", pr_matrix);
		Shader.SKYBOX.setUniform1i("skybox", 0);
		
		Shader.LIGHTING.setUniform1i("tex_position", 0);
		Shader.LIGHTING.setUniform1i("tex_normal", 1);
		Shader.LIGHTING.setUniform1i("tex_diffuse", 2);
		Shader.LIGHTING.setUniform1i("shadowMap", 3);
		Shader.LIGHTING.setUniform1i("shadowCubemap", 4);
		
		Shader.POST_PROCESS.setUniform1i("tex_color", 0);
		Shader.POST_PROCESS.setUniform1i("tex_position", 1);
		Shader.POST_PROCESS.setUniform1i("skybox", 2);
		
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
		// -- GEOMETRY -- : render 3d perspective to geometry buffer
		geometryBuffer.bind();
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_CULL_FACE);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer
		Shader.GEOMETRY.enable();
		world.render(Shader.GEOMETRY, world.player.camera);
		
		// -- LIGHTING -- : using information from the geometry buffer, calculate lighting.
		lightingBuffer.bind();
		Shader.LIGHTING.enable();
		glClear(GL_COLOR_BUFFER_BIT);
		glDisable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
		glBlendFunc(GL_ONE, GL_ONE);
		
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, geometryBuffer.getColorBuffer(GL_COLOR_ATTACHMENT0));
		glActiveTexture(GL_TEXTURE1);
		glBindTexture(GL_TEXTURE_2D, geometryBuffer.getColorBuffer(GL_COLOR_ATTACHMENT1));
		glActiveTexture(GL_TEXTURE2);
		glBindTexture(GL_TEXTURE_2D, geometryBuffer.getColorBuffer(GL_COLOR_ATTACHMENT2));
		glActiveTexture(GL_TEXTURE3);
		glBindTexture(GL_TEXTURE_2D, shadowDepthMapID);
		Shader.LIGHTING.setUniform3f("view_pos", this.world.player.camera.getPos());
		
		//calculate lighting with each light seperately
		ArrayList<Light> lights = World.lights;
		for(int i = 0; i < lights.size(); i++) {
			//generate depth map for light
			if(lights.get(i).type == Light.DIR_LIGHT) {
				Vec3 lightDir = new Vec3(lights.get(i).dir).normalize();
				Vec3 eye = new Vec3(0);
				Mat4 lightMat = Mat4.lookAt(eye, lightDir, new Vec3(0, 1, 0));
				
				//re-bind directional depth map texture as depth map
				shadowBuffer.bindTextureToBuffer(GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, shadowDepthMapID);
				
				//do this for each cascade near / far plane
				for(int cascade = 0; cascade < Main.SHADOW_MAP_NR_CASCADES; cascade++) {
					//calculate orthographic projection matrix
					//generate perspective frustum corners in camera space
					float near = Main.shadowCascades[cascade];
					float far = Main.shadowCascades[cascade + 1];
					float y1 = near * (float) Math.tan(Main.FOV / 2f);
					float y2 = far * (float) Math.tan(Main.FOV / 2f);
					float x1 = y1 * Main.ASPECT_RATIO;
					float x2 = y2 * Main.ASPECT_RATIO;
					Vec3[] corners = new Vec3[] {
						new Vec3(x1, y1, -near),
						new Vec3(-x1, y1, -near),
						new Vec3(-x1, -y1, -near),
						new Vec3(x1, -y1, -near),
						
						new Vec3(x2, y2, -far),
						new Vec3(-x2, y2, -far),
						new Vec3(-x2, -y2, -far),
						new Vec3(x2, -y2, -far),
					};
					
					Shader.LIGHTING.setUniform1f("shadowMapNear", near);
					Shader.LIGHTING.setUniform1f("shadowMapFar", far);
					
					//transform frustum corners from camera space to light space
					Mat4 transformMatrix = new Mat4(world.player.camera.getInvViewMatrix());	//from camera to world space
					transformMatrix.muli(lightMat);	//apply rotation to align light dir with -z axis
					for(int j = 0; j < corners.length; j++) {
						corners[j] = transformMatrix.mul(corners[j], 1f);
					}
					
					//generate the AABB that bounds the corners in light space
					float left = corners[0].x;
					float right = corners[0].x;
					float bottom = corners[0].y;
					float top = corners[0].y;
					near = corners[0].z;
					far = corners[0].z;
					
					for(Vec3 v : corners) {
						left = Math.min(left, v.x);
						right = Math.max(right, v.x);
						bottom = Math.min(bottom, v.y);
						top = Math.max(top, v.y);
						near = Math.min(near, v.z);
						far = Math.max(far, v.z);
					}
					
					//construct orthographic projection matrix
					Mat4 projectionMat = Mat4.orthographic(left, right, bottom, top, near - 100f, far + 20f);
					
					Camera lightCamera = new Camera(left, right, bottom, top, near - 100f, far + 20f);
					lightCamera.setFacing(lightDir);
					
					//render shadow map
					shadowBuffer.bind();
					glEnable(GL_DEPTH_TEST);
					glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
					glDisable(GL_BLEND);
					
					Shader.LIGHTING.setUniformMat4("lightSpace_matrix", lightMat.mul(projectionMat));
					Shader.DEPTH.enable();
					//world.renderDepth(projectionMat, lightMat, Shader.DEPTH);
					world.renderDepth(Shader.DEPTH, lightCamera);
					
					//render portion of lit scene
					lightingBuffer.bind();
					glDisable(GL_DEPTH_TEST);
					glEnable(GL_BLEND);
					Shader.LIGHTING.enable();
					lights.get(i).bind(Shader.LIGHTING, i);
					screenQuad.render();
				}
			}
			else {
				Light light = lights.get(i);
				
				//generate cubemap
				shadowBuffer.bind();
				Shader.CUBE_DEPTH.enable();
				float near = 0.1f;
				float far = 50f;
				
				Shader.CUBE_DEPTH.setUniform1f("far", far);
				
				Vec3[][] camVectors = new Vec3[][] {
					{new Vec3(1, 0, 0), new Vec3(0, -1, 0)},	//-x
					{new Vec3(-1, 0, 0), new Vec3(0, -1, 0)},	//+x
					{new Vec3(0, 1, 0), new Vec3(0, 0, 1)},		//-y
					{new Vec3(0, -1, 0), new Vec3(0, 0, -1)},	//+y
					{new Vec3(0, 0, 1), new Vec3(0, -1, 0)},	//-z
					{new Vec3(0, 0, -1), new Vec3(0, -1, 0)},	//+z
				};
				
				Camera cubemapCamera = new Camera((float) Math.toRadians(90), 1f, 1f, near, far);	//aspect ratio of 1
				cubemapCamera.setPos(light.pos);
				
				glViewport(0, 0, shadowCubemap.getSize(), shadowCubemap.getSize());
				glEnable(GL_DEPTH_TEST);
				glDisable(GL_BLEND);
				glClear(GL_DEPTH_BUFFER_BIT);
				
				//render each side of cubemap separately
				for(int j = 0; j < 6; j++) {
					cubemapCamera.setFacing(camVectors[j][0]);
					cubemapCamera.setUp(camVectors[j][1]);
					
					int face = GL_TEXTURE_CUBE_MAP_POSITIVE_X + j;
					shadowBuffer.bindTextureToBuffer(GL_DEPTH_ATTACHMENT, face, shadowCubemap.getID());
					shadowBuffer.bind();
					glClear(GL_DEPTH_BUFFER_BIT);
					world.renderDepth(Shader.CUBE_DEPTH, cubemapCamera);
				}
				
				//render lit scene
				lightingBuffer.bind();
				glViewport(0, 0, Main.windowWidth, Main.windowHeight);
				glDisable(GL_DEPTH_TEST);
				glEnable(GL_BLEND);
				glEnable(GL_CULL_FACE);
				
				Shader.LIGHTING.enable();
				Shader.LIGHTING.setUniform1f("shadowCubemapFar", far);
				
				shadowCubemap.bind(GL_TEXTURE4);
				
				lights.get(i).bind(Shader.LIGHTING, i);
				screenQuad.render();
			}
		}
		
		// -- SKYBOX -- : we'll use this texture in the post-processing step
		skyboxBuffer.bind();
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glDisable(GL_CULL_FACE);
		//glCullFace(GL_FRONT);  
		Shader.SKYBOX.enable();
		Shader.SKYBOX.setUniformMat4("vw_matrix", world.player.camera.getViewMatrix());
		World.skybox.bind(GL_TEXTURE0);
		skyboxCube.render();
				
		// -- POST PROCESSING -- : render contents of lighting buffer onto screen sized quad
		glBindFramebuffer(GL_FRAMEBUFFER, 0);	//back to default framebuffer
		glClear(GL_COLOR_BUFFER_BIT);
		glDisable(GL_DEPTH_TEST);
		glDisable(GL_BLEND);
		Shader.POST_PROCESS.enable();
		
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, lightingBuffer.getColorBuffer(GL_COLOR_ATTACHMENT0));
		glActiveTexture(GL_TEXTURE1);
		glBindTexture(GL_TEXTURE_2D, geometryBuffer.getColorBuffer(GL_COLOR_ATTACHMENT0));
		glActiveTexture(GL_TEXTURE2);
		glBindTexture(GL_TEXTURE_2D, skyboxBuffer.getColorBuffer(GL_COLOR_ATTACHMENT0));
		
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