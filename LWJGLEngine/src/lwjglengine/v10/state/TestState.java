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

	public TestState() {

	}

	@Override
	public void load() {
		//		Window w1 = new AdjustableWindow(120, 10, Main.windowWidth / 2, Main.windowHeight / 2, "Test window 1", this.sm.getRootWindow());
		//		Window w2 = new AdjustableWindow(10, 10, 200, 200, "ABCDEFG abcdefg", this.sm.getRootWindow());
		//
		//		Window w3 = new AdjustableWindow(10, 10, 200, 200, "Test nest window 1", w1);
		//		Window w4 = new AdjustableWindow(220, 10, 200, 200, "nesting window owo", w1);
		//
		//		Window w5 = new AdjustableWindow(30, 10, 100, 100, "We can nest twice??!?!", w4);

		//Window w1 = new RaytracingWindow(20, 20, 400, 400, "Raytracing Window", this.sm.getRootWindow());
		//Window w2 = new RaytracingWindow(10, 10, 200, 200, "Raytracing Window", w1);

		//Window fileExplorer = new FileExplorerWindow(20, 20, 400, 300, this.sm.getRootWindow());

		Window fileExplorer = new AdjustableWindow(20, 20, 400, 400, "File Explorer", new FileExplorerWindow(), this.sm.getRootWindow());
		Window raytracer = new AdjustableWindow(200, 30, 400, 400, "Raytracing", new RaytracingWindow(), this.sm.getRootWindow());
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

	}

	@Override
	public void keyReleased(int key) {
		// TODO Auto-generated method stub

	}

}
