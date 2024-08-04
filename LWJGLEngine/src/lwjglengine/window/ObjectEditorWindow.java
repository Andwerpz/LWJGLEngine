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
import lwjglengine.input.Input.InputCallback;
import lwjglengine.input.TextField;
import lwjglengine.input.ToggleButton;
import lwjglengine.screen.UIScreen;
import lwjglengine.ui.ObjectEditor;
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

public class ObjectEditorWindow extends Window {
	//just a window wrapper for the object editor ui element. 

	private UISection editorSection;

	private ObjectEditor objectEditor;

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
		this.editorSection.getBackgroundRect().setMaterial(Material.TOP_BAR_DEFAULT_MATERIAL);
		this.editorSection.getBackgroundRect().bind(this.rootUIElement);
		this.editorSection.getBackgroundRect().setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.editorSection.getBackgroundRect().setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);

		this.rootUIElement.setMaterial(Material.TOP_BAR_DEFAULT_MATERIAL);

		this.objectEditor = new ObjectEditor(this.editorSection);
		this.objectEditor.setFillWidth(true);
		this.objectEditor.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.objectEditor.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		this.objectEditor.bind(this.editorSection.getScrollBackgroundRect());

		this._resize();
	}

	public void setObject(Object o) {
		this.objectEditor.setObject(o);
		this.editorSection.setScrollRectHeight((int) this.objectEditor.getHeight());
	}

	@Override
	protected void _kill() {
		this.editorSection.kill();
	}

	@Override
	protected void _resize() {
		this.editorSection.setScreenDimensions(this.getWidth(), this.getHeight());
	}

	@Override
	public String getDefaultTitle() {
		return "Object Editor";
	}

	@Override
	protected void _update() {
		this.editorSection.update();
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

}
