package state;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

import graphics.Framebuffer;
import graphics.Shader;
import graphics.Texture;
import input.Input;
import main.Main;
import player.Player;
import screen.ScreenQuad;
import util.Vec3;

public class StateManager {

	protected Framebuffer outputBuffer;
	protected Texture outputColorMap;

	private ScreenQuad screenQuad;

	public State activeState;
	public LoadState loadState;

	public StateManager() {
		this.buildBuffers();

		this.activeState = null;
		//this.loadState = new LoadState(this, new SplashState(this));
		this.loadState = new LoadState(this, new MainMenuState(this));
		//this.loadState = new LoadState(this, new LobbyState(this));
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
		if (!this.loadState.isFinishedLoading()) {
			return;
		}
		this.loadState = new LoadState(this, nextState);
	}

	public void update() {
		if (this.activeState != null) {
			this.activeState.update();
		}
		this.loadState.update();
		if (this.loadState.isFinishedLoading() && this.activeState != this.loadState.getNextState()) {
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
		this.loadState.render(outputBuffer);

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
