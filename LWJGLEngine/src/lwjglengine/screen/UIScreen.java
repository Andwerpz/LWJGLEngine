package lwjglengine.screen;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

import lwjglengine.entity.Entity;
import lwjglengine.graphics.Framebuffer;
import lwjglengine.graphics.Shader;
import lwjglengine.graphics.Texture;
import lwjglengine.input.MouseInput;
import lwjglengine.main.Main;
import lwjglengine.model.Model;
import lwjglengine.player.Camera;
import myutils.math.Mat4;
import myutils.math.Vec2;
import myutils.math.Vec3;
import myutils.misc.Pair;

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

	private Framebuffer colorIDBuffer;
	private Texture colorIDMap;

	//if set to true, this will clear the color id buffer upon every call of render(), 
	//as opposed to only clearing it once each frame. 
	private boolean clearColorIDBufferOnRender = false;

	//if set to true, then the depth testing will be reversed when drawing to the colorID buffer. 
	//this allows buttons to work better, as the button background will be drawn over the text,
	//but in other cases where you want proper depth ids, you should turn this off. 
	private boolean reverseDepthColorID = true;

	//renders color id if true. 
	private boolean renderColorID = false;

	private Vec2 viewportOffset; //where the bottom left corner is
	private float viewportWidth, viewportHeight;

	private float left, right, bottom, top, near, far;

	public UIScreen() {
		super();
	}

	@Override
	protected void _kill() {
		this.geometryBuffer.kill();
		this.colorIDBuffer.kill();
	}

	@Override
	public void buildBuffers() {
		if (this.geometryBuffer != null) {
			this.geometryBuffer.kill();
			this.colorIDBuffer.kill();
		}

		this.geometryBuffer = new Framebuffer(this.screenWidth, this.screenHeight);
		this.geometryPositionMap = new Texture(GL_RGBA16F, this.screenWidth, this.screenHeight, GL_RGBA, GL_FLOAT);
		this.geometryNormalMap = new Texture(GL_RGBA16F, this.screenWidth, this.screenHeight, GL_RGBA, GL_FLOAT);
		this.geometrySpecularMap = new Texture(GL_RGBA16F, this.screenWidth, this.screenHeight, GL_RGBA, GL_FLOAT);
		this.geometryColorMap = new Texture(GL_RGBA16F, this.screenWidth, this.screenHeight, GL_RGBA, GL_FLOAT);
		this.geometryColorIDMap = new Texture(GL_RGBA8, this.screenWidth, this.screenHeight, GL_RGBA, GL_FLOAT);
		this.geometryBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.geometryPositionMap.getID());
		this.geometryBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, this.geometryNormalMap.getID());
		this.geometryBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT2, GL_TEXTURE_2D, this.geometrySpecularMap.getID());
		this.geometryBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT3, GL_TEXTURE_2D, this.geometryColorMap.getID());
		this.geometryBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT4, GL_TEXTURE_2D, this.geometryColorIDMap.getID());
		this.geometryBuffer.addDepthBuffer();
		this.geometryBuffer.setDrawBuffers(new int[] { GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1, GL_COLOR_ATTACHMENT2, GL_COLOR_ATTACHMENT3, GL_COLOR_ATTACHMENT4 });
		this.geometryBuffer.isComplete();

		this.colorIDBuffer = new Framebuffer(this.screenWidth, this.screenHeight);
		this.colorIDMap = new Texture(GL_RGBA8, this.screenWidth, this.screenHeight, GL_RGBA, GL_FLOAT);
		this.colorIDBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.colorIDMap.getID());
		this.colorIDBuffer.setDrawBuffers(new int[] { GL_COLOR_ATTACHMENT0 });
		this.colorIDBuffer.isComplete();

		this.viewportOffset = new Vec2(0);
		this.viewportWidth = this.screenWidth;
		this.viewportHeight = this.screenHeight;

		this.calculateBounds();
		this.near = -1000;
		this.far = 1000;

		this.camera = new Camera(Mat4.orthographic(left, right, bottom, top, near, far));
	}

	private void calculateBounds() {
		this.left = this.viewportOffset.x;
		this.right = this.viewportOffset.x + this.viewportWidth;
		this.bottom = this.viewportOffset.y;
		this.top = this.viewportOffset.y + this.viewportHeight;

		this.updateCamera();
	}

	public void setLeft(float f) {
		this.viewportOffset.x = f;
		this.viewportWidth = this.right - f;
		this.calculateBounds();
	}

	public void setRight(float f) {
		this.viewportWidth = f - this.left;
		this.calculateBounds();
	}

	public void setBottom(float f) {
		this.viewportOffset.y = f;
		this.viewportHeight = this.top - f;
		this.calculateBounds();
	}

	public void setTop(float f) {
		this.viewportHeight = f - this.bottom;
		this.calculateBounds();
	}

	public void setViewportWidth(float w) {
		this.viewportWidth = w;
		this.calculateBounds();
	}

	public void setViewportHeight(float h) {
		this.viewportHeight = h;
		this.calculateBounds();
	}

	/**
	 * Viewport bounds is the size of the screen in worldspace
	 * @param w
	 * @param h
	 */
	public void setViewportDimensions(float w, float h) {
		this.viewportWidth = w;
		this.viewportHeight = h;
		this.calculateBounds();
	}

	/**
	 * Viewport offset is the offset of the bottom left corner of what you see in worldspace
	 * @param v
	 */
	public void setViewportOffset(Vec2 v) {
		this.viewportOffset.set(v);
		this.calculateBounds();
	}

	public void incrementViewportOffset(Vec2 v) {
		this.viewportOffset.addi(v);
		this.calculateBounds();
	}

	public void setNear(float f) {
		this.near = f;
		this.updateCamera();
	}

	public void setFar(float f) {
		this.far = f;
		this.updateCamera();
	}

	private void updateCamera() {
		if (this.camera == null) {
			return;
		}
		this.camera.setProjectionMatrix(Mat4.orthographic(left, right, bottom, top, near, far));
	}

	public void setUIScene(int scene) {
		this.ui_scene = scene;
	}

	/**
	 * Coordinate is relative to the bottom left corner
	 * @param x
	 * @param y
	 * @return
	 */
	public long getEntityIDAtCoord(int x, int y) {
		Vec3 color = this.colorIDBuffer.sampleColorAtPoint(x, y, GL_COLOR_ATTACHMENT0);
		long modelInstanceID = Model.convertRGBToID(color);
		long entityID = Entity.getEntityIDFromModelID(modelInstanceID);
		return entityID;
	}

	@Override
	protected void update() {
		if (this.shouldSampleEntityIDDelayed) {
			this.updateEntityIDDelayed();
		}
	}

	public void updateEntityIDDelayed() {
		this.shouldSampleEntityIDDelayed = false;
		this.entityIDDelayed = this.getEntityIDAtCoord(this.entityIDCoordDelayed.first, this.entityIDCoordDelayed.second);
	}

	private Pair<Integer, Integer> entityIDCoordDelayed = new Pair<Integer, Integer>(-1, -1);
	private long entityIDDelayed = -1;
	private boolean shouldSampleEntityIDDelayed = false;

	/**
	 * Returns the entity ID that was sampled on the previous frame.
	 * Calling this also prompts UIScreen to resample the entity ID for the next frame. 
	 * @param x
	 * @param y
	 * @return
	 */
	public long getEntityIDAtCoordDelayed(int x, int y) {
		this.shouldSampleEntityIDDelayed = true;
		this.entityIDCoordDelayed.first = x;
		this.entityIDCoordDelayed.second = y;
		return this.entityIDDelayed;
	}

	public void clearColorIDBuffer() {
		this.colorIDBuffer.bind();
		glClear(GL_COLOR_BUFFER_BIT);
	}

	public void setClearColorIDBufferOnRender(boolean b) {
		this.clearColorIDBufferOnRender = b;
	}

	public void setReverseDepthColorID(boolean b) {
		this.reverseDepthColorID = b;
	}

	public void setRenderColorID(boolean b) {
		this.renderColorID = b;
	}

	@Override
	protected void _render(Framebuffer outputBuffer) {
		if (this.clearColorIDBufferOnRender) {
			this.clearColorIDBuffer();
		}

		// -- RENDER UI --
		this.geometryBuffer.bind();
		glEnable(GL_DEPTH_TEST);
		glDepthFunc(GL_LESS);
		glEnable(GL_CULL_FACE);
		glEnable(GL_BLEND);
		//premultiplied alpha
		glBlendFuncSeparate(GL_ONE, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
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

		Shader.SPLASH.enable();
		Shader.SPLASH.setUniform1f("alpha", 1f);
		geometryColorMap.bind(GL_TEXTURE0);
		screenQuad.render();

		// -- RENDER PROPER UI HITBOXES --
		this.geometryBuffer.bind();
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_CULL_FACE);
		glDisable(GL_BLEND);

		if (this.reverseDepthColorID) {
			glDepthFunc(GL_GREATER);
			glClearDepth(0); // minimum value
		}
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

		Shader.GEOMETRY.enable();
		Shader.GEOMETRY.setUniformMat4("pr_matrix", camera.getProjectionMatrix());
		Shader.GEOMETRY.setUniformMat4("vw_matrix", camera.getViewMatrix());
		Shader.GEOMETRY.setUniform3f("view_pos", camera.getPos());
		Model.renderModels(this.ui_scene);

		// -- RENDER COLOR ID TO SAVE --
		this.colorIDBuffer.bind();
		glDisable(GL_DEPTH_TEST);
		glEnable(GL_BLEND); //should this be enabled?
		glClear(GL_COLOR_BUFFER_BIT);

		Shader.SPLASH.enable();
		Shader.SPLASH.setUniform1f("alpha", 1f);
		geometryColorIDMap.bind(GL_TEXTURE0);
		screenQuad.render();

		// -- RENDER COLOR ID TO OUTPUT --
		if (this.renderColorID) {
			outputBuffer.bind();
			glDisable(GL_DEPTH_TEST);
			glEnable(GL_BLEND);

			Shader.SPLASH.enable();
			Shader.SPLASH.setUniform1f("alpha", 1f);
			this.geometryColorIDMap.bind(GL_TEXTURE0);
			screenQuad.render();
		}

	}

	public Texture getColorIDMap() {
		return this.geometryColorIDMap;
	}

}
