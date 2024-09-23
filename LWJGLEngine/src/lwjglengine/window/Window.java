package lwjglengine.window;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import lwjglengine.graphics.Framebuffer;
import lwjglengine.graphics.Shader;
import lwjglengine.graphics.Texture;
import lwjglengine.input.MouseInput;
import lwjglengine.main.Main;
import lwjglengine.model.FilledRectangle;
import lwjglengine.model.Model;
import lwjglengine.player.Camera;
import lwjglengine.scene.Scene;
import lwjglengine.screen.ScreenQuad;
import lwjglengine.screen.UIScreen;
import lwjglengine.ui.UIFilledRectangle;
import lwjglengine.util.BufferUtils;
import myutils.math.Mat4;
import myutils.math.MathUtils;
import myutils.math.Vec2;
import myutils.misc.Pair;

public abstract class Window {
	//A window should be a container for rendering. 
	//should be relatively modular. 

	//how should this class interact with states?
	//should the state call upon this class, or should this class be a container for the state?

	//perhaps we need to do away with the notion of a state. 

	//the problem is that how are we going to decide when to put up a loading screen without states?

	//TODO
	// - drop shadow?

	//all these alignments will just align the nearest edge with the side of the parent window
	public static final int FROM_LEFT = 0;
	public static final int FROM_RIGHT = 1;
	public static final int FROM_TOP = 2;
	public static final int FROM_BOTTOM = 3;

	private static int defaultHorizontalAlignmentStyle = FROM_LEFT;
	private static int defaultVerticalAlignmentStyle = FROM_TOP;

	//keep track of the selected window here. 
	public static Window selectedWindow = null;
	public static Window hoveredWindow = null;

	private final int ROOT_UI_SCENE = Scene.generateScene();

	private int width, height;
	private boolean fillWidth = false;
	private boolean fillHeight = false;

	private int xOffset, yOffset;

	//offset from GLFW window
	//useful for determining where the mouse is
	private int globalXOffset, globalYOffset;

	//offset of bottom left corner from specified parent window's bottom left corner
	private int alignedX, alignedY;

	private int horizontalAlignStyle, verticalAlignStyle;

	private Framebuffer colorBuffer;
	private Texture colorTexture;

	protected Window parentWindow;
	protected ArrayList<Window> childWindows;
	private boolean allowModifyingChildren = true; //allows modification of the childWindows list

	//every ui element on this window should be a child of the root ui element. 
	protected UIFilledRectangle rootUIElement;

	//there should only be 1 window selected at a time. 
	private boolean isSelected = false;

	//true if this window, or a child of this window is selected. 
	private boolean isSubtreeSelected = false;

	//if this thing is dead, then it is not alive
	private boolean isAlive = true;

	private boolean updateWhenNotSelected = true;
	private boolean renderWhenNotSelected = true;

	//if this is false, then no inputs will get to this window, regardless of if it's selected. 
	private boolean allowInput = true;

	//if this is true, then input is allowed if this window, or some child of this window is selected
	private boolean allowInputWhenSubtreeSelected = false;

	//if this is true, input is allowed regardless of selected status
	private boolean allowInputWhenNotSelected = false;

	//if this is true, input is allowed if the parent or some child of the parent window is selected. 
	private boolean allowInputWhenParentSubtreeSelected = false;

	//if this is true, mouse input will be allowed regardless of whether or not this window is hovered. 
	private boolean allowMouseInputWhenNotHovered = false;

	//for debugging
	public boolean renderAlpha = false;

	//if true, right clicking will create a context menu
	private boolean contextMenuRightClick = false;

	private ArrayList<String> contextMenuOptions;

	private ContextMenuWindow contextMenuWindow = null;

	//if true, then it will close on the next update. 
	private boolean shouldClose = false;

	//is the cursor locked?
	//TODO try to clamp the cursor to the window using glfwSetCursorPos so that the user can't click out of the window 
	//when the cursor is locked. 

	//alternatively, make it so that you can't change the selected window when the cursor is locked. 

	//ok, every time we call the selectWindow function, it first checks to make sure that Main.isCursorLocked() returns false
	//before doing anything. Might be kinda jank if we ever want to deal with multiple glfw windows, but for now it works. 
	private boolean cursorLocked = false;

	private boolean lockCursorOnSelect = false;
	private boolean unlockCursorOnEscPressed = true;

