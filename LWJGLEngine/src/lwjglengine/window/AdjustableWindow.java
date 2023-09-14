package lwjglengine.window;

import java.awt.Color;

import org.lwjgl.glfw.GLFW;

import lwjglengine.graphics.Framebuffer;
import lwjglengine.graphics.Material;
import lwjglengine.input.Button;
import lwjglengine.input.Input;
import lwjglengine.input.KeyboardInput;
import lwjglengine.input.MouseInput;
import lwjglengine.main.Main;
import lwjglengine.model.Line;
import lwjglengine.model.Model;
import lwjglengine.scene.Scene;
import lwjglengine.screen.UIScreen;
import lwjglengine.ui.Text;
import lwjglengine.ui.UIElement;
import lwjglengine.ui.UIFilledRectangle;
import myutils.v10.math.Vec2;
import myutils.v10.math.Vec3;
import myutils.v10.math.Vec4;

public class AdjustableWindow extends BorderedWindow {
	//TODO 
	// - implement snapping to windows that are children of the same parent. 
	// - fullscreen

	private final int BACKGROUND_SCENE = Scene.generateScene();

	private final int TITLE_BAR_BACKGROUND_SCENE = Scene.generateScene();
	private final int TITLE_BAR_SELECTION_SCENE = Scene.generateScene();
	private final int TITLE_BAR_TEXT_SCENE = Scene.generateScene();

	private final int BORDER_SCENE = Scene.generateScene();

	private Window contentWindow;

	private UIScreen uiScreen;

	private static int titleBarHeight = 24;
	private UIFilledRectangle titleBarRect;

	private String title;
	private int titleLeftMargin = 5;
	private Text titleBarText;
	private Material deselectedTitleTextMaterial = new Material(new Vec3((float) (205 / 255.0)));
	private Material selectedTitleTextMaterial = new Material(new Vec3((float) (255 / 255.0)));

	private Button titleBarCloseBtn;

	//content is always going to be aligned to the bottom. 
	private int contentWidth, contentHeight;
	protected UIFilledRectangle contentRootUIElement;

	private UIFilledRectangle backgroundRect;

	private Material titleBarMaterial = new Material(new Vec3((float) (15 / 255.0)));

	private Material deselectedBackgroundMaterial = new Material(new Vec3((float) (25 / 255.0)));
	private Material selectedBackgroundMaterial = new Material(new Vec3((float) (35 / 255.0)));

	private Material deselectedBorderMaterial = new Material(new Vec3((float) (55 / 255.0)));
	private Material selectedBorderMaterial = new Material(new Vec3((float) (65 / 255.0)));

	private static int defaultMinWidth = 50;
	private static int defaultMinHeight = titleBarHeight * 2;

	private int minWidth = defaultMinWidth;
	private int minHeight = defaultMinHeight;

	//if an edge is grabbed, then the title bar cannot be grabbed, and vice versa
	private boolean titleBarGrabbed = false;
	private long titleBarSceneMouseEntityID = -1;
	private int titleBarGrabMouseX, titleBarGrabMouseY;

	//how many pixels away from the edge can you be to still grab it?
	//the edges are grabbed from the outside
	private int edgeGrabTolerancePx = 10;
	private boolean leftEdgeGrabbed = false;
	private boolean rightEdgeGrabbed = false;
	private boolean bottomEdgeGrabbed = false;
	private boolean topEdgeGrabbed = false;

	//are these variables unused??
	protected boolean allowInputIfContentNotSelected = false;
	protected boolean allowUpdateIfContentNotSelected = true;
	protected boolean allowRenderIfContentNotSelected = true;

	private boolean renderBackground = true;
	private boolean renderTopBar = true;

	private boolean isFullscreen = false;

	//whether or not the user can resize this window,
	//more specifically, this just enables and disables the ability to grab onto an edge of the window. 
	private boolean allowManualResizing = true;

	public AdjustableWindow(int xOffset, int yOffset, int contentWidth, int contentHeight, String title, Window contentWindow, Window parentWindow) {
		super(xOffset, yOffset, contentWidth, contentHeight + titleBarHeight, parentWindow);
		this.init(contentWindow, title);
	}

