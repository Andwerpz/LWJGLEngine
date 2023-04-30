package lwjglengine.v10.window;

import java.awt.Color;

import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.graphics.Material;
import lwjglengine.v10.scene.Scene;
import lwjglengine.v10.screen.UIScreen;
import lwjglengine.v10.ui.UIElement;
import lwjglengine.v10.ui.UIFilledRectangle;

public class FileExplorerWindow extends AdjustableWindow {

	private final int UI_SCENE = Scene.generateScene();

	private UIScreen uiScreen;

	private int directoryWidth = 75;
	private UIFilledRectangle directoryRect;

	public FileExplorerWindow(int xOffset, int yOffset, int contentWidth, int contentHeight, Window parentWindow) {
		super(xOffset, yOffset, contentWidth, contentHeight, "File Explorer", parentWindow);

		this.uiScreen = new UIScreen();

		this.directoryRect = new UIFilledRectangle(10, 10, 0, 100, 100, UI_SCENE);
		this.directoryRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.directoryRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		this.directoryRect.setMaterial(new Material(Color.GRAY));
		this.directoryRect.bind(this.contentRootUIElement);

		this.__resize();
	}

	@Override
	protected void __kill() {
		this.uiScreen.kill();
		Scene.removeScene(UI_SCENE);
	}

	@Override
	protected void __resize() {
		if (this.uiScreen != null) {
			this.uiScreen.setScreenDimensions(this.getContentWidth(), this.getContentHeight());
		}
	}

	@Override
	protected void __update() {

	}

	@Override
	protected void _renderContent(Framebuffer outputBuffer) {
		this.uiScreen.setUIScene(UI_SCENE);
		this.uiScreen.render(outputBuffer);
	}

	@Override
	protected void contentSelected() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void contentDeselected() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void __mousePressed(int button) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void __mouseReleased(int button) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void __mouseScrolled(float wheelOffset, float smoothOffset) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void __keyPressed(int key) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void __keyReleased(int key) {
		// TODO Auto-generated method stub

	}

}
