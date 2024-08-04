package lwjglengine.ui;

import java.awt.Color;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import lwjglengine.input.Input;
import lwjglengine.input.TextField;
import lwjglengine.input.ToggleButton;
import lwjglengine.graphics.Material;
import lwjglengine.input.Input.InputCallback;
import lwjglengine.model.FilledRectangle;
import lwjglengine.window.ObjectEditorWindow;
import myutils.math.Vec2;
import myutils.math.Vec3;
import myutils.math.Vec4;
import myutils.misc.Pair;
import myutils.misc.Triple;

public class ObjectEditor extends UIElement implements InputCallback {
	//this is an editor that we should be able to put any object inside and edit. 

	//it will first list out all getters and setters of the given class, and will generate input fields 
	//based off of the type of fields it sees. 

	//it will only generate input fields for some predetermined set of basic types. 

	//in order for this to not interfere with other input ids, we'll generate a random string that we'll append 
	//to the front of any inputs generated by this uielement. 

	//TODO
	// - allow option to filter what variables we generate inputs for. 
	//   - we shouldn't generate a gui element for this, rather it should be the caller's responsibility to handle that. 

	//finished:
	// - generate random string for each object editor as to not interfere with other input sID.
	//   - in the future, find a more permanent solution. 

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

	//bounding boxes for all the input fields. 
	//we'll use these to adjust spacing and placement of inputs. 
	private ArrayList<UIFilledRectangle> inputFields;

	//class type, getter, setter
	private HashMap<String, Triple<Integer, Method, Method>> variableNameToMethods;

	private Object object;

	private int verticalMargin = 5;
	private int horizontalMargin = 5;

	private int verticalPadding = 5;
	private int inputWidth = 150;

	private int backgroundScene, selectionScene, textScene;

	private static final int SID_PREFIX_LEN = 64; //this gives us around 10^-90 chance of collision. 
	private String sIDPrefix;

	private ArrayList<ObjectEditorCallback> callbacks;

	public ObjectEditor(UISection section) {
		this(null, section);
	}

	public ObjectEditor(Object object, UISection section) {
		this(object, section.getBackgroundScene(), section.getSelectionScene(), section.getTextScene());
	}

	public ObjectEditor(Object object, int background_scene, int selection_scene, int text_scene) {
		super(0, 0, 0, 100, 100, background_scene);
		this.init(object, background_scene, selection_scene, text_scene);
	}

	private void init(Object _object, int background_scene, int selection_scene, int text_scene) {
		this.backgroundScene = background_scene;
		this.selectionScene = selection_scene;
		this.textScene = text_scene;

		this.inputFields = new ArrayList<>();
		this.variableNameToMethods = new HashMap<>();

		{
			char[] c = new char[SID_PREFIX_LEN];
			for (int i = 0; i < SID_PREFIX_LEN; i++) {
				c[i] = (char) ('a' + (int) (Math.random() * 26));
			}
			this.sIDPrefix = new String(c);
		}

		this.setMaterial(Material.TOP_BAR_DEFAULT_MATERIAL);

		this.setObject(_object);
	}

	public Object getObject() {
		return this.object;
	}

	public void setObject(Object o) {
		this.object = o;
		this.generateInputFields();
	}

	public void setInputWidth(int _width) {
		this.inputWidth = _width;
		this.generateInputFields();
	}

	public void addCallback(ObjectEditorCallback c) {
		this.callbacks.add(c);
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

		//go through all methods, find the ones that have 0 arguments and start with 'get'
		HashMap<String, Method> getMethods = new HashMap<>();
		for (Method m : methods) {
			if (m.getName().startsWith("get") && m.getParameterCount() == 0) {
				getMethods.put(m.getName().substring(3), m);
			}
		}

		//create all valid 'set' and 'get' method pairs. 
		//Ensure that the type of the argument of the 'set' method is the same as the return type of the 'get' method. 
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
			boundingRect.bind(this);

			yptr += boundingRect.getHeight() + verticalPadding;
		}
		this.setHeight(yptr);
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
		int classType = ObjectEditor.getClassType(getter.getReturnType());
		if (classType == CLASS_TYPE_UNKNOWN) {
			return null;
		}

		//make first letter lower case
		String variableName = getter.getName().substring(3);
		char[] c = variableName.toCharArray();
		c[0] = Character.toLowerCase(c[0]);
		variableName = new String(c);

		String inputName = this.sIDPrefix + variableName;

