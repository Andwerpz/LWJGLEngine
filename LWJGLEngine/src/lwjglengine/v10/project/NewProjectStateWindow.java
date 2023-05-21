package lwjglengine.v10.project;

import java.awt.Color;

import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.input.Button;
import lwjglengine.v10.input.Input;
import lwjglengine.v10.input.TextField;
import lwjglengine.v10.screen.UIScreen;
import lwjglengine.v10.ui.Text;
import lwjglengine.v10.ui.UIElement;
import lwjglengine.v10.ui.UIFilledRectangle;
import lwjglengine.v10.ui.UISection;
import lwjglengine.v10.window.Window;

public class NewProjectStateWindow extends Window {
	//utility window just to create a new project. 

	private UIScreen uiScreen;

	private UISection newStateDialogue;

	private Text newStatePromptText;
	private TextField newStateTf;
	private Button newStateBtn;

	private Project project;

	private long hoveredSectionID;

	public NewProjectStateWindow(int xOffset, int yOffset, int width, int height, Project project, Window parentWindow) {
		super(xOffset, yOffset, width, height, parentWindow);
		this.init(project);
	}

	public void init(Project project) {
		this.project = project;

		this.uiScreen = new UIScreen();

		this.newStateDialogue = new UISection(0, 0, this.getWidth(), this.getHeight(), this.uiScreen);

		UIFilledRectangle newStateDialogueBackgroundRect = this.newStateDialogue.getBackgroundRect();
		newStateDialogueBackgroundRect.setMaterial(this.contentDefaultMaterial);
		newStateDialogueBackgroundRect.setFillWidth(true);
		newStateDialogueBackgroundRect.setFillHeight(true);
		newStateDialogueBackgroundRect.bind(this.rootUIElement);

		this.newStatePromptText = new Text(0, 10, "Create new State Asset for project: " + this.project.getProjectName(), 12, Color.WHITE, this.newStateDialogue.getTextScene());
		this.newStatePromptText.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_TOP);
		this.newStatePromptText.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_BOTTOM);
		this.newStatePromptText.setDoAntialiasing(false);
		this.newStatePromptText.bind(newStateDialogueBackgroundRect);

		this.newStateBtn = new Button(5, 0, 100, 20, "btn_new_state", "Confirm", 12, this.newStateDialogue.getSelectionScene(), this.newStateDialogue.getTextScene());
		this.newStateBtn.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_BOTTOM);
		this.newStateBtn.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_TOP);
		this.newStateBtn.getButtonText().setDoAntialiasing(false);
		this.newStateBtn.bind(newStateDialogueBackgroundRect);

		this.newStateTf = new TextField(5, 0, 100, 20, "tf_new_state", "State Name", 12, this.newStateDialogue.getSelectionScene(), this.newStateDialogue.getTextScene());
		this.newStateTf.setFrameAlignmentStyle(UIElement.FROM_CENTER_RIGHT, UIElement.FROM_CENTER_BOTTOM);
		this.newStateTf.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		this.newStateTf.getTextUIElement().setDoAntialiasing(false);
		this.newStateTf.bind(newStateDialogueBackgroundRect);

		this._resize();
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
		this.newStateDialogue.update();
	}

	@Override
	protected void renderContent(Framebuffer outputBuffer) {
		this.newStateDialogue.render(outputBuffer, this.getWindowMousePos());
		if (this.newStateDialogue.sectionHovered()) {
			this.hoveredSectionID = this.newStateDialogue.getBackgroundRect().getID();
		}
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
		this.newStateDialogue.mousePressed(button);
	}

	@Override
	protected void _mouseReleased(int button) {
		this.newStateDialogue.mouseReleased(button);

		if (this.hoveredSectionID == this.newStateDialogue.getBackgroundRect().getID()) {
			switch (Input.getClicked(this.newStateDialogue.getSelectionScene())) {
			case "btn_new_state": {
				String newStateName = this.newStateTf.getText();
				if (newStateName.length() == 0) {
					break;
				}

				//create state
				this.project.createStateAsset(newStateName);
				this.close();
				break;
			}
			}
		}
	}

	@Override
	protected void _mouseScrolled(float wheelOffset, float smoothOffset) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _keyPressed(int key) {
		this.newStateDialogue.keyPressed(key);
	}

	@Override
	protected void _keyReleased(int key) {
		this.newStateDialogue.keyReleased(key);
	}

}
