package lwjglengine.window;

import lwjglengine.graphics.Framebuffer;
import lwjglengine.input.Button;
import lwjglengine.input.Input;
import lwjglengine.input.Input.InputCallback;
import lwjglengine.input.TextField;
import lwjglengine.ui.UIElement;
import lwjglengine.ui.UISection;

public class TextEntryWindow extends Window implements InputCallback {

	private TextEntryWindowCallback callback;

	private UISection uiSection;

	private TextField textField;
	private Button submitBtn;

	public TextEntryWindow(TextEntryWindowCallback callback) {
		super(null);
		this.init(callback, "you forgot to set the hint text");
	}

	public TextEntryWindow(TextEntryWindowCallback callback, String hint_text) {
		super(null);
		this.init(callback, hint_text);
	}

	private void init(TextEntryWindowCallback callback, String hint_text) {
		this.callback = callback;

		this.uiSection = new UISection();
		this.uiSection.getBackgroundRect().setFillWidth(true);
		this.uiSection.getBackgroundRect().setFillHeight(true);
		this.uiSection.getBackgroundRect().setMaterial(contentDefaultMaterial);
		this.uiSection.getBackgroundRect().bind(this.rootUIElement);

		this.textField = new TextField(0, 0, 100, 100, "text_field", hint_text, this, this.uiSection.getSelectionScene(), this.uiSection.getTextScene());
		this.textField.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_TOP);
		this.textField.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_CENTER);
		this.textField.setFillWidthMargin(24);
		this.textField.setFillHeightMargin(24);
		this.textField.setFillWidth(true);
		this.textField.setFillHeight(true);
		this.textField.setTextWrapping(true);
		this.textField.getTextUIElement().setDoAntialiasing(false);
		this.textField.bind(this.uiSection.getBackgroundRect());

		this.submitBtn = new Button(2, 2, 100, 20, "submit_btn", "Submit", this, this.uiSection.getSelectionScene(), this.uiSection.getTextScene());
		this.submitBtn.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_BOTTOM);
		this.submitBtn.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_BOTTOM);
		this.submitBtn.getButtonText().setDoAntialiasing(false);
		this.submitBtn.bind(this.uiSection.getBackgroundRect());
	}

	@Override
	protected void _kill() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _resize() {
		// TODO Auto-generated method stub

	}

	@Override
	public String getDefaultTitle() {
		return "Text Entry Window";
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
	}

	@Override
	protected void _mouseScrolled(float wheelOffset, float smoothOffset) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _keyPressed(int key) {
		this.uiSection.keyPressed(key);
	}

	@Override
	protected void _keyReleased(int key) {
		this.uiSection.keyReleased(key);
	}

	public interface TextEntryWindowCallback {
		void handleCallback(String text);
	}

	@Override
	public void inputClicked(String sID) {
		switch (sID) {
		case "submit_btn": {
			this.callback.handleCallback(this.textField.getText());
			this.close();
			break;
		}
		}
	}

	@Override
	public void inputChanged(String sID) {

	}

}