		UIFilledRectangle boundingRect = new UIFilledRectangle(0, 0, 0, 1, 1, this.backgroundScene);
		boundingRect.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_TOP);
		boundingRect.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_TOP);
		boundingRect.setFillWidth(true);
		boundingRect.setFillWidthMargin(5);
		boundingRect.setMaterial(Material.TOP_BAR_DEFAULT_MATERIAL);

		Text variableNameText = new Text(0, 0, variableName, 12, Color.WHITE, this.textScene);
		variableNameText.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_TOP);
		variableNameText.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
		variableNameText.bind(boundingRect);

		if (classType == CLASS_TYPE_FLOAT || classType == CLASS_TYPE_DOUBLE || classType == CLASS_TYPE_BYTE || classType == CLASS_TYPE_SHORT || classType == CLASS_TYPE_INTEGER || classType == CLASS_TYPE_LONG) {
			//numerical primitives
			TextField textField = new TextField(0, 0, inputWidth, 20, inputName, variableName, this, this.selectionScene, this.textScene);
			textField.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_CENTER_TOP);
			textField.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
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
			ToggleButton toggleButton = new ToggleButton(0, 0, inputWidth, 20, inputName, variableName, this, this.selectionScene, this.textScene);
			toggleButton.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_CENTER_TOP);
			toggleButton.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
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
			TextField textField = new TextField(0, 0, inputWidth, 20, inputName, variableName, this, this.selectionScene, this.textScene);
			textField.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_CENTER_TOP);
			textField.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
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
				TextField tfX = new TextField(0, 25, inputWidth, 20, inputName + " x", variableName, this, this.selectionScene, this.textScene);
				tfX.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_TOP);
				tfX.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
				tfX.setFieldType(TextField.FIELD_TYPE_FLOAT);
				tfX.bind(boundingRect);

				TextField tfY = new TextField(0, 50, inputWidth, 20, inputName + " y", variableName, this, this.selectionScene, this.textScene);
				tfY.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_TOP);
				tfY.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
				tfY.setFieldType(TextField.FIELD_TYPE_FLOAT);
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

				Text tX = new Text(0, 25, "X", 12, Color.WHITE, this.textScene);
				tX.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
				tX.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
				tX.bind(boundingRect);

				Text tY = new Text(0, 50, "Y", 12, Color.WHITE, this.textScene);
				tY.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
				tY.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
				tY.bind(boundingRect);

				boundingRect.setHeight(60);
				break;
			}

			case CLASS_TYPE_VEC3: {
				TextField tfX = new TextField(0, 25, inputWidth, 20, inputName + " x", variableName, this, this.selectionScene, this.textScene);
				tfX.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_TOP);
				tfX.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
				tfX.setFieldType(TextField.FIELD_TYPE_FLOAT);
				tfX.bind(boundingRect);

				TextField tfY = new TextField(0, 50, inputWidth, 20, inputName + " y", variableName, this, this.selectionScene, this.textScene);
				tfY.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_TOP);
				tfY.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
				tfY.setFieldType(TextField.FIELD_TYPE_FLOAT);
				tfY.bind(boundingRect);

				TextField tfZ = new TextField(0, 75, inputWidth, 20, inputName + " z", variableName, this, this.selectionScene, this.textScene);
				tfZ.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_TOP);
				tfZ.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
				tfZ.setFieldType(TextField.FIELD_TYPE_FLOAT);
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

				Text tX = new Text(0, 25, "X", 12, Color.WHITE, this.textScene);
				tX.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
				tX.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
				tX.bind(boundingRect);

				Text tY = new Text(0, 50, "Y", 12, Color.WHITE, this.textScene);
				tY.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
				tY.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
				tY.bind(boundingRect);

				Text tZ = new Text(0, 75, "Z", 12, Color.WHITE, this.textScene);
				tZ.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
				tZ.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
				tZ.bind(boundingRect);

				boundingRect.setHeight(85);
				break;
			}

			case CLASS_TYPE_VEC4: {
				TextField tfX = new TextField(0, 25, inputWidth, 20, inputName + " x", variableName, this, this.selectionScene, this.textScene);
				tfX.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_TOP);
				tfX.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
				tfX.setFieldType(TextField.FIELD_TYPE_FLOAT);
				tfX.bind(boundingRect);

				TextField tfY = new TextField(0, 50, inputWidth, 20, inputName + " y", variableName, this, this.selectionScene, this.textScene);
				tfY.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_TOP);
				tfY.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
				tfY.setFieldType(TextField.FIELD_TYPE_FLOAT);
				tfY.bind(boundingRect);

				TextField tfZ = new TextField(0, 75, inputWidth, 20, inputName + " z", variableName, this, this.selectionScene, this.textScene);
				tfZ.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_TOP);
				tfZ.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
				tfZ.setFieldType(TextField.FIELD_TYPE_FLOAT);
				tfZ.bind(boundingRect);

				TextField tfW = new TextField(0, 100, inputWidth, 20, inputName + " w", variableName, this, this.selectionScene, this.textScene);
				tfW.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_TOP);
				tfW.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
				tfW.setFieldType(TextField.FIELD_TYPE_FLOAT);
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

				Text tX = new Text(0, 25, "X", 12, Color.WHITE, this.textScene);
				tX.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
				tX.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
				tX.bind(boundingRect);

				Text tY = new Text(0, 50, "Y", 12, Color.WHITE, this.textScene);
				tY.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
				tY.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
				tY.bind(boundingRect);

				Text tZ = new Text(0, 75, "Z", 12, Color.WHITE, this.textScene);
				tZ.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
				tZ.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
				tZ.bind(boundingRect);

				Text tW = new Text(0, 100, "W", 12, Color.WHITE, this.textScene);
				tW.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
				tW.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
				tW.bind(boundingRect);

				boundingRect.setHeight(110);
				break;
			}
			}
		}

		return boundingRect;
	}

	private void callSetter(String sID) {
		assert sID.substring(0, SID_PREFIX_LEN) == this.sIDPrefix : "Unexpected sID : " + sID;
		String inputName = sID.split(" ")[0];
		String variableName = inputName.substring(SID_PREFIX_LEN);

		if (!this.variableNameToMethods.containsKey(variableName)) {
			return;
		}

		int classType = this.variableNameToMethods.get(variableName).first;
		Method getter = this.variableNameToMethods.get(variableName).second;
		Method setter = this.variableNameToMethods.get(variableName).third;

		Object val = null;

		switch (classType) {
		case CLASS_TYPE_FLOAT: {
			TextField tf = (TextField) Input.getInput(inputName, this.selectionScene);
			if (!tf.isInputValid()) {
				break;
			}
			val = Float.parseFloat(tf.getText());
			break;
		}

		case CLASS_TYPE_DOUBLE: {
			TextField tf = (TextField) Input.getInput(inputName, this.selectionScene);
			if (!tf.isInputValid()) {
				break;
			}
			val = Double.parseDouble(tf.getText());
			break;
		}

		case CLASS_TYPE_BYTE: {
			TextField tf = (TextField) Input.getInput(inputName, this.selectionScene);
			if (!tf.isInputValid()) {
				break;
			}
			val = Byte.parseByte(tf.getText());
			break;
		}

		case CLASS_TYPE_SHORT: {
			TextField tf = (TextField) Input.getInput(inputName, this.selectionScene);
			if (!tf.isInputValid()) {
				break;
			}
			val = Short.parseShort(tf.getText());
			break;
		}

		case CLASS_TYPE_INTEGER: {
			TextField tf = (TextField) Input.getInput(inputName, this.selectionScene);
			if (!tf.isInputValid()) {
				break;
			}
			val = Integer.parseInt(tf.getText());
			break;
		}

		case CLASS_TYPE_LONG: {
			TextField tf = (TextField) Input.getInput(inputName, this.selectionScene);
			if (!tf.isInputValid()) {
				break;
			}
			val = Long.parseLong(tf.getText());
			break;
		}

		case CLASS_TYPE_CHAR: {
			TextField tf = (TextField) Input.getInput(inputName, this.selectionScene);
			if (!tf.isInputValid()) {
				break;
			}
			val = tf.getText().charAt(0);
			break;
		}

		case CLASS_TYPE_STRING: {
			TextField tf = (TextField) Input.getInput(inputName, this.selectionScene);
			if (!tf.isInputValid()) {
				break;
			}
			val = tf.getText();
			break;
		}

		case CLASS_TYPE_BOOLEAN: {
			ToggleButton tb = (ToggleButton) Input.getInput(inputName, this.selectionScene);
			val = tb.isToggled();
			break;
		}

		case CLASS_TYPE_VEC2: {
			TextField tfX = (TextField) Input.getInput(inputName + " x", this.selectionScene);
			if (!tfX.isInputValid()) {
				break;
			}
			TextField tfY = (TextField) Input.getInput(inputName + " y", this.selectionScene);
			if (!tfY.isInputValid()) {
				break;
			}
			float x = Float.parseFloat(tfX.getText());
			float y = Float.parseFloat(tfY.getText());
			val = new Vec2(x, y);
			break;
		}

		case CLASS_TYPE_VEC3: {
			TextField tfX = (TextField) Input.getInput(inputName + " x", this.selectionScene);
			if (!tfX.isInputValid()) {
				break;
			}
			TextField tfY = (TextField) Input.getInput(inputName + " y", this.selectionScene);
			if (!tfY.isInputValid()) {
				break;
			}
			TextField tfZ = (TextField) Input.getInput(inputName + " z", this.selectionScene);
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
			TextField tfX = (TextField) Input.getInput(inputName + " x", this.selectionScene);
			if (!tfX.isInputValid()) {
				break;
			}
			TextField tfY = (TextField) Input.getInput(inputName + " y", this.selectionScene);
			if (!tfY.isInputValid()) {
				break;
			}
			TextField tfZ = (TextField) Input.getInput(inputName + " z", this.selectionScene);
			if (!tfZ.isInputValid()) {
				break;
			}
			TextField tfW = (TextField) Input.getInput(inputName + " w", this.selectionScene);
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

		//notify callbacks
		for (ObjectEditorCallback c : this.callbacks) {
			c.objectModified(this.object);
		}
	}

	@Override
	protected void __kill() {
		//shouldn't have to do anything, as everything is bound to the root. 
	}

	@Override
	protected void _alignContents() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _update() {
		// TODO Auto-generated method stub

	}

	@Override
	public void inputClicked(String sID) {
		//do nothing. 
	}

	@Override
	public void inputChanged(String sID) {
		this.callSetter(sID);
	}

	//notifies when the object in question has been modified. 
	public interface ObjectEditorCallback {
		void objectModified(Object o);
	}

}
