package lwjglengine.state;

import lwjglengine.graphics.Framebuffer;
import lwjglengine.graphics.Texture;
import lwjglengine.scene.Scene;
import lwjglengine.screen.Screen;
import lwjglengine.screen.ScreenQuad;

public class SplashState extends State {
	//this state just has to render nothing. Root window is going to render logo

	private static final long TIME_ON_SCREEN_MILLIS = 1500;
	private long startTime;

	//TODO figure out how to do this D:
	private State nextState = new TestState(); //what state loads after the splash screen

	public SplashState(State nextState) {
		this.nextState = nextState;
	}

	@Override
	public void load() {
		this.startTime = System.currentTimeMillis();
	}

	@Override
	public void buildBuffers() {

	}

	@Override
	public void kill() {

	}

	@Override
	public void update() {
		long timeElapsed = System.currentTimeMillis() - this.startTime;
		if (timeElapsed > TIME_ON_SCREEN_MILLIS) {
			if (nextState.sm == null) {
				nextState.sm = this.sm;
			}
			this.sm.switchState(nextState);
		}
	}

	@Override
	public void render(Framebuffer outputBuffer) {

	}

	@Override
	public void mousePressed(int button) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseReleased(int button) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseScrolled(float wheelOffset, float smoothOffset) {
		// TODO Auto-generated method stub
	}

	@Override
	public void keyPressed(int key) {

	}

	@Override
	public void keyReleased(int key) {

	}

}
