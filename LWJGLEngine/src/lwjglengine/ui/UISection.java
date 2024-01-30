package lwjglengine.ui;

import lwjglengine.graphics.Framebuffer;
import lwjglengine.graphics.Material;
import lwjglengine.input.Input;
import lwjglengine.scene.Scene;
import lwjglengine.screen.UIScreen;
import myutils.math.MathUtils;
import myutils.math.Vec2;
import myutils.math.Vec4;

public class UISection {
	//just making ui easier. 
	//idk if this is inefficient, maybe there's a better way to do this. 

	private final int BACKGROUND_SCENE = Scene.generateScene();
	private final int SELECTION_SCENE = Scene.generateScene();
	private final int TEXT_SCENE = Scene.generateScene();
	private final int SCROLL_BAR_SCENE = Scene.generateScene();

	//this class IS responsible for removing the ui screen
	private UIScreen backgroundScreen, selectionScreen, textScreen;
	private UIScreen scrollBarScreen;

	//initially, this will be transparent, and not bound to anything. 
	//used for checking if this section is selected. 
	private UIFilledRectangle backgroundRect;

	private boolean sectionHovered = false;
	private long hoveredEntityID;

	private boolean doHoverChecks = true;

	//scrolling will modify the offset of the scroll background rect.
	//the min and max scroll distance is based off of the background rect, and scroll background rect's heights. 
	private boolean isScrollable = false;
	private UIFilledRectangle scrollBackgroundRect, scrollBarRect;
	private static int scrollBarWidth = 15;
	private static int minScrollBarHeight = 30;
	private static Material scrollBarMaterial = new Material(new Vec4(1, 1, 1, 0.3f));
	private int scrollOffset = 0;

	private boolean renderScrollBar = true;

	//TODO implement horizontal scrolling
	private boolean isHorizontalScroll = false;

	public UISection() {
		this.init();
	}

	public void init() {
		this.backgroundScreen = new UIScreen();
		this.selectionScreen = new UIScreen();
		this.textScreen = new UIScreen();
		this.scrollBarScreen = new UIScreen();

		this.backgroundScreen.setUIScene(BACKGROUND_SCENE);
		this.selectionScreen.setUIScene(SELECTION_SCENE);
		this.textScreen.setUIScene(TEXT_SCENE);
		this.scrollBarScreen.setUIScene(SCROLL_BAR_SCENE);

		this.backgroundRect = new UIFilledRectangle(0, 0, 0, 400, 300, BACKGROUND_SCENE);
		this.backgroundRect.setMaterial(Material.transparent());

		//-1 is undefined scene. perhaps this is unintended behaviour?
		//do this so that it doesn't get rendered. 
		this.scrollBackgroundRect = new UIFilledRectangle(0, 0, 0, 400, 300, -1);
		this.scrollBackgroundRect.setFillWidth(true);
		this.scrollBackgroundRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.scrollBackgroundRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		this.scrollBackgroundRect.setMaterial(Material.transparent());
		this.scrollBackgroundRect.bind(this.backgroundRect);

		this.scrollBarRect = new UIFilledRectangle(0, 0, 0, scrollBarWidth, minScrollBarHeight, SCROLL_BAR_SCENE);
		this.scrollBarRect.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_TOP);
		this.scrollBarRect.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_TOP);
		this.scrollBarRect.setMaterial(Material.transparent());
		this.scrollBarRect.bind(this.backgroundRect);
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
		this.scrollBarScreen.setScreenDimensions(width, height);
	}

	public void kill() {
		Scene.removeScene(BACKGROUND_SCENE);
		Scene.removeScene(SELECTION_SCENE);
		Scene.removeScene(TEXT_SCENE);
		Scene.removeScene(SCROLL_BAR_SCENE);

		this.backgroundScreen.kill();
		this.selectionScreen.kill();
		this.textScreen.kill();
		this.scrollBarScreen.kill();
	}

	public void update() {
		Input.inputsHovered(hoveredEntityID, SELECTION_SCENE);

		if (this.isScrollable) {
			//make sure scroll offset is correct. 
			//TODO try not to do this every frame via some event listener or something. 
			this.setScrollOffset(this.scrollOffset);
		}
	}

	public void setDoHoverChecks(boolean b) {
		this.doHoverChecks = b;
	}

	public void setIsScrollable(boolean b) {
		this.isScrollable = b;
	}

	public void setRenderScrollBar(boolean b) {
		this.renderScrollBar = b;
	}

	public void setIsHorizontalScroll(boolean b) {
		this.isHorizontalScroll = b;
	}

	public int getScrollOffset() {
		return this.scrollOffset;
	}

	public UIFilledRectangle getScrollBackgroundRect() {
		return this.scrollBackgroundRect;
	}

	private void setScrollOffset(int offset) {
		int minOffset = 0;
		int maxOffset = Math.max(0, (int) (this.scrollBackgroundRect.getHeight() - this.backgroundRect.getHeight()));
		this.scrollOffset = MathUtils.clamp(minOffset, maxOffset, offset);
		this.scrollBackgroundRect.setYOffset(-this.scrollOffset);

		int scrollBarHeight = (int) (this.backgroundRect.getHeight() * (this.backgroundRect.getHeight() / this.scrollBackgroundRect.getHeight()));
		scrollBarHeight = Math.max(scrollBarHeight, minScrollBarHeight);
		if (scrollBarHeight >= this.backgroundRect.getHeight()) {
			this.scrollBarRect.setMaterial(Material.transparent());
		}
		else {
			this.scrollBarRect.setMaterial(scrollBarMaterial);
			this.scrollBarRect.setHeight(scrollBarHeight);

			int scrollBarOffset = (int) (this.backgroundRect.getHeight() * (this.scrollOffset / this.scrollBackgroundRect.getHeight()));
			this.scrollBarRect.setYOffset(scrollBarOffset);
		}
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
		if (this.isScrollable && this.renderScrollBar) {
			this.scrollBarScreen.render(outputBuffer);
		}

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
		if (this.isScrollable) {
			this.setScrollOffset((int) (this.scrollOffset - smoothOffset * 10.0f));
		}
	}

	public void keyPressed(int key) {
		Input.inputsKeyPressed(key, SELECTION_SCENE);
	}

	public void keyReleased(int key) {
		Input.inputsKeyReleased(key, SELECTION_SCENE);
	}
}
