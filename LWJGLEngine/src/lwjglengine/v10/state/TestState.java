package lwjglengine.v10.state;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_E;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_P;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_R;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_C;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_T;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
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

	public TestState() {

	}

	@Override
	public void load() {
		System.out.println("LOAD : " + this.sm.smID);

		Window fileExplorer = new AdjustableWindow(20, 20, 400, 400, "File Explorer", new FileExplorerWindow(), this.sm);
		Window fileExplore2r = new AdjustableWindow(20, 20, 400, 400, "File Explorer", new FileExplorerWindow(), this.sm);
		//Window raytracer = new AdjustableWindow(200, 30, 400, 400, "Raytracing", new RaytracingWindow(), this.sm);
	}

	@Override
	public void buildBuffers() {

	}

	@Override
	public void kill() {

	}

	@Override
	public void update() {

	}

	@Override
	public void render(Framebuffer outputBuffer) {

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

		}
	}

	@Override
	public void keyReleased(int key) {
		// TODO Auto-generated method stub

	}

}
