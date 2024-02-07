package lwjglengine.window;

import java.awt.Color;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import lwjglengine.graphics.Framebuffer;
import lwjglengine.graphics.Material;
import lwjglengine.input.Input;
import lwjglengine.input.TextField;
import lwjglengine.input.ToggleButton;
import lwjglengine.screen.UIScreen;
import lwjglengine.ui.Text;
import lwjglengine.ui.UIElement;
import lwjglengine.ui.UIFilledRectangle;
import lwjglengine.ui.UISection;
import lwjglengine.ui.UISectionListener;
import myutils.math.MathUtils;
import myutils.math.Vec2;
import myutils.math.Vec3;
import myutils.math.Vec4;
import myutils.misc.Pair;
import myutils.misc.Triple;

public class ObjectEditorWindow extends Window implements UISectionListener {
	//this is an editor that we should be able to put any object inside and edit. 

	//it will first list out all getters and setters of the given class, and will generate input fields 
	//based off of the type of fields it sees. 

	//it will only generate input fields for some predetermined set of basic types. 

	//TODO 
	// - only call the get and set functions when we need to. 
	//   - calling the setters every frame is actually very expensive. 
	// - add filter to search for attributes. 
	//	 - perhaps we can just regenerate all input fields with the filter in mind. 

	private static final int CLASS_TYPE_UNKNOWN = -1;

	private static final int CLASS_TYPE_FLOAT = 0;
	private static final int CLASS_TYPE_DOUBLE = 1;
	private static final int CLASS_TYPE_BYTE = 2;
	private static final int CLASS_TYPE_SHORT = 3;
	private static final int CLASS_TYPE_INTEGER = 4;
	private static final int CLASS_TYPE_LONG = 5;
	private static final int CLASS_TYPE_CHAR = 6;
	private static final int CLASS_TYPE_STRING = 7;
	private static final int CLASS_TYPE_BOOLEAN = 8;

	private static final int CLASS_TYPE_VEC2 = 9;
	private static final int CLASS_TYPE_VEC3 = 10;
	private static final int CLASS_TYPE_VEC4 = 11;

	private UISection editorSection;

	//bounding boxes for all the input fields. 
	//we'll use these to adjust spacing and placement of inputs. 
	private ArrayList<UIFilledRectangle> inputFields;

	//class type, getter, setter
	private HashMap<String, Triple<Integer, Method, Method>> variableNameToMethods;

	private Object object;

	private static int verticalMargin = 5;
	private static int horizontalMargin = 5;

	private static int verticalPadding = 5;

	private UIFilledRectangle editorBackgroundRect;
	private int editorHeight = 0;

	public ObjectEditorWindow(Window parentWindow) {
		super(parentWindow);
		this.init();
	}

	public ObjectEditorWindow(Object object, Window parentWindow) {
		super(parentWindow);
		this.init();
		this.setObject(object);
	}

	public ObjectEditorWindow(Object object) {
		super(null);
		this.init();
		this.setObject(object);
	}

	private void init() {
		this.editorSection = new UISection();
		this.editorSection.setIsScrollable(true);
		this.editorSection.getBackgroundRect().setFillWidth(true);
		this.editorSection.getBackgroundRect().setFillHeight(true);
		this.editorSection.getBackgroundRect().setMaterial(this.topBarDefaultMaterial);
		this.editorSection.getBackgroundRect().bind(this.rootUIElement);
		this.editorSection.getBackgroundRect().setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.editorSection.getBackgroundRect().setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);

		this.editorBackgroundRect = this.editorSection.getScrollBackgroundRect();

		this.rootUIElement.setMaterial(this.topBarDefaultMaterial);

		this.inputFields = new ArrayList<>();

		this.variableNameToMethods = new HashMap<>();

		this.editorSection.addListener(this);

