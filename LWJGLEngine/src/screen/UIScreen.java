package screen;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

import entity.Entity;
import graphics.Framebuffer;
import graphics.Shader;
import graphics.Texture;
import input.MouseInput;
import main.Main;
import model.Model;
import player.Camera;
import util.Mat4;

public class UIScreen extends Screen {
	// higher values of z will go over lower values.
	// colorID will go in reverse values.

	private int ui_scene;

	private Framebuffer geometryBuffer;

	private Texture geometryPositionMap; // RGB: pos, A: depth
	private Texture geometryNormalMap; // RGB: normal
	private Texture geometrySpecularMap; // RGB: specular, A: shininess
	private Texture geometryColorMap; // RGB: color, A: alpha
	private Texture geometryColorIDMap; // RGB: colorID

	public UIScreen() {
		super();
	}

	@Override
	protected void _kill() {
		this.geometryBuffer.kill();
	}

	@Override
	public void buildBuffers() {
		this.geometryBuffer = new Framebuffer(Main.windowWidth, Main.windowHeight);
		this.geometryPositionMap = new Texture(GL_RGBA16F, Main.windowWidth, Main.windowHeight, GL_RGBA, GL_FLOAT);
		this.geometryNormalMap = new Texture(GL_RGBA16F, Main.windowWidth, Main.windowHeight, GL_RGBA, GL_FLOAT);
		this.geometrySpecularMap = new Texture(GL_RGBA16F, Main.windowWidth, Main.windowHeight, GL_RGBA, GL_FLOAT);
		this.geometryColorMap = new Texture(GL_RGBA, Main.windowWidth, Main.windowHeight, GL_RGBA, GL_FLOAT);
		this.geometryColorIDMap = new Texture(GL_RGBA, Main.windowWidth, Main.windowHeight, GL_RGBA, GL_FLOAT);
		this.geometryBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.geometryPositionMap.getID());
		this.geometryBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, this.geometryNormalMap.getID());
		this.geometryBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT2, GL_TEXTURE_2D, this.geometrySpecularMap.getID());
		this.geometryBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT3, GL_TEXTURE_2D, this.geometryColorMap.getID());
		this.geometryBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT4, GL_TEXTURE_2D, this.geometryColorIDMap.getID());
		this.geometryBuffer.addDepthBuffer();
		this.geometryBuffer.setDrawBuffers(new int[] { GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1, GL_COLOR_ATTACHMENT2, GL_COLOR_ATTACHMENT3, GL_COLOR_ATTACHMENT4 });
		this.geometryBuffer.isComplete();

		this.camera = new Camera(Mat4.orthographic(0, Main.windowWidth, 0, Main.windowHeight, -10, 10));
	}

	public void setUIScene(int scene) {
		this.ui_scene = scene;
	}

	public long getEntityIDAtMouse() {
		long modelInstanceID = Model.convertRGBToID(geometryBuffer.sampleColorAtPoint((int) MouseInput.getMousePos().x, (int) MouseInput.getMousePos().y, GL_COLOR_ATTACHMENT4));
		long entityID = Entity.getEntityIDFromModelID(modelInstanceID);
		return entityID;
	}

	@Override
	public void render(Framebuffer outputBuffer) {
		// -- RENDER UI --
		this.geometryBuffer.bind();
		glEnable(GL_DEPTH_TEST);
		glDepthFunc(GL_LESS);
		glEnable(GL_CULL_FACE);
		glDisable(GL_BLEND);
		glClearDepth(1); // maximum value
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

		Shader.GEOMETRY.enable();
		Shader.GEOMETRY.setUniformMat4("pr_matrix", camera.getProjectionMatrix());
		Shader.GEOMETRY.setUniformMat4("vw_matrix", camera.getViewMatrix());
		Shader.GEOMETRY.setUniform3f("view_pos", camera.getPos());
		Model.renderModels(this.ui_scene);

		// -- RENDER TO OUTPUT --
		outputBuffer.bind();
		glDisable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		Shader.SPLASH.enable();
		Shader.SPLASH.setUniform1f("alpha", 1f);
		geometryColorMap.bind(GL_TEXTURE0);
		// geometryColorIDMap.bind(GL_TEXTURE0);
		screenQuad.render();

		// -- RENDER PROPER UI HITBOXES --
		// we just reverse the depth det.
		this.geometryBuffer.bind();
		glEnable(GL_DEPTH_TEST);
		glDepthFunc(GL_GREATER);
		glEnable(GL_CULL_FACE);
		glDisable(GL_BLEND);
		glClearDepth(0); // minimum value
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

		Shader.GEOMETRY.enable();
		Shader.GEOMETRY.setUniformMat4("pr_matrix", camera.getProjectionMatrix());
		Shader.GEOMETRY.setUniformMat4("vw_matrix", camera.getViewMatrix());
		Shader.GEOMETRY.setUniform3f("view_pos", camera.getPos());
		Model.renderModels(this.ui_scene);

	}

}
