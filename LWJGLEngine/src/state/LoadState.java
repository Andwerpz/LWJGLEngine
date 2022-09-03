package state;

import graphics.Framebuffer;
import graphics.Texture;
import scene.Scene;
import screen.LoadScreen;
import screen.Screen;
import util.MathTools;

public class LoadState extends State {
	
	//makes screen dark, then loads the next state. 
	
	public static final int TRANSITIONING_IN = 0;
	public static final int READY_TO_LOAD = 1;
	public static final int TRANSITIONING_OUT = 2;
	public static final int FINISHED = 3;
	
	public static final long TRANSITION_IN_DURATION = 1;
	public static final long TRANSITION_OUT_DURATION = 1;
	
	private long startTime, endTime;
	
	public static LoadScreen loadScreen = new LoadScreen();
	
	private boolean finishedLoading = false;
	private int state;
	private State nextState;
	
	private float alpha;
	
	public LoadState(StateManager sm, State nextState) {
		super(sm);
		
		this.nextState = nextState;
		init();
	}
	
	private void init() {
		this.alpha = 0;
		this.state = TRANSITIONING_IN;
		this.startTime = System.currentTimeMillis();
		this.endTime = startTime + TRANSITION_IN_DURATION * 1000L;
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
		//should have nothing, this state is meant to load other states
	}

	@Override
	public void update() {
		long curTime = System.currentTimeMillis();
		if(this.state == TRANSITIONING_IN) {
			this.alpha = MathTools.interpolate(0, 0, 1, TRANSITION_IN_DURATION * 1000f, curTime - startTime);
			if(curTime > endTime) {
				alpha = 1;
				this.state = READY_TO_LOAD;
			}
		}
		else if(this.state == READY_TO_LOAD) {
			this.nextState.load();
			this.finishedLoading = true;
			this.state = TRANSITIONING_OUT;
			this.startTime = curTime;
			this.endTime = startTime + TRANSITION_OUT_DURATION * 1000L;
		}
		else if(this.state == TRANSITIONING_OUT) {
			this.alpha = MathTools.interpolate(1, 0, 0, TRANSITION_OUT_DURATION * 1000f, curTime - startTime);
			if(curTime > endTime) {
				alpha = 0;
				this.state = FINISHED;
			}
		}
		loadScreen.setAlpha(this.alpha);
	}

	@Override
	public void render(Framebuffer outputBuffer) {
		loadScreen.render(outputBuffer, Scene.FRAMEBUFFER_SCENE);
	}

}
