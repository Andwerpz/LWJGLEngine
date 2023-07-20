package lwjglengine.project;

import java.awt.Color;

import lwjglengine.graphics.Framebuffer;
import lwjglengine.graphics.Material;
import lwjglengine.input.TextField;
import lwjglengine.model.Model;
import lwjglengine.model.ModelInstance;
import lwjglengine.model.ModelTransform;
import lwjglengine.screen.UIScreen;
import lwjglengine.ui.Text;
import lwjglengine.ui.UIElement;
import lwjglengine.ui.UIFilledRectangle;
import lwjglengine.ui.UISection;
import lwjglengine.window.Window;
import myutils.v10.math.Mat4;
import myutils.v10.math.MathUtils;
import myutils.v10.math.Quaternion;
import myutils.v10.math.Vec3;
import myutils.v10.math.Vec4;

public class ModelTransformEditorWindow extends Window {

	private ModelInstance modelInstance;
	private ModelTransform transform;

	private UISection uiSection;

	private UIScreen uiScreen;

	private UIFilledRectangle tfRect;

	private TextField scaleTf;

	private TextField translateXTf;
	private TextField translateYTf;
	private TextField translateZTf;

	private TextField eulerXTf;
	private TextField eulerYTf;

	private UIFilledRectangle textRect;

	private Text scaleText;

	private Text translateXText;
	private Text translateYText;
	private Text translateZText;

	private Text eulerXText;
	private Text eulerYText;

	public ModelTransformEditorWindow(ModelInstance modelInstance, ModelTransform transform, Window parentWindow) {
		super(0, 0, 300, 300, parentWindow);
		this.init(modelInstance, transform);
	}

	private void init(ModelInstance modelInstance, ModelTransform transform) {
		this.modelInstance = modelInstance;
		this.transform = transform;

		this.uiScreen = new UIScreen();

		this.uiSection = new UISection(0, 0, this.getWidth(), this.getHeight(), this.uiScreen);

		UIFilledRectangle contentBackgroundRect = this.uiSection.getBackgroundRect();
		contentBackgroundRect.setFillWidth(true);
		contentBackgroundRect.setFillHeight(true);
		contentBackgroundRect.setMaterial(this.contentDefaultMaterial);
		contentBackgroundRect.bind(this.rootUIElement);

		String floatRegex = "[-+]?[0-9]*\\.?[0-9]+";

		this.textRect = new UIFilledRectangle(0, 0, 0, 1, this.getHeight(), this.uiSection.getBackgroundScene());
		this.textRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.textRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		this.textRect.setFillHeight(true);
		this.textRect.setMaterial(this.contentDefaultMaterial);
		this.textRect.bind(contentBackgroundRect);

		this.scaleText = new Text(10, 15, "Scale", 12, Color.WHITE, this.uiSection.getTextScene());
		this.scaleText.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.scaleText.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
		this.scaleText.setDoAntialiasing(false);
		this.scaleText.bind(this.textRect);

		this.translateXText = new Text(10, 45, "X  ", 12, Color.WHITE, this.uiSection.getTextScene());
		this.translateXText.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.translateXText.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
		this.translateXText.setDoAntialiasing(false);
		this.translateXText.bind(this.textRect);

		this.translateYText = new Text(10, 70, "Y  ", 12, Color.WHITE, this.uiSection.getTextScene());
		this.translateYText.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.translateYText.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
		this.translateYText.setDoAntialiasing(false);
		this.translateYText.bind(this.textRect);

		this.translateZText = new Text(10, 95, "Z  ", 12, Color.WHITE, this.uiSection.getTextScene());
		this.translateZText.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.translateZText.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
		this.translateZText.setDoAntialiasing(false);
		this.translateZText.bind(this.textRect);

		this.eulerXText = new Text(10, 125, "X Axis Deg", 12, Color.WHITE, this.uiSection.getTextScene());
		this.eulerXText.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.eulerXText.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
		this.eulerXText.setDoAntialiasing(false);
		this.eulerXText.bind(this.textRect);

		this.eulerYText = new Text(10, 150, "Y Axis Deg", 12, Color.WHITE, this.uiSection.getTextScene());
		this.eulerYText.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.eulerYText.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
		this.eulerYText.setDoAntialiasing(false);
		this.eulerYText.bind(this.textRect);

		this.tfRect = new UIFilledRectangle(70, 0, 0, 1, this.getHeight(), this.uiSection.getBackgroundScene());
		this.tfRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.tfRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		this.tfRect.setFillHeight(true);
		this.tfRect.setMaterial(this.contentDefaultMaterial);
		this.tfRect.bind(contentBackgroundRect);

		this.scaleTf = new TextField(10, 15, 150, 20, "tf_scale", "", 12, this.uiSection.getSelectionScene(), this.uiSection.getTextScene());
		this.scaleTf.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.scaleTf.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
		this.scaleTf.getTextUIElement().setDoAntialiasing(false);
		this.scaleTf.setValidInputRegex(floatRegex);
		this.scaleTf.setText(this.transform.scale + "");
		this.scaleTf.bind(this.tfRect);

		this.translateXTf = new TextField(10, 45, 150, 20, "tf_translate_x", "", 12, this.uiSection.getSelectionScene(), this.uiSection.getTextScene());
		this.translateXTf.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.translateXTf.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
		this.translateXTf.getTextUIElement().setDoAntialiasing(false);
		this.translateXTf.setValidInputRegex(floatRegex);
		this.translateXTf.setText(this.transform.translate.x + "");
		this.translateXTf.bind(this.tfRect);

		this.translateYTf = new TextField(10, 70, 150, 20, "tf_translate_y", "", 12, this.uiSection.getSelectionScene(), this.uiSection.getTextScene());
		this.translateYTf.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.translateYTf.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
		this.translateYTf.getTextUIElement().setDoAntialiasing(false);
		this.translateYTf.setValidInputRegex(floatRegex);
		this.translateYTf.setText(this.transform.translate.y + "");
		this.translateYTf.bind(this.tfRect);

		this.translateZTf = new TextField(10, 95, 150, 20, "tf_translate_z", "", 12, this.uiSection.getSelectionScene(), this.uiSection.getTextScene());
		this.translateZTf.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.translateZTf.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
		this.translateZTf.getTextUIElement().setDoAntialiasing(false);
		this.translateZTf.setValidInputRegex(floatRegex);
		this.translateZTf.setText(this.transform.translate.z + "");
		this.translateZTf.bind(this.tfRect);

		this.eulerXTf = new TextField(10, 125, 150, 20, "tf_euler_x", "", 12, this.uiSection.getSelectionScene(), this.uiSection.getTextScene());
		this.eulerXTf.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.eulerXTf.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
		this.eulerXTf.getTextUIElement().setDoAntialiasing(false);
		this.eulerXTf.setValidInputRegex(floatRegex);
		this.eulerXTf.bind(this.tfRect);

		this.eulerYTf = new TextField(10, 150, 150, 20, "tf_euler_y", "", 12, this.uiSection.getSelectionScene(), this.uiSection.getTextScene());
		this.eulerYTf.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.eulerYTf.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
		this.eulerYTf.getTextUIElement().setDoAntialiasing(false);
		this.eulerYTf.setValidInputRegex(floatRegex);
		this.eulerYTf.bind(this.tfRect);

		this._resize();
	}