		this._resize();
	}

	private void removeInputFields() {
		for (UIElement e : this.inputFields) {
			e.kill();
		}
		this.inputFields.clear();

		this.variableNameToMethods.clear();
	}

	private void generateInputFields() {
		this.removeInputFields();

		if (this.object == null) {
			return;
		}

		//go through and find all methods that start with 'set', and only have one argument.
		Method[] methods = this.object.getClass().getMethods();
		ArrayList<Method> setMethods = new ArrayList<>();
		System.out.println("ObjectEditorWindow: Found " + methods.length + " methods");
		for (Method m : methods) {
			if (m.getName().startsWith("set") && m.getParameterCount() == 1) {
				setMethods.add(m);
			}
		}

		//for each 'set' method, find a matching 'get' method. Ensure that the type of the argument of the 'set' method 
		//is the same as the return type of the 'get' method. 
		HashMap<String, Method> getMethods = new HashMap<>();
		for (Method m : methods) {
			if (m.getName().startsWith("get") && m.getParameterCount() == 0) {
				getMethods.put(m.getName().substring(3), m);
			}
		}

		ArrayList<Pair<Method, Method>> validMethodPairs = new ArrayList<>(); //'get', 'set'
		for (Method m : setMethods) {
			String variableName = m.getName().substring(3);
			if (getMethods.get(variableName) == null) { //'get' method must exist
				continue;
			}
			if (getMethods.get(variableName).getReturnType() != m.getParameters()[0].getType()) { //return type of 'get' must be same as argument in 'set'
				continue;
			}
			validMethodPairs.add(new Pair<Method, Method>(getMethods.get(variableName), m));
		}

		//sort method pairs for easier navigation of generated editor. 
		validMethodPairs.sort((a, b) -> {
			return a.first.getName().compareTo(b.first.getName());
		});

		//For all pairs of 'set' and 'get' functions, generate inputs for each. 
		for (Pair<Method, Method> p : validMethodPairs) {
			UIFilledRectangle inputRect = this.generateInputField(p.first);
			if (inputRect == null) {
				continue;
			}

			this.inputFields.add(inputRect);
			String variableName = p.first.getName().substring(3);
			char[] c = variableName.toCharArray();
			c[0] = Character.toLowerCase(c[0]);
			variableName = new String(c);
			this.variableNameToMethods.put(variableName, new Triple<Integer, Method, Method>(getClassType(p.first.getReturnType()), p.first, p.second));
		}

		//properly align input fields
		int yptr = verticalMargin;
		for (int i = 0; i < this.inputFields.size(); i++) {
			UIFilledRectangle boundingRect = this.inputFields.get(i);
			boundingRect.setYOffset(yptr);
			boundingRect.bind(this.editorBackgroundRect);

			yptr += boundingRect.getHeight() + verticalPadding;
		}
		this.editorBackgroundRect.setHeight(yptr);
		this.editorHeight = yptr;
	}

	private static int getClassType(Class<?> c) {
		if (c == Float.class || c == float.class) {
			return CLASS_TYPE_FLOAT;
		}
		else if (c == Double.class || c == double.class) {
			return CLASS_TYPE_DOUBLE;
		}
		else if (c == Byte.class || c == byte.class) {
			return CLASS_TYPE_BYTE;
		}
		else if (c == Short.class || c == short.class) {
			return CLASS_TYPE_SHORT;
		}
		else if (c == Integer.class || c == int.class) {
			return CLASS_TYPE_INTEGER;
		}
		else if (c == Long.class || c == long.class) {
			return CLASS_TYPE_LONG;
		}
		else if (c == String.class) {
			return CLASS_TYPE_STRING;
		}
		else if (c == Character.class || c == char.class) {
			return CLASS_TYPE_CHAR;
		}
		else if (c == Boolean.class || c == boolean.class) {
			return CLASS_TYPE_BOOLEAN;
		}
		else if (c == Vec2.class) {
			return CLASS_TYPE_VEC2;
		}
		else if (c == Vec3.class) {
			return CLASS_TYPE_VEC3;
		}
		else if (c == Vec4.class) {
			return CLASS_TYPE_VEC4;
		}
		else {
			return CLASS_TYPE_UNKNOWN;
		}
	}

	private UIFilledRectangle generateInputField(Method getter) {
		int classType = ObjectEditorWindow.getClassType(getter.getReturnType());
		if (classType == CLASS_TYPE_UNKNOWN) {
			return null;
		}

		//make first letter lower case
		String variableName = getter.getName().substring(3);
		char[] c = variableName.toCharArray();
		c[0] = Character.toLowerCase(c[0]);
		variableName = new String(c);

		UIFilledRectangle boundingRect = new UIFilledRectangle(0, 0, 0, 1, 1, this.editorSection.getBackgroundScene());
		boundingRect.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_TOP);
		boundingRect.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_TOP);
		boundingRect.setFillWidth(true);
		boundingRect.setFillWidthMargin(5);
		boundingRect.setMaterial(this.topBarDefaultMaterial);

		Text variableNameText = new Text(0, 0, variableName, 12, Color.WHITE, this.editorSection.getTextScene());
		variableNameText.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_TOP);
		variableNameText.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
		variableNameText.setDoAntialiasing(false);
		variableNameText.bind(boundingRect);

		if (classType == CLASS_TYPE_FLOAT || classType == CLASS_TYPE_DOUBLE || classType == CLASS_TYPE_BYTE || classType == CLASS_TYPE_SHORT || classType == CLASS_TYPE_INTEGER || classType == CLASS_TYPE_LONG) {
			//numerical primitives
			TextField textField = new TextField(0, 0, 150, 20, variableName, variableName, 12, this.editorSection.getSelectionScene(), this.editorSection.getTextScene());
			textField.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_CENTER_TOP);
			textField.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
			textField.getTextUIElement().setDoAntialiasing(false);
			textField.setFieldType(TextField.FIELD_TYPE_FLOAT);
			textField.bind(boundingRect);

			if (classType == CLASS_TYPE_BYTE || classType == CLASS_TYPE_SHORT || classType == CLASS_TYPE_INTEGER || classType == CLASS_TYPE_LONG) {
				textField.setFieldType(TextField.FIELD_TYPE_INT);

				//set bounds
				long fieldMin = 0, fieldMax = 0;
				switch (classType) {
				case CLASS_TYPE_BYTE:
					fieldMin = Byte.MIN_VALUE;
					fieldMax = Byte.MAX_VALUE;
					break;
				case CLASS_TYPE_SHORT:
					fieldMin = Short.MIN_VALUE;
					fieldMax = Short.MAX_VALUE;
					break;
				case CLASS_TYPE_INTEGER:
					fieldMin = Integer.MIN_VALUE;
					fieldMax = Integer.MAX_VALUE;
					break;
				case CLASS_TYPE_LONG:
					fieldMin = Long.MIN_VALUE;
					fieldMax = Long.MAX_VALUE;
					break;
				}

				textField.setIntFieldMinimum(fieldMin);
				textField.setIntFieldMaximum(fieldMax);
			}

			try {
				textField.setText(getter.invoke(this.object) + "");
			}
			catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			catch (InvocationTargetException e) {
				e.printStackTrace();
			}

			boundingRect.setHeight(20);
		}
		else if (classType == CLASS_TYPE_BOOLEAN) {
			//toggle button
			ToggleButton toggleButton = new ToggleButton(0, 0, 150, 20, variableName, variableName, 12, this.editorSection.getSelectionScene(), this.editorSection.getTextScene());
			toggleButton.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_CENTER_TOP);
			toggleButton.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
			toggleButton.getButtonText().setDoAntialiasing(false);
			toggleButton.setChangeTextOnToggle(true);
			toggleButton.bind(boundingRect);

			try {
				boolean isToggled = (boolean) getter.invoke(this.object);
				toggleButton.setIsToggled(isToggled);
			}
			catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			catch (InvocationTargetException e) {
				e.printStackTrace();
			}

			boundingRect.setHeight(20);
		}
		else if (classType == CLASS_TYPE_STRING || classType == CLASS_TYPE_CHAR) {
			//free text field
			TextField textField = new TextField(0, 0, 150, 20, variableName, variableName, 12, this.editorSection.getSelectionScene(), this.editorSection.getTextScene());
			textField.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_CENTER_TOP);
			textField.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
			textField.getTextUIElement().setDoAntialiasing(false);
			textField.bind(boundingRect);

			try {
				String fieldText = getter.invoke(this.object) + "";
				textField.setText(fieldText);
			}
			catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			catch (InvocationTargetException e) {
				e.printStackTrace();
			}

			if (classType == CLASS_TYPE_CHAR) {
				textField.setValidInputRegex("."); //regex to match a single character
			}

			boundingRect.setHeight(20);
		}
		else {
			//move variable text to top, since these are most likely multi input fields
			variableNameText.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_TOP);
			variableNameText.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_TOP);

			switch (classType) {
			case CLASS_TYPE_VEC2: {
				TextField tfX = new TextField(0, 25, 150, 20, variableName + ".x", variableName, 12, this.editorSection.getSelectionScene(), this.editorSection.getTextScene());
				tfX.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_TOP);
				tfX.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
				tfX.setFieldType(TextField.FIELD_TYPE_FLOAT);
				tfX.getTextUIElement().setDoAntialiasing(false);
				tfX.bind(boundingRect);

				TextField tfY = new TextField(0, 50, 150, 20, variableName + ".y", variableName, 12, this.editorSection.getSelectionScene(), this.editorSection.getTextScene());
				tfY.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_TOP);
				tfY.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
				tfY.setFieldType(TextField.FIELD_TYPE_FLOAT);
				tfY.getTextUIElement().setDoAntialiasing(false);
				tfY.bind(boundingRect);

				try {
					Vec2 vec = (Vec2) getter.invoke(this.object);
					tfX.setText(vec.x + "");
					tfY.setText(vec.y + "");
				}
				catch (IllegalAccessException e) {
					e.printStackTrace();
				}
				catch (InvocationTargetException e) {
					e.printStackTrace();
				}

				Text tX = new Text(0, 25, "X", 12, Color.WHITE, this.editorSection.getTextScene());
				tX.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
				tX.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
				tX.setDoAntialiasing(false);
				tX.bind(boundingRect);

				Text tY = new Text(0, 50, "Y", 12, Color.WHITE, this.editorSection.getTextScene());
				tY.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
				tY.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
				tY.setDoAntialiasing(false);
				tY.bind(boundingRect);

				boundingRect.setHeight(60);
				break;
			}

			case CLASS_TYPE_VEC3: {
				TextField tfX = new TextField(0, 25, 150, 20, variableName + ".x", variableName, 12, this.editorSection.getSelectionScene(), this.editorSection.getTextScene());
				tfX.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_TOP);
				tfX.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
				tfX.setFieldType(TextField.FIELD_TYPE_FLOAT);
				tfX.getTextUIElement().setDoAntialiasing(false);
				tfX.bind(boundingRect);

				TextField tfY = new TextField(0, 50, 150, 20, variableName + ".y", variableName, 12, this.editorSection.getSelectionScene(), this.editorSection.getTextScene());
				tfY.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_TOP);
				tfY.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
				tfY.setFieldType(TextField.FIELD_TYPE_FLOAT);
				tfY.getTextUIElement().setDoAntialiasing(false);
				tfY.bind(boundingRect);

				TextField tfZ = new TextField(0, 75, 150, 20, variableName + ".z", variableName, 12, this.editorSection.getSelectionScene(), this.editorSection.getTextScene());
				tfZ.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_TOP);
				tfZ.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
				tfZ.setFieldType(TextField.FIELD_TYPE_FLOAT);
				tfZ.getTextUIElement().setDoAntialiasing(false);
				tfZ.bind(boundingRect);

				try {
					Vec3 vec = (Vec3) getter.invoke(this.object);
					tfX.setText(vec.x + "");
					tfY.setText(vec.y + "");
					tfZ.setText(vec.z + "");
				}
				catch (IllegalAccessException e) {
					e.printStackTrace();
				}
				catch (InvocationTargetException e) {
					e.printStackTrace();
				}

				Text tX = new Text(0, 25, "X", 12, Color.WHITE, this.editorSection.getTextScene());
				tX.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
				tX.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
				tX.setDoAntialiasing(false);
				tX.bind(boundingRect);

				Text tY = new Text(0, 50, "Y", 12, Color.WHITE, this.editorSection.getTextScene());
				tY.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
				tY.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
				tY.setDoAntialiasing(false);
				tY.bind(boundingRect);

				Text tZ = new Text(0, 75, "Z", 12, Color.WHITE, this.editorSection.getTextScene());
				tZ.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
				tZ.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
				tZ.setDoAntialiasing(false);
				tZ.bind(boundingRect);

				boundingRect.setHeight(85);
				break;
			}

			case CLASS_TYPE_VEC4: {
				TextField tfX = new TextField(0, 25, 150, 20, variableName + ".x", variableName, 12, this.editorSection.getSelectionScene(), this.editorSection.getTextScene());
				tfX.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_TOP);
				tfX.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
				tfX.setFieldType(TextField.FIELD_TYPE_FLOAT);
				tfX.getTextUIElement().setDoAntialiasing(false);
				tfX.bind(boundingRect);

				TextField tfY = new TextField(0, 50, 150, 20, variableName + ".y", variableName, 12, this.editorSection.getSelectionScene(), this.editorSection.getTextScene());
				tfY.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_TOP);
				tfY.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
				tfY.setFieldType(TextField.FIELD_TYPE_FLOAT);
				tfY.getTextUIElement().setDoAntialiasing(false);
				tfY.bind(boundingRect);

				TextField tfZ = new TextField(0, 75, 150, 20, variableName + ".z", variableName, 12, this.editorSection.getSelectionScene(), this.editorSection.getTextScene());
				tfZ.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_TOP);
				tfZ.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
				tfZ.setFieldType(TextField.FIELD_TYPE_FLOAT);
				tfZ.getTextUIElement().setDoAntialiasing(false);
				tfZ.bind(boundingRect);

				TextField tfW = new TextField(0, 100, 150, 20, variableName + ".w", variableName, 12, this.editorSection.getSelectionScene(), this.editorSection.getTextScene());
				tfW.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_TOP);
				tfW.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
				tfW.setFieldType(TextField.FIELD_TYPE_FLOAT);
				tfW.getTextUIElement().setDoAntialiasing(false);
				tfW.bind(boundingRect);

				try {
					Vec4 vec = (Vec4) getter.invoke(this.object);
					tfX.setText(vec.x + "");
					tfY.setText(vec.y + "");
					tfZ.setText(vec.z + "");
					tfW.setText(vec.w + "");
				}
				catch (IllegalAccessException e) {
					e.printStackTrace();
				}
				catch (InvocationTargetException e) {
					e.printStackTrace();
				}

				Text tX = new Text(0, 25, "X", 12, Color.WHITE, this.editorSection.getTextScene());
				tX.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
				tX.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
				tX.setDoAntialiasing(false);
				tX.bind(boundingRect);

				Text tY = new Text(0, 50, "Y", 12, Color.WHITE, this.editorSection.getTextScene());
				tY.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
				tY.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
				tY.setDoAntialiasing(false);
				tY.bind(boundingRect);

				Text tZ = new Text(0, 75, "Z", 12, Color.WHITE, this.editorSection.getTextScene());
				tZ.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
				tZ.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
				tZ.setDoAntialiasing(false);
				tZ.bind(boundingRect);

				Text tW = new Text(0, 100, "W", 12, Color.WHITE, this.editorSection.getTextScene());
				tW.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
				tW.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
				tW.setDoAntialiasing(false);
				tW.bind(boundingRect);

				boundingRect.setHeight(110);
				break;
			}
			}
		}

		return boundingRect;
	}

	public void setObject(Object o) {
		this.object = o;

		this.generateInputFields();
	}

	@Override
	protected void _kill() {
		this.editorSection.removeListener(this);
		this.editorSection.kill();
	}

	@Override
	protected void _resize() {
		this.editorSection.setScreenDimensions(this.getWidth(), this.getHeight());
	}

	@Override
	public String getDefaultTitle() {
		return "Object Editor Window";
	}

	@Override
	protected void _update() {
		this.editorSection.update();

		//go through all method pairs, and run all the setters. 
		for (String variableName : this.variableNameToMethods.keySet()) {
			int classType = this.variableNameToMethods.get(variableName).first;
			Method getter = this.variableNameToMethods.get(variableName).second;
			Method setter = this.variableNameToMethods.get(variableName).third;

			Object val = null;

			//just call the setter. TODO first check if the getter is equal to whatever is in the input fields. 
			switch (classType) {
			case CLASS_TYPE_FLOAT: {
				TextField tf = (TextField) Input.getInput(variableName, this.editorSection.getSelectionScene());
				if (!tf.isInputValid()) {
					break;
				}
				val = Float.parseFloat(tf.getText());
				break;
			}

			case CLASS_TYPE_DOUBLE: {
				TextField tf = (TextField) Input.getInput(variableName, this.editorSection.getSelectionScene());
				if (!tf.isInputValid()) {
					break;
				}
				val = Double.parseDouble(tf.getText());
				break;
			}

			case CLASS_TYPE_BYTE: {
				TextField tf = (TextField) Input.getInput(variableName, this.editorSection.getSelectionScene());
				if (!tf.isInputValid()) {
					break;
				}
				val = Byte.parseByte(tf.getText());
				break;
			}

			case CLASS_TYPE_SHORT: {
				TextField tf = (TextField) Input.getInput(variableName, this.editorSection.getSelectionScene());
				if (!tf.isInputValid()) {
					break;
				}
				val = Short.parseShort(tf.getText());
				break;
			}

			case CLASS_TYPE_INTEGER: {
				TextField tf = (TextField) Input.getInput(variableName, this.editorSection.getSelectionScene());
				if (!tf.isInputValid()) {
					break;
				}
				val = Integer.parseInt(tf.getText());
				break;
			}

			case CLASS_TYPE_LONG: {
				TextField tf = (TextField) Input.getInput(variableName, this.editorSection.getSelectionScene());
				if (!tf.isInputValid()) {
					break;
				}
				val = Long.parseLong(tf.getText());
				break;
			}

			case CLASS_TYPE_CHAR: {
				TextField tf = (TextField) Input.getInput(variableName, this.editorSection.getSelectionScene());
				if (!tf.isInputValid()) {
					break;
				}
				val = tf.getText().charAt(0);
				break;
			}

			case CLASS_TYPE_STRING: {
				TextField tf = (TextField) Input.getInput(variableName, this.editorSection.getSelectionScene());
				if (!tf.isInputValid()) {
					break;
				}
				val = tf.getText();
				break;
			}

			case CLASS_TYPE_BOOLEAN: {
				ToggleButton tb = (ToggleButton) Input.getInput(variableName, this.editorSection.getSelectionScene());
				val = tb.isToggled();
				break;
			}

			case CLASS_TYPE_VEC2: {
				TextField tfX = (TextField) Input.getInput(variableName + ".x", this.editorSection.getSelectionScene());
				if (!tfX.isInputValid()) {
					break;
				}
				TextField tfY = (TextField) Input.getInput(variableName + ".y", this.editorSection.getSelectionScene());
				if (!tfY.isInputValid()) {
					break;
				}
				float x = Float.parseFloat(tfX.getText());
				float y = Float.parseFloat(tfY.getText());
				val = new Vec2(x, y);
				break;
			}

			case CLASS_TYPE_VEC3: {
				TextField tfX = (TextField) Input.getInput(variableName + ".x", this.editorSection.getSelectionScene());
				if (!tfX.isInputValid()) {
					break;
				}
				TextField tfY = (TextField) Input.getInput(variableName + ".y", this.editorSection.getSelectionScene());
				if (!tfY.isInputValid()) {
					break;
				}
				TextField tfZ = (TextField) Input.getInput(variableName + ".z", this.editorSection.getSelectionScene());
				if (!tfZ.isInputValid()) {
					break;
				}
				float x = Float.parseFloat(tfX.getText());
				float y = Float.parseFloat(tfY.getText());
				float z = Float.parseFloat(tfZ.getText());
				val = new Vec3(x, y, z);
				break;
			}

			case CLASS_TYPE_VEC4: {
				TextField tfX = (TextField) Input.getInput(variableName + ".x", this.editorSection.getSelectionScene());
				if (!tfX.isInputValid()) {
					break;
				}
				TextField tfY = (TextField) Input.getInput(variableName + ".y", this.editorSection.getSelectionScene());
				if (!tfY.isInputValid()) {
					break;
				}
				TextField tfZ = (TextField) Input.getInput(variableName + ".z", this.editorSection.getSelectionScene());
				if (!tfZ.isInputValid()) {
					break;
				}
				TextField tfW = (TextField) Input.getInput(variableName + ".w", this.editorSection.getSelectionScene());
				if (!tfW.isInputValid()) {
					break;
				}
				float x = Float.parseFloat(tfX.getText());
				float y = Float.parseFloat(tfY.getText());
				float z = Float.parseFloat(tfZ.getText());
				float w = Float.parseFloat(tfW.getText());
				val = new Vec4(x, y, z, w);
				break;
			}
			}

			//call the setter. 
			if (val != null) {
				try {
					setter.invoke(this.object, val);
				}
				catch (IllegalArgumentException e) {
					e.printStackTrace();
				}
				catch (IllegalAccessException e) {
					e.printStackTrace();
				}
				catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	protected void renderContent(Framebuffer outputBuffer) {
		this.editorSection.render(outputBuffer, this.getWindowMousePos());
	}

	@Override
	protected void renderOverlay(Framebuffer outputBuffer) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void selected() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void deselected() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void subtreeSelected() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void subtreeDeselected() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _mousePressed(int button) {
		this.editorSection.mousePressed(button);
	}

	@Override
	protected void _mouseReleased(int button) {
		this.editorSection.mouseReleased(button);
	}

	@Override
	protected void _mouseScrolled(float wheelOffset, float smoothOffset) {
		this.editorSection.mouseScrolled(wheelOffset, smoothOffset);
	}

	@Override
	protected void _keyPressed(int key) {
		this.editorSection.keyPressed(key);
	}

	@Override
	protected void _keyReleased(int key) {
		this.editorSection.keyReleased(key);
	}

	@Override
	public void uiSectionScrolled(UISection section) {

	}

}
