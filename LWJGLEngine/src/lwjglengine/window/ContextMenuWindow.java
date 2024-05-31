package lwjglengine.window;

import java.awt.Font;
import java.util.ArrayList;

import lwjglengine.graphics.Framebuffer;
import lwjglengine.graphics.Material;
import lwjglengine.input.Button;
import lwjglengine.input.Input;
import lwjglengine.input.Input.InputCallback;
import lwjglengine.main.Main;
import lwjglengine.model.Line;
import lwjglengine.model.Model;
import lwjglengine.scene.Scene;
import lwjglengine.screen.UIScreen;
import lwjglengine.ui.UIElement;
import lwjglengine.ui.UISection;
import myutils.math.Vec3;
import myutils.math.Vec4;

public class ContextMenuWindow extends BorderedWindow implements InputCallback {

	private static int entryHeight = 20;
	private static int entryTextHorizontalMargin = 5;

	private UIScreen uiScreen;

	private UISection uiSection;

	private ArrayList<String> options;
	private ArrayList<Button> buttons;

	//when an option is pressed, call the callback window's handle context menu action function. 
	private Window callbackWindow;

	//is true after being deselected, or being clicked
	private boolean shouldClose = false;

	public ContextMenuWindow(ArrayList<String> options, Window callbackWindow) {
		super(0, 0, 100, 100, Main.getStateManagerWindow());

		this.callbackWindow = callbackWindow;

		this.options = options;

		this.uiScreen = new UIScreen();

		this.uiSection = new UISection();
		this.uiSection.getBackgroundRect().setFillWidth(true);
		this.uiSection.getBackgroundRect().setFillHeight(true);
		this.uiSection.getBackgroundRect().setMaterial(Material.transparent());
		this.uiSection.getBackgroundRect().bind(rootUIElement);

		int height = this.options.size() * entryHeight;
		int width = 100;

		this.buttons = new ArrayList<>();

		for (int i = 0; i < this.options.size(); i++) {
			Button b = new Button(0, i * entryHeight, 100, entryHeight, this.options.get(i), this.options.get(i), new Font("Dialogue", Font.PLAIN, 12), 12, this, this.uiSection.getSelectionScene(), this.uiSection.getTextScene());
			width = Math.max(width, b.getButtonText().getTextWidth() + entryTextHorizontalMargin * 2);
			b.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
			b.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
			b.setFillWidth(true);
			b.setFillWidthMargin(0);
			b.setHoveredMaterial(new Material(new Vec4(1, 1, 1, 0.5f)));
			b.setPressedMaterial(new Material(new Vec4(1, 1, 1, 0.6f)));
			b.bind(this.rootUIElement);

			b.getButtonText().setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_TOP);
			b.getButtonText().setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
			b.getButtonText().setXOffset(entryTextHorizontalMargin);
			b.getButtonText().setDoAntialiasing(false);

			this.buttons.add(b);
		}

		this.setDimensions(width, height);

		int mouseX = (int) Main.getStateManagerWindow().getGlobalMousePos().x;
		int mouseY = (int) Main.getStateManagerWindow().getGlobalMousePos().y;

		int blX = mouseX;
		int blY = mouseY - this.getHeight(); //make it so that mouse is at top left corner. 

		//first, clamp the bottom of the window to the screen. 
		//Afterwards, we clamp the top, this makes it so that the top gets priority over the bottom

		//clamp bottom
		blY = Math.max(0, blY);

		//clamp top
		blY = Math.min(Main.windowHeight - this.getHeight(), blY);

		//clamp right
		blX = Math.min(Main.windowWidth - this.getWidth(), blX);

		this.setBottomLeftCoords(blX, blY);

		//force select this window. 
		Window.setSelectedWindow(this);
	}

	@Override
	public String getDefaultTitle() {
		return "Context Menu";
	}

	@Override
	protected void __kill() {
		this.uiScreen.kill();
		this.uiSection.kill();
	}

	@Override
	protected void __resize() {
		this.uiScreen.setScreenDimensions(this.getWidth(), this.getHeight());
		this.uiSection.setScreenDimensions(this.getWidth(), this.getHeight());
	}

	@Override
	protected void renderContent(Framebuffer outputBuffer) {
		this.uiSection.render(outputBuffer, this.getWindowMousePos());
	}

	@Override
	protected void _renderOverlay(Framebuffer outputBuffer) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _update() {
		this.uiSection.update();

		if (this.shouldClose) {
			this.kill();
		}
	}

	@Override
	protected void _subtreeSelected() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _subtreeDeselected() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void selected() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void deselected() {
		this.shouldClose = true;
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
		// TODO Auto-generated method stub

	}

	@Override
	protected void _keyReleased(int key) {
		// TODO Auto-generated method stub

	}

	@Override
	public void inputClicked(String sID) {
		this.callbackWindow.handleContextMenuAction(sID);
		this.close();
	}

	@Override
	public void inputChanged(String sID) {

	}

}
