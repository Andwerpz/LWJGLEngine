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

	//this class isn't responsible for removing the ui screen. 
	private UIScreen uiScreen;

	//initially, this will be transparent, and not bound to anything. 
	//used for checking if this section is selected. 
	private UIFilledRectangle backgroundRect;

	private boolean sectionHovered = false;
	private long hoveredEntityID;

	public UISection(int x, int y, int width, int height, UIScreen uiScreen) {
		this.init(x, y, width, height, uiScreen);
	}

	public void init(int x, int y, int width, int height, UIScreen uiScreen) {
		this.uiScreen = uiScreen;

		this.backgroundRect = new UIFilledRectangle(x, y, 0, width, height, BACKGROUND_SCENE);
		this.backgroundRect.setMaterial(new Material(new Vec4(0)));
	}

	public void kill() {
		Scene.removeScene(BACKGROUND_SCENE);
		Scene.removeScene(SELECTION_SCENE);
		Scene.removeScene(TEXT_SCENE);
	}

	public void update() {
		Input.inputsHovered(hoveredEntityID, SELECTION_SCENE);
	}

	public void render(Framebuffer outputBuffer, Vec2 mousePos) {
		int mouseX = (int) mousePos.x;
		int mouseY = (int) mousePos.y;

		this.uiScreen.setUIScene(BACKGROUND_SCENE);
		this.uiScreen.render(outputBuffer);
		this.sectionHovered = this.uiScreen.getEntityIDAtCoord(mouseX, mouseY) == this.backgroundRect.getID();
		this.uiScreen.setUIScene(SELECTION_SCENE);
		this.uiScreen.render(outputBuffer);
		this.hoveredEntityID = this.uiScreen.getEntityIDAtCoord(mouseX, mouseY);
		this.uiScreen.setUIScene(TEXT_SCENE);
		this.uiScreen.render(outputBuffer);
	}

	public boolean sectionHovered() {
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
		Input.inputsPressed(this.hoveredEntityID, SELECTION_SCENE);
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
