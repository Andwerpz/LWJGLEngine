package screen;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import java.util.ArrayList;

import graphics.Cubemap;
import graphics.Framebuffer;
import graphics.Shader;
import graphics.Texture;
import main.Main;
import model.Model;
import player.Camera;
import scene.Light;
import scene.Scene;
import util.Mat4;
import util.Vec3;

public class PerspectiveScreen extends Screen {
	// renders the scene with a perspective projection matrix.

	private static final float NEAR = 0.1f;
	private static final float FAR = 200.0f;

	private int world_scene;
	private int decal_scene;

	private int playermodel_scene;
	private boolean renderPlayermodel = false;
	private float playermodelFOV;
	private Framebuffer playermodelBuffer;

	private int particle_scene;
	private boolean renderParticles = false;

	private static final int SHADOW_MAP_NR_CASCADES = 6;
	private static float[] shadowCascades = new float[] { NEAR, 1, 3, 7, 15, 30, FAR };

	private float worldFOV;

	private Framebuffer geometryBuffer;
	private Framebuffer lightingBuffer;
	private Framebuffer shadowBuffer;
	private Framebuffer skyboxBuffer;

	private Texture geometryPositionMap; // RGB: pos, A: normalized depth; 0 - 1
	private Texture geometryNormalMap; // RGB: normal
	private Texture geometrySpecularMap; // RGB: specular, A: shininess
	private Texture geometryColorMap; // RGB: color, A: alpha
	private Texture geometryColorIDMap; // RGB: colorID

	//TODO move shininess out of the alpha channel, and add emissiveness buffer so we can do full bright particles. 
	//actually, we can do full bright particles by just moving particle rendering after the lighting step. 

	private Texture lightingColorMap; // RGB: color
	private Texture lightingBrightnessMap; // R: brightness

	private Texture shadowDepthMap; // R: depth
	private Texture shadowBackfaceMap; // R: isBackface
	private Cubemap shadowCubemap; // R: depth

	private Texture skyboxColorMap; // RGB: color

	private SkyboxCube skyboxCube;
	private boolean renderSkybox = false;

	private boolean renderDecals = false;

	public PerspectiveScreen() {

	}

	@Override
	protected void _kill() {
		this.geometryBuffer.kill();
		this.lightingBuffer.kill();
		this.shadowBuffer.kill();
		this.skyboxBuffer.kill();
		this.playermodelBuffer.kill();
	}

