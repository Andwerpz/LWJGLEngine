package lwjglengine.screen;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import lwjglengine.graphics.Cubemap;
import lwjglengine.graphics.Framebuffer;
import lwjglengine.graphics.Shader;
import lwjglengine.graphics.Texture;
import lwjglengine.model.Model;
import lwjglengine.player.Camera;
import lwjglengine.scene.Light;
import lwjglengine.scene.Scene;
import myutils.math.Mat4;
import myutils.math.Vec3;

public class PerspectiveScreen extends Screen {
	// renders the scene with a perspective projection matrix.
	// uses traditional blinn-phong shading. 

	//TODO move shininess out of the alpha channel, and add emissiveness buffer so we can do full bright particles. 
	//actually, we can do full bright particles by just moving particle rendering after the lighting step. 

	//TODO : 
	// - haven't tested this with premultiplied alpha. Probably will need to fix decal and particle blend mode.
	//   - ok, fixed premultiplied alpha with lighting, but still need to test decal and particles. 
	// - give option to precompute point light shadow cubemaps so we don't need to re-compute them every frame. 

	private static final float NEAR = 0.1f;
	private static final float FAR = 400.0f;

	private int[] world_scenes;
	private int decal_scene;

	private int playermodel_scene;
	private boolean renderPlayermodel = false;
	private Framebuffer playermodelBuffer;

	private int particle_scene;
	private boolean renderParticles = false;

	private static final int SHADOW_MAP_NR_CASCADES = 7;
	private static float[] shadowCascades = new float[] { NEAR, 1, 3, 7, 15, 30, 100, FAR };

	private float playermodelFOV = 50f;
	private float worldFOV = 90f;

	private Framebuffer geometryBuffer;
	private Framebuffer lightingBuffer;
	private Framebuffer shadowCascadeBuffer;
	private Framebuffer shadowCubemapBuffer;
	private Framebuffer skyboxBuffer;

	public Texture geometryPositionMap; // RGB: pos, A: normalized depth; 0 - 1
	public Texture geometryNormalMap; // RGB: normal
	public Texture geometrySpecularMap; // RGB: specular, A: shininess
	public Texture geometryColorMap; // RGB: color, A: alpha
	public Texture geometryColorIDMap; // RGB: colorID

	public Texture lightingColorMap; // RGB: color
	public Texture lightingBrightnessMap; // R: brightness

	private Texture shadowDepthMap; // R: depth
	private Texture shadowBackfaceMap; // R: isBackface

	private Cubemap shadowCubemap; // R: depth

	public Texture skyboxColorMap; // RGB: color

	private boolean renderSkybox = false;

	private boolean renderDecals = false;

	public PerspectiveScreen() {
		Vec3 cameraPos = new Vec3();
		Vec3 cameraFacing = new Vec3(0, 0, -1);

		this.camera = new Camera((float) Math.toRadians(this.worldFOV), this.screenWidth, this.screenHeight, NEAR, FAR);
		this.camera.setPos(cameraPos);
		this.camera.setFacing(cameraFacing);
	}

	@Override
	protected void _kill() {
		this.geometryBuffer.kill();
		this.lightingBuffer.kill();
		this.shadowCascadeBuffer.kill();
		this.shadowCubemapBuffer.kill();
		this.skyboxBuffer.kill();
		this.playermodelBuffer.kill();
	}

