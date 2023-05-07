package lwjglengine.v10.screen;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.glBlendFuncSeparate;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL21.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL41.*;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43.*;

import static org.lwjgl.opengl.GL44.*;
import static org.lwjgl.opengl.GL45.*;
import static org.lwjgl.opengl.GL46.*;

import java.util.ArrayList;

import lwjglengine.v10.graphics.Cubemap;
import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.graphics.Material;
import lwjglengine.v10.graphics.Shader;
import lwjglengine.v10.graphics.Texture;
import lwjglengine.v10.model.AssetManager;
import lwjglengine.v10.player.Camera;
import lwjglengine.v10.scene.Scene;
import lwjglengine.v10.util.BufferUtils;
import myutils.v10.math.Vec3;

public class RaytracingScreen extends Screen {
	//raytracing, wowee, very nice

	//TODO read triangles in from attached raytracing scene. 

	//Preview Mode - camera can move around, and minimal rays are sent
	//Render Mode - camera cannot move around, and previous frames get blended with new frames to create the render
	//Display Prev Render Mode - look at the previous render, and tweak postprocessing stuff.

	public static final int RENDER_MODE_PREVIEW = 0;
	public static final int RENDER_MODE_RENDER = 1;
	public static final int RENDER_MODE_DISPLAY_PREV_RENDER = 2;

	private int renderMode = RENDER_MODE_PREVIEW;

	//raytracing is pretty simple, we only need one buffer for color lol. 
	private Framebuffer renderBuffer;
	private Texture renderColorMap;

	private Framebuffer prevRenderBuffer;
	private Texture prevRenderColorMap;

	//after doing raytracing, saves the hdr image to be postprocessed
	private Framebuffer outputBuffer;
	private Texture outputColorMap;

	private Framebuffer postprocessHDRBuffer;
	private Texture postprocessHDRMap;

	private Framebuffer postprocessBloomBuffer;
	private Texture postprocessBloomMap;

	private Framebuffer postprocessTempBuffer;
	private Texture postprocessTempMap;

	private int raytracingScene;

	private float fov;

	private ArrayList<Sphere> spheres;
	private ArrayList<Triangle> triangles;

	private int sphereBuffer = -1;
	private int triangleBuffer = -1;

	private int numRenderedFrames;

	private int maxBounceCount;
	private int numRaysPerPixel;

	private float blurStrength; //good to keep around 1 to 5 for antialiasing

	private float defocusStrength;
	private float focusDist;

	private float ambientStrength;
	private float sunStrength;
	private Vec3 sunDir;

	//more bounces have drastically diminishing returns along with drastically increasing 
	//render times
	private static int previewMaxBounceCount = 5;
	private static int renderMaxBounceCount = 10;

	//increase number of rays per pixel while rendering to speed it up?
	//downside is lower fps
	private static int previewNumRaysPerPixel = 1;
	private static int renderNumRaysPerPixel = 20;

	private float exposure;
	private float gamma;

	//smudging bright areas with gaussian blur
	private float bloomThreshold; //how bright does a pixel have to be to be blurred?

	public RaytracingScreen() {
		this.fov = 90f;

		this.numRenderedFrames = 0;

		this.blurStrength = 3f;

		this.defocusStrength = 0f;
		this.focusDist = 30f;

		this.ambientStrength = 1;
		this.sunStrength = 0;
		this.sunDir = new Vec3(1, 1, 0.4f);

		this.exposure = 1;
		this.gamma = 1;

		this.bloomThreshold = 2.5f;
	}

	public void setRaytracingScene(int scene) {
		this.raytracingScene = scene;
	}

	@Override
	protected void _kill() {
		this.renderBuffer.kill();
		this.prevRenderBuffer.kill();
		this.outputBuffer.kill();
		this.postprocessTempBuffer.kill();
		this.postprocessBloomBuffer.kill();
		this.postprocessHDRBuffer.kill();
	}

