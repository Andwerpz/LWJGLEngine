package lwjglengine.screen;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.glBlendFuncSeparate;
import static org.lwjgl.opengl.GL30.*;

import lwjglengine.graphics.Framebuffer;
import lwjglengine.graphics.Shader;
import lwjglengine.graphics.Texture;
import myutils.v10.math.MathUtils;
import myutils.v11.file.FileUtils;
import myutils.v11.file.JarUtils;

public class LoadScreen extends Screen {

	private static Texture loadingTexture = new Texture(JarUtils.loadImage("/load_screen/black_background.png"));
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
	public void _render(Framebuffer outputBuffer) {
		outputBuffer.bind();
		glDisable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
		glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

		Shader.SPLASH.enable();
		Shader.SPLASH.setUniform1f("alpha", this.alpha);
		loadingTexture.bind(GL_TEXTURE0);
		screenQuad.render();
	}

}
