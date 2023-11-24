package lwjglengine.ui;

import lwjglengine.graphics.Framebuffer;
import lwjglengine.graphics.Material;
import lwjglengine.input.Input;
import lwjglengine.scene.Scene;
import lwjglengine.screen.UIScreen;
import myutils.math.Vec2;
import myutils.math.Vec4;

public class UISection {
	//just making ui easier. 
	//idk if this is inefficient, maybe there's a better way to do this. 

	private final int BACKGROUND_SCENE = Scene.generateScene();
	private final int SELECTION_SCENE = Scene.generateScene();
	private final int TEXT_SCENE = Scene.generateScene();

	//	//this class isn't responsible for removing the ui screen. 
	//	private UIScreen uiScreen;

	private UIScreen backgroundScreen, selectionScreen, textScreen;

	//initially, this will be transparent, and not bound to anything. 
	//used for checking if this section is selected. 
	private UIFilledRectangle backgroundRect;

	private boolean sectionHovered = false;
	private long hoveredEntityID;

	private boolean doHoverChecks = true;

	public UISection() {
		this.init();
	}

	public void init() {
		this.backgroundScreen = new UIScreen();
		this.selectionScreen = new UIScreen();
		this.textScreen = new UIScreen();

		this.backgroundScreen.setUIScene(BACKGROUND_SCENE);
		this.selectionScreen.setUIScene(SELECTION_SCENE);
		this.textScreen.setUIScene(TEXT_SCENE);

		this.backgroundRect = new UIFilledRectangle(0, 0, 0, 400, 300, BACKGROUND_SCENE);
		this.backgroundRect.setMaterial(new Material(new Vec4(0)));
	}

	/**
	 * Resizes the underlying UIScreens. 
	 * You have to handle the background rect seperately though
	 * @param width
	 * @param height
	 */
	public void setScreenDimensions(int width, int height) {
		this.backgroundScreen.setScreenDimensions(width, height);
		this.selectionScreen.setScreenDimensions(width, height);
		this.textScreen.setScreenDimensions(width, height);
	}

	public void kill() {
		Scene.removeScene(BACKGROUND_SCENE);
		Scene.removeScene(SELECTION_SCENE);
		Scene.removeScene(TEXT_SCENE);
	}

	public void update() {
		Input.inputsHovered(hoveredEntityID, SELECTION_SCENE);
	}

	public void setDoHoverChecks(boolean b) {
		this.doHoverChecks = b;
	}

	public void setViewportOffset(Vec2 offset) {
		this.backgroundScreen.setViewportOffset(offset);
		this.selectionScreen.setViewportOffset(offset);
		this.textScreen.setViewportOffset(offset);
	}

	public void render(Framebuffer outputBuffer, Vec2 mousePos) {
		int mouseX = (int) mousePos.x;
		int mouseY = (int) mousePos.y;

		this.backgroundScreen.render(outputBuffer);
		this.selectionScreen.render(outputBuffer);
		this.textScreen.render(outputBuffer);

		this.sectionHovered = this.backgroundScreen.getEntityIDAtCoordDelayed(mouseX, mouseY) == this.backgroundRect.getID();
		this.hoveredEntityID = this.selectionScreen.getEntityIDAtCoordDelayed(mouseX, mouseY);
	}

	public boolean isSectionHovered() {
		return this.sectionHovered;
	}

	public long getHoveredEntityID() {
		return this.hoveredEntityID;
	}

	public UIFilledRectangle getBackgroundRect() {
		return this.backgroundRect;
	}

	public int getBackgroundScene() {
		return BACKGROUND_SCENE;
	}

	public int getSelectionScene() {
		return SELECTION_SCENE;
	}

	public int getTextScene() {
		return TEXT_SCENE;
	}

	public void mousePressed(int button) {
		if (this.sectionHovered) {
			Input.inputsPressed(this.hoveredEntityID, SELECTION_SCENE);
		}
	}

	public void mouseReleased(int button) {
		Input.inputsReleased(this.hoveredEntityID, SELECTION_SCENE);
	}

	public void mouseScrolled(float wheelOffset, float smoothOffset) {

	}

	public void keyPressed(int key) {
		Input.inputsKeyPressed(key, SELECTION_SCENE);
	}

	public void keyReleased(int key) {
		Input.inputsKeyReleased(key, SELECTION_SCENE);
	}
}
