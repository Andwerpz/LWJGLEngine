package lwjglengine.window;

import java.awt.Color;
import java.awt.Font;
import java.io.File;

import lwjglengine.graphics.Framebuffer;
import lwjglengine.graphics.Material;
import lwjglengine.input.Button;
import lwjglengine.input.Input;
import lwjglengine.input.Input.InputCallback;
import lwjglengine.ui.Text;
import lwjglengine.ui.UIElement;
import lwjglengine.ui.UIFilledRectangle;
import lwjglengine.ui.UISection;
import myutils.math.Vec3;

public class FileSelectorWindow extends Window implements InputCallback {

	private UISection bottomBarSection;

	private static int bottomBarHeight = 24;
	private UIFilledRectangle bottomBarRect;
	public static Material bottomBarMaterial = new Material(new Vec3((float) (20 / 255.0)));
	private Text bottomBarSelectedFileText;
	private Button bottomBarSubmitFileButton;

	private Window callbackWindow;

	private FileExplorerWindow fileExplorer;

	public FileSelectorWindow(Window callbackWindow) {
		super(callbackWindow);
		this.init(callbackWindow);
	}

	private void init(Window callbackWindow) {
		this.callbackWindow = callbackWindow;

		this.fileExplorer = new FileExplorerWindow(this);
		this.fileExplorer.setAlignmentStyle(Window.FROM_LEFT, Window.FROM_TOP);
		this.fileExplorer.setOffset(0, 0);
		this.fileExplorer.setFillWidth(true);

		this.bottomBarSection = new UISection();

		this.bottomBarRect = this.bottomBarSection.getBackgroundRect();
		this.bottomBarRect.setFrameAlignmentOffset(0, 0);
		this.bottomBarRect.setDimensions(this.getWidth(), bottomBarHeight);
		this.bottomBarRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_BOTTOM);
		this.bottomBarRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_BOTTOM);
		this.bottomBarRect.setFillWidth(true);
		this.bottomBarRect.setFillWidthMargin(0);
		this.bottomBarRect.setMaterial(bottomBarMaterial);
		this.bottomBarRect.bind(this.rootUIElement);

		this.bottomBarSelectedFileText = new Text(3, 0, "            ", 12, Color.WHITE, this.bottomBarSection.getTextScene());
		this.bottomBarSelectedFileText.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_TOP);
		this.bottomBarSelectedFileText.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
		this.bottomBarSelectedFileText.setDoAntialiasing(false);
		this.bottomBarSelectedFileText.bind(this.bottomBarRect);

		this.bottomBarSubmitFileButton = new Button(3, 0, 100, 20, "btn_submit_file", "Select File", new Font("Dialog", Font.PLAIN, 12), 12, this, this.bottomBarSection.getSelectionScene(), this.bottomBarSection.getTextScene());
		this.bottomBarSubmitFileButton.getButtonText().setDoAntialiasing(false);
		this.bottomBarSubmitFileButton.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_CENTER_TOP);
		this.bottomBarSubmitFileButton.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
		this.bottomBarSubmitFileButton.bind(this.bottomBarRect);

		this._resize();
	}

	@Override
	protected void _kill() {
		this.bottomBarSection.kill();
	}

	@Override
	protected void _resize() {
		this.bottomBarSection.setScreenDimensions(this.getWidth(), this.getHeight());

		if (this.bottomBarRect != null) {
			this.bottomBarSelectedFileText.setWidth(this.bottomBarRect.getWidth() - this.bottomBarSubmitFileButton.getWidth() - 6);
		}

		this.fileExplorer.setHeight(this.getHeight() - bottomBarHeight);
	}

	@Override
	public String getDefaultTitle() {
		return "File Selector";
	}

	public void setSingleEntrySelection(boolean b) {
		this.fileExplorer.setSingleEntrySelection(b);
	}

	@Override
	protected void _update() {
		this.bottomBarSection.update();
	}

	@Override
	protected void renderContent(Framebuffer outputBuffer) {
		this.bottomBarSection.render(outputBuffer, this.getWindowMousePos());
	}

	@Override
	protected void renderOverlay(Framebuffer outputBuffer) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void selected() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void deselected() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void subtreeSelected() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void subtreeDeselected() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _mousePressed(int button) {
		this.bottomBarSection.mousePressed(button);
	}

	@Override
	protected void _mouseReleased(int button) {
		this.bottomBarSection.mouseReleased(button);
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

	@Override
	public void inputClicked(String sID) {
		switch (sID) {
		case "btn_submit_file": {
			File[] files = this.fileExplorer.getSelectedFiles();
			if (files.length != 0) {
				this.callbackWindow.handleFiles(files);
				this.close();
			}
			break;
		}
		}
	}

	@Override
	public void inputChanged(String sID) {

	}

}
