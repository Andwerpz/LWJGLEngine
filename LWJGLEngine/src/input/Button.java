package input;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL33.*;

import java.awt.Color;
import java.awt.Font;

import entity.Entity;
import graphics.TextureMaterial;
import graphics.Material;
import graphics.Texture;
import graphics.VertexArray;
import model.FilledRectangle;
import model.Model;
import scene.Scene;
import ui.Text;
import ui.UIElement;
import util.FontUtils;
import util.GraphicsTools;
import util.Mat4;
import util.Vec3;
import util.Vec4;

public class Button extends Input {

	// for now, each button has it's own button model. Later, i would like it so that all buttons
	// of the same shape and size can share one model, or something like that.

	// the button isn't responsible for checking if it is pressed, another class, probably ButtonManager
	// or InputManager should do that, and swap textures.

	private long buttonInnerID;
	private Text buttonText;

	private Material pressedMaterial, releasedMaterial, hoveredMaterial;
	private Material pressedTextMaterial, releasedTextMaterial, hoveredTextMaterial;
	private Material currentMaterial;

	public Button(int x, int y, int width, int height, String sID, String text, Font font, int fontSize, int scene) {
		super(x, y, 0, width, height, sID, scene);
		this.init(text, FontUtils.deriveSize(fontSize, font));
	}

	// text size should already be included in the font
	private void init(String text, Font font) {
		this.setFrameAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_BOTTOM);

		this.horizontalAlignContent = UIElement.ALIGN_LEFT;
		this.verticalAlignContent = UIElement.ALIGN_BOTTOM;

		this.pressedTextMaterial = new Material(Color.YELLOW);
		this.releasedTextMaterial = new Material(Color.WHITE);

		this.buttonText = new Text(0, 0, this.z + depthSpacing, text, font, this.releasedTextMaterial, scene);
		this.buttonText.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_CENTER);

		this.pressedMaterial = new Material(new Vec4(0, 0, 0, 0.6f));
		this.hoveredMaterial = new Material(new Vec4(0, 0, 0, 0.3f));
		this.releasedMaterial = new Material(new Vec4(0, 0, 0, 0.1f));

		this.buttonInnerID = FilledRectangle.DEFAULT_RECTANGLE.addRectangle(this.x, this.y, this.z, this.width, this.height, this.scene);
		this.registerModelInstance(this.buttonInnerID);
		this.updateModelInstance(this.buttonInnerID, releasedMaterial);

		this.currentMaterial = this.releasedMaterial;
	}

	@Override
	protected void ___kill() {
		this.buttonText.kill();
	}

	@Override
	protected void _alignContents() {
		Mat4 modelMat4 = Mat4.scale(this.width, this.height, 1).mul(Mat4.translate(new Vec3(this.alignedX, this.alignedY, this.z)));
		this.updateModelInstance(this.buttonInnerID, modelMat4);

		int centerX = alignedX + this.width / 2;
		int centerY = alignedY + this.height / 2;
		this.buttonText.setFrameAlignment(UIElement.FROM_LEFT, UIElement.FROM_BOTTOM, centerX, centerY);
		this.buttonText.align();
	}

	@Override
	public void update() {
		Material nextMaterial = null;
		if (this.clicked) { // check for clicks happens when mouse is released.
			this.clicked = false;
			nextMaterial = this.pressedMaterial;
		}
		else if (this.pressed) {
			nextMaterial = this.pressedMaterial;
		}
		else if (this.hovered) {
			nextMaterial = this.hoveredMaterial;
		}
		else {
			nextMaterial = this.releasedMaterial;
		}
		if (this.currentMaterial != nextMaterial) {
			this.currentMaterial = nextMaterial;
			this.updateModelInstance(this.buttonInnerID, this.currentMaterial);
		}
	}

}
