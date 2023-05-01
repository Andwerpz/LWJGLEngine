package lwjglengine.v10.window;

import java.awt.Color;

import org.lwjgl.glfw.GLFW;

import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.graphics.Material;
import lwjglengine.v10.input.KeyboardInput;
import lwjglengine.v10.input.MouseInput;
import lwjglengine.v10.main.Main;
import lwjglengine.v10.model.Line;
import lwjglengine.v10.model.Model;
import lwjglengine.v10.scene.Scene;
import lwjglengine.v10.screen.UIScreen;
import lwjglengine.v10.ui.Text;
import lwjglengine.v10.ui.UIElement;
import lwjglengine.v10.ui.UIFilledRectangle;
import myutils.v10.math.Vec2;
import myutils.v10.math.Vec3;
import myutils.v10.math.Vec4;

public abstract class AdjustableWindow extends Window {
	//TODO 
	// - implement snapping to windows that are children of the same parent. 
	// - add an x on the top right corner so we can close windows 

	private final int BACKGROUND_SCENE = Scene.generateScene();
	private final int TITLE_BAR_SCENE = Scene.generateScene();
	private final int BORDER_SCENE = Scene.generateScene();

	private UIScreen uiScreen;

	private static int titleBarHeight = 24;
	private UIFilledRectangle titleBarRect;

	private String title;
	private int titleLeftMargin = 5;
	private Text titleBarText;
	private Material deselectedTitleTextMaterial = new Material(new Vec3((float) (205 / 255.0)));
	private Material selectedTitleTextMaterial = new Material(new Vec3((float) (255 / 255.0)));

	//content is always going to be aligned to the bottom. 
	private int contentWidth, contentHeight;
	protected UIFilledRectangle contentRootUIElement;

	private UIFilledRectangle backgroundRect;

	private long[] windowBorder;

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

	private int edgeGrabTolerancePx = 5; //how many pixels away from the edge can you be to still grab it?
	private boolean leftEdgeGrabbed = false;
	private boolean rightEdgeGrabbed = false;
	private boolean bottomEdgeGrabbed = false;
	private boolean topEdgeGrabbed = false;

	//this is true if mouse was pressed, and none of the things were grabbed. 
	private boolean contentSelected = false;

	protected boolean allowInputIfContentNotSelected = false;
	protected boolean allowUpdateIfContentNotSelected = true;
	protected boolean allowRenderIfContentNotSelected = true;

	public AdjustableWindow(int xOffset, int yOffset, int contentWidth, int contentHeight, String title, Window parentWindow) {
		super(xOffset, yOffset, contentWidth, contentHeight + titleBarHeight, parentWindow);

		this.contentWidth = contentWidth;
		this.contentHeight = contentHeight;

		this.uiScreen = new UIScreen();

		this.contentRootUIElement = new UIFilledRectangle(0, 0, -20, this.contentWidth, this.contentHeight, BACKGROUND_SCENE);
		this.contentRootUIElement.setFillWidth(true);
		this.contentRootUIElement.setFillWidthMargin(0);
		this.contentRootUIElement.setFillHeight(true);
		this.contentRootUIElement.setFillHeightMargin(titleBarHeight / 2);
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

		this.titleBarRect = new UIFilledRectangle(0, 0, -9, this.getWidth(), titleBarHeight, TITLE_BAR_SCENE);
		this.titleBarRect.setFillWidth(true);
		this.titleBarRect.setFillWidthMargin(0);
		this.titleBarRect.setMaterial(this.titleBarMaterial);
		this.titleBarRect.bind(this.rootUIElement);
		this.titleBarRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.titleBarRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);