	@Override
	public void buildBuffers() {

		System.out.println("BUILDING BUFFERS : " + this.screenWidth + " " + this.screenHeight);

		if (this.renderBuffer != null) {
			this.renderBuffer.kill();
			this.prevRenderBuffer.kill();
			this.outputBuffer.kill();
			this.postprocessTempBuffer.kill();
			this.postprocessBloomBuffer.kill();
			this.postprocessHDRBuffer.kill();
		}

		this.renderBuffer = new Framebuffer(this.screenWidth, this.screenHeight);
		this.renderColorMap = new Texture(GL_RGBA32F, this.screenWidth, this.screenHeight, GL_RGBA, GL_FLOAT);
		this.renderBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.renderColorMap.getID());
		this.renderBuffer.setDrawBuffers(new int[] { GL_COLOR_ATTACHMENT0 });
		this.renderBuffer.isComplete();

		this.prevRenderBuffer = new Framebuffer(this.screenWidth, this.screenHeight);
		this.prevRenderColorMap = new Texture(GL_RGBA32F, this.screenWidth, this.screenHeight, GL_RGBA, GL_FLOAT);
		this.prevRenderBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.prevRenderColorMap.getID());
		this.prevRenderBuffer.setDrawBuffers(new int[] { GL_COLOR_ATTACHMENT0 });
		this.prevRenderBuffer.isComplete();

		this.outputBuffer = new Framebuffer(this.screenWidth, this.screenHeight);
		this.outputColorMap = new Texture(GL_RGBA32F, this.screenWidth, this.screenHeight, GL_RGBA, GL_FLOAT);
		this.outputBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.outputColorMap.getID());
		this.outputBuffer.setDrawBuffers(new int[] { GL_COLOR_ATTACHMENT0 });
		this.outputBuffer.isComplete();

		this.postprocessHDRBuffer = new Framebuffer(this.screenWidth, this.screenHeight);
		this.postprocessHDRMap = new Texture(GL_RGBA32F, this.screenWidth, this.screenHeight, GL_RGBA, GL_FLOAT);
		this.postprocessHDRBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.postprocessHDRMap.getID());
		this.postprocessHDRBuffer.setDrawBuffers(new int[] { GL_COLOR_ATTACHMENT0 });
		this.postprocessHDRBuffer.isComplete();

		this.postprocessBloomBuffer = new Framebuffer(this.screenWidth, this.screenHeight);
		this.postprocessBloomMap = new Texture(GL_RGBA32F, this.screenWidth, this.screenHeight, GL_RGBA, GL_FLOAT);
		glBindTexture(GL_TEXTURE_2D, this.postprocessBloomMap.getID());
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		this.postprocessBloomBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.postprocessBloomMap.getID());
		this.postprocessBloomBuffer.setDrawBuffers(new int[] { GL_COLOR_ATTACHMENT0 });
		this.postprocessBloomBuffer.isComplete();

		this.postprocessTempBuffer = new Framebuffer(this.screenWidth, this.screenHeight);
		this.postprocessTempMap = new Texture(GL_RGBA32F, this.screenWidth, this.screenHeight, GL_RGBA, GL_FLOAT);
		glBindTexture(GL_TEXTURE_2D, this.postprocessTempMap.getID());
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		this.postprocessTempBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.postprocessTempMap.getID());
		this.postprocessTempBuffer.setDrawBuffers(new int[] { GL_COLOR_ATTACHMENT0 });
		this.postprocessTempBuffer.isComplete();

		Vec3 cameraPos = new Vec3();
		Vec3 cameraFacing = new Vec3(0, 0, -1);

		if (this.camera != null) {
			cameraPos = this.camera.getPos();
			cameraFacing = this.camera.getFacing();
		}

		this.camera = new Camera((float) Math.toRadians(this.fov), this.screenWidth, this.screenHeight, 0.1f, 200f);
		this.camera.setPos(cameraPos);
		this.camera.setFacing(cameraFacing);

		this.buildObjectBuffers();

	}

	public void setCameraPos(Vec3 pos) {
		if (this.renderMode == RENDER_MODE_RENDER) {
			return;
		}
		this.camera.setPos(pos);
	}

	public void setCameraFacing(Vec3 facing) {
		if (this.renderMode == RENDER_MODE_RENDER) {
			return;
		}
		this.camera.setFacing(facing);
	}

	public void incrementExposure(float inc) {
		this.exposure += inc;
	}

	private void buildObjectBuffers() {
		if (this.spheres == null) {
			this.spheres = new ArrayList<>();
			this.triangles = new ArrayList<>();
		}

		if (this.sphereBuffer == -1) {
			this.sphereBuffer = glGenBuffers();
		}

		if (this.triangleBuffer == -1) {
			this.triangleBuffer = glGenBuffers();
		}

		int sizeofSphere = 4 + 16;
		float[] sphereData = new float[this.spheres.size() * sizeofSphere];
		for (int i = 0; i < this.spheres.size(); i++) {
			Sphere s = this.spheres.get(i);
			sphereData[i * sizeofSphere + 0] = s.center.x;
			sphereData[i * sizeofSphere + 1] = s.center.y;
			sphereData[i * sizeofSphere + 2] = s.center.z;
			sphereData[i * sizeofSphere + 3] = s.radius;
			float[] matArr = s.material.toFloatArr();
			for (int j = 0; j < matArr.length; j++) {
				sphereData[i * sizeofSphere + 4 + j] = matArr[j];
			}
			sphereData[i * sizeofSphere + 19] = 0;
		}
		glBindBuffer(GL_SHADER_STORAGE_BUFFER, this.sphereBuffer);
		glBufferData(GL_SHADER_STORAGE_BUFFER, BufferUtils.createFloatBuffer(sphereData), GL_STATIC_DRAW);

		int sizeofTriangle = 12 + 16;
		float[] triangleData = new float[this.triangles.size() * sizeofTriangle];
		for (int i = 0; i < this.triangles.size(); i++) {
			Triangle t = this.triangles.get(i);
			triangleData[i * sizeofTriangle + 0] = t.a.x;
			triangleData[i * sizeofTriangle + 1] = t.a.y;
			triangleData[i * sizeofTriangle + 2] = t.a.z;
			triangleData[i * sizeofTriangle + 3] = 0;
			triangleData[i * sizeofTriangle + 4] = t.b.x;
			triangleData[i * sizeofTriangle + 5] = t.b.y;
			triangleData[i * sizeofTriangle + 6] = t.b.z;
			triangleData[i * sizeofTriangle + 7] = 0;
			triangleData[i * sizeofTriangle + 8] = t.c.x;
			triangleData[i * sizeofTriangle + 9] = t.c.y;
			triangleData[i * sizeofTriangle + 10] = t.c.z;
			triangleData[i * sizeofTriangle + 11] = 0;
			float[] matArr = t.material.toFloatArr();
			for (int j = 0; j < matArr.length; j++) {
				triangleData[i * sizeofTriangle + 12 + j] = matArr[j];
			}
			triangleData[i * sizeofTriangle + 27] = 0;
		}

		glBindBuffer(GL_SHADER_STORAGE_BUFFER, this.triangleBuffer);
		glBufferData(GL_SHADER_STORAGE_BUFFER, BufferUtils.createFloatBuffer(triangleData), GL_STATIC_DRAW);

		glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
	}

	public void addSphere(Vec3 center, float radius, Material material) {
		Sphere s = new Sphere(center, radius, material);
		this.spheres.add(s);
		this.buildObjectBuffers();
	}

	public void addTriangle(Vec3 a, Vec3 b, Vec3 c, Material material) {
		Triangle t = new Triangle(a, b, c, material);
		this.triangles.add(t);
		this.buildObjectBuffers();
	}

	private void setRaytracingShaderUniforms() {
		Vec3 cameraRight = this.camera.getFacing().cross(this.camera.getUp());
		Vec3 cameraUp = this.camera.getFacing().cross(cameraRight);

		Shader.RAYTRACING.enable();
		Shader.RAYTRACING.setUniformMat4("vw_matrix", this.camera.getViewMatrix());
		Shader.RAYTRACING.setUniformMat4("pr_matrix", this.camera.getProjectionMatrix());
		Shader.RAYTRACING.setUniform3f("camera_pos", this.camera.getPos());
		Shader.RAYTRACING.setUniform1i("numSpheres", this.spheres.size());
		Shader.RAYTRACING.setUniform1i("numTriangles", this.triangles.size());
		Shader.RAYTRACING.setUniform1i("maxBounceCount", this.maxBounceCount);
		Shader.RAYTRACING.setUniform1i("numRaysPerPixel", this.numRaysPerPixel);
		Shader.RAYTRACING.setUniform1i("numRenderedFrames", this.numRenderedFrames);
		Shader.RAYTRACING.setUniform1i("windowWidth", this.screenWidth);
		Shader.RAYTRACING.setUniform1i("windowHeight", this.screenHeight);
		Shader.RAYTRACING.setUniform1f("blurStrength", this.blurStrength); //for antialiasing
		Shader.RAYTRACING.setUniform1f("defocusStrength", this.defocusStrength);
		Shader.RAYTRACING.setUniform1f("focusDist", this.focusDist);
		Shader.RAYTRACING.setUniform3f("cameraRight", cameraRight);
		Shader.RAYTRACING.setUniform3f("cameraUp", cameraUp);
		Shader.RAYTRACING.setUniform3f("sunDir", this.sunDir.normalize());
		Shader.RAYTRACING.setUniform1f("sunStrength", this.sunStrength);
		Shader.RAYTRACING.setUniform1f("ambientStrength", this.ambientStrength);
	}

	@Override
	protected void _render(Framebuffer outputBuffer) {

		//pre-render checks
		if (!Scene.skyboxes.containsKey(this.raytracingScene)) {
			System.err.println("NO SKYBOX ENTRY FOR RAYTRACING SCENE " + this.raytracingScene);
			return;
		}

		//set blend mode
		glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

		// -- RENDER SCENE --
		//at the end, the hdr output should be in prevRenderColorMap
		switch (this.renderMode) {
		case RENDER_MODE_PREVIEW: {
			this.numRenderedFrames = 0;

			//render
			renderBuffer.bind();
			glDisable(GL_DEPTH_TEST);
			glDisable(GL_CULL_FACE);
			glDisable(GL_BLEND);
			glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, this.sphereBuffer);
			glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, this.triangleBuffer);
			this.setRaytracingShaderUniforms();
			Shader.RAYTRACING.enable();
			this.prevRenderColorMap.bind(GL_TEXTURE0);
			Scene.skyboxes.get(this.raytracingScene).bind(GL_TEXTURE1);
			SkyboxCube.skyboxCube.render();

			this.outputBuffer.bind();
			glClear(GL_COLOR_BUFFER_BIT);
			glDisable(GL_DEPTH_TEST);
			glEnable(GL_BLEND);
			this.renderColorMap.bind(GL_TEXTURE0);
			Shader.SPLASH.enable();
			Shader.SPLASH.setUniform1f("alpha", 1f);
			screenQuad.render();
			break;
		}

		case RENDER_MODE_RENDER: {
			//render
			renderBuffer.bind();
			glDisable(GL_DEPTH_TEST);
			glDisable(GL_CULL_FACE);
			glDisable(GL_BLEND);
			glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, this.sphereBuffer);
			glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, this.triangleBuffer);
			this.setRaytracingShaderUniforms();
			Shader.RAYTRACING.enable();
			this.prevRenderColorMap.bind(GL_TEXTURE0);
			Scene.skyboxes.get(this.raytracingScene).bind(GL_TEXTURE1);
			SkyboxCube.skyboxCube.render();

			this.numRenderedFrames++;

			//render to prev buffer
			prevRenderBuffer.bind();
			glClear(GL_COLOR_BUFFER_BIT);
			glDisable(GL_DEPTH_TEST);
			glEnable(GL_BLEND);
			this.renderColorMap.bind(GL_TEXTURE0);
			Shader.SPLASH.enable();
			Shader.SPLASH.setUniform1f("alpha", 1f);
			screenQuad.render();

			this.outputBuffer.bind();
			glClear(GL_COLOR_BUFFER_BIT);
			glDisable(GL_DEPTH_TEST);
			glEnable(GL_BLEND);
			this.renderColorMap.bind(GL_TEXTURE0);
			Shader.SPLASH.enable();
			Shader.SPLASH.setUniform1f("alpha", 1f);
			screenQuad.render();
			break;
		}

		case RENDER_MODE_DISPLAY_PREV_RENDER: {
			this.outputBuffer.bind();
			glClear(GL_COLOR_BUFFER_BIT);
			glDisable(GL_DEPTH_TEST);
			glEnable(GL_BLEND);
			this.prevRenderColorMap.bind(GL_TEXTURE0);
			Shader.SPLASH.enable();
			Shader.SPLASH.setUniform1f("alpha", 1f);
			screenQuad.render();
			break;
		}
		}

		// -- RENDER TO OUTPUT --
		//extract bright pixels
		this.postprocessBloomBuffer.bind();
		glClear(GL_COLOR_BUFFER_BIT);
		glDisable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
		this.outputColorMap.bind(GL_TEXTURE0);
		Shader.RAYTRACING_EXTRACT_BLOOM.enable();
		Shader.RAYTRACING_EXTRACT_BLOOM.setUniform1f("bloomThreshold", this.bloomThreshold);
		screenQuad.render();

		Shader.GAUSSIAN_BLUR.enable();
		glDisable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
		for (int i = 0; i < 5; i++) {
			//blur horizontally
			this.postprocessTempBuffer.bind();
			this.postprocessBloomMap.bind(GL_TEXTURE0);
			Shader.GAUSSIAN_BLUR.setUniform1i("horizontal", 1);
			screenQuad.render();

			//blur vertically
			this.postprocessBloomBuffer.bind();
			this.postprocessTempMap.bind(GL_TEXTURE0);
			Shader.GAUSSIAN_BLUR.setUniform1i("horizontal", 0);
			screenQuad.render();
		}

		//do postprocessing
		this.postprocessHDRBuffer.bind();
		glClear(GL_COLOR_BUFFER_BIT);
		glDisable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
		this.outputColorMap.bind(GL_TEXTURE0);
		this.postprocessBloomMap.bind(GL_TEXTURE1);
		Shader.RAYTRACING_HDR.enable();
		Shader.RAYTRACING_HDR.setUniform1f("exposure", this.exposure);
		Shader.RAYTRACING_HDR.setUniform1f("gamma", this.gamma);
		screenQuad.render();

		//render to output
		outputBuffer.bind();
		glDisable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
		this.postprocessHDRMap.bind(GL_TEXTURE0);
		Shader.SPLASH.enable();
		Shader.SPLASH.setUniform1f("alpha", 1f);
		screenQuad.render();

	}

	public void setRenderMode(int renderMode) {
		this.renderMode = renderMode;

		switch (this.renderMode) {
		case RENDER_MODE_DISPLAY_PREV_RENDER:
		case RENDER_MODE_PREVIEW:
			this.maxBounceCount = previewMaxBounceCount;
			this.numRaysPerPixel = previewNumRaysPerPixel;
			break;

		case RENDER_MODE_RENDER:
			this.maxBounceCount = renderMaxBounceCount;
			this.numRaysPerPixel = renderNumRaysPerPixel;
			break;
		}
	}

	public int getRenderMode() {
		return this.renderMode;
	}

}

class Sphere {
	public Vec3 center;
	public float radius;
	public Material material;

	public Sphere(Vec3 center, float radius, Material material) {
		this.center = new Vec3(center);
		this.radius = radius;
		this.material = new Material(material);
	}
}

class Triangle {
	public Vec3 a, b, c;
	public Material material;

	public Triangle(Vec3 a, Vec3 b, Vec3 c, Material material) {
		this.a = new Vec3(a);
		this.b = new Vec3(b);
		this.c = new Vec3(c);
		this.material = new Material(material);
	}
}
