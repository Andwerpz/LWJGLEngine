package lwjglengine.window;

import java.awt.Font;
import java.util.ArrayList;

import lwjglengine.graphics.Framebuffer;
import lwjglengine.graphics.Material;
import lwjglengine.input.Button;
import lwjglengine.input.Input;
import lwjglengine.main.Main;
import lwjglengine.model.Line;
import lwjglengine.model.Model;
import lwjglengine.scene.Scene;
import lwjglengine.screen.UIScreen;
import lwjglengine.ui.UIElement;
import myutils.v10.math.Vec3;
import myutils.v10.math.Vec4;

public class ContextMenuWindow extends BorderedWindow {

	private static int entryHeight = 20;
	private static int entryTextHorizontalMargin = 5;

	private final int BACKGROUND_SCENE = Scene.generateScene();
	private final int SELECTION_SCENE = Scene.generateScene();
	private final int TEXT_SCENE = Scene.generateScene();

	private UIScreen uiScreen;

	private ArrayList<String> options;
	private ArrayList<Button> buttons;

	private long hoveredInputID;

	//when an option is pressed, call the callback window's handle context menu action function. 
	private Window callbackWindow;

	//is true after being deselected, or being clicked
	private boolean shouldClose = false;

	public ContextMenuWindow(ArrayList<String> options, Window callbackWindow) {
		super(0, 0, 100, 100, Main.getStateManagerWindow());

		this.callbackWindow = callbackWindow;

		this.options = options;

		this.uiScreen = new UIScreen();

		int height = this.options.size() * entryHeight;
		int width = 100;

		this.buttons = new ArrayList<>();

		for (int i = 0; i < this.options.size(); i++) {
			Button b = new Button(0, i * entryHeight, 100, entryHeight, "btn " + this.options.get(i), this.options.get(i), new Font("Dialogue", Font.PLAIN, 12), 12, SELECTION_SCENE, TEXT_SCENE);
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

		//this is kinda jank, because it violates the principle that only one window can be selected at a time.
		//it is required that this window is initially selected tho, because we want to know if the user clicked away from it. 
		//perhaps some sort of event on click that unselected windows have?
		//or maybe some way to force select a specific window. 
		//TODO decide whether or not to fix this. 
		this.select();
	}

	@Override
	public String getDefaultTitle() {
		return "Context Menu";
	}

	@Override
	protected void __kill() {
		this.uiScreen.kill();

		Scene.removeScene(BACKGROUND_SCENE);
		Scene.removeScene(SELECTION_SCENE);
		Scene.removeScene(TEXT_SCENE);
	}

	@Override
	protected void __resize() {
		this.uiScreen.setScreenDimensions(this.getWidth(), this.getHeight());
	}

	@Override
	protected void renderContent(Framebuffer outputBuffer) {
		int mouseX = (int) this.getWindowMousePos().x;
		int mouseY = (int) this.getWindowMousePos().y;

		this.uiScreen.setUIScene(BACKGROUND_SCENE);
		this.uiScreen.render(outputBuffer);
		this.uiScreen.setUIScene(SELECTION_SCENE);
		this.uiScreen.render(outputBuffer);
		this.hoveredInputID = this.uiScreen.getEntityIDAtCoord(mouseX, mouseY);
		this.uiScreen.setUIScene(TEXT_SCENE);
		this.uiScreen.render(outputBuffer);
	}

	@Override
	protected void _renderOverlay(Framebuffer outputBuffer) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _update() {
		Input.inputsHovered(this.hoveredInputID, SELECTION_SCENE);

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
		Input.inputsPressed(this.hoveredInputID, SELECTION_SCENE);
	}

	@Override
	protected void _mouseReleased(int button) {
		Input.inputsReleased(this.hoveredInputID, SELECTION_SCENE);

		String which = Input.getClicked(SELECTION_SCENE);

		if (which != "") {
			this.callbackWindow.handleContextMenuAction(which.substring(which.indexOf(" ") + 1));
			this.shouldClose = true;
		}
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
