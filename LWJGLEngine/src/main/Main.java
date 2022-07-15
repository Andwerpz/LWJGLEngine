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
	
	private boolean depthDebugMode = false;
	private int depthDebugToggleDelay = 15;
	private int depthDebugToggleDelayCounter = 0;
	
	private VertexArray perspectiveFrustum;
	private boolean resamplePerspectiveFrustum = false;
	
	private VertexArray orthographicFrustum;
	private boolean resampleOrthographicFrustum = false;
	
	private static final float NEAR = 0.1f;
	private static final float FAR = 200.0f;
	private static final float ASPECT_RATIO = (float) windowWidth / (float) windowHeight;
	private static final float FOV = (float) Math.toRadians(90f);	//vertical FOV
	
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
		this.lightingBuffer.addRenderBuffer();
		this.lightingBuffer.setDrawBuffers(new int[] {GL_COLOR_ATTACHMENT0});
		this.lightingBuffer.isComplete();
		
		this.shadowBuffer = new Framebuffer(Main.windowWidth, Main.windowHeight);
		this.shadowBuffer.addRenderBuffer();
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
		
		Shader.GEOMETRY.setUniformMat4("pr_matrix", pr_matrix);
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
		
		if(KeyboardInput.isKeyPressed(GLFW_KEY_B) && depthDebugToggleDelayCounter >= depthDebugToggleDelay) {
			depthDebugMode = !depthDebugMode;
			depthDebugToggleDelayCounter = 0;
		}
		depthDebugToggleDelayCounter ++;
		
		resamplePerspectiveFrustum = false;
		if(KeyboardInput.isKeyPressed(GLFW_KEY_X)) {
			resamplePerspectiveFrustum = true;
		}
		
		resampleOrthographicFrustum = false;
		if(KeyboardInput.isKeyPressed(GLFW_KEY_Z)) {
			resampleOrthographicFrustum = true;
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
		world.renderGeometry(world.player.camera);
		if(perspectiveFrustum != null) {
			perspectiveFrustum.render();
		}
		
		if(orthographicFrustum != null) {
			orthographicFrustum.render();
		}
		
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
		Shader.LIGHTING.setUniform3f("view_pos", this.world.player.camera.pos);
		
		//calculate lighting with each light seperately
		ArrayList<Light> lights = World.lights;
		for(int i = 0; i < lights.size(); i++) {
			//generate depth map for light
			if(lights.get(i).type == Light.DIR_LIGHT) {
				Vec3 dir = new Vec3(lights.get(i).dir).normalize();
				Vec3 eye = new Vec3(0);
				Mat4 lightMat = Mat4.lookAt(eye, dir, new Vec3(0, 1, 0));
				
				//re-bind directional depth map texture as depth map
				shadowBuffer.bind();
				glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, shadowDepthMapID, 0);
				
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
					
					float[] vertices = new float[8 * 3];
					byte[] indices = new byte[] {
						0, 1, 2,
						0, 2, 3,
						4, 5, 6,
						4, 6, 7
					};
					
					float[] tex = new float[] {
						1, 1,
						0, 1,
						0, 0,
						1, 0,
						
						1, 1,
						0, 1,
						0, 0,
						1, 0,
					};
					
					//transform frustum corners from camera space to light space
					Mat4 transformMatrix = new Mat4(world.player.camera.getInvViewMatrix());	//from camera to world space
					transformMatrix.muli(lightMat);	//apply rotation to align light dir with -z axis
					for(int j = 0; j < corners.length; j++) {
						corners[j] = world.player.camera.getInvViewMatrix().mul(corners[j], 1f);
						vertices[j * 3 + 0] = corners[j].x;
						vertices[j * 3 + 1] = corners[j].y;
						vertices[j * 3 + 2] = corners[j].z;
						
						corners[j] = lightMat.mul(corners[j], 1f);
					}
					
					if(resamplePerspectiveFrustum) {
						perspectiveFrustum = new VertexArray(vertices, indices, tex, GL_TRIANGLES);
						perspectiveFrustum.updateModelMats(new Mat4[] {Mat4.identity()});
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
					
					Mat4 invLightMat = new Mat4(lightMat).transpose();
					vertices = new float[] {
						left, bottom, near,
						right, bottom, near,
						right, top, near,
						left, top, near,
						
						left, bottom, far,
						right, bottom, far,
						right, top, far,
						left, top, far,
					};
					
					for(int j = 0; j < vertices.length; j += 3) {
						Vec3 vec = new Vec3(vertices[j], vertices[j + 1], vertices[j + 2]);
						vec = invLightMat.mul(vec, 1.0f);
						vertices[j] = vec.x;
						vertices[j + 1] = vec.y;
						vertices[j + 2] = vec.z;
					}
					
					if(resampleOrthographicFrustum) {
						orthographicFrustum = new VertexArray(vertices, indices, tex, GL_TRIANGLES);
						orthographicFrustum.updateModelMats(new Mat4[] {Mat4.identity()});
					}
					
					//construct orthographic projection matrix
					Mat4 projectionMat = Mat4.orthographic(left, right, bottom, top, near - 20f, far + 20f);
					
					//render shadow map
					shadowBuffer.bind();
					glEnable(GL_DEPTH_TEST);
					glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
					glDisable(GL_BLEND);
					
					Shader.LIGHTING.setUniformMat4("lightSpace_matrix", lightMat.mul(projectionMat));
					Shader.DEPTH.enable();
					world.renderDepth(projectionMat, lightMat, Shader.DEPTH);
					
					//render lit scene
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
				Vec3 pos = light.pos;
				
				//generate cubemap
				shadowBuffer.bind();
				Shader.CUBE_DEPTH.enable();
				float near = 0.1f;
				float far = 50f;
				Mat4 pr_matrix = Mat4.perspective((float) Math.toRadians(90), 1024f, 1024f, near, far);	//aspect ratio of 1
				
				Shader.CUBE_DEPTH.setUniform1f("far", far);
				
				Mat4[] vw_matrix = new Mat4[] {
					Mat4.lookAt(pos, pos.add(new Vec3(1, 0, 0)), new Vec3(0, -1, 0)),
					Mat4.lookAt(pos, pos.add(new Vec3(-1, 0, 0)), new Vec3(0, -1, 0)),
					Mat4.lookAt(pos, pos.add(new Vec3(0, 1, 0)), new Vec3(0, 0, 1)),
					Mat4.lookAt(pos, pos.add(new Vec3(0, -1, 0)), new Vec3(0, 0, -1)),
					Mat4.lookAt(pos, pos.add(new Vec3(0, 0, 1)), new Vec3(0, -1, 0)),
					Mat4.lookAt(pos, pos.add(new Vec3(0, 0, -1)), new Vec3(0, -1, 0)),
				};
				
				glViewport(0, 0, shadowCubemap.getSize(), shadowCubemap.getSize());
				glEnable(GL_DEPTH_TEST);
				glDisable(GL_BLEND);
				glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
				
				for(int j = 0; j < 6; j++) {
					int face = GL_TEXTURE_CUBE_MAP_POSITIVE_X + j;
					//glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, face, shadowCubemap.getID(), 0);
					glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, face, shadowCubemap.getID(), 0);
					glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
					world.renderDepth(pr_matrix, vw_matrix[j], Shader.CUBE_DEPTH);
				}
				//glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, 0, 0);	//make sure to unbind
				
				//render lit scene
				lightingBuffer.bind();
				glViewport(0, 0, Main.windowWidth, Main.windowHeight);
				glDisable(GL_DEPTH_TEST);
				glEnable(GL_BLEND);
				glEnable(GL_CULL_FACE);
				Shader.LIGHTING.enable();
				
				Shader.LIGHTING.setUniform1f("shadowCubemapFar", far);
				
				glActiveTexture(GL_TEXTURE4);
				glBindTexture(GL_TEXTURE_CUBE_MAP, shadowCubemap.getID());
				
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
		
		if(depthDebugMode) {
			glBindTexture(GL_TEXTURE_2D, testTextureID);
		}
		else {
			glBindTexture(GL_TEXTURE_2D, lightingBuffer.getColorBuffer(GL_COLOR_ATTACHMENT0));
		}
		
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