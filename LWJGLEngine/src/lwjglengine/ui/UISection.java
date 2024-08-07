package lwjglengine.ui;

import java.util.HashSet;

import lwjglengine.graphics.Framebuffer;
import lwjglengine.graphics.Material;
import lwjglengine.input.Input;
import lwjglengine.scene.Scene;
import lwjglengine.screen.UIScreen;
import myutils.math.MathUtils;
import myutils.math.Vec2;
import myutils.math.Vec4;

public class UISection implements UIElementListener {
	//just making ui easier. 
	//idk if this is inefficient, maybe there's a better way to do this. 

	//TODO:

	//finished:
	// - make it so that the user can click and drag the scroll bar
	//   - problem is that with stuff that dynamically loads stuff based off of scroll position, how are we going to notify them?
	//   - i implemented a uiSection listener interface that should notify them :)

	private HashSet<UISectionListener> listeners;

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
	private long hoveredSelectionID, hoveredBackgroundID;

	private boolean doHoverChecks = true;

	private boolean allowInputWhenSectionNotHovered = false;

	//scrolling will modify the offset of the scroll background rect.
	//the min and max scroll distance is based off of the background rect, and scroll background rect's heights. 
	private boolean isScrollable = false;
	private UIFilledRectangle scrollBackgroundRect, scrollBarRect;
	private static int scrollBarWidth = 15; //or height if horizontal scroll is enabled
	private static int minScrollBarHeight = 30;
	private static Material scrollBarMaterial = new Material(new Vec4(1, 1, 1, 0.3f));
	private int scrollOffset = 0;

	private boolean renderScrollBar = true;
	private boolean scrollBarGrabbed = false;
	private int scrollBarGrabDist; //if vertical, it's distance to top of the scroll bar. if horizontal, it's dist to left. 

	//TODO make sure horizontal scrolling actually works as intended
	private boolean isHorizontalScroll = false;

	private Vec2 relMousePos = new Vec2(0); //relative to bottom left corner of background rect

	public UISection() {
		this.init();
	}

	public void init() {
		this.listeners = new HashSet<>();

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

		this.backgroundRect.addListener(this);
		this.scrollBackgroundRect.addListener(this);
	}

	public void addListener(UISectionListener o) {
		this.listeners.add(o);
	}

	public void removeListener(UISectionListener o) {
		this.listeners.remove(o);
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

		this.backgroundRect.removeListener(this);
	}

	public void update() {
		if (this.scrollBarGrabbed) {
			//reverse engineer a scroll offset such that the scroll bar matches with the mouse
			float nextScrollBarOffset, scrollRange, scrollBarRange;
			if (this.isHorizontalScroll) {
				nextScrollBarOffset = (this.relMousePos.x) - this.scrollBarGrabDist;
				nextScrollBarOffset = MathUtils.clamp(0, this.backgroundRect.getWidth(), nextScrollBarOffset);
				scrollRange = Math.max(0, this.scrollBackgroundRect.getWidth() - this.backgroundRect.getWidth());
				scrollBarRange = Math.max(0, this.backgroundRect.getWidth() - this.scrollBarRect.getWidth());
			}
			else {
				nextScrollBarOffset = (this.backgroundRect.getHeight() - this.relMousePos.y) - this.scrollBarGrabDist;
				nextScrollBarOffset = MathUtils.clamp(0, this.backgroundRect.getHeight() - this.scrollBarRect.getHeight(), nextScrollBarOffset);
				scrollRange = Math.max(0, this.scrollBackgroundRect.getHeight() - this.backgroundRect.getHeight());
				scrollBarRange = Math.max(0, this.backgroundRect.getHeight() - this.scrollBarRect.getHeight());
			}
			int nextScrollOffset = (int) (scrollRange * (nextScrollBarOffset / scrollBarRange));
			if (nextScrollOffset != this.scrollOffset) {
				this.setScrollOffset(nextScrollOffset);
			}
		}

		Input.inputsHovered(hoveredSelectionID, SELECTION_SCENE);
	}

	public void setDoHoverChecks(boolean b) {
		this.doHoverChecks = b;
	}

	public void setAllowInputWhenSectionNotHovered(boolean b) {
		this.allowInputWhenSectionNotHovered = b;
	}

