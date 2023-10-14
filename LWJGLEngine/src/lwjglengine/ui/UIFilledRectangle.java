package lwjglengine.ui;

import lwjglengine.graphics.Material;
import lwjglengine.graphics.TextureMaterial;
import lwjglengine.model.FilledRectangle;
import lwjglengine.model.Model;
import myutils.math.Mat4;

public class UIFilledRectangle extends UIElement {

	//this is pretty much deprecated. 
	//base UIElement has all this functionality. 

	//still here since it's used alot in the project

	public UIFilledRectangle(float xOffset, float yOffset, float z, float width, float height, int scene) {
		super(xOffset, yOffset, z, width, height, scene);
	}

	public UIFilledRectangle(float xOffset, float yOffset, float z, float width, float height, FilledRectangle rectangle, int scene) {
		super(xOffset, yOffset, z, width, height, rectangle, scene);
	}

	@Override
	protected void __kill() {

	}

	@Override
	protected void _alignContents() {

	}

	@Override
	protected void _update() {

	}

}
