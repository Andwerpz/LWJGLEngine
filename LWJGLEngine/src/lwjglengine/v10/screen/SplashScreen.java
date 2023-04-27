package lwjglengine.v10.screen;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;

import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.graphics.Shader;
import lwjglengine.v10.graphics.Texture;

public class SplashScreen extends Screen {

	private Texture splashTexture;

	public SplashScreen() {
		super();
	}

	@Override
	protected void _kill() {
		this.splashTexture.kill();
	}

	@Override
	public void buildBuffers() {
		this.splashTexture = new Texture("/csgo splash.png", Texture.VERTICAL_FLIP_BIT);
	}

	@Override
	public void _render(Framebuffer outputBuffer) {
		outputBuffer.bind();
		glDisable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		Shader.SPLASH.enable();
		Shader.SPLASH.setUniform1f("alpha", 1);
		this.splashTexture.bind(GL_TEXTURE0);
		screenQuad.render();
	}

}
