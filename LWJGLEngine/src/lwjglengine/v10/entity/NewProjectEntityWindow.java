package lwjglengine.v10.entity;

import java.awt.Color;

import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.input.Button;
import lwjglengine.v10.input.Input;
import lwjglengine.v10.input.TextField;
import lwjglengine.v10.project.Project;
import lwjglengine.v10.screen.UIScreen;
import lwjglengine.v10.ui.Text;
import lwjglengine.v10.ui.UIElement;
import lwjglengine.v10.ui.UIFilledRectangle;
import lwjglengine.v10.ui.UISection;
import lwjglengine.v10.window.Window;

public class NewProjectEntityWindow extends Window {

	private Project project;

	private UIScreen uiScreen;
	private UISection uiSection;

	private Text promptText;

	private Button confirmBtn;
	private TextField nameTf;

	public NewProjectEntityWindow(int xOffset, int yOffset, int width, int height, Project project, Window parentWindow) {
		super(xOffset, yOffset, width, height, parentWindow);
		this.init(project);
	}

	private void init(Project project) {
		this.project = project;

		this.uiScreen = new UIScreen();
		this.uiSection = new UISection(0, 0, this.getWidth(), this.getHeight(), this.uiScreen);

		UIFilledRectangle backgroundRect = this.uiSection.getBackgroundRect();
		backgroundRect.setFillWidth(true);
		backgroundRect.setFillHeight(true);
		backgroundRect.setMaterial(this.contentDefaultMaterial);
		backgroundRect.bind(this.rootUIElement);

		this.promptText = new Text(0, 10, "Create new Entity Asset for project: " + this.project.getProjectName(), 12, Color.WHITE, this.uiSection.getTextScene());
		this.promptText.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_TOP);
		this.promptText.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_BOTTOM);
		this.promptText.setDoAntialiasing(false);
		this.promptText.bind(backgroundRect);

		this.nameTf = new TextField(5, 0, 100, 20, "tf_entity_name", "Entity Name", 12, this.uiSection.getSelectionScene(), this.uiSection.getTextScene());
		this.nameTf.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_BOTTOM);
		this.nameTf.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_TOP);
		this.nameTf.getTextUIElement().setDoAntialiasing(false);
		this.nameTf.bind(backgroundRect);

		this.confirmBtn = new Button(5, 0, 100, 20, "btn_confirm", "Confirm", 12, this.uiSection.getSelectionScene(), this.uiSection.getTextScene());
		this.confirmBtn.setFrameAlignmentStyle(UIElement.FROM_CENTER_RIGHT, UIElement.FROM_CENTER_BOTTOM);
		this.confirmBtn.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		this.confirmBtn.getButtonText().setDoAntialiasing(false);
		this.confirmBtn.bind(backgroundRect);
	}

	@Override
	protected void _kill() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _resize() {
		this.uiScreen.setScreenDimensions(this.getWidth(), this.getHeight());
	}

	@Override
	protected void _update() {
		this.uiSection.update();
	}

	@Override
	protected void renderContent(Framebuffer outputBuffer) {
		this.uiSection.render(outputBuffer, this.getWindowMousePos());
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
		this.uiSection.mousePressed(button);
	}

	@Override
	protected void _mouseReleased(int button) {
		this.uiSection.mouseReleased(button);

		switch (Input.getClicked(this.uiSection.getSelectionScene())) {
		case "btn_confirm":
			String name = this.nameTf.getText();
			if (name.length() == 0) {
				break;
			}

			this.project.createEntityAsset(name);
			this.close();
			break;
		}
	}

	@Override
	protected void _mouseScrolled(float wheelOffset, float smoothOffset) {
		this.uiSection.mouseScrolled(wheelOffset, smoothOffset);
	}

	@Override
	protected void _keyPressed(int key) {
		this.uiSection.keyPressed(key);
	}

	@Override
	protected void _keyReleased(int key) {
		this.uiSection.keyReleased(key);
	}

}
