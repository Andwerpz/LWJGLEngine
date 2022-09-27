package screen;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;

import graphics.Framebuffer;
import graphics.Texture;
import main.Main;
import model.ScreenQuad;
import player.Camera;
import scene.Scene;

public abstract class Screen {
	// the screen class is what's called on to render stuff
	// it takes a camera view into a 3D scene, and a framebuffer, and layers its own
	// view on top of the input buffer.

	// each game state might have multiple screens that are layered on top of each
	// other,
	// for example: a UI might be layered on top of a 3D game scene.

	protected static ScreenQuad screenQuad = new ScreenQuad();

	protected Camera camera;

	public Screen() {

	}

	public Camera getCamera() {
		return this.camera;
	}

	public void setCamera(Camera c) {
		this.camera = c;
	}

	public abstract void render(Framebuffer outputBuffer, int scene);
}
