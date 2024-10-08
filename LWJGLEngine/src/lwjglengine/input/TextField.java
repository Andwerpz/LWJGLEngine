package lwjglengine.input;

import static org.lwjgl.glfw.GLFW.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import lwjglengine.entity.Entity;
import lwjglengine.graphics.Material;
import lwjglengine.graphics.Texture;
import lwjglengine.graphics.TextureMaterial;
import lwjglengine.model.FilledRectangle;
import lwjglengine.ui.Text;
import lwjglengine.ui.UIElement;
import myutils.graphics.FontUtils;
import myutils.graphics.GraphicsTools;
import myutils.math.Mat4;
import myutils.math.MathUtils;
import myutils.math.Vec2;
import myutils.math.Vec3;
import myutils.math.Vec4;

public class TextField extends Input {

	//private long fieldInnerID;
	private Text fieldText;

	private String text, hintText;
	private int textLeftMargin = 5;
	private int textRightMargin = 5;
	private Font font;

	public static final int FIELD_TYPE_DEFAULT = 0;
	public static final int FIELD_TYPE_INT = 1;
	public static final int FIELD_TYPE_FLOAT = 2;

	private int fieldType = FIELD_TYPE_DEFAULT;

	private static final String INT_REGEX = "[-+]?[0-9]+";
	private long intFieldDragIncrement = 1;
	private long intFieldMinimum = Long.MIN_VALUE;
	private long intFieldMaximum = Long.MAX_VALUE;

	//same as text field, but only allows floats. 
	//supports dragging to the left/right to increment/decrement the value held in the field by a set amount. 
	//use doubles here for more precision and range
	private static final String FLOAT_REGEX = "[-+]?[0-9]*\\.?[0-9]+";
	private double floatFieldDragIncrement = 0.01; //how much will the input change per pixel
	private double floatFieldMinimum = -Double.MAX_VALUE;
	private double floatFieldMaximum = Double.MAX_VALUE;
	private DecimalFormat floatFieldFormat = new DecimalFormat("0.00####;-0.00####");

	private static final String DEFAULT_REGEX = ".*"; //default should match anything. 
	private String validInputRegex = DEFAULT_REGEX;
	private boolean isInputValid = true;

	private HashSet<Integer> pressedKeys; // stores key codes, not chars

	private Material textMaterial, hintTextMaterial;
	private Material releasedMaterial, pressedMaterial, hoveredMaterial, selectedMaterial;
	private Material invalidInputMaterial;
	private Material currentMaterial;

	private boolean textWrapping = false;

	public TextField(float x, float y, float width, float height, String sID, String hintText, Font font, int fontSize, InputCallback callback, int selectionScene, int textScene) {
		super(x, y, 0, width, height, sID, callback, selectionScene);
		this.init(hintText, FontUtils.deriveSize(fontSize, font), textScene);
	}

	public TextField(float x, float y, float width, float height, String sID, String hintText, InputCallback callback, int selectionScene, int textScene) {
		this(x, y, width, height, sID, hintText, Text.DEFAULT_FONT, 12, callback, selectionScene, textScene);
	}

	public TextField(float x, float y, float width, float height, String sID, String hintText, int fontSize, InputCallback callback, int selectionScene, int textScene) {
		this(x, y, width, height, sID, hintText, Text.DEFAULT_FONT, fontSize, callback, selectionScene, textScene);
	}

	private void init(String hintText, Font font, int textScene) {
		this.font = font;

		this.setFrameAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_BOTTOM);

		this.horizontalAlignContent = UIElement.ALIGN_LEFT;
		this.verticalAlignContent = UIElement.ALIGN_BOTTOM;

		this.text = "";
		this.hintText = hintText;
		this.pressedKeys = new HashSet<>();

		this.textMaterial = new Material(Color.WHITE);
		this.hintTextMaterial = new Material(new Vec4(1, 1, 1, 0.3f));

		float textWidth = this.width - (textLeftMargin + textRightMargin);
		textWidth = Math.max(textWidth, 1);

