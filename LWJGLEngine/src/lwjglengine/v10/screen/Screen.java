package lwjglengine.v10.screen;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
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
	// it takes a camera view into a 3D scene, and a framebuffer, and layers its own
	// view on top of the input buffer. **should it?? 

	// each game state might have multiple screens that are layered on top of each other,
	// for example: a UI might be layered on top of a 3D game scene.

	private static HashSet<Screen> activeScreens = new HashSet<>();

	protected static ScreenQuad screenQuad = new ScreenQuad();

	protected Camera camera;

	public Screen() {
		this.buildBuffers();
		Screen.activeScreens.add(this);
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

	public abstract void render(Framebuffer outputBuffer);

	public void kill() {
		Screen.activeScreens.remove(this);
		this._kill();
	}

	protected abstract void _kill();
}