	@Override
	public String getDefaultTitle() {
		return "Model Transform Editor";
	}

	@Override
	protected void _kill() {
		this.uiScreen.kill();

		this.uiSection.kill();
	}

	@Override
	protected void _resize() {
		this.uiScreen.setScreenDimensions(this.getWidth(), this.getHeight());
	}

	@Override
	protected void _update() {
		this.uiSection.update();
	}

	@Override
	protected void renderContent(Framebuffer outputBuffer) {
		this.uiSection.render(outputBuffer, getWindowMousePos());
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
		this.uiSection.mousePressed(button);
	}

	@Override
	protected void _mouseReleased(int button) {
		this.uiSection.mouseReleased(button);
	}

	@Override
	protected void _mouseScrolled(float wheelOffset, float smoothOffset) {
		this.uiSection.mouseScrolled(wheelOffset, smoothOffset);
	}

	@Override
	protected void _keyPressed(int key) {
		this.uiSection.keyPressed(key);

		//make sure all inputs are valid before updating the model transform
		if (this.scaleTf.isInputValid() && this.translateXTf.isInputValid() && this.translateYTf.isInputValid() && this.translateZTf.isInputValid() && this.eulerXTf.isInputValid() && this.eulerYTf.isInputValid()) {
			float scale = Float.parseFloat(this.scaleTf.getText());
			Vec3 translate = new Vec3(Float.parseFloat(this.translateXTf.getText()), Float.parseFloat(this.translateYTf.getText()), Float.parseFloat(this.translateZTf.getText()));
			float xRot = (float) Math.toRadians(Float.parseFloat(this.eulerXTf.getText()));
			float yRot = (float) Math.toRadians(Float.parseFloat(this.eulerYTf.getText()));

			Quaternion rotation = Quaternion.identity();
			rotation.muli(MathUtils.quaternionFromRotationMat4(Mat4.rotateX(xRot)));
			rotation.muli(MathUtils.quaternionFromRotationMat4(Mat4.rotateY(yRot)));

			this.transform.setScale(scale);
			this.transform.setTranslation(translate);
			this.transform.setRotation(rotation);

			this.modelInstance.setModelTransform(this.transform);
		}
	}

	@Override
	protected void _keyReleased(int key) {
		this.uiSection.keyReleased(key);
	}

}