	private boolean deselectOnEscPressed = false;

	//if true, then will use parent's cursor shape if parent exists. 
	private boolean inheritParentCursor = false;

	public Window(int xOffset, int yOffset, int width, int height, Window parentWindow) {
		this.childWindows = new ArrayList<>();

		this.setParent(parentWindow);

		this.globalXOffset = xOffset;
		this.globalYOffset = yOffset;

		this.horizontalAlignStyle = defaultHorizontalAlignmentStyle;
		this.verticalAlignStyle = defaultVerticalAlignmentStyle;

		this.xOffset = xOffset;
		this.yOffset = yOffset;

		this.width = width;
		this.height = height;

		this.rootUIElement = new UIFilledRectangle(0, 0, 0, this.width, this.height, ROOT_UI_SCENE);

		this.buildBuffers();

		this.align();
	}

	public Window(int xOffset, int yOffset, Window parentWindow) {
		this.childWindows = new ArrayList<>();

		this.setParent(parentWindow);

		this.globalXOffset = xOffset;
		this.globalYOffset = yOffset;

		this.horizontalAlignStyle = defaultHorizontalAlignmentStyle;
		this.verticalAlignStyle = defaultVerticalAlignmentStyle;

		this.xOffset = xOffset;
		this.yOffset = yOffset;

		this.width = 400;
		this.height = 300;

		this.rootUIElement = new UIFilledRectangle(0, 0, 0, this.width, this.height, ROOT_UI_SCENE);

		this.buildBuffers();

		this.align();
	}

	public Window(Window parentWindow) {
		this.childWindows = new ArrayList<>();

		this.setParent(parentWindow);

		this.globalXOffset = 0;
		this.globalYOffset = 0;

		this.horizontalAlignStyle = defaultHorizontalAlignmentStyle;
		this.verticalAlignStyle = defaultVerticalAlignmentStyle;

		this.xOffset = 0;
		this.yOffset = 0;

		this.width = 400;
		this.height = 300;

		this.rootUIElement = new UIFilledRectangle(0, 0, 0, this.width, this.height, ROOT_UI_SCENE);

		this.buildBuffers();

		this.align();
	}

	public void kill() {
		if (!this.isAlive) {
			//can't kill what's already dead :P
			return;
		}
		this.isAlive = false;

		//check if this is the selected window
		if (this.isSelected()) {
			Window.setSelectedWindow(null);
		}

		this.colorBuffer.kill();
		this.rootUIElement.kill();
		Scene.removeScene(ROOT_UI_SCENE);

		if (this.parentWindow != null) {
			//even if the parent window doesn't allow child modifications, force remove it
			this.parentWindow.childWindows.remove(this);
		}

		this._kill();

		for (int i = this.childWindows.size() - 1; i >= 0; i--) {
			Window w = this.childWindows.get(i);
			w.kill();
		}
	}

	protected abstract void _kill();

	/**
	 * Schedules this window to close on the next update
	 */
	public void close() {
		this.shouldClose = true;
	}

	public void setAlignmentStyle(int horizontal, int vertical) {
		this.horizontalAlignStyle = horizontal;
		this.verticalAlignStyle = vertical;
		this.align();
	}

	private void resize(int width, int height) {
		//nothing has changed, so don't bother
		if (this.width == width && this.height == height) {
			return;
		}

		this.width = width;
		this.height = height;

		this.rootUIElement.setWidth(this.width);
		this.rootUIElement.setHeight(this.height);

		this.align();

		this.buildBuffers();

		this._resize();
	}

	protected abstract void _resize();

