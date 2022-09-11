package screen;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import graphics.Cubemap;
import graphics.Framebuffer;
import graphics.Shader;
import graphics.Texture;
import input.MouseInput;
import main.Main;
import model.Model;
import model.ScreenQuad;
import model.SkyboxCube;
import player.Camera;
import scene.Light;
import scene.Scene;
import util.BufferUtils;
import util.Mat4;
import util.Vec2;
import util.Vec3;

public class PerspectiveScreen extends Screen {
	//renders the scene with a perspective projection matrix. 
	
	private static final int SHADOW_MAP_NR_CASCADES = 6;
	private static float[] shadowCascades = new float[] {Main.NEAR, 1, 3, 7, 15, 30, Main.FAR};
	
	private Framebuffer geometryBuffer;
	private Framebuffer lightingBuffer;
	private Framebuffer shadowBuffer;
	private Framebuffer skyboxBuffer;
	
	private Texture geometryPositionMap;	//RGB: pos, A: depth
	private Texture geometryNormalMap;		//RGB: normal, A: specular
	private Texture geometryColorMap;		//RGB: color, A: alpha
	private Texture geometryColorIDMap;		//RGB: colorID
	
	private Texture lightingColorMap;		//RGB: color
	private Texture lightingBrightnessMap;	//R: brightness
	
	private Texture shadowDepthMap;			//R: depth
	private Texture shadowBackfaceMap;		//R: isBackface
	private Cubemap shadowCubemap;			//R: depth
	
	private Texture skyboxColorMap;			//RGB: color
	
	private SkyboxCube skyboxCube;
	