	public AdjustableWindow(String title, Window contentWindow, Window parentWindow) {
		super(0, 0, contentWindow.getWidth(), contentWindow.getHeight() + titleBarHeight, parentWindow);
		this.setOffset(contentWindow.getXOffset(), contentWindow.getYOffset());
		this.setAlignmentStyle(contentWindow.getHorizontalAlignmentStyle(), contentWindow.getVerticalAlignmentStyle());
		this.init(contentWindow, title);
	}

	public AdjustableWindow(Window contentWindow, Window parentWindow) {
		super(0, 0, contentWindow.getWidth(), contentWindow.getHeight() + titleBarHeight, parentWindow);
		this.setOffset(contentWindow.getXOffset(), contentWindow.getYOffset());
		this.setAlignmentStyle(contentWindow.getHorizontalAlignmentStyle(), contentWindow.getVerticalAlignmentStyle());
		this.init(contentWindow, contentWindow.getDefaultTitle());
	}

	private void init(Window contentWindow, String title) {
		this.contentWindow = contentWindow;
		this.contentWindow.setParent(this);

		this.contentWindow.setDimensions(contentWidth, contentHeight);
		this.contentWindow.setAlignmentStyle(Window.FROM_LEFT, Window.FROM_BOTTOM);
		this.contentWindow.setBottomLeftCoords(0, 0);

		this.setAllowModifyingChildren(false);

		this.uiScreen = new UIScreen();

		this.contentRootUIElement = new UIFilledRectangle(0, 0, -20, this.getContentWidth(), this.getContentHeight(), BACKGROUND_SCENE);
		this.contentRootUIElement.setFillWidth(true);
		this.contentRootUIElement.setFillWidthMargin(0);
		this.contentRootUIElement.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_BOTTOM);
		this.contentRootUIElement.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_BOTTOM);
		this.contentRootUIElement.setMaterial(new Material(new Vec4(0)));
		this.contentRootUIElement.bind(this.rootUIElement);

		this.backgroundRect = new UIFilledRectangle(0, 0, -10, this.getWidth(), this.getHeight(), BACKGROUND_SCENE);
		this.backgroundRect.setFillWidth(true);
		this.backgroundRect.setFillHeight(true);
		this.backgroundRect.setFillWidthMargin(0);
		this.backgroundRect.setFillHeightMargin(0);
		this.backgroundRect.setMaterial(this.deselectedBackgroundMaterial);
		this.backgroundRect.bind(this.rootUIElement);

		this.titleBarRect = new UIFilledRectangle(0, 0, -9, this.getWidth(), titleBarHeight, TITLE_BAR_BACKGROUND_SCENE);
		this.titleBarRect.setFillWidth(true);
		this.titleBarRect.setFillWidthMargin(0);
		this.titleBarRect.setMaterial(this.titleBarMaterial);
		this.titleBarRect.bind(this.rootUIElement);
		this.titleBarRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.titleBarRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);

		this.title = title;
		this.titleBarText = new Text(this.titleLeftMargin, 0, this.title, 12, this.deselectedTitleTextMaterial, TITLE_BAR_TEXT_SCENE);
		this.titleBarText.setDoAntialiasing(false);
		this.titleBarText.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_TOP);
		this.titleBarText.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
		this.titleBarText.bind(this.titleBarRect);

		this.titleBarCloseBtn = new Button(0, 0, titleBarHeight, titleBarHeight, "btn_close", "X", 12, TITLE_BAR_SELECTION_SCENE, TITLE_BAR_TEXT_SCENE);
		this.titleBarCloseBtn.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_TOP);
		this.titleBarCloseBtn.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_TOP);
		this.titleBarCloseBtn.getButtonText().setDoAntialiasing(false);
		this.titleBarCloseBtn.setReleasedMaterial(this.titleBarMaterial);
		this.titleBarCloseBtn.setPressedMaterial(this.selectedBackgroundMaterial);
		this.titleBarCloseBtn.setHoveredMaterial(this.deselectedBackgroundMaterial);
		this.titleBarCloseBtn.bind(this.titleBarRect);

		this._resize();
	}

	@Override
	public String getDefaultTitle() {
		return "Adjustable Window";
	}

	@Override
	protected void __kill() {
		this.uiScreen.kill();
		Scene.removeScene(BACKGROUND_SCENE);
		Scene.removeScene(TITLE_BAR_BACKGROUND_SCENE);
		Scene.removeScene(TITLE_BAR_SELECTION_SCENE);
		Scene.removeScene(TITLE_BAR_TEXT_SCENE);
		Scene.removeScene(BORDER_SCENE);
	}

	public void setFullscreen(boolean b) {
		if (this.isFullscreen == b) {
			return;
		}
		this.isFullscreen = b;

		if (this.isFullscreen) {
			this.renderTopBar = false;

			this.setDimensions(this.parentWindow.getWidth(), this.parentWindow.getHeight());
		}
		else {
			this.renderTopBar = true;

			this.__resize();
		}
	}

	@Override
	protected void __resize() {
		this.contentWidth = this.getWidth();
		this.contentHeight = this.getHeight();
		if (!this.isFullscreen) {
			this.contentHeight = this.getHeight() - titleBarHeight;
		}

		this.contentWindow.setDimensions(this.contentWidth, this.contentHeight);

		this.uiScreen.setScreenDimensions(this.getWidth(), this.getHeight());

		this.uiScreen.setViewportWidth(this.getWidth());
		this.uiScreen.setViewportHeight(this.getHeight());

		this.contentRootUIElement.setHeight(this.getContentHeight());
	}

	public Window getContentWindow() {
		return this.contentWindow;
	}

	public int getContentWidth() {
		return this.contentWidth;
	}

	public int getContentHeight() {
		return this.contentHeight;
	}

	public void setMinWidth(int width) {
		this.setMinDimensions(width, this.minHeight);
	}

	public void setMinHeight(int height) {
		this.setMinDimensions(this.minWidth, height);
	}

	public void setMinDimensions(int width, int height) {
		this.minWidth = Math.max(defaultMinWidth, width);
		this.minHeight = Math.max(defaultMinHeight, height + titleBarHeight);

		if (this.getWidth() < this.minWidth || this.getHeight() < this.minHeight) {
			this.setDimensions(Math.max(this.getWidth(), this.minWidth), Math.max(this.getHeight(), this.minHeight));
		}
	}

	public void setAllowManualResizing(boolean b) {
		this.allowManualResizing = b;
	}

	public boolean allowManualResizing() {
		return this.allowManualResizing;
	}

	@Override
	protected void _update() {
		//check if should close
		if (!this.contentWindow.isAlive()) {
			this.close();
		}

		Input.inputsHovered(this.titleBarSceneMouseEntityID, TITLE_BAR_SELECTION_SCENE);

		//update size and / or offset if the edges are grabbed
		{
			int mouseX = (int) this.getWindowMousePosClampedToParentWindow().x;
			int mouseY = (int) this.getWindowMousePosClampedToParentWindow().y;

			int newWidth = this.getWidth();
			int newHeight = this.getHeight();

			int newXOffset = this.getAlignedX();
			int newYOffset = this.getAlignedY();
			if (this.leftEdgeGrabbed) {
				newXOffset += mouseX;
				newWidth -= mouseX;
				if (newWidth < this.minWidth) {
					newWidth = this.minWidth;
					newXOffset = this.getAlignedX() - (this.minWidth - this.getWidth());
				}
			}
			if (this.rightEdgeGrabbed) {
				newWidth = mouseX;
				newWidth = Math.max(newWidth, this.minWidth);
			}
			if (this.bottomEdgeGrabbed) {
				newYOffset += mouseY;
				newHeight -= mouseY;
				if (newHeight < this.minHeight) {
					newHeight = this.minHeight;
					newYOffset = this.getAlignedY() - (this.minHeight - this.getHeight());
				}
			}
			if (this.topEdgeGrabbed) {
				newHeight = mouseY;
				newHeight = Math.max(newHeight, this.minHeight);
			}

			if (newWidth != this.getWidth() || newHeight != this.getHeight()) {
				this.setDimensions(newWidth, newHeight);
			}
			if (newXOffset != this.getAlignedX() || newYOffset != this.getAlignedY()) {
				this.setBottomLeftCoords(newXOffset, newYOffset);
			}
		}

		//update offset if the title bar is grabbed
		if (this.titleBarGrabbed) {
			//nesting and un-nesting
			if (KeyboardInput.isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL) || KeyboardInput.isKeyPressed(GLFW.GLFW_KEY_RIGHT_CONTROL)) {
				//check if we should un-nest
				{
					//find furthest window that we can un-nest to
					Window curParent = this.parentWindow;
					while (curParent.parentWindow != null) {
						int pMouseX = (int) curParent.getWindowMousePos().x;
						int pMouseY = (int) curParent.getWindowMousePos().y;

						if (pMouseX < 0 || pMouseY < 0 || pMouseX > curParent.getWidth() || pMouseY > curParent.getHeight()) {
							//we should un-nest
							curParent = curParent.parentWindow;
						}
						else {
							break;
						}
					}
					this.setParent(curParent);
				}

				//check if we should nest
				//find deepest window that we can nest to
				{
					Window curParent = this.parentWindow;
					while (true) {
						int pMouseX = (int) curParent.getWindowMousePos().x;
						int pMouseY = (int) curParent.getWindowMousePos().y;

						boolean foundNewParent = false;
						for (Window w : curParent.childWindows) {
							if (w == this) {
								continue;
							}

							//check if mouse is inside this window
							int left = w.getAlignedX();
							int right = w.getAlignedX() + w.getWidth();
							int bottom = w.getAlignedY();
							int top = w.getAlignedY() + w.getHeight();
							if (left <= pMouseX && pMouseX <= right && bottom <= pMouseY && pMouseY <= top) {
								foundNewParent = true;
								curParent = w;
								break;
							}
						}
						if (!foundNewParent) {
							break;
						}
					}
					this.setParent(curParent);
				}
			}
			int mouseX = (int) this.getWindowMousePosClampedToParentWindow().x;
			int mouseY = (int) this.getWindowMousePosClampedToParentWindow().y;

			int newXOffset = this.getAlignedX() + (mouseX - this.titleBarGrabMouseX);
			int newYOffset = this.getAlignedY() + (mouseY - this.titleBarGrabMouseY);
			this.setBottomLeftCoords(newXOffset, newYOffset);
		}

		//check if top of window goes above content portion of parent screen. If so, clamp window top to top of content. 
		{
			int windowTop = this.getAlignedY() + this.getHeight();
			int parentHeight = this.parentWindow.getHeight();
			if (this.parentWindow instanceof AdjustableWindow) {
				parentHeight = ((AdjustableWindow) this.parentWindow).getContentHeight();
			}
			if (windowTop > parentHeight) {
				int diff = parentHeight - windowTop;
				this.setBottomLeftY(this.getAlignedY() + diff);
			}
		}
	}

	@Override
	protected void renderContent(Framebuffer outputBuffer) {
		if (this.renderBackground) {
			this.uiScreen.setUIScene(BACKGROUND_SCENE);
			this.uiScreen.render(outputBuffer);
		}
	}

	@Override
	protected void _renderOverlay(Framebuffer outputBuffer) {
		if (this.renderTopBar) {
			this.uiScreen.setUIScene(TITLE_BAR_BACKGROUND_SCENE);
			this.uiScreen.render(outputBuffer);
			this.uiScreen.setUIScene(TITLE_BAR_SELECTION_SCENE);
			this.uiScreen.render(outputBuffer);
			Vec2 mousePos = this.getWindowMousePos();
			this.titleBarSceneMouseEntityID = this.uiScreen.getEntityIDAtCoord((int) mousePos.x, (int) (mousePos.y));
			this.uiScreen.setUIScene(TITLE_BAR_TEXT_SCENE);
			this.uiScreen.render(outputBuffer);
		}
	}

	public void setTitle(String title) {
		this.title = title;
		this.titleBarText.setText(this.title);
		this.titleBarText.setWidth(this.titleBarText.getTextWidth());
	}

	@Override
	protected boolean isWindowClicked(int x, int y) {
		if (super.isWindowClicked(x, y)) {
			return true;
		}
		return this.canGrabLeftEdge() || this.canGrabRightEdge() || this.canGrabTopEdge() || this.canGrabBottomEdge();
	}

	@Override
	protected void selected() {

	}

	@Override
	protected void deselected() {

	}

	@Override
	protected void _subtreeSelected() {
		this.titleBarText.setMaterial(this.selectedTitleTextMaterial);
		this.backgroundRect.setMaterial(this.selectedBackgroundMaterial);
	}

	@Override
	protected void _subtreeDeselected() {
		this.titleBarText.setMaterial(this.deselectedTitleTextMaterial);
		this.backgroundRect.setMaterial(this.deselectedBackgroundMaterial);
	}

	private boolean canGrabLeftEdge() {
		int mx = (int) this.getWindowMousePos().x;
		int my = (int) this.getWindowMousePos().y;
		return -this.edgeGrabTolerancePx <= mx && mx <= 0 && -this.edgeGrabTolerancePx <= my && my <= this.edgeGrabTolerancePx + this.getHeight();
	}

	private boolean canGrabRightEdge() {
		int mx = this.getWidth() - (int) this.getWindowMousePos().x;
		int my = (int) this.getWindowMousePos().y;
		return -this.edgeGrabTolerancePx <= mx && mx <= 0 && -this.edgeGrabTolerancePx <= my && my <= this.edgeGrabTolerancePx + this.getHeight();
	}

	private boolean canGrabBottomEdge() {
		int my = (int) this.getWindowMousePos().y;
		int mx = (int) this.getWindowMousePos().x;
		return -this.edgeGrabTolerancePx <= my && my <= 0 && -this.edgeGrabTolerancePx <= mx && mx <= this.edgeGrabTolerancePx + this.getWidth();
	}

	private boolean canGrabTopEdge() {
		int my = this.getHeight() - (int) this.getWindowMousePos().y;
		int mx = (int) this.getWindowMousePos().x;
		return -this.edgeGrabTolerancePx <= my && my <= 0 && -this.edgeGrabTolerancePx <= mx && mx <= this.edgeGrabTolerancePx + this.getWidth();
	}

	@Override
	protected void _mousePressed(int button) {
		Input.inputsPressed(this.titleBarSceneMouseEntityID, TITLE_BAR_SELECTION_SCENE);

		//check if should grab the edge
		this.leftEdgeGrabbed = false;
		this.rightEdgeGrabbed = false;
		this.bottomEdgeGrabbed = false;
		this.topEdgeGrabbed = false;

		if (this.allowManualResizing()) {
			this.leftEdgeGrabbed = this.canGrabLeftEdge();
			this.rightEdgeGrabbed = this.canGrabRightEdge();
			this.bottomEdgeGrabbed = this.canGrabBottomEdge();
			this.topEdgeGrabbed = this.canGrabTopEdge();
		}

		//next, check if should grab the title
		if (!(this.leftEdgeGrabbed || this.rightEdgeGrabbed || this.bottomEdgeGrabbed || this.topEdgeGrabbed)) {
			if (this.titleBarSceneMouseEntityID == this.titleBarRect.getID()) {
				this.titleBarGrabbed = true;
				this.titleBarGrabMouseX = (int) this.getWindowMousePos().x;
				this.titleBarGrabMouseY = (int) this.getWindowMousePos().y;
			}
		}
	}

	@Override
	protected void _mouseReleased(int button) {
		Input.inputsReleased(this.titleBarSceneMouseEntityID, TITLE_BAR_SELECTION_SCENE);

		switch (Input.getClicked(TITLE_BAR_SELECTION_SCENE)) {
		case "btn_close":
			this.close();
			break;
		}

		this.leftEdgeGrabbed = false;
		this.rightEdgeGrabbed = false;
		this.bottomEdgeGrabbed = false;
		this.topEdgeGrabbed = false;
		this.titleBarGrabbed = false;
	}

	@Override
	protected void _mouseScrolled(float wheelOffset, float smoothOffset) {

	}

	@Override
	protected void _keyPressed(int key) {

	}

	@Override
	protected void _keyReleased(int key) {

	}

}
