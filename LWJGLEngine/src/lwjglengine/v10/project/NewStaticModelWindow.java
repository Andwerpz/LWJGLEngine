package lwjglengine.v10.project;

import java.awt.Color;

import lwjglengine.v10.asset.ModelAsset;
import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.input.Button;
import lwjglengine.v10.input.Input;
import lwjglengine.v10.model.ModelTransform;
import lwjglengine.v10.screen.UIScreen;
import lwjglengine.v10.ui.Text;
import lwjglengine.v10.ui.UIElement;
import lwjglengine.v10.ui.UIFilledRectangle;
import lwjglengine.v10.ui.UISection;
import lwjglengine.v10.window.AdjustableWindow;
import lwjglengine.v10.window.Window;
import myutils.v10.misc.Pair;

public class NewStaticModelWindow extends Window {

	private Window callbackWindow;

	private Project project;

	private UISection contentSection;

	private UIScreen uiScreen;

	private Button selectModelBtn;
	private Text selectedModelText;

	private Button confirmBtn;

	private ModelAsset selectedModelAsset = null;

	public NewStaticModelWindow(Project project, Window callbackWindow, Window parentWindow) {
		super(0, 0, 300, 300, parentWindow);
		this.init(project, callbackWindow);
	}

	private void init(Project project, Window callbackWindow) {
		this.project = project;

		this.callbackWindow = callbackWindow;

		this.uiScreen = new UIScreen();

		this.contentSection = new UISection(0, 0, this.getWidth(), this.getHeight(), this.uiScreen);

		UIFilledRectangle contentBackgroundRect = this.contentSection.getBackgroundRect();
		contentBackgroundRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_BOTTOM);
		contentBackgroundRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_BOTTOM);
		contentBackgroundRect.setFillWidth(true);
		contentBackgroundRect.setFillHeight(true);
		contentBackgroundRect.setMaterial(this.contentDefaultMaterial);
		contentBackgroundRect.bind(this.rootUIElement);

		this.selectModelBtn = new Button(10, 10, 100, 20, "btn_select_model", "Select Model", 12, this.contentSection.getSelectionScene(), this.contentSection.getTextScene());
		this.selectModelBtn.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.selectModelBtn.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		this.selectModelBtn.getButtonText().setDoAntialiasing(false);
		this.selectModelBtn.bind(contentBackgroundRect);

		this.selectedModelText = new Text(110, 0, "No model currently selected", 12, Color.WHITE, this.contentSection.getTextScene());
		this.selectedModelText.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_TOP);
		this.selectedModelText.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
		this.selectedModelText.setDoAntialiasing(false);
		this.selectedModelText.bind(this.selectModelBtn);

		this.confirmBtn = new Button(10, 40, 100, 20, "btn_confirm", "Confirm", 12, this.contentSection.getSelectionScene(), this.contentSection.getTextScene());
		this.confirmBtn.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.confirmBtn.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		this.confirmBtn.getButtonText().setDoAntialiasing(false);
		this.confirmBtn.bind(contentBackgroundRect);
	}

	@Override
	public void handleObject(Object o) {
		if (!(o instanceof ModelAsset)) {
			return;
		}

		ModelAsset a = (ModelAsset) o;

		this.selectedModelAsset = a;

		this.selectedModelText.setText(this.selectedModelAsset.getName());
		this.selectedModelText.setWidth(this.selectedModelText.getWidth());
	}

	@Override
	protected void _kill() {
		this.uiScreen.kill();
	}

	@Override
	protected void _resize() {
		this.uiScreen.setScreenDimensions(this.getWidth(), this.getHeight());
	}

	@Override
	protected void _update() {
		this.contentSection.update();
	}

	@Override
	protected void renderContent(Framebuffer outputBuffer) {
		this.contentSection.render(outputBuffer, getWindowMousePos());
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
		this.contentSection.mousePressed(button);
	}

	@Override
	protected void _mouseReleased(int button) {
		this.contentSection.mouseReleased(button);

		switch (Input.getClicked(this.contentSection.getSelectionScene())) {
		case "btn_select_model": {
			AdjustableWindow w = new AdjustableWindow("Select Model", new ProjectAssetViewerWindow(this.project, this, null), this);
			ProjectAssetViewerWindow assetViewer = (ProjectAssetViewerWindow) w.getContentWindow();
			assetViewer.setAssetTypeCategories(new String[] { ProjectAssetViewerWindow.MODEL_STR });
			break;
		}

		case "btn_confirm": {
			if (this.selectedModelAsset == null) {
				return;
			}

			Pair<Long, ModelTransform> staticModel = new Pair<Long, ModelTransform>(this.selectedModelAsset.getID(), new ModelTransform());
			this.callbackWindow.handleObject(staticModel);
			this.close();
			break;
		}
		}
	}

	@Override
	protected void _mouseScrolled(float wheelOffset, float smoothOffset) {
		this.contentSection.mouseScrolled(wheelOffset, smoothOffset);
	}

	@Override
	protected void _keyPressed(int key) {
		this.contentSection.keyPressed(key);
	}

	@Override
	protected void _keyReleased(int key) {
		this.contentSection.keyReleased(key);
	}

}
