package lwjglengine.v10.state;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_E;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_P;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_R;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_C;

import java.awt.Color;

import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.graphics.Material;
import lwjglengine.v10.main.Main;
import lwjglengine.v10.player.PlayerInputController;
import lwjglengine.v10.screen.RaytracingScreen;
import myutils.v10.math.Vec3;
import myutils.v10.math.Vec4;

public class TestState extends State {

	private static final int RAYTRACING_SCENE = 0;

	private RaytracingScreen raytracingScreen;

	private PlayerInputController pic;

	public TestState() {

	}

	@Override
	public void load() {
		Main.lockCursor();
		this.clearScene(RAYTRACING_SCENE);

		this.raytracingScreen = new RaytracingScreen();
		this.raytracingScreen.setRenderMode(RaytracingScreen.RENDER_MODE_PREVIEW);

		this.pic = new PlayerInputController(new Vec3(0, 50, 0));

		this.pic.setPos(new Vec3(93.494865f, 118.98625f, -126.06695f));

		//set up the scene
		Material lightMaterial = new Material(new Vec3(1));
		lightMaterial.setEmissive(new Vec4(1, 1, 1, 3f));

		this.raytracingScreen.addSphere(new Vec3(0, 0, -400), 200, lightMaterial); //'sun'

		int edgeSize = 5;

		for (int i = 0; i < edgeSize; i++) {
			for (int j = 0; j < edgeSize; j++) {
				Material m = new Material(Color.WHITE);
				m.setSmoothness((1.0f / (edgeSize - 1)) * i);
				m.setSpecularProbability((1.0f / (edgeSize - 1)) * j);

				this.raytracingScreen.addSphere(new Vec3(i * 21, j * 21, 0), 10, m);
			}
		}
	}

	@Override
	public void kill() {
		this.raytracingScreen.kill();
	}

	@Override
	public void update() {
		this.pic.update();

		this.raytracingScreen.setCameraPos(this.pic.getPos());
		this.raytracingScreen.setCameraFacing(this.pic.getFacing());
	}

	@Override
	public void render(Framebuffer outputBuffer) {
		this.raytracingScreen.setRaytracingScene(RAYTRACING_SCENE);
		this.raytracingScreen.render(outputBuffer);
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
		switch (key) {
		case GLFW_KEY_E:
			//			Vec3 center = new Vec3(0);
			//			center.x = (float) (Math.random() * 100 - 50);
			//			center.y = (float) (Math.random() * 100 - 50);
			//			center.z = (float) (Math.random() * 100 - 50);
			//
			//			float radius = (float) (Math.random() * 15) + 5;
			//
			//			Material material = new Material(new Vec3(1));
			//			Vec3 diffuse = new Vec3(0);
			//			diffuse.x = (float) Math.random();
			//			diffuse.y = (float) Math.random();
			//			diffuse.z = (float) Math.random();
			//
			//			material.setDiffuse(diffuse);
			//
			//			Vec4 emissive = new Vec4(1, 1, 1, 0);
			//			if (Math.random() > 0.5) {
			//				emissive.w = (float) Math.random();
			//			}
			//			material.setEmissive(emissive);
			//
			//			this.raytracingScreen.addSphere(center, radius, material);
			break;

		case GLFW_KEY_C:
			System.out.println(this.pic.getPos());
			System.out.println(this.pic.getCamXRot() + " " + this.pic.getCamYRot());
			break;

		case GLFW_KEY_P:
			this.raytracingScreen.setRenderMode(RaytracingScreen.RENDER_MODE_PREVIEW);
			break;

		case GLFW_KEY_R:
			this.raytracingScreen.setRenderMode(RaytracingScreen.RENDER_MODE_RENDER);
			break;
		}
	}

	@Override
	public void keyReleased(int key) {
		// TODO Auto-generated method stub

	}

}