		this.fieldText = new Text(textLeftMargin, 0, z + depthSpacing, textWidth, hintText, font, this.hintTextMaterial, textScene);
		this.fieldText.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_TOP);
		this.fieldText.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
		this.fieldText.bind(this);

		this.pressedMaterial = new Material(new Vec4(1, 1, 1, 0.5f));
		this.selectedMaterial = new Material(new Vec4(1, 1, 1, 0.45f));
		this.hoveredMaterial = new Material(new Vec4(1, 1, 1, 0.3f));
		this.releasedMaterial = new Material(new Vec4(1, 1, 1, 0.1f));

		this.invalidInputMaterial = new Material(new Vec4(1, 0, 0, 0.3f));

		this.setMaterial(this.releasedMaterial);

		this.checkIfInputTextIsValid();
	}

	@Override
	protected void __update() {
		// -- FIELD INNER --
		Material nextMaterial = null;
		if (this.isClicked()) {
			nextMaterial = this.selectedMaterial;
		}
		else if (this.pressed) {
			nextMaterial = this.pressedMaterial;
		}
		else if (this.hovered) {
			nextMaterial = this.hoveredMaterial;
		}
		else if (!this.isInputValid) {
			nextMaterial = this.invalidInputMaterial;
		}
		else {
			nextMaterial = this.releasedMaterial;
		}
		if (this.currentMaterial != nextMaterial) {
			this.currentMaterial = nextMaterial;
			this.setMaterial(this.currentMaterial);
		}

		// -- NUMERICAL DRAG INPUTS --
		switch (this.fieldType) {
		case FIELD_TYPE_INT: {
			if (this.isPressed() && this.text.matches(this.validInputRegex)) {
				//increment current input
				long curInt = Long.parseLong(this.getText());
				Vec2 mouseDiff = MouseInput.getMouseDiff();
				int horizontalDiff = (int) Math.abs(mouseDiff.x) * (mouseDiff.x < 0 ? -1 : 1);
				curInt += horizontalDiff * this.intFieldDragIncrement;
				curInt = MathUtils.clamp(this.intFieldMinimum, this.intFieldMaximum, curInt);
				this.setText(curInt + "");
			}
			break;
		}

		case FIELD_TYPE_FLOAT: {
			if (this.isPressed() && this.text.matches(this.validInputRegex)) {
				//increment current input
				double curFloat = Double.parseDouble(this.getText());
				Vec2 mouseDiff = MouseInput.getMouseDiff();
				int horizontalDiff = (int) Math.abs(mouseDiff.x) * (mouseDiff.x < 0 ? -1 : 1);
				curFloat += horizontalDiff * this.floatFieldDragIncrement;
				curFloat = MathUtils.clamp(this.floatFieldMinimum, this.floatFieldMaximum, curFloat);
				this.setText(this.floatFieldFormat.format(curFloat));
			}
			break;
		}
		}
	}

	@Override
	protected void ___kill() {
		this.fieldText.kill();
	}

	@Override
	protected void _alignContents() {
		float textWidth = this.width - (textLeftMargin + textRightMargin);
		textWidth = Math.max(textWidth, 1);

		this.fieldText.setWidth(textWidth);
	}

	public void setTextWrapping(boolean b) {
		this.textWrapping = b;
		if (this.textWrapping) {
			this.fieldText.setTextWrapping(true);
			this.fieldText.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
			this.fieldText.setFrameAlignmentOffset(this.textLeftMargin, this.textLeftMargin);
			this.fieldText.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		}
		else {
			this.fieldText.setTextWrapping(false);
			this.fieldText.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_TOP);
			this.fieldText.setFrameAlignmentOffset(this.textLeftMargin, 0);
			this.fieldText.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
		}
	}

	public String getText() {
		return this.text;
	}

	public void setText(String text) {
		if (this.text.equals(text)) {
			return;
		}

		this.text = text;

		if (this.text.length() == 0) {
			this.fieldText.setMaterial(this.hintTextMaterial);
			this.fieldText.setText(this.hintText);
		}
		else {
			this.fieldText.setMaterial(this.textMaterial);
			this.fieldText.setText(this.text);
		}

		this.checkIfInputTextIsValid();

		if (this.isInputValid) {
			this.notifyInputChanged();
		}
	}

	private void checkIfInputTextIsValid() {
		this.isInputValid = true;

		if (!this.text.matches(this.validInputRegex)) {
			this.isInputValid = false;
			return;
		}

		switch (this.fieldType) {
		case FIELD_TYPE_INT: {
			long curInt = Long.parseLong(this.text);
			if (curInt < this.intFieldMinimum || curInt > this.intFieldMaximum) {
				this.isInputValid = false;
			}
			break;
		}

		case FIELD_TYPE_FLOAT: {
			double curFloat = Double.parseDouble(this.text);
			if (curFloat < this.floatFieldMinimum || curFloat > this.floatFieldMaximum) {
				this.isInputValid = false;
			}
			break;
		}
		}
	}

	public void setValidInputRegex(String regex) {
		if (this.fieldType == FIELD_TYPE_INT && !regex.equals(INT_REGEX)) {
			return;
		}
		if (this.fieldType == FIELD_TYPE_FLOAT && !regex.equals(FLOAT_REGEX)) {
			return;
		}

		this.validInputRegex = regex;
		this.checkIfInputTextIsValid();
	}

	public void setFieldType(int type) {
		this.fieldType = type;
		switch (type) {
		case FIELD_TYPE_DEFAULT: {
			//do nothing
			break;
		}

		case FIELD_TYPE_INT: {
			this.setValidInputRegex(INT_REGEX);
			this.setText("0");
			break;
		}

		case FIELD_TYPE_FLOAT: {
			this.setValidInputRegex(FLOAT_REGEX);
			this.setText(this.floatFieldFormat.format(0));
			break;
		}
		}
	}

	public void setIntFieldMinimum(long i) {
		this.intFieldMinimum = i;
	}

	public void setIntFieldMaximum(long i) {
		this.intFieldMaximum = i;
	}

	public void setIntFieldDragIncrement(long i) {
		this.intFieldDragIncrement = i;
	}

	public void setFloatFieldMinimum(double f) {
		this.floatFieldMinimum = f;
	}

	public void setFloatFieldMaximum(double f) {
		this.floatFieldMaximum = f;
	}

	public void setFloatFieldDragIncrement(double f) {
		this.floatFieldDragIncrement = f;
	}

	public void setFloatFieldFormat(DecimalFormat df) {
		this.floatFieldFormat = df;
	}

	public boolean isInputValid() {
		return this.isInputValid;
	}

	public Text getTextUIElement() {
		return this.fieldText;
	}

	public void setReleasedMaterial(Material m) {
		this.releasedMaterial = m;
	}

	public void setHoveredMaterial(Material m) {
		this.hoveredMaterial = m;
	}

	public void setPressedMaterial(Material m) {
		this.pressedMaterial = m;
	}

	public void setSelectedMaterial(Material m) {
		this.selectedMaterial = m;
	}

	public void setTextMaterial(Material m) {
		this.textMaterial = m;
	}

	public void setHintTextMaterial(Material m) {
		this.hintTextMaterial = m;
	}

	@Override
	public void keyPressed(int key) {
		if (this.isClicked()) {
			pressedKeys.add(key);
			String n_text = this.text;

			// looking for ctrl + v
			if ((pressedKeys.contains(GLFW_KEY_LEFT_CONTROL) || pressedKeys.contains(GLFW_KEY_RIGHT_CONTROL)) && pressedKeys.contains(GLFW_KEY_V)) {
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				String result = "";
				try {
					result = (String) clipboard.getData(DataFlavor.stringFlavor);
				}
				catch (UnsupportedFlavorException e) {
					e.printStackTrace();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
				n_text += result;
			}
			else if (key == GLFW_KEY_BACKSPACE) {
				if (n_text.length() != 0) {
					n_text = n_text.substring(0, n_text.length() - 1);
				}
			}
			else if (key == GLFW_KEY_SPACE) {
				n_text += " ";
			}
			else {
				String keyName = glfwGetKeyName(key, 0);
				if (keyName == null) {
					return;
				}
				char k = keyName.charAt(0);
				if (pressedKeys.contains(GLFW_KEY_LEFT_SHIFT) || pressedKeys.contains(GLFW_KEY_RIGHT_SHIFT)) {
					k = KeyboardInput.shiftMap.get(k);
				}
				n_text += k;
			}
			this.setText(n_text);
		}
	}

	@Override
	public void keyReleased(int key) {
		if (this.isClicked()) {
			if (pressedKeys.contains(key)) {
				pressedKeys.remove(key);
			}
		}
	}

}
