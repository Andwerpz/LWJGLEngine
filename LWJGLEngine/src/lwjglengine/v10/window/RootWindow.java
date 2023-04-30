package lwjglengine.v10.window;

import java.awt.Color;

import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.graphics.Material;
import lwjglengine.v10.main.Main;
import lwjglengine.v10.scene.Scene;
import lwjglengine.v10.screen.UIScreen;
import lwjglengine.v10.ui.UIElement;
import lwjglengine.v10.ui.UIFilledRectangle;
import myutils.v10.math.Vec3;
import myutils.v10.math.Vec4;

public class RootWindow extends Window {
	//the base window

	//this should always be the base window; some other windows depend on the fact that they are not the base window. 

	private final int BACKGROUND_SCENE = Scene.generateScene();

	private UIScreen uiScreen;

	private UIFilledRectangle backgroundRect;

	public RootWindow() {
		super(0, 0, Main.windowWidth, Main.windowHeight, null);

		this.uiScreen = new UIScreen();

		this.backgroundRect = new UIFilledRectangle(0, 0, 0, this.getWidth(), this.getHeight(), BACKGROUND_SCENE);
		this.backgroundRect.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_TOP);
		this.backgroundRect.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_CENTER);
		this.backgroundRect.setMaterial(new Material(new Vec4(0)));
		this.backgroundRect.bind(this.rootUIElement);
	}

	@Override
	protected void _kill() {
		Scene.removeScene(BACKGROUND_SCENE);
		this.uiScreen.kill();
	}

	@Override
	protected void _resize() {
		this.uiScreen.setScreenDimensions(this.getWidth(), this.getHeight());
	}

	public void killAllChildren() {
		for (Window w : this.childWindows) {
			w.kill();
		}
		this.childWindows.clear();
	}

	@Override
	protected void _update() {

	}

	@Override
	protected void renderContent(Framebuffer outputBuffer) {
		this.uiScreen.setUIScene(BACKGROUND_SCENE);
		this.uiScreen.render(outputBuffer);
	}

	@Override
	protected void renderOverlay(Framebuffer outputBuffer) {

	}

	@Override
	protected void selected() {

	}

	@Override
	protected void deselected() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _mousePressed(int button) {

	}

	@Override
	protected void _mouseReleased(int button) {

	}

	@Override
	protected void _mouseScrolled(float wheelOffset, float smoothOffset) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _keyPressed(int key) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _keyReleased(int key) {
		// TODO Auto-generated method stub

	}

}
