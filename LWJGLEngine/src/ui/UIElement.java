package ui;

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

	private int horizontalAlignFrame, verticalAlignFrame;
	protected int horizontalAlignContent, verticalAlignContent;
	private int xOffset, yOffset;
	protected int x, y, z, width, height;

	public UIElement(int xOffset, int yOffset) {
		this.horizontalAlignFrame = FROM_LEFT;
		this.verticalAlignFrame = FROM_BOTTOM;

		this.horizontalAlignContent = ALIGN_LEFT;
		this.verticalAlignContent = ALIGN_BOTTOM;

		this.xOffset = xOffset;
		this.yOffset = yOffset;

		UIElement.uiElements.add(this);
		UIElement.shouldAlignUIElements = true;
	}

	@Override
	protected void _kill() {
		uiElements.remove(this);
		this.__kill();
	}

	protected abstract void __kill();

	public void setFrameAlignmentStyle(int horizontalAlign, int verticalAlign) {
		this.horizontalAlignFrame = horizontalAlign;
		this.verticalAlignFrame = verticalAlign;
		UIElement.shouldAlignUIElements = true;
	}

	public void setFrameAlignmentOffset(int xOffset, int yOffset) {
		this.xOffset = xOffset;
		this.yOffset = yOffset;
		UIElement.shouldAlignUIElements = true;
	}

	public void setFrameAlignment(int horizontalAlign, int verticalAlign, int xOffset, int yOffset) {
		this.horizontalAlignFrame = horizontalAlign;
		this.verticalAlignFrame = verticalAlign;
		this.xOffset = xOffset;
		this.yOffset = yOffset;
		UIElement.shouldAlignUIElements = true;
	}

	public void setContentAlignmentStyle(int horizontalAlign, int verticalAlign) {
		this.horizontalAlignContent = horizontalAlign;
		this.verticalAlignContent = verticalAlign;
		UIElement.shouldAlignUIElements = true;
	}

	public static void alignAllUIElements() {
		for (UIElement i : UIElement.uiElements) {
			i.alignFrame();
		}
		UIElement.shouldAlignUIElements = false;
	}

	public void alignFrame() {
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

		this.alignContents();
	}

	protected abstract void alignContents();

	@Override
	protected abstract void update();

}