	@Override
	public void buildBuffers() {
		if (this.geometryBuffer != null) {
			this.geometryBuffer.kill();
		}
		if (this.playermodelBuffer != null) {
			this.playermodelBuffer.kill();
		}
		if (this.lightingBuffer != null) {
			this.lightingBuffer.kill();
		}
		if (this.skyboxBuffer != null) {
			this.skyboxBuffer.kill();
		}
		if (this.shadowCascadeBuffer != null) {
			this.shadowCascadeBuffer.kill();
		}
		if (this.shadowCubemapBuffer != null) {
			this.shadowCubemapBuffer.kill();
		}

		this.geometryBuffer = new Framebuffer(this.screenWidth, this.screenHeight);
		this.geometryPositionMap = new Texture(this.screenWidth, this.screenHeight, GL_RGBA32F, GL_RGBA, GL_FLOAT);
		this.geometryNormalMap = new Texture(this.screenWidth, this.screenHeight, GL_RGBA32F, GL_RGBA, GL_FLOAT);
		this.geometrySpecularMap = new Texture(this.screenWidth, this.screenHeight, GL_RGBA32F, GL_RGBA, GL_FLOAT);
		this.geometryColorMap = new Texture(this.screenWidth, this.screenHeight, GL_RGBA32F, GL_RGBA, GL_FLOAT);
		this.geometryColorIDMap = new Texture(this.screenWidth, this.screenHeight, GL_RGBA8, GL_RGBA, GL_FLOAT);
		this.geometryBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.geometryPositionMap.getID());
		this.geometryBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, this.geometryNormalMap.getID());
		this.geometryBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT2, GL_TEXTURE_2D, this.geometrySpecularMap.getID());
		this.geometryBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT3, GL_TEXTURE_2D, this.geometryColorMap.getID());
		this.geometryBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT4, GL_TEXTURE_2D, this.geometryColorIDMap.getID());
		this.geometryBuffer.addDepthBuffer();
		this.geometryBuffer.setDrawBuffers(new int[] { GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1, GL_COLOR_ATTACHMENT2, GL_COLOR_ATTACHMENT3, GL_COLOR_ATTACHMENT4 });
		this.geometryBuffer.isComplete();

		this.playermodelBuffer = new Framebuffer(this.screenWidth, this.screenHeight);
		this.playermodelBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.geometryPositionMap.getID());
		this.playermodelBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, this.geometryNormalMap.getID());
		this.playermodelBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT2, GL_TEXTURE_2D, this.geometrySpecularMap.getID());
		this.playermodelBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT3, GL_TEXTURE_2D, this.geometryColorMap.getID());
		this.playermodelBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT4, GL_TEXTURE_2D, this.geometryColorIDMap.getID());
		this.playermodelBuffer.addDepthBuffer();
		this.playermodelBuffer.setDrawBuffers(new int[] { GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1, GL_COLOR_ATTACHMENT2, GL_COLOR_ATTACHMENT3, GL_COLOR_ATTACHMENT4 });
		this.playermodelBuffer.isComplete();

		this.lightingBuffer = new Framebuffer(this.screenWidth, this.screenHeight);
		this.lightingColorMap = new Texture(this.screenWidth, this.screenHeight, GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE); //RGBA8 because RGBA32 will let alpha overflow to over 1
		this.lightingBrightnessMap = new Texture(this.screenWidth, this.screenHeight, GL_RGBA8, GL_RGBA, GL_FLOAT);
		this.lightingBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.lightingColorMap.getID());
		this.lightingBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, this.lightingBrightnessMap.getID());
		this.lightingBuffer.setDrawBuffers(new int[] { GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1 });
		this.lightingBuffer.isComplete();

		this.shadowCascadeBuffer = new Framebuffer(this.screenWidth, this.screenHeight);
		this.shadowDepthMap = new Texture(this.screenWidth, this.screenHeight, GL_DEPTH_COMPONENT32F, GL_DEPTH_COMPONENT, GL_FLOAT);
		this.shadowBackfaceMap = new Texture(this.screenWidth, this.screenHeight, GL_RGBA32F, GL_RGBA, GL_FLOAT);
		this.shadowCascadeBuffer.bindTextureToBuffer(GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, this.shadowDepthMap.getID());
		this.shadowCascadeBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.shadowBackfaceMap.getID());
		this.shadowCascadeBuffer.setDrawBuffers(new int[] { GL_COLOR_ATTACHMENT0 });
		this.shadowCascadeBuffer.isComplete();

		int shadow_cubemap_resolution = 1024;
		this.shadowCubemapBuffer = new Framebuffer(shadow_cubemap_resolution, shadow_cubemap_resolution);
		this.shadowCubemap = new Cubemap(GL_DEPTH_COMPONENT, GL_DEPTH_COMPONENT, GL_FLOAT, shadow_cubemap_resolution);

		this.skyboxBuffer = new Framebuffer(this.screenWidth, this.screenHeight);
		this.skyboxColorMap = new Texture(this.screenWidth, this.screenHeight, GL_RGBA32F, GL_RGBA, GL_FLOAT);
		this.skyboxBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.skyboxColorMap.getID());
		this.skyboxBuffer.setDrawBuffers(new int[] { GL_COLOR_ATTACHMENT0 });
		this.skyboxBuffer.isComplete();
	}

	private void setCameraFOV(float degrees) {
		float cameraFOV = degrees;
		Vec3 cameraPos = this.camera.getPos();
		Vec3 cameraFacing = this.camera.getFacing();
		this.camera = new Camera((float) Math.toRadians(cameraFOV), this.screenWidth, this.screenHeight, NEAR, FAR);
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
		this.world_scenes = new int[] { scene };
	}

	public void setWorldScenes(int[] scenes) {
		this.world_scenes = scenes;
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
	protected void _render(Framebuffer outputBuffer) {
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
		for (int scene : this.world_scenes) {
			Model.renderModels(scene);
		}

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
			this.geometryPositionMap.bind(GL_TEXTURE5);
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
		ArrayList<Light> lights = Light.lights.get(this.world_scenes[0]);
		if (lights == null || lights.size() == 0) {
			System.err.println("PerspectiveScreen : Must have at least 1 light in world scene to render");
			return;
		}
		for (int i = 0; i < lights.size(); i++) {
			// generate depth map for light
			if (lights.get(i).type == Light.DIR_LIGHT) {

				Vec3 lightDir = new Vec3(lights.get(i).dir).normalize();
				Mat4 lightMat = Mat4.lookAt(new Vec3(0), lightDir, new Vec3(0, 1, 0));

				// re-bind directional depth map texture as depth map
				shadowCascadeBuffer.bindTextureToBuffer(GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, this.shadowDepthMap.getID());

				// do this for each cascade near / far plane
				for (int cascade = 0; cascade < PerspectiveScreen.SHADOW_MAP_NR_CASCADES; cascade++) {
					// calculate orthographic projection matrix
					// generate perspective frustum corners in camera space
					float near = shadowCascades[cascade];
					float far = shadowCascades[cascade + 1];
					float y1 = near * (float) Math.tan(this.camera.getVerticalFOV() / 2f);
					float y2 = far * (float) Math.tan(this.camera.getVerticalFOV() / 2f);

					float aspectRatio = (float) this.screenWidth / (float) this.screenHeight;
					float x1 = y1 * aspectRatio;
					float x2 = y2 * aspectRatio;
					Vec3[] corners = new Vec3[] { new Vec3(x1, y1, -near), new Vec3(-x1, y1, -near), new Vec3(-x1, -y1, -near), new Vec3(x1, -y1, -near), new Vec3(x2, y2, -far), new Vec3(-x2, y2, -far), new Vec3(-x2, -y2, -far), new Vec3(x2, -y2, -far), };

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
					//it's important that all geometry is captured inside this orthographic frustum, so we have to consider
					//stuff that's behind the camera as well. 
					float diff = FAR - NEAR;
					Camera lightCamera = new Camera(left, right, bottom, top, near - diff, far + diff);
					lightCamera.setFacing(lightDir);

					// render shadow map
					shadowCascadeBuffer.bind();
					glViewport(0, 0, this.screenWidth, this.screenHeight);
					glEnable(GL_DEPTH_TEST);
					glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
					glDisable(GL_BLEND);
					glEnable(GL_CULL_FACE);
					glCullFace(GL_FRONT);

					Shader.LIGHTING.setUniformMat4("lightSpace_matrix", lightMat.mul(lightCamera.getProjectionMatrix()));
					Shader.DEPTH.enable();

					this.setShaderCameraUniforms(Shader.DEPTH, lightCamera);
					for (int scene : this.world_scenes) {
						Model.renderModels(scene);
					}

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
				shadowCubemapBuffer.bind();
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
					shadowCubemapBuffer.bindTextureToBuffer(GL_DEPTH_ATTACHMENT, face, shadowCubemap.getID());
					shadowCubemapBuffer.bind();
					glClear(GL_DEPTH_BUFFER_BIT);

					this.setShaderCameraUniforms(Shader.CUBE_DEPTH, cubemapCamera);
					Shader.CUBE_DEPTH.enable();
					for (int scene : this.world_scenes) {
						Model.renderModels(scene);
					}
				}

				// render lit scene
				lightingBuffer.bind();
				glViewport(0, 0, this.screenWidth, this.screenHeight);
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

			this.geometryPositionMap.bind(GL_TEXTURE5);

			Shader.PARTICLE.enable();
			this.setCameraFOV(this.worldFOV);
			this.setShaderCameraUniforms(Shader.PARTICLE, this.camera);
			Model.renderModels(this.particle_scene);
		}

		// -- SKYBOX -- : we'll use this texture in the post-processing step
		if (this.renderSkybox) {
			if (Scene.skyboxes.containsKey(this.world_scenes[0])) {
				skyboxBuffer.bind();
				glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
				glDisable(GL_CULL_FACE);
				glDisable(GL_BLEND);
				Shader.SKYBOX.enable();
				Shader.SKYBOX.setUniformMat4("vw_matrix", this.camera.getViewMatrix());
				Shader.SKYBOX.setUniformMat4("pr_matrix", this.camera.getProjectionMatrix());
				Scene.skyboxes.get(this.world_scenes[0]).bind(GL_TEXTURE0);
				SkyboxCube.skyboxCube.render();
			}
			else {
				System.err.println("PerspectiveScreen : NO SKYBOX ENTRY FOR SCENE " + this.world_scenes[0]);
			}
		}

		// -- RENDER TO OUTPUT --
		outputBuffer.bind();
		glDisable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		Shader.SPLASH.enable();
		Shader.SPLASH.setUniform1f("alpha", 1f);

		if (this.renderSkybox) {
			this.skyboxColorMap.bind(GL_TEXTURE0);
			screenQuad.render();
		}

		this.lightingColorMap.bind(GL_TEXTURE0);
		//this.geometryPositionMap.bind(GL_TEXTURE0);
		//		this.geometryColorMap.bind(GL_TEXTURE0);
		//		this.geometryNormalMap.bind(GL_TEXTURE0);
		screenQuad.render();

	}

}
