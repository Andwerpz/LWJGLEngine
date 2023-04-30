package lwjglengine.v10.window;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import java.util.ArrayList;

import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.graphics.Shader;
import lwjglengine.v10.graphics.Texture;
import lwjglengine.v10.input.MouseInput;
import lwjglengine.v10.main.Main;
import lwjglengine.v10.model.FilledRectangle;
import lwjglengine.v10.model.Model;
import lwjglengine.v10.player.Camera;
import lwjglengine.v10.scene.Scene;
import lwjglengine.v10.screen.ScreenQuad;
import lwjglengine.v10.screen.UIScreen;
import lwjglengine.v10.ui.UIFilledRectangle;
import lwjglengine.v10.util.BufferUtils;
import myutils.v10.math.Mat4;
import myutils.v10.math.Vec2;
import myutils.v10.math.Vec3;
import myutils.v10.misc.Pair;

public abstract class Window {
	//A window should be a container for rendering. 
	//should be relatively modular. 

	//how should this class interact with states?
	//should the state call upon this class, or should this class be a container for the state?

	//perhaps we need to do away with the notion of a state. 

	//the problem is that how are we going to decide when to put up a loading screen without states?

	//TODO
	// - drop shadow?

	public static final int FROM_LEFT = 0;
	public static final int FROM_RIGHT = 1;
	public static final int FROM_TOP = 2;
	public static final int FROM_BOTTOM = 3;
	public static final int FROM_CENTER_LEFT = 4;
	public static final int FROM_CENTER_RIGHT = 5;
	public static final int FROM_CENTER_TOP = 6;
	public static final int FROM_CENTER_BOTTOM = 7;

	//requests come in the form <child, new parent>
	public static ArrayList<Pair<Window, Window>> renestRequests = new ArrayList<>();

	private final int ROOT_UI_SCENE = Scene.generateScene();

	private int width, height;

	//offset from specified parent window
	private int xOffset, yOffset;

	//offset from GLFW window
	//useful for determining where the mouse is
	private int globalXOffset, globalYOffset;

	private int alignedX, alignedY;

	private int horizontalAlignStyle, verticalAlignStyle;

	private Framebuffer colorBuffer;
	private Texture colorTexture;

	protected Window parentWindow;
	protected ArrayList<Window> childWindows;

	//every ui element on this window should be a child of the root ui element. 
	protected UIFilledRectangle rootUIElement;

	//there should only be 1 window selected at a time. 
	private boolean isSelected = false;

	public Window(int xOffset, int yOffset, int width, int height, Window parentWindow) {
		this.childWindows = new ArrayList<>();

		this.parentWindow = parentWindow;
		if (this.parentWindow != null) {
			this.parentWindow.addChild(this);
		}

		this.globalXOffset = xOffset;
		this.globalYOffset = yOffset;

		this.horizontalAlignStyle = FROM_LEFT;
		this.verticalAlignStyle = FROM_BOTTOM;

		this.xOffset = xOffset;
		this.yOffset = yOffset;

		this.width = width;
		this.height = height;

		this.rootUIElement = new UIFilledRectangle(0, 0, 0, this.width, this.height, ROOT_UI_SCENE);

		this.buildBuffers();

		this.updateGlobalOffset();

		this.align();
	}

	public void kill() {
		this.colorBuffer.kill();
		this.rootUIElement.kill();
		Scene.removeScene(ROOT_UI_SCENE);

		if (this.parentWindow != null) {
			this.parentWindow.removeChild(this);
		}

		this._kill();

		for (Window w : this.childWindows) {
			w.kill();
		}
	}

	protected abstract void _kill();

	private void resize(int width, int height) {
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
		}