	public void setIsScrollable(boolean b) {
		this.isScrollable = b;
		if (this.isScrollable) {
			this.setScrollOffset(this.scrollOffset);
		}
		else {
			this.setScrollOffset(0);
		}
	}

	public void setRenderScrollBar(boolean b) {
		this.renderScrollBar = b;
	}

	public void setIsHorizontalScroll(boolean b) {
		this.isHorizontalScroll = b;

		if (this.isHorizontalScroll) {
			this.scrollBackgroundRect.setFillWidth(false);
			this.scrollBackgroundRect.setFillHeight(true);
			this.scrollBarRect.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_BOTTOM);
			this.scrollBarRect.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_BOTTOM);
			this.scrollBarRect.setWidth(scrollBarWidth);
		}
		else {
			this.scrollBackgroundRect.setFillWidth(true);
			this.scrollBackgroundRect.setFillHeight(false);
			this.scrollBarRect.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_TOP);
			this.scrollBarRect.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_TOP);
			this.scrollBarRect.setHeight(scrollBarWidth);
		}

		if (this.isScrollable) {
			this.setScrollOffset(this.scrollOffset);
		}
	}

	public int getScrollOffset() {
		return this.scrollOffset;
	}

	public UIFilledRectangle getScrollBackgroundRect() {
		return this.scrollBackgroundRect;
	}

	public void setScrollRectHeight(int height) {
		if (this.scrollBackgroundRect.getHeight() == height) {
			return;
		}
		this.scrollBackgroundRect.setHeight(height);
		this.setScrollOffset(this.scrollOffset);
	}

	public void setScrollRectWidth(int width) {
		this.scrollBackgroundRect.setWidth(width);
		this.setScrollOffset(this.scrollOffset);
	}

	public void setScrollOffset(int offset) {
		if (!this.isScrollable) {
			return;
		}

		if (this.isHorizontalScroll) {
			int minOffset = 0;
			int maxOffset = Math.max(0, (int) (this.scrollBackgroundRect.getWidth() - this.backgroundRect.getWidth()));
			int newOffset = MathUtils.clamp(minOffset, maxOffset, offset);
			this.scrollOffset = newOffset;
			this.scrollBackgroundRect.setFrameAlignmentOffset(-this.scrollOffset, 0);

			int scrollBarWidth = (int) (this.backgroundRect.getWidth() * (this.backgroundRect.getWidth() / this.scrollBackgroundRect.getWidth()));
			scrollBarWidth = Math.max(scrollBarWidth, minScrollBarHeight);
			if (scrollBarWidth >= this.backgroundRect.getWidth()) {
				this.scrollBarRect.setMaterial(Material.transparent());
			}
			else {
				this.scrollBarRect.setMaterial(scrollBarMaterial);
				this.scrollBarRect.setWidth(scrollBarWidth);

				int scrollBarOffset = (int) ((this.backgroundRect.getWidth() - scrollBarWidth) * (this.scrollOffset / (this.scrollBackgroundRect.getWidth() - this.backgroundRect.getWidth())));
				this.scrollBarRect.setYOffset(scrollBarOffset);
			}
		}
		else {
			int minOffset = 0;
			int maxOffset = Math.max(0, (int) (this.scrollBackgroundRect.getHeight() - this.backgroundRect.getHeight()));
			int newOffset = MathUtils.clamp(minOffset, maxOffset, offset);
			this.scrollOffset = newOffset;
			this.scrollBackgroundRect.setFrameAlignmentOffset(0, -this.scrollOffset);

			int scrollBarHeight = (int) (this.backgroundRect.getHeight() * (this.backgroundRect.getHeight() / this.scrollBackgroundRect.getHeight()));
			scrollBarHeight = Math.max(scrollBarHeight, minScrollBarHeight);
			this.scrollBarRect.setHeight(scrollBarHeight);
			int scrollBarOffset = (int) ((this.backgroundRect.getHeight() - scrollBarHeight) * (this.scrollOffset / (this.scrollBackgroundRect.getHeight() - this.backgroundRect.getHeight())));
			this.scrollBarRect.setYOffset(scrollBarOffset);

			if (scrollBarHeight >= this.backgroundRect.getHeight()) {
				this.scrollBarRect.setMaterial(Material.transparent());
			}
			else {
				this.scrollBarRect.setMaterial(scrollBarMaterial);
			}
		}

		//notify listeners
		for (UISectionListener o : this.listeners) {
			o.uiSectionScrolled(this);
		}
	}

	public void setViewportOffset(Vec2 offset) {
		this.backgroundScreen.setViewportOffset(offset);
		this.selectionScreen.setViewportOffset(offset);
		this.textScreen.setViewportOffset(offset);
	}

	//mouse pos is relative to whatever screen / window this section is rendered on. 
	public void render(Framebuffer outputBuffer, Vec2 mousePos) {
		this.relMousePos.x = mousePos.x - this.backgroundRect.getAlignedX();
		this.relMousePos.y = mousePos.y - this.backgroundRect.getAlignedY();
		int mouseX = (int) mousePos.x;
		int mouseY = (int) mousePos.y;

		this.backgroundScreen.render(outputBuffer);
		this.selectionScreen.render(outputBuffer);
		this.textScreen.render(outputBuffer);
		if (this.isScrollable && this.renderScrollBar) {
			this.scrollBarScreen.render(outputBuffer);
		}

		this.sectionHovered = this.backgroundScreen.getEntityIDAtCoordDelayed(mouseX, mouseY) == this.backgroundRect.getID();
		this.hoveredBackgroundID = this.backgroundScreen.getEntityIDAtCoordDelayed(mouseX, mouseY);
		this.hoveredSelectionID = this.selectionScreen.getEntityIDAtCoordDelayed(mouseX, mouseY);
		if (this.isScrollable && this.scrollBarScreen.getEntityIDAtCoordDelayed(mouseX, mouseY) == this.scrollBarRect.getID()) {
			this.hoveredSelectionID = this.scrollBarRect.getID();
		}
	}

	public boolean isSectionHovered() {
		return this.sectionHovered;
	}

	public long getHoveredSelectionID() {
		return this.hoveredSelectionID;
	}

	public long getHoveredBackgroundID() {
		return this.hoveredBackgroundID;
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

	public UIScreen getBackgroundScreen() {
		return this.backgroundScreen;
	}

	public UIScreen getSelectionScreen() {
		return this.selectionScreen;
	}

	public UIScreen getTextScreen() {
		return this.textScreen;
	}

	public void mousePressed(int button) {
		if (this.sectionHovered || this.allowInputWhenSectionNotHovered) {
			Input.inputsPressed(this.hoveredSelectionID, SELECTION_SCENE);

			if (this.isScrollable && this.hoveredSelectionID == this.scrollBarRect.getID()) {
				this.scrollBarGrabbed = true;

				if (this.isHorizontalScroll) {
					int mouseDistToLeft = (int) (this.relMousePos.x);
					int scrollBarDistToLeft = (int) (this.scrollBarRect.getXOffset());
					this.scrollBarGrabDist = mouseDistToLeft - scrollBarDistToLeft;
				}
				else {
					int mouseDistToTop = (int) (this.backgroundRect.getHeight() - this.relMousePos.y);
					int scrollBarDistToTop = (int) (this.scrollBarRect.getYOffset());
					this.scrollBarGrabDist = mouseDistToTop - scrollBarDistToTop;
				}
			}
		}
	}

	public void mouseReleased(int button) {
		Input.inputsReleased(this.hoveredSelectionID, SELECTION_SCENE);
		this.scrollBarGrabbed = false;
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

	@Override
	public void uiElementChangedDimensions(UIElement e) {
		if (e == this.backgroundRect) {
			this.setScrollOffset(this.scrollOffset);
		}
		if (e == this.scrollBackgroundRect) {
			this.setScrollOffset(this.scrollOffset);
		}
	}

	@Override
	public void uiElementChangedFrameAlignmentOffset(UIElement e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void uiElementChangedFrameAlignmentStyle(UIElement e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void uiElementChangedContentAlignmentStyle(UIElement e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void uiElementChangedRotationRads(UIElement e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void uiElementChangedGlobalFrameAlignmentOffset(UIElement e) {
		// TODO Auto-generated method stub
		
	}
}
