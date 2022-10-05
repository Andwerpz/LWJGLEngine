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

	protected int scene;

	private int horizontalAlignFrame, verticalAlignFrame;
	private int xOffset, yOffset; //along with alignment style, determines reference coordinates for drawing

	protected int horizontalAlignContent, verticalAlignContent;
	protected int x, y, z; //reference coordinates for drawing
	protected int width, height;

	protected int alignedX, alignedY;

	public UIElement(int xOffset, int yOffset, int z, int width, int height, int scene) {
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

		UIElement.uiElements.add(this);
		UIElement.shouldAlignUIElements = true;
	}

	@Override
	protected void _kill() {
		uiElements.remove(this);
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

	public void setFrameAlignmentOffset(int xOffset, int yOffset) {
		this.xOffset = xOffset;
		this.yOffset = yOffset;
		this.alignFrame();
	}

	public void setFrameAlignment(int horizontalAlign, int verticalAlign, int xOffset, int yOffset) {
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

	public void setWidth(int width) {
		this.width = width;
		this.alignContents();
	}

	public void setHeight(int height) {
		this.height = height;
		this.alignContents();
	}

	public static void alignAllUIElements() {
		System.out.println("ALIGN ALL UI ELEMENTS");
		for (UIElement i : UIElement.uiElements) {
			i.align();
		}
		UIElement.shouldAlignUIElements = false;
	}

	protected void alignFrame() {
		switch (this.horizontalAlignFrame) {
		case FROM_LEFT:
			this.x = this.xOffset;
			break;

		case FROM_RIGHT:
			this.x = Main.windowWidth - this.xOffset;
			break;

		case FROM_CENTER_LEFT:
			this.x = Main.windowWidth / 2 - this.xOffset;
			break;

		case FROM_CENTER_RIGHT:
			this.x = Main.windowWidth / 2 + this.xOffset;
			break;
		}

		switch (this.verticalAlignFrame) {
		case FROM_BOTTOM:
			this.y = this.yOffset;
			break;

		case FROM_TOP:
			this.y = Main.windowHeight - this.yOffset;
			break;

		case FROM_CENTER_BOTTOM:
			this.y = Main.windowHeight / 2 - this.yOffset;
			break;

		case FROM_CENTER_TOP:
			this.y = Main.windowHeight / 2 + this.yOffset;
			break;
		}
	}

	protected void alignContents() {
		switch (this.horizontalAlignContent) {
		case ALIGN_CENTER:
			this.alignedX = this.x - this.width / 2;
			break;

		case ALIGN_RIGHT:
			this.alignedX = this.x - this.width;
			break;

		case ALIGN_LEFT:
			this.alignedX = this.x;
			break;
		}

		switch (this.verticalAlignContent) {
		case ALIGN_CENTER:
			this.alignedY = this.y - this.height / 2;
			break;

		case ALIGN_TOP:
			this.alignedY = this.y - this.height;
			break;

		case ALIGN_BOTTOM:
			this.alignedY = this.y;
			break;
		}
		this._alignContents();
	}

	public int getScene() {
		return this.scene;
	}

	protected abstract void _alignContents();

	public void align() {
		this.alignFrame();
		this.alignContents();
	}

	@Override
	protected abstract void update();

}