		this.colorBuffer = new Framebuffer(this.width, this.height);
		this.colorTexture = new Texture(GL_RGBA32F, this.width, this.height, GL_RGBA, GL_FLOAT, GL_NEAREST);
		this.colorBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.colorTexture.getID());
		this.colorBuffer.setDrawBuffers(new int[] { GL_COLOR_ATTACHMENT0 });
		this.colorBuffer.isComplete();
	}

	public void addChild(Window w) {
		this.childWindows.add(0, w);
		w.parentWindow = this;
		w.updateGlobalOffset();
	}

	public void removeChild(Window w) {
		this.childWindows.remove(w);
		w.parentWindow = null;
	}

	public void switchParent(Window newParent) {
		if (this.parentWindow != null) {
			this.parentWindow.removeChild(this);
		}
		newParent.addChild(this);
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
			this.alignedX = parentWidth - this.xOffset;
			break;

		case FROM_CENTER_LEFT:
			this.alignedX = parentWidth / 2 - this.xOffset;
			break;

		case FROM_CENTER_RIGHT:
			this.alignedX = parentWidth / 2 + this.xOffset;
			break;
		}

		switch (this.verticalAlignStyle) {
		case FROM_BOTTOM:
			this.alignedY = this.yOffset;
			break;

		case FROM_TOP:
			this.alignedY = parentHeight - this.yOffset;
			break;

		case FROM_CENTER_BOTTOM:
			this.alignedY = parentHeight / 2 - this.yOffset;
			break;

		case FROM_CENTER_TOP:
			this.alignedY = parentHeight / 2 + this.yOffset;
			break;
		}

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

	public int getGlobalXOffset() {
		return this.globalXOffset;
	}

	public int getGlobalYOffset() {
		return this.globalYOffset;
	}

	public void setWidth(int w) {
		this.resize(w, this.height);
	}

	public void setHeight(int h) {
		this.resize(this.width, h);
	}

	public void setDimensions(int w, int h) {
		this.resize(w, h);
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
		this.updateGlobalOffset();
	}

	private void updateGlobalOffset() {
		this.globalXOffset = this.xOffset;
		this.globalYOffset = this.yOffset;

		if (this.parentWindow != null) {
			this.globalXOffset += this.parentWindow.globalXOffset;
			this.globalYOffset += this.parentWindow.globalYOffset;
		}

		for (Window w : this.childWindows) {
			w.updateGlobalOffset();
		}
	}

	/**
	 * Returns where the mouse is relative to the top left corner of the window
	 * @return
	 */
	protected Vec2 getRelativeMousePos() {
		Vec2 mousePos = MouseInput.getMousePos();

		mousePos.x -= this.globalXOffset;

		mousePos.y = Main.windowHeight - mousePos.y;
		mousePos.y -= this.globalYOffset;
		mousePos.y = this.height - mousePos.y;

		return mousePos;
	}

	/**
	 * Returns where the mouse is, but clamped to this window. 
	 * @return
	 */
	protected Vec2 getRelativeMousePosClampedToWindow() {
		int mouseX = (int) this.getRelativeMousePos().x;
		int mouseY = (int) this.getRelativeMousePos().y;

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
	protected Vec2 getRelativeMousePosClampedToParent() {
		if (this.parentWindow == null) {
			return this.getRelativeMousePos();
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

	public void update() {
		this._update();

		//go in descending order, because windows might choose to re-nest. 
		for (int i = this.childWindows.size() - 1; i >= 0; i--) {
			Window w = this.childWindows.get(i);
			w.update();
		}
	}

	protected abstract void _update();

	//output buffer going to have two texturebuffers attached
	// - color buffer
	// - window id buffer
	public void render(Framebuffer colorBuffer) {
		this.colorBuffer.bind();
		glClear(GL_COLOR_BUFFER_BIT);

		this.renderContent(this.colorBuffer);

		//render child windows back to front. 
		for (int i = this.childWindows.size() - 1; i >= 0; i--) {
			Window w = this.childWindows.get(i);
			w.render(this.colorBuffer);
		}

		this.renderOverlay(this.colorBuffer);

		//render whatever we have to the output buffer
		colorBuffer.bind();
		glDisable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glViewport(this.alignedX, this.alignedY, this.width, this.height);
		Shader.SPLASH.enable();
		Shader.SPLASH.setUniform1f("alpha", 1);
		this.colorTexture.bind(GL_TEXTURE0);
		ScreenQuad.screenQuad.render();

		glViewport(0, 0, Main.windowWidth, Main.windowHeight);
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

	//what do when selected?
	protected abstract void selected();

	//what do when deselected?
	protected abstract void deselected();

	//relative to bottom left corner
	//returns -1 if the point doesn't fall onto one of the child windows
	//returns -2 if the point falls outside of the current window
	private int getClickedWindowIndex(int x, int y) {
		if (x < 0 || y < 0 || x > this.width || y > this.height) {
			return -2;
		}

		for (int i = 0; i < this.childWindows.size(); i++) {
			Window w = this.childWindows.get(i);
			int x1 = w.alignedX;
			int x2 = w.alignedX + w.width;
			int y1 = w.alignedY;
			int y2 = w.alignedY + w.height;
			if (x1 <= x && x <= x2 && y1 <= y && y <= y2) {
				return i;
			}
		}
		return -1;
	}

	//this should be called every time the mousePressed function is called externally
	public void selectWindow(int x, int y, boolean covered) {
		int selectedWindow = getClickedWindowIndex(x, y);
		if (covered) {
			if (this.isSelected) {
				this.deselected();
				this.isSelected = false;
			}
			for (Window w : this.childWindows) {
				w.selectWindow(x - w.alignedX, y - w.alignedY, true);
			}
			return;
		}

		//deal with this window
		if (selectedWindow == -1) {
			if (!this.isSelected) {
				this.selected();
				this.isSelected = true;
			}
		}
		else {
			if (this.isSelected) {
				this.deselected();
				this.isSelected = false;
			}
		}

		//deal with children 
		//propogate selection to children
		for (int i = 0; i < this.childWindows.size(); i++) {
			Window w = this.childWindows.get(i);
			w.selectWindow(x - w.alignedX, y - w.alignedY, i != selectedWindow);
		}

		//make the newly selected window the top one
		if (selectedWindow >= 0) {
			Window w = this.childWindows.get(selectedWindow);
			this.childWindows.remove(selectedWindow);
			this.childWindows.add(0, w);
		}

	}

	public void mousePressed(int button) {
		if (this.isSelected) {
			this._mousePressed(button);
		}
		for (Window w : this.childWindows) {
			w.mousePressed(button);
		}
	}

	protected abstract void _mousePressed(int button);

	public void mouseReleased(int button) {
		if (this.isSelected) {
			this._mouseReleased(button);
		}
		for (Window w : this.childWindows) {
			w.mouseReleased(button);
		}
	}

	protected abstract void _mouseReleased(int button);

	public void mouseScrolled(float wheelOffset, float smoothOffset) {
		if (this.isSelected) {
			this._mouseScrolled(wheelOffset, smoothOffset);
		}
		for (Window w : this.childWindows) {
			w.mouseScrolled(wheelOffset, smoothOffset);
		}
	}

	protected abstract void _mouseScrolled(float wheelOffset, float smoothOffset);

	public void keyPressed(int key) {
		if (this.isSelected) {
			this._keyPressed(key);
		}
		for (Window w : this.childWindows) {
			w.keyPressed(key);
		}
	}

	protected abstract void _keyPressed(int key);

	public void keyReleased(int key) {
		if (this.isSelected) {
			this._keyReleased(key);
		}
		for (Window w : this.childWindows) {
			w.keyReleased(key);
		}
	}

	protected abstract void _keyReleased(int key);
}