	public PerspectiveScreen() {
		super();
		
		this.skyboxCube = new SkyboxCube();
		
		this.geometryBuffer = new Framebuffer(Main.windowWidth, Main.windowHeight);
		this.geometryPositionMap = new Texture(GL_RGBA16F, Main.windowWidth, Main.windowHeight, GL_RGBA, GL_FLOAT);
		this.geometryNormalMap = new Texture(GL_RGBA16F, Main.windowWidth, Main.windowHeight, GL_RGBA, GL_FLOAT);
		this.geometryColorMap = new Texture(GL_RGBA, Main.windowWidth, Main.windowHeight, GL_RGBA, GL_FLOAT);
		this.geometryColorIDMap = new Texture(GL_RGBA, Main.windowWidth, Main.windowHeight, GL_RGBA, GL_FLOAT);
		this.geometryBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.geometryPositionMap.getID());
		this.geometryBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, this.geometryNormalMap.getID());
		this.geometryBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT2, GL_TEXTURE_2D, this.geometryColorMap.getID());
		this.geometryBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT3, GL_TEXTURE_2D, this.geometryColorIDMap.getID());
		this.geometryBuffer.addDepthBuffer();
		this.geometryBuffer.setDrawBuffers(new int[] {GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1, GL_COLOR_ATTACHMENT2, GL_COLOR_ATTACHMENT3});
		this.geometryBuffer.isComplete();
		
		this.lightingBuffer = new Framebuffer(Main.windowWidth, Main.windowHeight);
		this.lightingColorMap = new Texture(GL_RGB, Main.windowWidth, Main.windowHeight, GL_RGB, GL_UNSIGNED_BYTE);
		this.lightingBrightnessMap = new Texture(GL_RGBA16F, Main.windowWidth, Main.windowHeight, GL_RGBA, GL_FLOAT);
		this.lightingBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.lightingColorMap.getID());
		this.lightingBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, this.lightingBrightnessMap.getID());
		this.lightingBuffer.setDrawBuffers(new int[] {GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1});
		this.lightingBuffer.isComplete();
		
		this.shadowBuffer = new Framebuffer(Main.windowWidth, Main.windowHeight);
		this.shadowDepthMap = new Texture(GL_DEPTH_COMPONENT, Main.windowWidth, Main.windowHeight, GL_DEPTH_COMPONENT, GL_FLOAT);
		this.shadowBackfaceMap = new Texture(GL_RGBA16F, Main.windowWidth, Main.windowHeight, GL_RGBA, GL_FLOAT);
		this.shadowBuffer.bindTextureToBuffer(GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, this.shadowDepthMap.getID());
		this.shadowBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.shadowBackfaceMap.getID());
		this.shadowBuffer.setDrawBuffers(new int[] {GL_COLOR_ATTACHMENT0});
		this.shadowBuffer.isComplete();
		this.shadowCubemap = new Cubemap(GL_DEPTH_COMPONENT, GL_DEPTH_COMPONENT, GL_FLOAT);
		
		this.skyboxBuffer = new Framebuffer(Main.windowWidth, Main.windowHeight);
		this.skyboxColorMap = new Texture(GL_RGBA, Main.windowWidth, Main.windowHeight, GL_RGBA, GL_FLOAT);
		this.skyboxBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.skyboxColorMap.getID());
		this.skyboxBuffer.setDrawBuffers(new int[] {GL_COLOR_ATTACHMENT0});
		this.skyboxBuffer.isComplete();
	}
	
	public void setShaderUniforms(Shader shader, Camera camera) {
		shader.setUniformMat4("pr_matrix", camera.getProjectionMatrix());
		shader.setUniformMat4("vw_matrix", camera.getViewMatrix());
		shader.setUniform3f("view_pos", camera.getPos());
	}
	
	public void render(Framebuffer outputBuffer, int scene) {
		// -- GEOMETRY -- : render 3d perspective to geometry buffer
		geometryBuffer.bind();
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_CULL_FACE);
		glPolygonMode(GL_FRONT, GL_FILL);
		glDisable(GL_BLEND);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		
		Shader.GEOMETRY.enable();
		this.setShaderUniforms(Shader.GEOMETRY, this.camera);
		Model.renderModels(scene);
		
		// -- LIGHTING -- : using information from the geometry buffer, calculate lighting.
		lightingBuffer.bind();
		Shader.LIGHTING.enable();
		glClear(GL_COLOR_BUFFER_BIT);
		glDisable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
		glPolygonMode(GL_FRONT, GL_FILL);
		glBlendFunc(GL_ONE, GL_ONE);
		
		this.geometryPositionMap.bind(GL_TEXTURE0);
		this.geometryNormalMap.bind(GL_TEXTURE1);
		this.geometryColorMap.bind(GL_TEXTURE2);
		this.shadowDepthMap.bind(GL_TEXTURE3);
		this.shadowBackfaceMap.bind(GL_TEXTURE4);
		this.shadowCubemap.bind(GL_TEXTURE5);
		
		Shader.LIGHTING.setUniform3f("view_pos", this.camera.getPos());
		
		//disable to prevent overwriting geometry buffer textures
		//don't forget to re-enable
		Texture.bindingEnabled = false;
		
		//backfaces should also be able to cast shadows
		glDisable(GL_CULL_FACE);
		
		//calculate lighting with each light seperately
		ArrayList<Light> lights = Light.lights.get(scene);
		for(int i = 0; i < lights.size(); i++) {
			//generate depth map for light
			if(lights.get(i).type == Light.DIR_LIGHT) {
				
				Vec3 lightDir = new Vec3(lights.get(i).dir).normalize();
				Mat4 lightMat = Mat4.lookAt(new Vec3(0), lightDir, new Vec3(0, 1, 0));
				
				//re-bind directional depth map texture as depth map
				shadowBuffer.bindTextureToBuffer(GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, this.shadowDepthMap.getID());
				
				//do this for each cascade near / far plane
				for(int cascade = 0; cascade < PerspectiveScreen.SHADOW_MAP_NR_CASCADES; cascade++) {
					//calculate orthographic projection matrix
					//generate perspective frustum corners in camera space
					float near = shadowCascades[cascade];
					float far = shadowCascades[cascade + 1];
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
					Mat4 transformMatrix = new Mat4(this.camera.getInvViewMatrix());	//from camera to world space
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
					Camera lightCamera = new Camera(left, right, bottom, top, near - 100f, far + 100f);
					lightCamera.setFacing(lightDir);
					
					//render shadow map
					shadowBuffer.bind();
					glViewport(0, 0, Main.windowWidth, Main.windowHeight);
					glEnable(GL_DEPTH_TEST);
					glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
					glDisable(GL_BLEND);
					glEnable(GL_CULL_FACE);
					glCullFace(GL_FRONT);
					
					Shader.LIGHTING.setUniformMat4("lightSpace_matrix", lightMat.mul(lightCamera.getProjectionMatrix()));
					Shader.DEPTH.enable();
					//world.render(Shader.DEPTH, lightCamera);
					this.setShaderUniforms(Shader.DEPTH, lightCamera);
					Model.renderModels(scene);
					
					//render portion of lit scene
					lightingBuffer.bind();
					glDisable(GL_DEPTH_TEST);
					glEnable(GL_BLEND);
					glDisable(GL_CULL_FACE);
					glCullFace(GL_BACK);
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
					//world.render(Shader.CUBE_DEPTH, cubemapCamera);
					this.setShaderUniforms(Shader.CUBE_DEPTH, cubemapCamera);
					Shader.CUBE_DEPTH.enable();
					Model.renderModels(scene);
				}
				
				//render lit scene
				lightingBuffer.bind();
				glViewport(0, 0, Main.windowWidth, Main.windowHeight);
				glDisable(GL_DEPTH_TEST);
				glEnable(GL_BLEND);
				
				Shader.LIGHTING.enable();
				Shader.LIGHTING.setUniform1f("shadowCubemapFar", far);
				
				lights.get(i).bind(Shader.LIGHTING, i);
				screenQuad.render();
			}
		}
		
		Texture.bindingEnabled = true;
		
		// -- SKYBOX -- : we'll use this texture in the post-processing step
		skyboxBuffer.bind();
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glDisable(GL_CULL_FACE);
		Shader.SKYBOX.enable();
		Shader.SKYBOX.setUniformMat4("vw_matrix", this.camera.getViewMatrix());
		Shader.SKYBOX.setUniformMat4("pr_matrix", this.camera.getProjectionMatrix());
		Scene.skyboxes.get(scene).bind(GL_TEXTURE0);
		skyboxCube.render();
				
		// -- POST PROCESSING -- : render contents of lighting buffer onto screen sized quad
		outputBuffer.bind();
		glDisable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA); 
		Shader.GEOM_POST_PROCESS.enable();
		
		this.lightingColorMap.bind(GL_TEXTURE0);
		this.geometryPositionMap.bind(GL_TEXTURE1);
		this.skyboxColorMap.bind(GL_TEXTURE2);
		
		screenQuad.render();	
	}

}