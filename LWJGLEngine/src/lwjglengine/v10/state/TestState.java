package lwjglengine.v10.state;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_E;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_P;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_R;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_C;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_T;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_O;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_L;

import java.awt.Color;

import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.graphics.Material;
import lwjglengine.v10.main.Main;
import lwjglengine.v10.model.AssetManager;
import lwjglengine.v10.player.PlayerInputController;
import lwjglengine.v10.scene.Scene;
import lwjglengine.v10.screen.RaytracingScreen;
import lwjglengine.v10.window.AdjustableWindow;
import lwjglengine.v10.window.FileExplorerWindow;
import lwjglengine.v10.window.RaytracingWindow;
import lwjglengine.v10.window.RootWindow;
import lwjglengine.v10.window.Window;
import myutils.v10.math.Vec3;
import myutils.v10.math.Vec4;

public class TestState extends State {

	//private final int RAYTRACING_SCENE = Scene.generateScene();

	//private RaytracingScreen raytracingScreen;

	//private PlayerInputController pic;

	public TestState() {

	}

	@Override
	public void load() {
		//Main.lockCursor();

		//		Window w1 = new AdjustableWindow(120, 10, Main.windowWidth / 2, Main.windowHeight / 2, "Test window 1", this.sm.getRootWindow());
		//		Window w2 = new AdjustableWindow(10, 10, 200, 200, "ABCDEFG abcdefg", this.sm.getRootWindow());
		//
		//		Window w3 = new AdjustableWindow(10, 10, 200, 200, "Test nest window 1", w1);
		//		Window w4 = new AdjustableWindow(220, 10, 200, 200, "nesting window owo", w1);
		//
		//		Window w5 = new AdjustableWindow(30, 10, 100, 100, "We can nest twice??!?!", w4);

		Window w1 = new RaytracingWindow(20, 20, 400, 400, "Raytracing Window", this.sm.getRootWindow());
		Window w2 = new RaytracingWindow(10, 10, 200, 200, "Raytracing Window", w1);

		Window fileExplorer = new FileExplorerWindow(20, 20, 400, 300, this.sm.getRootWindow());

		//		this.raytracingScreen = new RaytracingScreen();
		//		this.raytracingScreen.setRenderMode(RaytracingScreen.RENDER_MODE_PREVIEW);
		//
		//		Scene.skyboxes.put(RAYTRACING_SCENE, AssetManager.getSkybox("lake_skybox"));
		//
		//		this.pic = new PlayerInputController(new Vec3(0, 50, 0));
		//
		//		//set up the scene
		//		Material lightMaterial = new Material(new Vec3(1));
		//		lightMaterial.setEmissive(new Vec4(1, 1, 1, 10f));
		//
		//		int edgeSize = 5;
		//		float radius = 5f;
		//		float gap = 5;
		//
		//		float edgeLength = (radius * 2 + gap) * (edgeSize - 1);
		//
		//		for (int i = 0; i < edgeSize; i++) {
		//			for (int j = 0; j < edgeSize; j++) {
		//				Material m = new Material(Color.WHITE);
		//				m.setSmoothness((1.0f / (edgeSize - 1)) * i);
		//				m.setSpecularProbability((1.0f / (edgeSize - 1)) * j);
		//
		//				this.raytracingScreen.addSphere(new Vec3(i * (radius * 2 + gap) - edgeLength / 2.0f, 5, j * (radius * 2 + gap) - edgeLength / 2.0f), radius, m);
		//			}
		//		}
		//
		//		Material whiteMaterial = new Material(new Vec3(1, 1, 1));
		//		Material grayMaterial = new Material(Color.GRAY);
		//		Material blueMaterial = new Material(Color.BLUE);
		//		Material greenMaterial = new Material(Color.GREEN);
		//		Material redMaterial = new Material(Color.RED);
		//		Material sphereMaterial = new Material(Color.WHITE);
		//
		//		sphereMaterial.setSmoothness(1f);
		//		sphereMaterial.setSpecularProbability(0.3f);
		//
		//		//		redMaterial.setSmoothness(1);
		//		//		redMaterial.setSpecularProbability(0.95f);
		//		//
		//		//		blueMaterial.setSmoothness(1);
		//		//		blueMaterial.setSpecularProbability(0.95f);
		//
		//		//whiteMaterial.setSmoothness(1);
		//		//whiteMaterial.setSpecularProbability(1f);
		//
		//		Vec3 v0 = new Vec3(-50, 0, -50);
		//		Vec3 v1 = new Vec3(-50, 0, 50);
		//		Vec3 v2 = new Vec3(50, 0, 50);
		//		Vec3 v3 = new Vec3(50, 0, -50);
		//		Vec3 v4 = new Vec3(-50, 100, -50);
		//		Vec3 v5 = new Vec3(-50, 100, 50);
		//		Vec3 v6 = new Vec3(50, 100, 50);
		//		Vec3 v7 = new Vec3(50, 100, -50);
		//
		//		//floor
		//		this.raytracingScreen.addTriangle(v0, v1, v2, greenMaterial);
		//		this.raytracingScreen.addTriangle(v0, v2, v3, greenMaterial);
		//
		//		//ceiling
		//		this.raytracingScreen.addTriangle(v5, v4, v6, whiteMaterial);
		//		this.raytracingScreen.addTriangle(v6, v4, v7, whiteMaterial);
		//
		//		//back wall
		//		this.raytracingScreen.addTriangle(v0, v3, v7, grayMaterial);
		//		this.raytracingScreen.addTriangle(v0, v7, v4, grayMaterial);
		//
		//		//left wall
		//		this.raytracingScreen.addTriangle(v1, v0, v4, blueMaterial);
		//		this.raytracingScreen.addTriangle(v1, v4, v5, blueMaterial);
		//
		//		//right wall
		//		this.raytracingScreen.addTriangle(v2, v7, v3, redMaterial);
		//		this.raytracingScreen.addTriangle(v2, v6, v7, redMaterial);
		//
		//		//front wall
		//		this.raytracingScreen.addTriangle(v2, v1, v5, whiteMaterial);
		//		this.raytracingScreen.addTriangle(v2, v5, v6, whiteMaterial);
		//
		//		//ceiling light
		//		this.raytracingScreen.addSphere(new Vec3(0, 100, 0), 10f, lightMaterial);
		//
		//		//big ball
		//		this.raytracingScreen.addSphere(new Vec3(0, 50, 0), 30, sphereMaterial);
	}

