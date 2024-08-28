package lwjglengine.screen;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;

import java.util.HashSet;

import lwjglengine.graphics.Framebuffer;
import lwjglengine.graphics.Texture;
import lwjglengine.main.Main;
import lwjglengine.player.Camera;
import lwjglengine.scene.Scene;
import lwjglengine.ui.UIElement;

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

		Screen.activeScreens.add(this);

		if (this.screenWidth == 0 || this.screenHeight == 0) {
			return;
		}

		this.buildBuffers();
	}

	public void setScreenDimensions(int width, int height) {
		this.screenWidth = width;
		this.screenHeight = height;

		if (this.screenWidth <= 0 || this.screenHeight <= 0) {
			return;
		}

		this.buildBuffers();
	}

	public void setScreenWidth(int width) {
		this.setScreenDimensions(width, this.screenHeight);
	}

	public void setScreenHeight(int height) {
		this.setScreenDimensions(this.screenWidth, height);
	}

	public abstract void buildBuffers();

	public Camera getCamera() {
		return this.camera;
	}

	public void setCamera(Camera c) {
		this.camera = c;
	}

	public int getScreenWidth() {
		return this.screenWidth;
	}

	public int getScreenHeight() {
		return this.screenHeight;
	}

	public static void updateActiveScreens() {
		for (Screen s : activeScreens) {
			s.update();
		}
	}

	protected void update() {
		/* Keeping it optional to implement */
	}

	public void render(Framebuffer outputBuffer) {
		//maybe we should handle this externally, and just say that screens render to whatever viewport dimensions 
		//are currently active. 
		//but then, the textures bound to the screen have to match in size huh.. 
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
