package lwjglengine.input;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL33.*;

import java.awt.Color;
import java.awt.Font;

import lwjglengine.entity.Entity;
import lwjglengine.graphics.Material;
import lwjglengine.graphics.Texture;
import lwjglengine.graphics.TextureMaterial;
import lwjglengine.input.Input;
import lwjglengine.model.FilledRectangle;
import lwjglengine.model.Model;
import lwjglengine.model.VertexArray;
import lwjglengine.scene.Scene;
import lwjglengine.ui.Text;
import lwjglengine.ui.UIElement;
import myutils.graphics.FontUtils;
import myutils.graphics.GraphicsTools;
import myutils.math.Mat4;
import myutils.math.Vec3;
import myutils.math.Vec4;

public class Button extends Input {
	// the button isn't responsible for checking if it is pressed, another class, probably ButtonManager
	// or InputManager should do that, and swap textures.

	private Text buttonText;

	private Material pressedMaterial, releasedMaterial, hoveredMaterial;
	private Material pressedTextMaterial, releasedTextMaterial, hoveredTextMaterial;
	private Material currentMaterial;

	public Button(float x, float y, float width, float height, String sID, String text, Font font, int fontSize, InputCallback callback, int selectionScene, int textScene) {
		super(x, y, 0, width, height, sID, callback, selectionScene);
		this.init(text, FontUtils.deriveSize(fontSize, font), textScene);
	}

	public Button(float x, float y, float width, float height, String sID, String text, InputCallback callback, int selectionScene, int textScene) {
		this(x, y, width, height, sID, text, Text.DEFAULT_FONT, 12, callback, selectionScene, textScene);
	}

	public Button(float x, float y, float width, float height, String sID, String text, int fontSize, InputCallback callback, int selectionScene, int textScene) {
		this(x, y, width, height, sID, text, Text.DEFAULT_FONT, fontSize, callback, selectionScene, textScene);
	}

	// text size should already be included in the font
	private void init(String text, Font font, int textScene) {
		this.setFrameAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_BOTTOM);

		this.horizontalAlignContent = UIElement.ALIGN_LEFT;
		this.verticalAlignContent = UIElement.ALIGN_BOTTOM;

		this.pressedTextMaterial = new Material(Color.YELLOW);
		this.releasedTextMaterial = new Material(Color.WHITE);

		this.buttonText = new Text(0, 0, this.z + depthSpacing, text, font, this.releasedTextMaterial, textScene);
		this.buttonText.setFrameAlignmentStyle(UIElement.FROM_CENTER_RIGHT, UIElement.FROM_CENTER_TOP);
		this.buttonText.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_CENTER);
		this.buttonText.bind(this);

		this.pressedMaterial = new Material(new Vec4(1, 1, 1, 0.5f));
		this.hoveredMaterial = new Material(new Vec4(1, 1, 1, 0.3f));
		this.releasedMaterial = new Material(new Vec4(1, 1, 1, 0.1f));

		this.setMaterial(this.releasedMaterial);

		this.currentMaterial = this.releasedMaterial;
	}

	@Override
	protected void ___kill() {

	}

	@Override
	protected void _alignContents() {

	}

	@Override
	protected void __update() {
		Material nextMaterial = null;
		if (this.pressed) {
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
			this.setMaterial(nextMaterial);
		}
	}

	public Text getButtonText() {
		return this.buttonText;
	}

	public void setPressedMaterial(Material m) {
		this.pressedMaterial = m;
	}

	public void setHoveredMaterial(Material m) {
		this.hoveredMaterial = m;
	}

	public void setReleasedMaterial(Material m) {
		this.releasedMaterial = m;
	}

	@Override
	public void keyPressed(int key) {
	}

	@Override
	public void keyReleased(int key) {
	}

}
