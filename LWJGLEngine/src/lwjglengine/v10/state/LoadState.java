package lwjglengine.v10.state;

import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.graphics.Texture;
import lwjglengine.v10.scene.Scene;
import lwjglengine.v10.screen.LoadScreen;
import lwjglengine.v10.screen.Screen;
import myutils.v10.math.MathUtils;

public class LoadState extends State {

	// makes screen dark, then loads the next state.

	public static final int TRANSITIONING_IN = 0;
	public static final int READY_TO_LOAD = 1;
	public static final int TRANSITIONING_OUT = 2;
	public static final int FINISHED = 3;

	public static final long TRANSITION_IN_DURATION_MILLIS = 700;
	public static final long TRANSITION_OUT_DURATION_MILLIS = 700;

	private long startTime, endTime;

	public LoadScreen loadScreen;

	private boolean finishedLoading = false;
	private int state;
	private State nextState;

	private float alpha;

	public LoadState(State nextState) {
		this.nextState = nextState;
		this.loadScreen = new LoadScreen();
		init();
	}

	private void init() {
		this.alpha = 0;
		this.state = TRANSITIONING_IN;
		this.startTime = System.currentTimeMillis();
		this.endTime = startTime + TRANSITION_IN_DURATION_MILLIS;
	}

	public float getAlpha() {
		return this.alpha;
	}

	public State getNextState() {
		return this.nextState;
	}

	public boolean isFinishedLoading() {
		return this.finishedLoading;
	}

	@Override
	public void load() {
		// should have nothing, this state is meant to load other states
	}

	@Override
	public void buildBuffers() {
		this.loadScreen.setScreenDimensions(this.bufferWidth, this.bufferHeight);
	}

	@Override
	public void kill() {

	}

	@Override
	public void update() {
		long curTime = System.currentTimeMillis();
		if (this.state == TRANSITIONING_IN) {
			this.alpha = MathUtils.interpolate(0, 0, 1, TRANSITION_IN_DURATION_MILLIS, curTime - startTime);
			if (curTime > endTime) {
				alpha = 1;
				this.state = READY_TO_LOAD;
			}
		}
		else if (this.state == READY_TO_LOAD) {
			this.sm.killAllChildren();
			this.nextState.load();
			this.finishedLoading = true;
			this.state = TRANSITIONING_OUT;
			this.startTime = System.currentTimeMillis();
			this.endTime = startTime + TRANSITION_OUT_DURATION_MILLIS;
		}
		else if (this.state == TRANSITIONING_OUT) {
			this.alpha = MathUtils.interpolate(1, 0, 0, TRANSITION_OUT_DURATION_MILLIS, curTime - startTime);
			if (curTime > endTime) {
				alpha = 0;
				this.state = FINISHED;
			}
		}
		else if (this.state == FINISHED) {
			// System.out.println("FIN");
		}
		loadScreen.setAlpha(this.alpha);
	}

	@Override
	public void render(Framebuffer outputBuffer) {
		loadScreen._render(outputBuffer);
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
