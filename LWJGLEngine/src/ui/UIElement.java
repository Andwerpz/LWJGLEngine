package ui;

import java.util.ArrayList;
import java.util.HashSet;

import entity.Entity;
import main.Main;

public abstract class UIElement extends Entity {
	//am i overengineering ui element alignment?

	//stores all UIElement entities, with alignment information. 
	//in the case of window resizing, a static method can re-align all ui elements by modifying x y coordinates. 

	private static HashSet<UIElement> uiElements = new HashSet<UIElement>();
	public static boolean shouldAlignUIElements = false;

	//setting the reference xy coordinates. 
	public static final int FROM_LEFT = 0;
	public static final int FROM_RIGHT = 1;
	public static final int FROM_TOP = 2;
	public static final int FROM_BOTTOM = 3;
	public static final int FROM_CENTER_LEFT = 4;
	public static final int FROM_CENTER_RIGHT = 5;
	public static final int FROM_CENTER_TOP = 6;
	public static final int FROM_CENTER_BOTTOM = 7;

	//aligning the actual element with the reference xy coords. 
	public static final int ALIGN_LEFT = 0;
	public static final int ALIGN_RIGHT = 1;
	public static final int ALIGN_TOP = 2;
	public static final int ALIGN_BOTTOM = 3;
	public static final int ALIGN_CENTER = 4;

	protected static float depthSpacing = 0.001f;

	//you can bind other ui elements to this one to align them to the bounding box of this ui element
	//when you re-align this ui element, any elements that are bound to this one will be re-aligned. 
	private ArrayList<UIElement> boundElements;

	private boolean isBound = false;
	private UIElement parentElement;

	protected int scene;

	private int horizontalAlignFrame, verticalAlignFrame;
	private float xOffset, yOffset; //along with alignment style, determines reference coordinates for drawing

	protected int horizontalAlignContent, verticalAlignContent;
	protected float x, y; //reference coordinates for drawing
	protected float z; //needed as float for layering purposes
	protected float width, height;

	//denotes the bottom left point of the bounding rectangle for this ui element
	//we want this to be int as opposed to float to not get any weird interpixel sampling issues
	//on ui elements that are constantly being resized tho, you might want them to remain as floats. 
	protected float alignedX, alignedY;
	private boolean clampAlignedCoordinatesToInt = true;

	public UIElement(float xOffset, float yOffset, float z, float width, float height, int scene) {
		this.horizontalAlignFrame = FROM_LEFT;
		this.verticalAlignFrame = FROM_BOTTOM;

		this.horizontalAlignContent = ALIGN_LEFT;
		this.verticalAlignContent = ALIGN_BOTTOM;

		this.xOffset = xOffset;
		this.yOffset = yOffset;
		this.z = z;

		this.width = width;
		this.height = height;

		this.scene = scene;

		this.boundElements = new ArrayList<>();

		UIElement.uiElements.add(this);
		UIElement.shouldAlignUIElements = true;
	}

	@Override
	protected void _kill() {
		this.unbind();
		uiElements.remove(this);
		for (int i = 0; i < this.boundElements.size(); i += 0) {
			UIElement e = this.boundElements.get(i);
			e.kill();
		}
		this.__kill();
	}

	protected abstract void __kill();

	public static void removeAllUIElementsFromScene(int scene) {
		ArrayList<UIElement> toRemove = new ArrayList<>();
		for (UIElement i : uiElements) {
			if (i.getScene() == scene) {
				toRemove.add(i);
			}
		}
		for (UIElement i : toRemove) {
			i.kill();
		}
	}

	public void setFrameAlignmentStyle(int horizontalAlign, int verticalAlign) {
		this.horizontalAlignFrame = horizontalAlign;
		this.verticalAlignFrame = verticalAlign;
		this.alignFrame();
	}

	public void setFrameAlignmentOffset(float xOffset, float yOffset) {
		this.xOffset = xOffset;
		this.yOffset = yOffset;
		this.alignFrame();
	}

	public void setFrameAlignment(int horizontalAlign, int verticalAlign, float xOffset, float yOffset) {
		this.horizontalAlignFrame = horizontalAlign;
		this.verticalAlignFrame = verticalAlign;
		this.xOffset = xOffset;
		this.yOffset = yOffset;
		this.alignFrame();
	}

	public void setContentAlignmentStyle(int horizontalAlign, int verticalAlign) {
		this.horizontalAlignContent = horizontalAlign;
		this.verticalAlignContent = verticalAlign;
		this.alignContents();
	}