	@Override
	public void buildBuffers() {
		this.skyboxCube = new SkyboxCube();

		this.geometryBuffer = new Framebuffer(Main.windowWidth, Main.windowHeight);
		this.geometryPositionMap = new Texture(GL_RGBA32F, Main.windowWidth, Main.windowHeight, GL_RGBA, GL_FLOAT);
		this.geometryNormalMap = new Texture(GL_RGBA32F, Main.windowWidth, Main.windowHeight, GL_RGBA, GL_FLOAT);
		this.geometrySpecularMap = new Texture(GL_RGBA32F, Main.windowWidth, Main.windowHeight, GL_RGBA, GL_FLOAT);
		this.geometryColorMap = new Texture(GL_RGBA, Main.windowWidth, Main.windowHeight, GL_RGBA, GL_FLOAT);
		this.geometryColorIDMap = new Texture(GL_RGBA, Main.windowWidth, Main.windowHeight, GL_RGBA, GL_FLOAT);
		this.geometryBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.geometryPositionMap.getID());
		this.geometryBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, this.geometryNormalMap.getID());
		this.geometryBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT2, GL_TEXTURE_2D, this.geometrySpecularMap.getID());
		this.geometryBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT3, GL_TEXTURE_2D, this.geometryColorMap.getID());
		this.geometryBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT4, GL_TEXTURE_2D, this.geometryColorIDMap.getID());
		this.geometryBuffer.addDepthBuffer();
		this.geometryBuffer.setDrawBuffers(new int[] { GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1, GL_COLOR_ATTACHMENT2, GL_COLOR_ATTACHMENT3, GL_COLOR_ATTACHMENT4 });
		this.geometryBuffer.isComplete();

		this.playermodelBuffer = new Framebuffer(Main.windowWidth, Main.windowHeight);
		this.playermodelBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.geometryPositionMap.getID());
		this.playermodelBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, this.geometryNormalMap.getID());
		this.playermodelBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT2, GL_TEXTURE_2D, this.geometrySpecularMap.getID());
		this.playermodelBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT3, GL_TEXTURE_2D, this.geometryColorMap.getID());
		this.playermodelBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT4, GL_TEXTURE_2D, this.geometryColorIDMap.getID());
		this.playermodelBuffer.addDepthBuffer();
		this.playermodelBuffer.setDrawBuffers(new int[] { GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1, GL_COLOR_ATTACHMENT2, GL_COLOR_ATTACHMENT3, GL_COLOR_ATTACHMENT4 });
		this.playermodelBuffer.addDepthBuffer();

		this.lightingBuffer = new Framebuffer(Main.windowWidth, Main.windowHeight);
		this.lightingColorMap = new Texture(GL_RGBA, Main.windowWidth, Main.windowHeight, GL_RGBA, GL_UNSIGNED_BYTE);
		this.lightingBrightnessMap = new Texture(GL_RGBA16F, Main.windowWidth, Main.windowHeight, GL_RGBA, GL_FLOAT);
		this.lightingBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.lightingColorMap.getID());
		this.lightingBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, this.lightingBrightnessMap.getID());
		this.lightingBuffer.setDrawBuffers(new int[] { GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1 });
		this.lightingBuffer.isComplete();

		this.shadowBuffer = new Framebuffer(Main.windowWidth, Main.windowHeight);
		this.shadowDepthMap = new Texture(GL_DEPTH_COMPONENT, Main.windowWidth, Main.windowHeight, GL_DEPTH_COMPONENT, GL_FLOAT);
		this.shadowBackfaceMap = new Texture(GL_RGBA16F, Main.windowWidth, Main.windowHeight, GL_RGBA, GL_FLOAT);
		this.shadowBuffer.bindTextureToBuffer(GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, this.shadowDepthMap.getID());
		this.shadowBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.shadowBackfaceMap.getID());
		this.shadowBuffer.setDrawBuffers(new int[] { GL_COLOR_ATTACHMENT0 });
		this.shadowBuffer.isComplete();
		this.shadowCubemap = new Cubemap(GL_DEPTH_COMPONENT, GL_DEPTH_COMPONENT, GL_FLOAT);

		this.skyboxBuffer = new Framebuffer(Main.windowWidth, Main.windowHeight);
		this.skyboxColorMap = new Texture(GL_RGBA, Main.windowWidth, Main.windowHeight, GL_RGBA, GL_FLOAT);
		this.skyboxBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.skyboxColorMap.getID());
		this.skyboxBuffer.setDrawBuffers(new int[] { GL_COLOR_ATTACHMENT0 });
		this.skyboxBuffer.isComplete();

		this.worldFOV = 90f;

		Vec3 cameraPos = new Vec3();
		Vec3 cameraFacing = new Vec3(0, 0, -1);

		if (this.camera != null) {
			cameraPos = this.camera.getPos();
			cameraFacing = this.camera.getFacing();
		}

		this.camera = new Camera((float) Math.toRadians(this.worldFOV), Main.windowWidth, Main.windowHeight, NEAR, FAR);
		this.camera.setPos(cameraPos);
		this.camera.setFacing(cameraFacing);

		this.playermodelFOV = 50f;
	}

	private void setCameraFOV(float degrees) {
		float cameraFOV = degrees;
		Vec3 cameraPos = this.camera.getPos();
		Vec3 cameraFacing = this.camera.getFacing();
		this.camera = new Camera((float) Math.toRadians(cameraFOV), Main.windowWidth, Main.windowHeight, NEAR, FAR);
		this.camera.setPos(cameraPos);
		this.camera.setFacing(cameraFacing);
	}

	public void setWorldCameraFOV(float degrees) {
		this.worldFOV = degrees;
	}

	public void setPlayermodelCameraFOV(float degrees) {
		this.playermodelFOV = degrees;
	}

	public void setWorldScene(int scene) {
		this.world_scene = scene;
	}

	public void setDecalScene(int scene) {
		this.decal_scene = scene;
	}

	public void setPlayermodelScene(int scene) {
		this.playermodel_scene = scene;
	}

	public void setParticleScene(int scene) {
		this.particle_scene = scene;
	}

	public void setShaderCameraUniforms(Shader shader, Camera camera) {
		shader.setUniformMat4("pr_matrix", camera.getProjectionMatrix());
		shader.setUniformMat4("vw_matrix", camera.getViewMatrix());
		shader.setUniform3f("view_pos", camera.getPos());
	}

	public void renderSkybox(boolean b) {
		this.renderSkybox = b;
	}

	public void renderDecals(boolean b) {
		this.renderDecals = b;
	}

	public void renderPlayermodel(boolean b) {
		this.renderPlayermodel = b;
	}

	public void renderParticles(boolean b) {
		this.renderParticles = b;
	}

	@Override
	public void render(Framebuffer outputBuffer) {
		// -- GEOMETRY -- : render 3d perspective to geometry buffer
		geometryBuffer.bind();
		glEnable(GL_DEPTH_TEST);
		glDepthFunc(GL_LESS);
		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);
		glPolygonMode(GL_FRONT, GL_FILL);
		glDisable(GL_BLEND);
		glClearDepth(1); // maximum value
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		Texture.bindingEnabled = true;

		Shader.GEOMETRY.enable();
		this.setCameraFOV(this.worldFOV);
		this.setShaderCameraUniforms(Shader.GEOMETRY, this.camera);
		Model.renderModels(this.world_scene);

		// -- DECALS -- : screen space decals
		//decals can be transparent, but they cannot have a shininess value greater than 0. 
		if (this.renderDecals) {
			geometryBuffer.bind();
			glEnable(GL_DEPTH_TEST);
			glDepthFunc(GL_LESS);
			glDepthMask(false); //disable writing to the depth buffer
			glEnable(GL_CULL_FACE);
			glCullFace(GL_BACK);
			glPolygonMode(GL_FRONT, GL_FILL);

			glEnable(GL_BLEND);
			//first two are for rgb, while last two are for alpha
			glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE);

			Shader.DECAL.enable();
			Shader.DECAL.setUniformMat4("pr_matrix", camera.getProjectionMatrix());
			Shader.DECAL.setUniformMat4("vw_matrix", camera.getViewMatrix());
			this.geometryPositionMap.bind(GL_TEXTURE4);
			Model.renderModels(this.decal_scene);

			glDisable(GL_BLEND);
		}

		// -- PLAYERMODEL -- : gun and hands
		if (this.renderPlayermodel) {
			//we clear the gDepth buffer in this step, should probably save it somewhere instead. 
			this.playermodelBuffer.bind();
			glEnable(GL_DEPTH_TEST);
			glDepthFunc(GL_LESS);
			glDepthMask(true);
			glEnable(GL_CULL_FACE);
			glCullFace(GL_BACK);
			glClearDepth(1); // maximum value
			glClear(GL_DEPTH_BUFFER_BIT);

			Shader.GEOMETRY.enable();
			this.setCameraFOV(this.playermodelFOV);
			this.setShaderCameraUniforms(Shader.GEOMETRY, this.camera);
			Model.renderModels(this.playermodel_scene);

			this.setCameraFOV(this.worldFOV);
		}

		// -- LIGHTING -- : using information from the geometry buffer, calculate lighting.
		lightingBuffer.bind();
		Shader.LIGHTING.enable();
		glClear(GL_COLOR_BUFFER_BIT);
		glDisable(GL_DEPTH_TEST);
		glDepthMask(true);
		glEnable(GL_BLEND);
		glPolygonMode(GL_FRONT, GL_FILL);
		glBlendFunc(GL_ONE, GL_ONE);

		// TODO split lighting shader into directional and cubemap lighting
		this.geometryPositionMap.bind(GL_TEXTURE0);
		this.geometryNormalMap.bind(GL_TEXTURE1);
		this.geometryColorMap.bind(GL_TEXTURE2);
		this.geometrySpecularMap.bind(GL_TEXTURE3);
		this.shadowDepthMap.bind(GL_TEXTURE4);
		this.shadowBackfaceMap.bind(GL_TEXTURE5);
		this.shadowCubemap.bind(GL_TEXTURE6);

		Shader.LIGHTING.setUniform3f("view_pos", this.camera.getPos());

		// disable to prevent overwriting geometry buffer textures
		// don't forget to re-enable
		Texture.bindingEnabled = false;

		// backfaces should also be able to cast shadows
		glDisable(GL_CULL_FACE);

		// calculate lighting with each light seperately
		ArrayList<Light> lights = Light.lights.get(this.world_scene);
		for (int i = 0; i < lights.size(); i++) {
			// generate depth map for light
			if (lights.get(i).type == Light.DIR_LIGHT) {

				Vec3 lightDir = new Vec3(lights.get(i).dir).normalize();
				Mat4 lightMat = Mat4.lookAt(new Vec3(0), lightDir, new Vec3(0, 1, 0));

				// re-bind directional depth map texture as depth map
				shadowBuffer.bindTextureToBuffer(GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, this.shadowDepthMap.getID());

				// do this for each cascade near / far plane
				for (int cascade = 0; cascade < PerspectiveScreen.SHADOW_MAP_NR_CASCADES; cascade++) {
					// calculate orthographic projection matrix
					// generate perspective frustum corners in camera space
					float near = shadowCascades[cascade];
					float far = shadowCascades[cascade + 1];
					float y1 = near * (float) Math.tan(Main.FOV / 2f);
					float y2 = far * (float) Math.tan(Main.FOV / 2f);
					float x1 = y1 * Main.ASPECT_RATIO;
					float x2 = y2 * Main.ASPECT_RATIO;
					Vec3[] corners = new Vec3[] { new Vec3(x1, y1, -near), new Vec3(-x1, y1, -near), new Vec3(-x1, -y1, -near), new Vec3(x1, -y1, -near),

							new Vec3(x2, y2, -far), new Vec3(-x2, y2, -far), new Vec3(-x2, -y2, -far), new Vec3(x2, -y2, -far), };

					//we have to normalize the near and far coordinates
					near = (1f / near - 1f / NEAR) / (1f / FAR - 1f / NEAR);
					far = (1f / far - 1f / NEAR) / (1f / FAR - 1f / NEAR);
					Shader.LIGHTING.setUniform1f("shadowMapNear", near);
					Shader.LIGHTING.setUniform1f("shadowMapFar", far);

					// transform frustum corners from camera space to light space
					Mat4 transformMatrix = new Mat4(this.camera.getInvViewMatrix()); // from camera to world space
					transformMatrix.muli(lightMat); // apply rotation to align light dir with -z axis
					for (int j = 0; j < corners.length; j++) {
						corners[j] = transformMatrix.mul(corners[j], 1f);
					}

					// generate the AABB that bounds the corners in light space
					float left = corners[0].x;
					float right = corners[0].x;
					float bottom = corners[0].y;
					float top = corners[0].y;
					near = corners[0].z;
					far = corners[0].z;

					for (Vec3 v : corners) {
						left = Math.min(left, v.x);
						right = Math.max(right, v.x);
						bottom = Math.min(bottom, v.y);
						top = Math.max(top, v.y);
						near = Math.min(near, v.z);
						far = Math.max(far, v.z);
					}

					// construct orthographic projection matrix
					Camera lightCamera = new Camera(left, right, bottom, top, near - 100f, far + 100f);
					lightCamera.setFacing(lightDir);

					// render shadow map
					shadowBuffer.bind();
					glViewport(0, 0, Main.windowWidth, Main.windowHeight);
					glEnable(GL_DEPTH_TEST);
					glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
					glDisable(GL_BLEND);
					glEnable(GL_CULL_FACE);
					glCullFace(GL_FRONT);

					Shader.LIGHTING.setUniformMat4("lightSpace_matrix", lightMat.mul(lightCamera.getProjectionMatrix()));
					Shader.DEPTH.enable();

					this.setShaderCameraUniforms(Shader.DEPTH, lightCamera);
					Model.renderModels(this.world_scene);

					// render portion of lit scene
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

				// generate cubemap
				shadowBuffer.bind();
				Shader.CUBE_DEPTH.enable();
				float near = 0.1f;
				float far = 50f;

				Shader.CUBE_DEPTH.setUniform1f("far", far);

				Vec3[][] camVectors = new Vec3[][] { { new Vec3(1, 0, 0), new Vec3(0, -1, 0) }, // -x
						{ new Vec3(-1, 0, 0), new Vec3(0, -1, 0) }, // +x
						{ new Vec3(0, 1, 0), new Vec3(0, 0, 1) }, // -y
						{ new Vec3(0, -1, 0), new Vec3(0, 0, -1) }, // +y
						{ new Vec3(0, 0, 1), new Vec3(0, -1, 0) }, // -z
						{ new Vec3(0, 0, -1), new Vec3(0, -1, 0) }, // +z
				};

				Camera cubemapCamera = new Camera((float) Math.toRadians(90), 1f, 1f, near, far); // aspect ratio of 1
				cubemapCamera.setPos(light.pos);

				glViewport(0, 0, shadowCubemap.getSize(), shadowCubemap.getSize());
				glEnable(GL_DEPTH_TEST);
				glDisable(GL_BLEND);
				glClear(GL_DEPTH_BUFFER_BIT);

				// render each side of cubemap separately
				for (int j = 0; j < 6; j++) {
					cubemapCamera.setFacing(camVectors[j][0]);
					cubemapCamera.setUp(camVectors[j][1]);

					int face = GL_TEXTURE_CUBE_MAP_POSITIVE_X + j;
					shadowBuffer.bindTextureToBuffer(GL_DEPTH_ATTACHMENT, face, shadowCubemap.getID());
					shadowBuffer.bind();
					glClear(GL_DEPTH_BUFFER_BIT);
					// world.render(Shader.CUBE_DEPTH, cubemapCamera);
					this.setShaderCameraUniforms(Shader.CUBE_DEPTH, cubemapCamera);
					Shader.CUBE_DEPTH.enable();
					Model.renderModels(this.world_scene);
				}

				// render lit scene
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

		// -- PARTICLES -- : front facing rectangular billboards
		//this renders after lighting, so we can do transparency.
		if (this.renderParticles) {
			lightingBuffer.bind();
			glEnable(GL_DEPTH_TEST);
			glDepthFunc(GL_LESS);
			glEnable(GL_CULL_FACE);
			glCullFace(GL_BACK);
			glPolygonMode(GL_FRONT, GL_FILL);
			glEnable(GL_BLEND);
			glBlendFunc(GL_ONE_MINUS_SRC_ALPHA, GL_ONE);

			this.geometryPositionMap.bind(GL_TEXTURE4);

			Shader.PARTICLE.enable();
			this.setCameraFOV(this.worldFOV);
			this.setShaderCameraUniforms(Shader.PARTICLE, this.camera);
			Model.renderModels(this.particle_scene);
		}

		// -- SKYBOX -- : we'll use this texture in the post-processing step
		if (this.renderSkybox) {
			skyboxBuffer.bind();
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			glDisable(GL_CULL_FACE);
			glDisable(GL_BLEND);
			Shader.SKYBOX.enable();
			Shader.SKYBOX.setUniformMat4("vw_matrix", this.camera.getViewMatrix());
			Shader.SKYBOX.setUniformMat4("pr_matrix", this.camera.getProjectionMatrix());
			Scene.skyboxes.get(this.world_scene).bind(GL_TEXTURE0);
			skyboxCube.render();
		}

		outputBuffer.bind();
		glDisable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
		glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
		Shader.SPLASH.enable();
		Shader.SPLASH.setUniform1f("alpha", 1f);

		if (this.renderSkybox) {
			this.skyboxColorMap.bind(GL_TEXTURE0);
			screenQuad.render();
		}

		this.lightingColorMap.bind(GL_TEXTURE0);
		//this.geometryPositionMap.bind(GL_TEXTURE0);
		screenQuad.render();
	}

}
