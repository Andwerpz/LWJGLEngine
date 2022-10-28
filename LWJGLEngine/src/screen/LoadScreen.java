package screen;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

import graphics.Framebuffer;
import graphics.Shader;
import graphics.Texture;
import util.MathUtils;

public class LoadScreen extends Screen {

	private static Texture loadingTexture = new Texture("/load_screen/black_background.png");
	private float alpha;

	public LoadScreen() {
		super();
	}

	@Override
	protected void _kill() {

	}

	@Override
	public void buildBuffers() {
		this.alpha = 0;
	}

	public void setAlpha(float alpha) {
		this.alpha = MathUtils.clamp(0, 1, alpha);
	}

	@Override
	public void render(Framebuffer outputBuffer) {
		outputBuffer.bind();
		glDisable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		Shader.SPLASH.enable();
		Shader.SPLASH.setUniform1f("alpha", this.alpha);
		loadingTexture.bind(GL_TEXTURE0);
		screenQuad.render();
	}

}
