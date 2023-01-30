package ui;

import graphics.Material;
import graphics.TextureMaterial;
import model.FilledRectangle;
import model.Model;
import util.Mat4;

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
