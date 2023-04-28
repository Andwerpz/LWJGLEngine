package lwjglengine.v10.window;

import java.awt.Color;

import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.graphics.Material;
import lwjglengine.v10.model.Line;
import lwjglengine.v10.model.Model;
import lwjglengine.v10.scene.Scene;
import lwjglengine.v10.screen.UIScreen;
import lwjglengine.v10.ui.Text;
import lwjglengine.v10.ui.UIElement;
import lwjglengine.v10.ui.UIFilledRectangle;
import myutils.v10.math.Vec2;
import myutils.v10.math.Vec3;

public class AdjustableWindow extends Window {

	//TODO implement snapping to windows that are children of the same parent. 

	private final int BACKGROUND_SCENE = Scene.generateScene();
	private final int TITLE_BAR_SCENE = Scene.generateScene();
	private final int BORDER_SCENE = Scene.generateScene();

	private UIScreen uiScreen;

	private int titleBarHeight = 25;
	private UIFilledRectangle titleBarRect;

	private String title;
	private int titleLeftMargin = 5;
	private Text titleBarText;

	private UIFilledRectangle backgroundRect;

	private long[] windowBorder;

	private Material titleBarMaterial = new Material(new Vec3((float) (15 / 255.0)));

	private Material deselectedBackgroundMaterial = new Material(new Vec3((float) (25 / 255.0)));
	private Material selectedBackgroundMaterial = new Material(new Vec3((float) (35 / 255.0)));

	private Material deselectedBorderMaterial = new Material(new Vec3((float) (55 / 255.0)));
	private Material selectedBorderMaterial = new Material(new Vec3((float) (65 / 255.0)));

	private int minWidth = 50;
	private int minHeight = this.titleBarHeight + 10;

	//if an edge is grabbed, then the title bar cannot be grabbed, and vice versa
	private boolean titleBarGrabbed = false;
	private long titleBarSceneMouseEntityID = -1;
	private int titleBarGrabMouseX, titleBarGrabMouseY;

	private int edgeGrabTolerancePx = 5; //how many pixels away from the edge can you be to still grab it?
	private boolean leftEdgeGrabbed = false;
	private boolean rightEdgeGrabbed = false;
	private boolean bottomEdgeGrabbed = false;
	private boolean topEdgeGrabbed = false;

	public AdjustableWindow(int xOffset, int yOffset, int width, int height, String title, Window parentWindow) {
		super(xOffset, yOffset, width, height, parentWindow);

		this.uiScreen = new UIScreen();

		this.backgroundRect = new UIFilledRectangle(0, 0, -10, this.getWidth(), this.getHeight(), BACKGROUND_SCENE);
		this.backgroundRect.setFillWidth(true);
		this.backgroundRect.setFillHeight(true);
		this.backgroundRect.setFillWidthMargin(0);
		this.backgroundRect.setFillHeightMargin(0);
		this.backgroundRect.setMaterial(this.deselectedBackgroundMaterial);
		this.backgroundRect.bind(this.rootUIElement);

		this.titleBarRect = new UIFilledRectangle(0, 0, -9, this.getWidth(), this.titleBarHeight, TITLE_BAR_SCENE);
		this.titleBarRect.setFillWidth(true);
		this.titleBarRect.setFillWidthMargin(0);
		this.titleBarRect.setMaterial(this.titleBarMaterial);
		this.titleBarRect.bind(this.rootUIElement);
		this.titleBarRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.titleBarRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);

		this.title = title;
		this.titleBarText = new Text(this.titleLeftMargin, 0, this.title, 12, Color.WHITE, TITLE_BAR_SCENE);
		this.titleBarText.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_TOP);
		this.titleBarText.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
		this.titleBarText.setBackgroundColor(new Color(this.titleBarMaterial.getDiffuse().x, this.titleBarMaterial.getDiffuse().y, this.titleBarMaterial.getDiffuse().z));
		this.titleBarText.setDrawBackground(true);
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
	}

	@Override
	protected void _resize() {
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
	}

	@Override
	protected void _update() {
		{
			int newWidth = this.getWidth();
			int newHeight = this.getHeight();

			int newXOffset = this.getXOffset();
			int newYOffset = this.getYOffset();
			if (this.leftEdgeGrabbed) {
				newXOffset += (int) this.getRelativeMousePos().x;
				newWidth -= (int) this.getRelativeMousePos().x;
				if (newWidth < this.minWidth) {
					newWidth = this.minWidth;
					newXOffset = this.getXOffset() - (this.minWidth - this.getWidth());
				}
			}
			if (this.rightEdgeGrabbed) {
				newWidth = (int) this.getRelativeMousePos().x;
				newWidth = Math.max(newWidth, this.minWidth);
			}
			if (this.topEdgeGrabbed) {
				newHeight -= (int) this.getRelativeMousePos().y;
				newHeight = Math.max(newHeight, this.minHeight);
			}
			if (this.bottomEdgeGrabbed) {
				newHeight = (int) this.getRelativeMousePos().y;
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

		if (this.titleBarGrabbed) {
			int newXOffset = this.getXOffset() + ((int) (this.getRelativeMousePos().x) - this.titleBarGrabMouseX);
			int newYOffset = this.getYOffset() - ((int) (this.getRelativeMousePos().y) - this.titleBarGrabMouseY);
			this.setOffset(newXOffset, newYOffset);
		}
	}

	@Override
	protected void _renderContent(Framebuffer outputBuffer) {
		this.uiScreen.setUIScene(BACKGROUND_SCENE);
		this.uiScreen.render(outputBuffer);
	}

	@Override
	protected void _renderOverlay(Framebuffer outputBuffer) {
		this.uiScreen.setUIScene(TITLE_BAR_SCENE);
		this.uiScreen.render(outputBuffer);

		Vec2 mousePos = this.getRelativeMousePos();
		this.titleBarSceneMouseEntityID = this.uiScreen.getEntityIDAtCoord((int) mousePos.x, (int) (this.getHeight() - mousePos.y));

		this.uiScreen.setUIScene(BORDER_SCENE);
		this.uiScreen.render(outputBuffer);
	}

	//protected abstract void __render(Framebuffer outputBuffer);

	@Override
	protected void selected() {
		this.backgroundRect.setMaterial(this.selectedBackgroundMaterial);
		for (int i = 0; i < 4; i++) {
			Model.updateInstance(this.windowBorder[i], this.selectedBorderMaterial);
		}
	}

	@Override
	protected void deselected() {
		this.backgroundRect.setMaterial(this.deselectedBackgroundMaterial);
		for (int i = 0; i < 4; i++) {
			Model.updateInstance(this.windowBorder[i], this.deselectedBorderMaterial);
		}
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
	}

	@Override
	protected void _mouseReleased(int button) {
		this.leftEdgeGrabbed = false;
		this.rightEdgeGrabbed = false;
		this.bottomEdgeGrabbed = false;
		this.topEdgeGrabbed = false;
		this.titleBarGrabbed = false;
	}

	@Override
	protected void _mouseScrolled(float wheelOffset, float smoothOffset) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _keyPressed(int key) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _keyReleased(int key) {
		// TODO Auto-generated method stub

	}

}
