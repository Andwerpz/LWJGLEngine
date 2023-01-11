package ui;

import graphics.Material;
import graphics.TextureMaterial;
import model.FilledRectangle;
import model.Model;
import util.Mat4;

public class UIFilledRectangle extends UIElement {

	private long rectangleID;

	public UIFilledRectangle(int xOffset, int yOffset, float z, int width, int height, int scene) {
		super(xOffset, yOffset, z, width, height, scene);
		this.rectangleID = FilledRectangle.DEFAULT_RECTANGLE.addRectangle(xOffset, yOffset, width, height, scene);
		this.registerModelInstance(this.rectangleID);
	}

	public UIFilledRectangle(int xOffset, int yOffset, float z, int width, int height, FilledRectangle rectangle, int scene) {
		super(xOffset, yOffset, z, width, height, scene);
		this.rectangleID = rectangle.addRectangle(xOffset, yOffset, width, height, scene);
		this.registerModelInstance(this.rectangleID);
	}

	public void setMaterial(Material m) {
		this.updateModelInstance(this.rectangleID, m);
	}

	public void setTextureMaterial(TextureMaterial m) {
		Model rect = Model.getModel(rectangleID);
		rect.setTextureMaterial(m);
	}

	@Override
	protected void __kill() {
	}

	@Override
	protected void _alignContents() {
		this.updateModelInstance(this.rectangleID, Mat4.scale(this.width, this.height, 1).mul(Mat4.translate(this.alignedX, this.alignedY, this.z)));
	}

	@Override
	protected void update() {
	}

}
