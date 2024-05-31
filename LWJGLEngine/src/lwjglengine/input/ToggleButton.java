package lwjglengine.input;

import java.awt.Color;
import java.awt.Font;

import lwjglengine.graphics.Material;
import lwjglengine.model.FilledRectangle;
import lwjglengine.ui.Text;
import lwjglengine.ui.UIElement;
import myutils.graphics.FontUtils;
import myutils.graphics.GraphicsTools;
import myutils.math.Vec4;

public class ToggleButton extends Input {

	private Text buttonText;

	private boolean isToggled = false;

	private Material pressedMaterial, releasedMaterial, hoveredMaterial, toggledMaterial;
	private Material currentMaterial;

	//if true, text will change to toggledText and untoggledText depending on toggle status. 
	private boolean changeTextOnToggle = false;
	private String toggledText = "True";
	private String untoggledText = "False";
	private String defaultText;

	public ToggleButton(float x, float y, float width, float height, String sID, String text, Font font, int fontSize, InputCallback callback, int selectionScene, int textScene) {
		super(x, y, 0, width, height, sID, callback, selectionScene);
		this.init(text, FontUtils.deriveSize(fontSize, font), textScene);
	}

	public ToggleButton(float x, float y, float width, float height, String sID, String text, InputCallback callback, int selectionScene, int textScene) {
		this(x, y, width, height, sID, text, new Font("Dialogue", Font.PLAIN, 12), 12, callback, selectionScene, textScene);
	}

	public ToggleButton(float x, float y, float width, float height, String sID, String text, int fontSize, InputCallback callback, int selectionScene, int textScene) {
		this(x, y, width, height, sID, text, new Font("Dialogue", Font.PLAIN, 12), fontSize, callback, selectionScene, textScene);
	}

	// text size should already be included in the font
	private void init(String text, Font font, int textScene) {
		this.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_BOTTOM);
		this.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_BOTTOM);

		this.defaultText = text;
		this.buttonText = new Text(0, 0, this.z + depthSpacing, text, font, new Material(Color.WHITE), textScene);
		this.buttonText.setFrameAlignmentStyle(UIElement.FROM_CENTER_RIGHT, UIElement.FROM_CENTER_TOP);
		this.buttonText.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_CENTER);
		this.buttonText.bind(this);

		this.pressedMaterial = new Material(new Vec4(1, 1, 1, 0.5f));
		this.hoveredMaterial = new Material(new Vec4(1, 1, 1, 0.3f));
		this.releasedMaterial = new Material(new Vec4(1, 1, 1, 0.1f));
		this.toggledMaterial = new Material(new Vec4(1, 1, 1, 0.45f));

		this.setMaterial(this.releasedMaterial);

		this.currentMaterial = this.releasedMaterial;
	}

	@Override
	protected void _clicked() {
		this.setIsToggled(!this.isToggled);
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
		else if (this.isToggled) {
			nextMaterial = this.toggledMaterial;
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

	public boolean isToggled() {
		return this.isToggled;
	}

	public void setIsToggled(boolean b) {
		if (this.isToggled == b) {
			return;
		}

		this.isToggled = b;

		if (this.changeTextOnToggle) {
			String nextText = this.isToggled ? this.toggledText : this.untoggledText;
			this.setButtonText(nextText);
		}

		this.notifyInputChanged();
	}

	private void setButtonText(String text) {
		this.buttonText.setWidth(GraphicsTools.calculateTextWidth(text, this.buttonText.getFont()));
		this.buttonText.setText(text);
		this.buttonText.align();
	}

	public void setChangeTextOnToggle(boolean b) {
		if (this.changeTextOnToggle == b) {
			return;
		}
		this.changeTextOnToggle = b;
		if (this.changeTextOnToggle) {
			String nextText = this.isToggled ? this.toggledText : this.untoggledText;
			this.setButtonText(nextText);
		}
		else {
			this.setButtonText(this.defaultText);
		}
	}

	public void setToggledText(String s) {
		this.toggledText = s;
		if (this.isToggled) {
			this.setButtonText(this.toggledText);
		}
	}

	public void setUntoggledText(String s) {
		this.untoggledText = s;
		if (!this.isToggled) {
			this.setButtonText(this.untoggledText);
		}
	}

	@Override
	public void keyPressed(int key) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyReleased(int key) {
		// TODO Auto-generated method stub

	}

}
