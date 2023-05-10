package lwjglengine.v10.window;

import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.graphics.Material;
import lwjglengine.v10.model.Line;
import lwjglengine.v10.model.Model;
import lwjglengine.v10.scene.Scene;
import lwjglengine.v10.screen.UIScreen;
import myutils.v10.math.Vec3;

public abstract class BorderedWindow extends Window {
	//1px wide window borders. 

	//change color on subtree select. 

	//renders border as overlay

	private final int BORDER_SCENE = Scene.generateScene();

	private UIScreen uiScreen;

	private Material deselectedBorderMaterial = new Material(new Vec3((float) (55 / 255.0)));
	private Material selectedBorderMaterial = new Material(new Vec3((float) (65 / 255.0)));

	private long[] windowBorder;

	private boolean renderBorder = true;

	public BorderedWindow(int xOffset, int yOffset, int width, int height, Window parentWindow) {
		super(xOffset, yOffset, width, height, parentWindow);

		this.uiScreen = new UIScreen();

		this.windowBorder = new long[4];
	}

	public void setRenderBorder(boolean b) {
		this.renderBorder = b;
	}

	@Override
	protected void _kill() {
		this.uiScreen.kill();
		Scene.removeScene(BORDER_SCENE);
		this.__kill();
	}

	protected abstract void __kill();

	@Override
	protected void _resize() {
		this.uiScreen.setScreenDimensions(this.getWidth(), this.getHeight());

		for (int i = 0; i < 4; i++) {
			Model.removeInstance(this.windowBorder[i]);
		}

		this.windowBorder[0] = Line.addLine(0, 1, this.getWidth(), 1, BORDER_SCENE);
		this.windowBorder[1] = Line.addLine(1, 0, 1, this.getHeight(), BORDER_SCENE);
		this.windowBorder[2] = Line.addLine(this.getWidth(), 0, this.getWidth(), this.getHeight(), BORDER_SCENE);
		this.windowBorder[3] = Line.addLine(0, this.getHeight(), this.getWidth(), this.getHeight(), BORDER_SCENE);

		for (int i = 0; i < 4; i++) {
			Model.updateInstance(this.windowBorder[i], this.isSubtreeSelected() ? this.selectedBorderMaterial : this.deselectedBorderMaterial);
		}

		this.__resize();
	}

	protected abstract void __resize();

	@Override
	protected void renderOverlay(Framebuffer outputBuffer) {
		this._renderOverlay(outputBuffer);

		if (this.renderBorder) {
			this.uiScreen.setUIScene(BORDER_SCENE);
			this.uiScreen.render(outputBuffer);
		}
	}

	protected abstract void _renderOverlay(Framebuffer outputBuffer);

	@Override
	protected void subtreeSelected() {
		for (int i = 0; i < 4; i++) {
			Model.updateInstance(this.windowBorder[i], this.selectedBorderMaterial);
		}

		this._subtreeSelected();
	}

	@Override
	protected void subtreeDeselected() {
		for (int i = 0; i < 4; i++) {
			Model.updateInstance(this.windowBorder[i], this.deselectedBorderMaterial);
		}

		this._subtreeDeselected();
	}

	protected abstract void _subtreeSelected();

	protected abstract void _subtreeDeselected();

}