	public void setWidth(float width) {
		this.width = width;
		this.alignContents();
	}

	public void setHeight(float height) {
		this.height = height;
		this.alignContents();
	}

	public static void alignAllUIElements() {
		System.out.println("ALIGN ALL UI ELEMENTS");
		for (UIElement i : UIElement.uiElements) {
			if (i.isBound) { //the parent will call on the bound element to be aligned. 
				continue;
			}
			i.align();
		}
		UIElement.shouldAlignUIElements = false;
	}

	protected void alignFrame() {
		float leftBorder = 0;
		float rightBorder = Main.windowWidth;
		float bottomBorder = 0;
		float topBorder = Main.windowHeight;

		if (this.isBound) {
			leftBorder = this.parentElement.getLeftBorder();
			rightBorder = this.parentElement.getRightBorder();
			bottomBorder = this.parentElement.getBottomBorder();
			topBorder = this.parentElement.getTopBorder();
		}

		switch (this.horizontalAlignFrame) {
		case FROM_LEFT:
			this.x = this.xOffset + leftBorder;
			break;

		case FROM_RIGHT:
			this.x = rightBorder - this.xOffset;
			break;

		case FROM_CENTER_LEFT:
			this.x = leftBorder + (rightBorder - leftBorder) / 2 - this.xOffset;
			break;

		case FROM_CENTER_RIGHT:
			this.x = leftBorder + (rightBorder - leftBorder) / 2 + this.xOffset;
			break;
		}

		switch (this.verticalAlignFrame) {
		case FROM_BOTTOM:
			this.y = this.yOffset + bottomBorder;
			break;

		case FROM_TOP:
			this.y = topBorder - this.yOffset;
			break;

		case FROM_CENTER_BOTTOM:
			this.y = bottomBorder + (topBorder - bottomBorder) / 2 - this.yOffset;
			break;

		case FROM_CENTER_TOP:
			this.y = bottomBorder + (topBorder - bottomBorder) / 2 + this.yOffset;
			break;
		}
	}

	protected void alignContents() {
		switch (this.horizontalAlignContent) {
		case ALIGN_CENTER:
			this.alignedX = (this.x - this.width / 2);
			break;

		case ALIGN_RIGHT:
			this.alignedX = (this.x - this.width);
			break;

		case ALIGN_LEFT:
			this.alignedX = (this.x);
			break;
		}

		switch (this.verticalAlignContent) {
		case ALIGN_CENTER:
			this.alignedY = (this.y - this.height / 2);
			break;

		case ALIGN_TOP:
			this.alignedY = (this.y - this.height);
			break;

		case ALIGN_BOTTOM:
			this.alignedY = (this.y);
			break;
		}

		if (this.clampAlignedCoordinatesToInt) {
			this.alignedX = (int) alignedX;
			this.alignedY = (int) alignedY;
		}

		this._alignContents();
	}

	public void setClampAlignedCoordinatesToInt(boolean b) {
		this.clampAlignedCoordinatesToInt = b;
	}

	public float getLeftBorder() {
		return this.alignedX;
	}

	public float getRightBorder() {
		return this.alignedX + this.width;
	}

	public float getBottomBorder() {
		return this.alignedY;
	}

	public float getTopBorder() {
		return this.alignedY + this.height;
	}

	public float getWidth() {
		return this.width;
	}

	public float getHeight() {
		return this.height;
	}

	public int getScene() {
		return this.scene;
	}

	protected abstract void _alignContents();

	public void align() {
		this.alignFrame();
		this.alignContents();

		for (UIElement e : this.boundElements) {
			e.align();
		}
	}

	@Override
	protected abstract void update();

	public float getXOffset() {
		return this.xOffset;
	}

	public float getYOffset() {
		return this.yOffset;
	}

	public void setZ(float z) {
		this.z = z;

		for (UIElement e : this.boundElements) {
			e.setZ(this.z + depthSpacing);
		}
	}

	//binds this element to another element
	public void bind(UIElement e) {
		if (this.isBound()) {
			this.unbind();
		}

		this.isBound = true;
		this.parentElement = e;
		e.boundElements.add(this);

		this.setZ(this.z + depthSpacing);
	}

	//if this element has a parent element, it unbinds itself
	public void unbind() {
		if (this.isBound()) {
			this.getParent().boundElements.remove(this);
			this.isBound = false;
			this.parentElement = null;
		}
	}

	public boolean isBound() {
		return this.isBound;
	}

	public UIElement getParent() {
		return this.isBound() ? this.parentElement : null;
	}

}
