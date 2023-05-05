package lwjglengine.v10.state;

import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.graphics.Texture;
import lwjglengine.v10.scene.Scene;
import lwjglengine.v10.screen.Screen;
import lwjglengine.v10.screen.ScreenQuad;

public class SplashState extends State {

	//this state just has to render nothing. Root window is going to render logo

	private static final long TIME_ON_SCREEN_MILLIS = 1500;
	private long startTime;

	private static State nextState; //what state loads after the splash screen

	public static void setNextState(State next) {
		nextState = next;
	}

	public SplashState() {

	}

	@Override
	public void load() {
		this.startTime = System.currentTimeMillis();
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
