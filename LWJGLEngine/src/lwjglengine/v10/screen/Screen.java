package lwjglengine.v10.screen;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;

import java.util.HashSet;

import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.graphics.Texture;
import lwjglengine.v10.main.Main;
import lwjglengine.v10.player.Camera;
import lwjglengine.v10.scene.Scene;
import lwjglengine.v10.ui.UIElement;

public abstract class Screen {
	// the screen class is what's called on to render stuff 
	// it takes in a framebuffer, and layers it's own output onto it. 

	// each game state might have multiple screens that are layered on top of each other,
	// for example: a UI might be layered on top of a 3D game scene.

	private static HashSet<Screen> activeScreens = new HashSet<>();

	protected static ScreenQuad screenQuad = new ScreenQuad();

	protected int screenWidth, screenHeight;

	protected Camera camera;

	public Screen() {
		this.screenWidth = Main.windowWidth;
		this.screenHeight = Main.windowHeight;

		this.buildBuffers();
		Screen.activeScreens.add(this);
	}

	public void setScreenDimensions(int width, int height) {
		this.screenWidth = width;
		this.screenHeight = height;
		this.buildBuffers();
	}

	public void setScreenWidth(int width) {
		this.setScreenDimensions(width, this.screenHeight);
	}

	public void setScreenHeight(int height) {
		this.setScreenDimensions(this.screenWidth, height);
	}

	public abstract void buildBuffers();

	public static void rebuildAllBuffers() {
		for (Screen s : activeScreens) {
			s.buildBuffers();
		}
	}

	public Camera getCamera() {
		return this.camera;
	}

	public void setCamera(Camera c) {
		this.camera = c;
	}

	public void render(Framebuffer outputBuffer) {
		glViewport(0, 0, this.screenWidth, this.screenHeight);
		this._render(outputBuffer);
		glViewport(0, 0, Main.windowWidth, Main.windowHeight);
	}

	protected abstract void _render(Framebuffer outputBuffer);

	public void kill() {
		Screen.activeScreens.remove(this);
		this._kill();
	}

	protected abstract void _kill();
}
