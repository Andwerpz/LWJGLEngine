package lwjglengine.v10.window;

import java.awt.Color;

import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.graphics.Material;
import lwjglengine.v10.model.Line;
import lwjglengine.v10.model.Model;
import lwjglengine.v10.scene.Scene;
import lwjglengine.v10.screen.UIScreen;
import lwjglengine.v10.ui.UIFilledRectangle;
import myutils.v10.math.Vec3;

public class AdjustableWindow extends Window {

	private final int BACKGROUND_SCENE = Scene.generateScene();

	private UIScreen uiScreen;

	private UIFilledRectangle backgroundRect;

	private long[] windowBorder;

	public AdjustableWindow(int xOffset, int yOffset, int width, int height, Window parentWindow) {
		super(xOffset, yOffset, width, height, parentWindow);

		this.uiScreen = new UIScreen();
		this.uiScreen.setScreenDimensions(this.width, this.height);

		this.uiScreen.setViewportWidth(this.width);
		this.uiScreen.setViewportHeight(this.height);

		this.backgroundRect = new UIFilledRectangle(0, 0, -10, this.width, this.height, BACKGROUND_SCENE);
		this.backgroundRect.setMaterial(new Material(new Vec3((float) (25 / 255.0))));
		this.backgroundRect.bind(this.rootUIElement);

		UIFilledRectangle r1 = new UIFilledRectangle(1, 1, -5, 3, 3, BACKGROUND_SCENE);
		r1.setMaterial(new Material(new Vec3((float) (35 / 255.0))));
		r1.bind(this.rootUIElement);

		this.windowBorder = new long[4];
	}

	@Override
	protected void _kill() {
		this.uiScreen.kill();
		Scene.removeScene(BACKGROUND_SCENE);
	}

	@Override
	protected void _update() {

	}

	@Override
	protected void _render(Framebuffer outputBuffer) {
		this.uiScreen.setUIScene(BACKGROUND_SCENE);
		this.uiScreen.render(outputBuffer);
	}

	@Override
	protected void selected() {
		this.windowBorder[0] = Line.addLine(0, 1, this.width, 1, BACKGROUND_SCENE);
		this.windowBorder[1] = Line.addLine(1, 0, 1, this.height, BACKGROUND_SCENE);
		this.windowBorder[2] = Line.addLine(this.width, 0, this.width, this.height, BACKGROUND_SCENE);
		this.windowBorder[3] = Line.addLine(0, this.height, this.width, this.height, BACKGROUND_SCENE);

		for (int i = 0; i < 4; i++) {
			Model.updateInstance(this.windowBorder[i], new Material(new Vec3((float) (71 / 255.0))));
		}
	}

	@Override
	protected void deselected() {
		this.backgroundRect.setMaterial(new Material(new Vec3((float) (25 / 255.0))));

		for (int i = 0; i < 4; i++) {
			Model.removeInstance(this.windowBorder[i]);
		}
	}

	@Override
	protected void _mousePressed(int button) {
		this.backgroundRect.setMaterial(new Material(new Vec3((float) (61 / 255.0))));
	}

	@Override
	protected void _mouseReleased(int button) {
		this.backgroundRect.setMaterial(new Material(new Vec3((float) (51 / 255.0))));
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