	@Override
	public void kill() {
		//this.raytracingScreen.kill();
		//Scene.removeScene(RAYTRACING_SCENE);
	}

	@Override
	public void update() {
		//this.pic.update();

		//this.raytracingScreen.setCameraPos(this.pic.getPos());
		//this.raytracingScreen.setCameraFacing(this.pic.getFacing());
	}

	@Override
	public void render(Framebuffer outputBuffer) {
		//this.raytracingScreen.setRaytracingScene(RAYTRACING_SCENE);
		//this.raytracingScreen.render(outputBuffer);
	}

	@Override
	public void mousePressed(int button) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseReleased(int button) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseScrolled(float wheelOffset, float smoothOffset) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyPressed(int key) {
		//		switch (key) {
		//		case GLFW_KEY_E:
		//			//			Vec3 center = new Vec3(0);
		//			//			center.x = (float) (Math.random() * 100 - 50);
		//			//			center.y = (float) (Math.random() * 100 - 50);
		//			//			center.z = (float) (Math.random() * 100 - 50);
		//			//
		//			//			float radius = (float) (Math.random() * 15) + 5;
		//			//
		//			//			Material material = new Material(new Vec3(1));
		//			//			Vec3 diffuse = new Vec3(0);
		//			//			diffuse.x = (float) Math.random();
		//			//			diffuse.y = (float) Math.random();
		//			//			diffuse.z = (float) Math.random();
		//			//
		//			//			material.setDiffuse(diffuse);
		//			//
		//			//			Vec4 emissive = new Vec4(1, 1, 1, 0);
		//			//			if (Math.random() > 0.5) {
		//			//				emissive.w = (float) Math.random();
		//			//			}
		//			//			material.setEmissive(emissive);
		//			//
		//			//			this.raytracingScreen.addSphere(center, radius, material);
		//			break;
		//
		//		case GLFW_KEY_C:
		//			System.out.println(this.pic.getPos());
		//			System.out.println(this.pic.getCamXRot() + " " + this.pic.getCamYRot());
		//			break;
		//
		//		case GLFW_KEY_P:
		//			this.raytracingScreen.setRenderMode(RaytracingScreen.RENDER_MODE_PREVIEW);
		//			break;
		//
		//		case GLFW_KEY_R:
		//			this.raytracingScreen.setRenderMode(RaytracingScreen.RENDER_MODE_RENDER);
		//			break;
		//
		//		case GLFW_KEY_T:
		//			this.raytracingScreen.setRenderMode(RaytracingScreen.RENDER_MODE_DISPLAY_PREV_RENDER);
		//			break;
		//
		//		case GLFW_KEY_O:
		//			this.raytracingScreen.incrementExposure(0.1f);
		//			break;
		//
		//		case GLFW_KEY_L:
		//			this.raytracingScreen.incrementExposure(-0.1f);
		//			break;
		//		}
	}

	@Override
	public void keyReleased(int key) {
		// TODO Auto-generated method stub

	}

}
