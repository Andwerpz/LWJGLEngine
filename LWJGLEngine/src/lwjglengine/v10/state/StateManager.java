package lwjglengine.v10.state;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

import lwjglengine.v10.entity.Entity;
import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.graphics.Shader;
import lwjglengine.v10.graphics.Texture;
import lwjglengine.v10.input.Input;
import lwjglengine.v10.main.Main;
import lwjglengine.v10.model.Model;
import lwjglengine.v10.player.Player;
import lwjglengine.v10.screen.ScreenQuad;
import myutils.v10.math.Vec3;

public class StateManager {

	protected Framebuffer outputBuffer;
	protected Texture outputColorMap;

	private ScreenQuad screenQuad;

	public State activeState;
	public LoadState loadState;

	public StateManager() {
		this.buildBuffers();

		this.activeState = null;
		this.loadState = new LoadState(this, new SplashState(this));
	}

	public void buildBuffers() {
		this.screenQuad = new ScreenQuad();

		this.outputBuffer = new Framebuffer(Main.windowWidth, Main.windowHeight);
		this.outputColorMap = new Texture(GL_RGBA, Main.windowWidth, Main.windowHeight, GL_RGBA, GL_FLOAT);
		this.outputBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.outputColorMap.getID());
		this.outputBuffer.setDrawBuffers(new int[] { GL_COLOR_ATTACHMENT0 });
		this.outputBuffer.isComplete();
	}

	// trigger a load screen
	public void switchState(State nextState) {
		if (this.loadState != null && !this.loadState.isFinishedLoading()) {
			return;
		}
		this.loadState = new LoadState(this, nextState);
	}

	public void update() {
		if (this.activeState != null) {
			this.activeState.update();
		}

		if (this.loadState != null) {
			this.loadState.update();
		}

		//normal updating stuff
		Entity.updateEntities();
		Model.updateModels();

		if (this.loadState != null && this.loadState.isFinishedLoading() && this.activeState != this.loadState.getNextState()) {
			if (this.activeState != null) {
				this.activeState.kill();
			}
			this.activeState = this.loadState.getNextState();
		}
	}

	public void render() {
		outputBuffer.bind();
		glClear(GL_COLOR_BUFFER_BIT);
		if (this.activeState != null) {
			this.activeState.render(outputBuffer);
		}
		if (this.loadState != null) {
			this.loadState.render(outputBuffer);
		}

		// render final product onto screen
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
		glClear(GL_COLOR_BUFFER_BIT);
		glDisable(GL_DEPTH_TEST);
		glDisable(GL_BLEND);

		Shader.IMG_POST_PROCESS.enable();
		this.outputColorMap.bind(GL_TEXTURE0);
		screenQuad.render();
	}

	public void kill() {
		this.activeState.kill();
		this.loadState.kill();
	}

	public void mousePressed(int button) {
		if (activeState == null) {
			return;
		}
		activeState.mousePressed(button);
	}

	public void mouseReleased(int button) {
		if (activeState == null) {
			return;
		}
		activeState.mouseReleased(button);
	}

	public void mouseScrolled(float wheelOffset, float smoothOffset) {
		if (activeState == null) {
			return;
		}
		activeState.mouseScrolled(wheelOffset, smoothOffset);
	}

	public void keyPressed(int key) {
		if (activeState == null) {
			return;
		}
		activeState.keyPressed(key);
		Input.inputsKeyPressed(key);
	}

	public void keyReleased(int key) {
		if (activeState == null) {
			return;
		}
		activeState.keyReleased(key);
		Input.inputsKeyReleased(key);
	}

}