	private void buildBuffers() {
		if (this.colorBuffer != null) {
			this.colorBuffer.kill();
			this.colorBuffer = null;
		}

		if (this.width <= 0 || this.height <= 0) {
			return;
		}

		this.colorBuffer = new Framebuffer(this.width, this.height);
		this.colorTexture = new Texture(this.width, this.height, GL_RGBA16F, GL_RGBA, GL_FLOAT);
		this.colorBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.colorTexture.getID());
		this.colorBuffer.setDrawBuffers(new int[] { GL_COLOR_ATTACHMENT0 });
		this.colorBuffer.isComplete();
	}

	/**
	 * Returns the default title used in adjustable windows. 
	 * @return
	 */
	public abstract String getDefaultTitle();

	protected void setContextMenuRightClick(boolean b) {
		this.contextMenuRightClick = b;
	}

	protected void setContextMenuActions(ArrayList<String> actions) {
		this.contextMenuOptions = actions;
	}

	protected void setContextMenuActions(String[] actions) {
		this.setContextMenuActions(new ArrayList<String>(Arrays.asList(actions)));
	}

	/**
	 * Default true
	 * @param b
	 */
	protected void setUpdateWhenNotSelected(boolean b) {
		this.updateWhenNotSelected = b;
	}

	//an option on the context menu has been pressed, handle it here. 
	public void handleContextMenuAction(String action) {
		/* keeping it optional to implement */
	}

	public int getCursorShape() {
		if (this.inheritParentCursor) {
			if (this.parentWindow != null) {
				return this.parentWindow.getCursorShape();
			}
		}
		return this._getCursorShape();
	}

	protected int _getCursorShape() {
		/* keeping it optional to implement */
		return GLFW.GLFW_ARROW_CURSOR;
	}

	public void setInheritParentCursor(boolean b) {
		this.inheritParentCursor = b;
	}

	protected void setRenderWhenNotSelected(boolean b) {
		this.renderWhenNotSelected = b;
	}

	protected void setAllowModifyingChildren(boolean b) {
		this.allowModifyingChildren = b;
	}

	public void setAllowInput(boolean b) {
		this.allowInput = b;
	}

	public void setAllowInputWhenSubtreeSelected(boolean b) {
		this.allowInputWhenSubtreeSelected = b;
	}

	public void setAllowInputWhenNotSelected(boolean b) {
		this.allowInputWhenNotSelected = b;
	}

	public void setAllowInputWhenParentSubtreeSelected(boolean b) {
		this.allowInputWhenParentSubtreeSelected = b;
	}

	public void setAllowMouseInputWhenNotHovered(boolean b) {
		this.allowMouseInputWhenNotHovered = b;
	}

	public boolean isAllowModifyingChildren() {
		return this.allowModifyingChildren;
	}

	public boolean isAlive() {
		return this.isAlive;
	}

	public boolean isCursorLocked() {
		return this.cursorLocked;
	}

	protected void lockCursor() {
		Main.lockCursor();
		this.cursorLocked = true;
	}

	protected void unlockCursor() {
		Main.unlockCursor();
		this.cursorLocked = false;
	}

	public void setLockCursorOnSelect(boolean b) {
		this.lockCursorOnSelect = b;
	}

	public void setUnlockCursorOnEscPressed(boolean b) {
		this.unlockCursorOnEscPressed = b;
	}

	public void setDeselectOnEscPressed(boolean b) {
		this.deselectOnEscPressed = b;
	}

	public void addChild(Window w) {
		if (!this.allowModifyingChildren) {
			return;
		}
		this.childWindows.add(0, w);
		w.parentWindow = this;
		w.updateGlobalOffset();
	}

	public void removeChild(Window w) {
		if (!this.allowModifyingChildren) {
			return;
		}
		this.childWindows.remove(w);
		w.parentWindow = null;
	}

	public AdjustableWindow addChildAdjWindow(Window contentWindow) {
		return new AdjustableWindow(contentWindow, this);
	}

	public void setParent(Window newParent) {
		if (this.parentWindow != null && !this.parentWindow.isAllowModifyingChildren()) {
			return;
		}
		if (newParent != null && !newParent.allowModifyingChildren) {
			return;
		}

		if (newParent == this.parentWindow) {
			return;
		}

		if (this.parentWindow != null) {
			this.parentWindow.removeChild(this);
		}
		newParent.addChild(this);
	}

	public Window getParent() {
		return this.parentWindow;
	}

	public int getParentWidth() {
		if (this.parentWindow != null) {
			return this.parentWindow.getWidth();
		}
		return Main.windowWidth;
	}

	public int getParentHeight() {
		if (this.parentWindow != null) {
			return this.parentWindow.getHeight();
		}
		return Main.windowHeight;
	}

	public int getParentGlobalXOffset() {
		if (this.parentWindow != null) {
			return this.parentWindow.getGlobalXOffset();
		}
		return 0;
	}

	public int getParentGlobalYOffset() {
		if (this.parentWindow != null) {
			return this.parentWindow.getGlobalYOffset();
		}
		return 0;
	}

	public void align() {
		int parentWidth = Main.windowWidth;
		int parentHeight = Main.windowHeight;

		if (this.parentWindow != null) {
			parentWidth = this.parentWindow.width;
			parentHeight = this.parentWindow.height;
		}

		switch (this.horizontalAlignStyle) {
		case FROM_LEFT:
			this.alignedX = this.xOffset;
			break;

		case FROM_RIGHT:
			this.alignedX = parentWidth - this.xOffset - this.width;
			break;
		}

		switch (this.verticalAlignStyle) {
		case FROM_BOTTOM:
			this.alignedY = this.yOffset;
			break;

		case FROM_TOP:
			this.alignedY = parentHeight - this.yOffset - this.height;
			break;
		}

		this.updateGlobalOffset();

		for (Window w : this.childWindows) {
			w.align();
		}
	}

	public int getWidth() {
		return this.width;
	}

	public int getHeight() {
		return this.height;
	}

	public int getXOffset() {
		return this.xOffset;
	}

	public int getYOffset() {
		return this.yOffset;
	}

	public int getHorizontalAlignmentStyle() {
		return this.horizontalAlignStyle;
	}

	public int getVerticalAlignmentStyle() {
		return this.verticalAlignStyle;
	}

	public int getGlobalXOffset() {
		return this.globalXOffset;
	}

	public int getGlobalYOffset() {
		return this.globalYOffset;
	}

	public Vec2 getGlobalOffset() {
		return new Vec2(this.globalXOffset, this.globalYOffset);
	}

	public int getAlignedX() {
		return this.alignedX;
	}

	public int getAlignedY() {
		return this.alignedY;
	}

	public void setWidth(int w) {
		this.setDimensions(w, this.height);
	}

	public void setHeight(int h) {
		this.setDimensions(this.width, h);
	}

	public void setDimensions(int w, int h) {
		this.resize(w, h);
	}

	public void setFillWidth(boolean b) {
		this.fillWidth = b;
	}

	public void setFillHeight(boolean b) {
		this.fillHeight = b;
	}

	public void setXOffset(int offset) {
		this.setOffset(offset, this.yOffset);
	}

	public void setYOffset(int offset) {
		this.setOffset(this.xOffset, offset);
	}

	public void setOffset(int xOffset, int yOffset) {
		this.xOffset = xOffset;
		this.yOffset = yOffset;
		this.align();
	}

	public void setBottomLeftX(int x) {
		this.setBottomLeftCoords(x, this.alignedY);
	}

	public void setBottomLeftY(int y) {
		this.setBottomLeftCoords(this.alignedX, y);
	}

	//computes what offsets are required to achieve these aligned coordinates with the given alignment settings
	public void setBottomLeftCoords(int x, int y) {
		int rxOffset = 0;
		int ryOffset = 0;

		int parentWidth = Main.windowWidth;
		int parentHeight = Main.windowHeight;

		if (this.parentWindow != null) {
			parentWidth = this.parentWindow.getWidth();
			parentHeight = this.parentWindow.getHeight();
		}

		switch (this.horizontalAlignStyle) {
		case Window.FROM_LEFT:
			rxOffset = x;
			break;

		case Window.FROM_RIGHT:
			rxOffset = parentWidth - x - this.width;
			break;
		}

		switch (this.verticalAlignStyle) {
		case Window.FROM_BOTTOM:
			ryOffset = y;
			break;

		case Window.FROM_TOP:
			ryOffset = parentHeight - y - this.height;
			break;
		}

		this.setOffset(rxOffset, ryOffset);
	}

	private void updateGlobalOffset() {
		this.globalXOffset = this.alignedX;
		this.globalYOffset = this.alignedY;

		if (this.parentWindow != null) {
			this.globalXOffset += this.parentWindow.globalXOffset;
			this.globalYOffset += this.parentWindow.globalYOffset;
		}

		for (Window w : this.childWindows) {
			w.updateGlobalOffset();
		}
	}

	/**
	 * Returns where the mouse is relative to the bottom left corner of the window
	 * @return
	 */
	public Vec2 getWindowMousePos() {
		Vec2 mousePos = MouseInput.getMousePos();

		mousePos.x -= this.globalXOffset;
		mousePos.y -= this.globalYOffset;

		return mousePos;
	}

	/**
	 * Returns where the mouse is relative to the bottom left corner of the GLFW window
	 * @return
	 */
	public Vec2 getGlobalMousePos() {
		return this.getWindowMousePos().add(this.getGlobalOffset());
	}

	/**
	 * Returns where the mouse is, but clamped to this window. 
	 * @return
	 */
	public Vec2 getWindowMousePosClampedToWindow() {
		int mouseX = (int) this.getWindowMousePos().x;
		int mouseY = (int) this.getWindowMousePos().y;

		if (mouseX < 0) {
			mouseX = 0;
		}
		else if (mouseX > this.width) {
			mouseX = this.width;
		}
		if (mouseY < 0) {
			mouseY = 0;
		}
		else if (mouseY > this.height) {
			mouseY = this.height;
		}

		return new Vec2(mouseX, mouseY);
	}

	/**
	 * Returns where the mouse is, but if the mouse is outside of the parent window, then it clamps the position so that 
	 * it is inside the parent window and the current window. 
	 * 
	 * Note that the returned position can still be outside of the current window
	 * 
	 * Behaves just like getRelativeMousePos() in the case where there is no parent window
	 * @return
	 */
	public Vec2 getWindowMousePosClampedToParentWindow() {
		if (this.parentWindow == null) {
			return this.getWindowMousePos();
		}

		int mouseX = (int) this.getWindowMousePos().x;
		int mouseY = (int) this.getWindowMousePos().y;

		int parentMouseX = (int) (this.parentWindow.getWindowMousePos().x);
		int parentMouseY = (int) (this.parentWindow.getWindowMousePos().y);

		if (parentMouseX < 0) {
			mouseX -= parentMouseX;
		}
		else if (parentMouseX > this.parentWindow.getWidth()) {
			mouseX += this.parentWindow.getWidth() - parentMouseX;
		}
		if (parentMouseY < 0) {
			mouseY -= parentMouseY;
		}
		else if (parentMouseY > this.parentWindow.getHeight()) {
			mouseY += this.parentWindow.getHeight() - parentMouseY;
		}

		return new Vec2(mouseX, mouseY);
	}

	public boolean isSelected() {
		return this.isSelected;
	}

	public boolean isSubtreeSelected() {
		return this.isSubtreeSelected;
	}

	public void update() {
		if (this.shouldClose) {
			this.kill();
			return;
		}

		//do fill width and height
		if (this.parentWindow != null) {
			if (this.fillWidth && this.parentWindow.getWidth() != this.width) {
				this.setWidth(this.parentWindow.getWidth());
			}
			if (this.fillHeight && this.parentWindow.getHeight() != this.height) {
				this.setHeight(this.parentWindow.getHeight());
			}
		}

		if (this.isSelected || this.updateWhenNotSelected) {
			this._update();
		}

		//go in descending order, because windows might choose to re-nest. 
		//when a window is re-nested, it might update twice on that update cycle. Just a thing to keep in mind. 
		for (int i = this.childWindows.size() - 1; i >= 0; i--) {
			Window w = this.childWindows.get(i);
			w.update();
		}
	}

	protected abstract void _update();

	//output buffer going to have two texturebuffers attached
	// - color buffer
	// - window id buffer
	public void render(Framebuffer outputBuffer) {
		if (this.colorBuffer == null) {
			return;
		}

		this.colorBuffer.bind();
		glClear(GL_COLOR_BUFFER_BIT);

		if (this.isSelected || this.renderWhenNotSelected) {
			this.renderContent(this.colorBuffer);
		}

		//render child windows back to front. 
		for (int i = this.childWindows.size() - 1; i >= 0; i--) {
			Window w = this.childWindows.get(i);
			w.render(this.colorBuffer);
		}

		if (this.isSelected || this.renderWhenNotSelected) {
			this.renderOverlay(this.colorBuffer);
		}

		//render whatever we have to the output buffer
		outputBuffer.bind();
		glDisable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
		glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
		glViewport(this.alignedX, this.alignedY, this.width, this.height);

		if (this.renderAlpha) {
			Shader.RENDER_ALPHA.enable();
		}
		else {
			Shader.SPLASH.enable();
			Shader.SPLASH.setUniform1f("alpha", 1);
		}

		this.colorTexture.bind(GL_TEXTURE0);
		ScreenQuad.screenQuad.render();

		glViewport(0, 0, Main.windowWidth, Main.windowHeight);
		glScissor(0, 0, Main.windowWidth, Main.windowHeight);
		glDisable(GL_SCISSOR_TEST);
	}

	//renders whatever is in the current color buffer to the glfw screen. 
	private void renderToGLFWScreen() {
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
		glDisable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
		glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
		glViewport(this.globalXOffset, this.globalYOffset, this.width, this.height);
		glScissor(this.getParentGlobalXOffset(), this.getParentGlobalYOffset(), this.getParentWidth(), this.getParentHeight());
		glEnable(GL_SCISSOR_TEST);

		Shader.SPLASH.enable();
		Shader.SPLASH.setUniform1f("alpha", 1);

		this.colorTexture.bind(GL_TEXTURE0);
		ScreenQuad.screenQuad.render();

		glViewport(0, 0, Main.windowWidth, Main.windowHeight);
		glScissor(0, 0, Main.windowWidth, Main.windowHeight);
		glDisable(GL_SCISSOR_TEST);
	}

	/**
	 * Render everything specific to this window
	 * @param outputBuffer
	 */
	protected abstract void renderContent(Framebuffer outputBuffer);

	/**
	 * If you want to render something over the other windows, here's where to do it. 
	 * @param outputBuffer
	 */
	protected abstract void renderOverlay(Framebuffer outputBuffer);

	protected void select() {
		if (this.isSelected) {
			return;
		}
		if (selectedWindow != null) {
			selectedWindow.deselect();
		}

		this.isSelected = true;
		selectedWindow = this;
		this.selected();
		this.subtreeSelect();

		if (this.lockCursorOnSelect) {
			this.lockCursor();
		}
	}

	protected void deselect() {
		if (!this.isSelected) {
			return;
		}
		if (selectedWindow != this) {
			System.err.println("Window : window is selected, but is not equal to selectedWindow");
		}

		this.isSelected = false;
		selectedWindow = null;
		this.deselected();
		this.subtreeDeselect();

		this.unlockCursor();
	}

	//what do when selected?
	protected abstract void selected();

	//what do when deselected?
	protected abstract void deselected();

	protected void subtreeSelect() {
		if (this.isSubtreeSelected) {
			return;
		}

		this.isSubtreeSelected = true;
		if (this.parentWindow != null) {
			this.parentWindow.subtreeSelect();
		}
		this.subtreeSelected();
	}

	protected void subtreeDeselect() {
		if (!this.isSubtreeSelected) {
			return;
		}

		this.isSubtreeSelected = false;
		if (this.parentWindow != null) {
			this.parentWindow.subtreeDeselect();
		}
		this.subtreeDeselected();
	}

	//a window of this subtree is selected
	protected abstract void subtreeSelected();

	//this subtree is now deselected
	protected abstract void subtreeDeselected();

	//gives coordinates relative to parent window, returns true if those coordinates are 'inside' this window. 
	//you can reimplement this if you need
	protected boolean isWindowClicked(int x, int y) {
		int x1 = this.alignedX;
		int x2 = this.alignedX + this.width;
		int y1 = this.alignedY;
		int y2 = this.alignedY + this.height;
		return x1 <= x && x <= x2 && y1 <= y && y <= y2;
	}

	//relative to bottom left corner
	//returns -1 if the point doesn't fall onto one of the child windows
	//returns -2 if the point falls outside of the current window
	private int getClickedWindowIndex(int x, int y) {
		if (!this.isWindowClicked(x + this.alignedX, y + this.alignedY)) {
			return -2;
		}

		//go in ascending order
		for (int i = 0; i < this.childWindows.size(); i++) {
			Window w = this.childWindows.get(i);
			if (w.isWindowClicked(x, y)) {
				return i;
			}
		}
		return -1;
	}

	public static void setSelectedWindow(Window w) {
		if (w == selectedWindow) {
			return;
		}
		if (selectedWindow != null) {
			selectedWindow.deselect();
		}
		if (w != null) {
			w.select();
		}
		selectedWindow = w;
	}

	public static void setHoveredWindow(Window w) {
		if (w == hoveredWindow) {
			return;
		}
		hoveredWindow = w;
	}

	//this should be called every time the mousePressed function is called externally
	//will return the topmost window that is at the x, y, position
	public Window selectWindow(int x, int y, boolean forceToTop) {
		//if the cursor is locked, we shouldn't change the selected window
		if (Main.isCursorLocked()) {
			return null;
		}

		//deal with this window
		int selectedWindow = getClickedWindowIndex(x, y);
		if (selectedWindow == -2) { //the cursor is outside of this window
			return null;
		}
		if (selectedWindow == -1) { //the cursor is inside of this window, but none of the child windows. 
			return this;
		}

		//the cursor is inside one of the child windows
		//propogate selection to children
		Window w = this.childWindows.get(selectedWindow);
		Window selected = this.childWindows.get(selectedWindow).selectWindow(x - w.alignedX, y - w.alignedY, forceToTop);

		//make the newly selected window the top one
		if (forceToTop) {
			if (selectedWindow >= 0 && selectedWindow < this.childWindows.size()) {
				Window wind = this.childWindows.get(selectedWindow);
				this.childWindows.remove(selectedWindow);
				this.childWindows.add(0, wind);
			}
		}

		return selected;
	}

	//helper to check if this window should allow inputs at this moment
	private boolean shouldAllowInput() {
		if (!this.allowInput) {
			return false;
		}
		if (this.isSubtreeSelected && this.allowInputWhenSubtreeSelected) {
			return true;
		}
		if (this.parentWindow != null && this.parentWindow.isSubtreeSelected() && this.allowInputWhenParentSubtreeSelected) {
			return true;
		}
		if (this.isSelected || this.allowInputWhenNotSelected) {
			return true;
		}
		return false;
	}

	private boolean shouldAllowMouseInput() {
		if (!this.shouldAllowInput()) {
			return false;
		}
		if (this.isSelected && Main.isCursorLocked()) {
			//we're locked into this window
			return true;
		}
		if (!this.allowMouseInputWhenNotHovered && this != hoveredWindow) {
			return false;
		}
		return true;
	}

	private boolean shouldAllowKeyboardInput() {
		if (!this.shouldAllowInput()) {
			return false;
		}
		return true;
	}

	//we try to make a guarantee that for every mouse pressed input, there has to be a corresponding mouse released input. 
	public void mousePressed(int button) {
		if (this.shouldAllowMouseInput()) {
			if (button == GLFW.GLFW_MOUSE_BUTTON_2 && this.contextMenuRightClick && !Main.isCursorLocked()) {
				//spawn context menu
				if (this.contextMenuWindow != null && this.contextMenuWindow.isAlive()) {
					this.contextMenuWindow.kill();
					this.contextMenuWindow = null;
				}
				this.contextMenuWindow = new ContextMenuWindow(this.contextMenuOptions, this);

				//deselect this window
				this.deselect();
			}
			else {
				this._mousePressed(button);
			}
		}
		for (int i = this.childWindows.size() - 1; i >= 0; i--) {
			Window w = this.childWindows.get(i);
			w.mousePressed(button);
		}
	}

	protected abstract void _mousePressed(int button);

	public void mouseReleased(int button) {
		if (this.shouldAllowInput()) {
			this._mouseReleased(button);
		}
		for (int i = this.childWindows.size() - 1; i >= 0; i--) {
			Window w = this.childWindows.get(i);
			w.mouseReleased(button);
		}
	}

	protected abstract void _mouseReleased(int button);

	public void mouseScrolled(float wheelOffset, float smoothOffset) {
		if (this.shouldAllowMouseInput()) {
			this._mouseScrolled(wheelOffset, smoothOffset);
		}
		for (int i = this.childWindows.size() - 1; i >= 0; i--) {
			Window w = this.childWindows.get(i);
			w.mouseScrolled(wheelOffset, smoothOffset);
		}
	}

	protected abstract void _mouseScrolled(float wheelOffset, float smoothOffset);

	public void keyPressed(int key) {
		if (this.shouldAllowKeyboardInput()) {
			if (key == GLFW.GLFW_KEY_ESCAPE) {
				if (this.deselectOnEscPressed) {
					this.deselect();
				}
				if (this.unlockCursorOnEscPressed) {
					this.unlockCursor();
				}
			}

			this._keyPressed(key);
		}
		for (int i = this.childWindows.size() - 1; i >= 0; i--) {
			Window w = this.childWindows.get(i);
			w.keyPressed(key);
		}
	}

	protected abstract void _keyPressed(int key);

	public void keyReleased(int key) {
		if (this.shouldAllowKeyboardInput()) {
			this._keyReleased(key);
		}
		for (int i = this.childWindows.size() - 1; i >= 0; i--) {
			Window w = this.childWindows.get(i);
			w.keyReleased(key);
		}
	}

	protected abstract void _keyReleased(int key);
}
