package screen;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;

import graphics.Framebuffer;
import graphics.Texture;
import main.Main;
import scene.Scene;

public abstract class Screen {
	//the screen class is what's called on to render stuff
	//it takes a camera view into a 3D scene, and produces a screen sized texture.
	
	//each game state might have multiple screens that are layered on top of each other,
	//for example: a UI might be layered on top of a 3D game scene. 
	
	protected Framebuffer outputBuffer;
	protected Texture outputColorMap;
	
	public Screen() {
		this.outputBuffer = new Framebuffer(Main.windowWidth, Main.windowHeight);
		this.outputColorMap = new Texture(GL_RGBA, Main.windowWidth, Main.windowHeight, GL_RGBA, GL_FLOAT);
		this.outputBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.outputColorMap.getID());
		this.outputBuffer.setDrawBuffers(new int[] {GL_COLOR_ATTACHMENT0});
		this.outputBuffer.isComplete();
	}
	
	public Texture getOutputTexture() {
		return this.outputColorMap;
	}
	
	public abstract void render(int scene);
}
