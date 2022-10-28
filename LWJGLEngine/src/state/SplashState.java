package state;

import graphics.Framebuffer;
import graphics.Texture;
import scene.Scene;
import screen.Screen;
import screen.ScreenQuad;
import screen.SplashScreen;

public class SplashState extends State {

	private SplashScreen splashScreen;

	private static final long TIME_ON_SCREEN = 2; // in seconds
	private long startTime;

	public SplashState(StateManager sm) {
		super(sm);
	}

	@Override
	public void load() {
		this.splashScreen = new SplashScreen();
		this.startTime = System.currentTimeMillis();
	}

	@Override
	public void kill() {
		this.splashScreen.kill();
	}

	@Override
	public void update() {
		long timeElapsed = System.currentTimeMillis() - this.startTime;
		if (timeElapsed / 1000L > TIME_ON_SCREEN) {
			this.sm.switchState(new MainMenuState(this.sm));
		}
	}

	@Override
	public void render(Framebuffer outputBuffer) {
		this.splashScreen.render(outputBuffer);
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
	public void keyPressed(int key) {

	}

	@Override
	public void keyReleased(int key) {

	}

}