		this.title = title;
		this.titleBarText = new Text(this.titleLeftMargin, 0, this.title, 12, this.deselectedTitleTextMaterial, TITLE_BAR_SCENE);
		this.titleBarText.setDoAntialiasing(false);
		this.titleBarText.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_TOP);
		this.titleBarText.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
		this.titleBarText.bind(this.titleBarRect);

		this.windowBorder = new long[4];

		this._resize();
	}

	@Override
	protected void _kill() {
		this.uiScreen.kill();
		Scene.removeScene(BACKGROUND_SCENE);
		Scene.removeScene(TITLE_BAR_SCENE);
		Scene.removeScene(BORDER_SCENE);

		this.__kill();
	}

	protected abstract void __kill();

	@Override
	protected void _resize() {
		this.contentWidth = this.getWidth();
		this.contentHeight = this.getHeight() - titleBarHeight;

		this.uiScreen.setScreenDimensions(this.getWidth(), this.getHeight());

		this.uiScreen.setViewportWidth(this.getWidth());
		this.uiScreen.setViewportHeight(this.getHeight());

		for (int i = 0; i < 4; i++) {
			if (this.windowBorder[i] == 0) {
				continue;
			}
			Model.removeInstance(this.windowBorder[i]);
		}

		this.windowBorder[0] = Line.addLine(0, 1, this.getWidth(), 1, BORDER_SCENE);
		this.windowBorder[1] = Line.addLine(1, 0, 1, this.getHeight(), BORDER_SCENE);
		this.windowBorder[2] = Line.addLine(this.getWidth(), 0, this.getWidth(), this.getHeight(), BORDER_SCENE);
		this.windowBorder[3] = Line.addLine(0, this.getHeight(), this.getWidth(), this.getHeight(), BORDER_SCENE);

		for (int i = 0; i < 4; i++) {
			Model.updateInstance(this.windowBorder[i], this.isSelected() ? this.selectedBorderMaterial : this.deselectedBorderMaterial);
		}

		this.__resize();
	}

	protected abstract void __resize();

	protected int getContentWidth() {
		return this.contentWidth;
	}

	protected int getContentHeight() {
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

	/**
	 * Returns where the mouse is, but if the mouse is outside of the parent window, then it clamps the position so that 
	 * it is inside the parent window and the current window. 
	 * 
	 * Note that the returned position can still be outside of the current window
	 * 
	 * Behaves just like getRelativeMousePos() in the case where there is no parent window
	 * 
	 * Note that this method clamps to the content, not the bounds of the window
	 * @return
	 */
	protected Vec2 getRelativeMousePosClampedToParentContent() {
		if (this.parentWindow == null) {
			return this.getRelativeMousePos();
		}

		if (!(this.parentWindow instanceof AdjustableWindow)) {
			return this.getRelativeMousePosClampedToParent();
		}

		int mouseX = (int) this.getRelativeMousePos().x;
		int mouseY = (int) this.getRelativeMousePos().y;

		int parentMouseX = (int) (this.parentWindow.getRelativeMousePos().x);
		int parentMouseY = (int) (this.parentWindow.getRelativeMousePos().y);

		if (parentMouseX < 0) {
			mouseX -= parentMouseX;
		}
		else if (parentMouseX > this.parentWindow.getWidth()) {
			mouseX += this.parentWindow.getWidth() - parentMouseX;
		}
		if (parentMouseY < titleBarHeight) {
			mouseY += titleBarHeight - parentMouseY;
		}
		else if (parentMouseY > this.parentWindow.getHeight()) {
			mouseY += this.parentWindow.getHeight() - parentMouseY;
		}

		return new Vec2(mouseX, mouseY);
	}

	@Override
	protected void _update() {
		//update size and / or offset if the edges are grabbed
		{
			int mouseX = (int) this.getRelativeMousePosClampedToParentContent().x;
			int mouseY = (int) this.getRelativeMousePosClampedToParentContent().y;

			int newWidth = this.getWidth();
			int newHeight = this.getHeight();

			int newXOffset = this.getXOffset();
			int newYOffset = this.getYOffset();
			if (this.leftEdgeGrabbed) {
				newXOffset += mouseX;
				newWidth -= mouseX;
				if (newWidth < this.minWidth) {
					newWidth = this.minWidth;
					newXOffset = this.getXOffset() - (this.minWidth - this.getWidth());
				}
			}
			if (this.rightEdgeGrabbed) {
				newWidth = mouseX;
				newWidth = Math.max(newWidth, this.minWidth);
			}
			if (this.topEdgeGrabbed) {
				newHeight -= mouseY;
				newHeight = Math.max(newHeight, this.minHeight);
			}
			if (this.bottomEdgeGrabbed) {
				newHeight = mouseY;
				newYOffset += this.getHeight() - newHeight;
				if (newHeight < this.minHeight) {
					newHeight = this.minHeight;
					newYOffset = this.getYOffset() - (this.minHeight - this.getHeight());
				}
			}

			if (newWidth != this.getWidth() || newHeight != this.getHeight()) {
				this.setDimensions(newWidth, newHeight);
			}
			if (newXOffset != this.getXOffset() || newYOffset != this.getYOffset()) {
				this.setOffset(newXOffset, newYOffset);
			}
		}

		//update offset if the title bar is grabbed
		if (this.titleBarGrabbed) {
			//nesting and un-nesting
			if (KeyboardInput.isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL) || KeyboardInput.isKeyPressed(GLFW.GLFW_KEY_RIGHT_CONTROL)) {
				//check if we should un-nest
				if (this.parentWindow.parentWindow != null) {
					int pMouseX = (int) this.parentWindow.getRelativeMousePos().x;
					int pMouseY = (int) this.parentWindow.getRelativeMousePos().y;

					if (pMouseX < 0 || pMouseY < 0 || pMouseX > this.parentWindow.getWidth() || pMouseY > this.parentWindow.getHeight()) {
						//we should un-nest
						this.switchParent(this.parentWindow.parentWindow);
					}
				}

				//check if we should nest
				{
					int pMouseX = (int) this.parentWindow.getRelativeMousePos().x;
					int pMouseY = (int) this.parentWindow.getRelativeMousePos().y;

					pMouseY = this.parentWindow.getHeight() - pMouseY;

					for (Window w : this.parentWindow.childWindows) {
						if (w == this) {
							continue;
						}
						//check if mouse is inside this window
						int left = w.getXOffset();
						int right = w.getXOffset() + w.getWidth();
						int bottom = w.getYOffset();
						int top = w.getYOffset() + w.getHeight();
						if (left <= pMouseX && pMouseX <= right && bottom <= pMouseY && pMouseY <= top) {
							this.switchParent(w);
							break;
						}
					}
				}
			}
			int mouseX = (int) this.getRelativeMousePosClampedToParentContent().x;
			int mouseY = (int) this.getRelativeMousePosClampedToParentContent().y;

			int newXOffset = this.getXOffset() + (mouseX - this.titleBarGrabMouseX);
			int newYOffset = this.getYOffset() - (mouseY - this.titleBarGrabMouseY);
			this.setOffset(newXOffset, newYOffset);
		}

		//check if top of window goes above content portion of parent screen. If so, clamp window top to top of content. 
		{
			int windowTop = this.getYOffset() + this.getHeight();
			int parentHeight = this.parentWindow.getHeight();
			if (this.parentWindow instanceof AdjustableWindow) {
				parentHeight = ((AdjustableWindow) this.parentWindow).getContentHeight();
			}
			if (windowTop > parentHeight) {
				int diff = parentHeight - windowTop;
				this.setYOffset(this.getYOffset() + diff);
			}
		}

		if (this.contentSelected || this.allowUpdateIfContentNotSelected) {
			this.__update();
		}
	}

	protected abstract void __update();

	@Override
	protected void renderContent(Framebuffer outputBuffer) {
		this.uiScreen.setUIScene(BACKGROUND_SCENE);
		this.uiScreen.render(outputBuffer);

		if (this.contentSelected || this.allowRenderIfContentNotSelected) {
			this._renderContent(outputBuffer);
		}
	}

	@Override
	protected void renderOverlay(Framebuffer outputBuffer) {
		this.uiScreen.setUIScene(TITLE_BAR_SCENE);
		this.uiScreen.render(outputBuffer);

		Vec2 mousePos = this.getRelativeMousePos();
		this.titleBarSceneMouseEntityID = this.uiScreen.getEntityIDAtCoord((int) mousePos.x, (int) (this.getHeight() - mousePos.y));

		this.uiScreen.setUIScene(BORDER_SCENE);
		this.uiScreen.render(outputBuffer);
	}

	protected abstract void _renderContent(Framebuffer outputBuffer);

	public void setTitle(String title) {
		this.title = title;
		this.titleBarText.setText(this.title);
		this.titleBarText.setWidth(this.titleBarText.getTextWidth());
	}

	@Override
	protected void selected() {
		this.titleBarText.setMaterial(this.selectedTitleTextMaterial);
		this.backgroundRect.setMaterial(this.selectedBackgroundMaterial);
		for (int i = 0; i < 4; i++) {
			Model.updateInstance(this.windowBorder[i], this.selectedBorderMaterial);
		}
	}

	protected abstract void contentSelected();

	@Override
	protected void deselected() {
		this.titleBarText.setMaterial(this.deselectedTitleTextMaterial);
		this.backgroundRect.setMaterial(this.deselectedBackgroundMaterial);
		for (int i = 0; i < 4; i++) {
			Model.updateInstance(this.windowBorder[i], this.deselectedBorderMaterial);
		}

		this.contentSelected = false;

		this.contentDeselected();
	}

	protected abstract void contentDeselected();

	protected boolean isContentSelected() {
		return this.contentSelected;
	}

	private boolean canGrabLeftEdge() {
		return Math.abs((int) (this.getRelativeMousePos().x)) <= this.edgeGrabTolerancePx;
	}

	private boolean canGrabRightEdge() {
		return Math.abs((int) (this.getRelativeMousePos().x - this.getWidth())) <= this.edgeGrabTolerancePx;
	}

	private boolean canGrabBottomEdge() {
		return Math.abs((int) (this.getRelativeMousePos().y - this.getHeight())) <= this.edgeGrabTolerancePx;
	}

	private boolean canGrabTopEdge() {
		return Math.abs((int) (this.getRelativeMousePos().y)) <= this.edgeGrabTolerancePx;
	}

	@Override
	protected void _mousePressed(int button) {
		//check if should grab the edge
		this.leftEdgeGrabbed = this.canGrabLeftEdge();
		this.rightEdgeGrabbed = this.canGrabRightEdge();
		this.bottomEdgeGrabbed = this.canGrabBottomEdge();
		this.topEdgeGrabbed = this.canGrabTopEdge();

		//next, check if should grab the title
		if (!(this.leftEdgeGrabbed || this.rightEdgeGrabbed || this.bottomEdgeGrabbed || this.topEdgeGrabbed)) {
			if (this.titleBarSceneMouseEntityID == this.titleBarRect.getID()) {
				this.titleBarGrabbed = true;
				this.titleBarGrabMouseX = (int) this.getRelativeMousePos().x;
				this.titleBarGrabMouseY = (int) this.getRelativeMousePos().y;
			}
		}

		//if we've been selected, and none of the edges are grabbed, then we should select the content
		if (!(this.titleBarGrabbed || this.leftEdgeGrabbed || this.rightEdgeGrabbed || this.bottomEdgeGrabbed || this.topEdgeGrabbed)) {
			if (!this.contentSelected) {
				this.contentSelected();
			}
			this.contentSelected = true;
		}

		if (this.contentSelected || this.allowInputIfContentNotSelected) {
			this.__mousePressed(button);
		}
	}

	protected abstract void __mousePressed(int button);

	@Override
	protected void _mouseReleased(int button) {
		this.leftEdgeGrabbed = false;
		this.rightEdgeGrabbed = false;
		this.bottomEdgeGrabbed = false;
		this.topEdgeGrabbed = false;
		this.titleBarGrabbed = false;

		if (this.contentSelected || this.allowInputIfContentNotSelected) {
			this.__mouseReleased(button);
		}
	}

	protected abstract void __mouseReleased(int button);

	@Override
	protected void _mouseScrolled(float wheelOffset, float smoothOffset) {
		if (this.contentSelected || this.allowInputIfContentNotSelected) {
			this.__mouseScrolled(wheelOffset, smoothOffset);
		}
	}

	protected abstract void __mouseScrolled(float wheelOffset, float smoothOffset);

	@Override
	protected void _keyPressed(int key) {
		if (key == GLFW.GLFW_KEY_ESCAPE) {
			if (this.contentSelected) {
				this.contentDeselected();
			}
			this.contentSelected = false;
		}

		if (this.contentSelected || this.allowInputIfContentNotSelected) {
			this.__keyPressed(key);
		}
	}

	protected abstract void __keyPressed(int key);

	@Override
	protected void _keyReleased(int key) {
		if (this.contentSelected || this.allowInputIfContentNotSelected) {
			this.__keyReleased(key);
		}
	}

	protected abstract void __keyReleased(int key);

}
