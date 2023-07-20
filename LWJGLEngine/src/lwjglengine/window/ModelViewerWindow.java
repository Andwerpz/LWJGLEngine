package lwjglengine.window;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import lwjglengine.graphics.Framebuffer;
import lwjglengine.graphics.Material;
import lwjglengine.model.Line;
import lwjglengine.model.Model;
import lwjglengine.model.ModelInstance;
import lwjglengine.model.ModelTransform;
import lwjglengine.player.PlayerInputController;
import lwjglengine.scene.DirLight;
import lwjglengine.scene.Light;
import lwjglengine.scene.Scene;
import lwjglengine.screen.PerspectiveScreen;
import lwjglengine.screen.UIScreen;
import lwjglengine.ui.UIElement;
import lwjglengine.ui.UIFilledRectangle;
import myutils.v10.math.Mat4;
import myutils.v10.math.Vec3;

public class ModelViewerWindow extends BorderedWindow {
	//TODO
	// - have an orientation compass thingy like blender does
	// - draw gridlines that automatically adjust depending on the current position of the camera. 
	// - let user know if the file failed to load
	// - should ask to open a file explorer if there currently is no model. 
	// - maybe allow users to load a skybox?

	private final int WORLD_SCENE = Scene.generateScene();
	private final int BACKGROUND_SCENE = Scene.generateScene();
	private final int UI_SCENE = Scene.generateScene();

	private UIScreen uiScreen;
	private PerspectiveScreen perspectiveScreen;

	//if this is true, then it means that this window loaded the model
	//this can be false in the case when the model was passed in thru the constructor, or set externally. 
	private boolean shouldUnload = false;
	private Model model;

	private ModelInstance modelInstance;

	private static int numGridlines = 101;
	private float gridlineInterval = 1;
	private ModelInstance[] horizontalGridlines, verticalGridlines;

	private PlayerInputController pic;
	private float cameraDistFromCenter = 1f;

	public ModelViewerWindow(int xOffset, int yOffset, int width, int height, Window parentWindow) {
		super(xOffset, yOffset, width, height, parentWindow);
		this.init();
	}

	public ModelViewerWindow(Model model) {
		super(0, 0, 300, 400, null);
		this.init();

		this.setModel(model);
	}

	public ModelViewerWindow(File file) {
		super(0, 0, 300, 400, null);
		this.init();

		this.setModel(file);
	}

	private void init() {
		this.model = null;
		this.shouldUnload = false;

		this.perspectiveScreen = new PerspectiveScreen();
		this.uiScreen = new UIScreen();

		this.perspectiveScreen.renderDecals(false);
		this.perspectiveScreen.renderParticles(false);
		this.perspectiveScreen.renderPlayermodel(false);
		this.perspectiveScreen.renderSkybox(false);

		Light.addLight(WORLD_SCENE, new DirLight(new Vec3(1, -1, 1), new Vec3(1), 0.4f));

		this.pic = new PlayerInputController(new Vec3(0, 0, -1));
		this.pic.setCamXRot((float) Math.PI / 4.0f);
		this.pic.setCamYRot((float) Math.PI / 4.0f);
		this.pic.setAcceptPlayerInputs(false);

		this.modelInstance = null;

		UIFilledRectangle backgroundRect = new UIFilledRectangle(0, 0, 0, 1, 1, BACKGROUND_SCENE);
		backgroundRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_BOTTOM);
		backgroundRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_BOTTOM);
		backgroundRect.setFillWidth(true);
		backgroundRect.setFillHeight(true);
		backgroundRect.setFillWidthMargin(0);
		backgroundRect.setFillHeightMargin(0);
		backgroundRect.setMaterial(new Material(new Vec3(51 / 255.0f)));
		backgroundRect.bind(this.rootUIElement);

		//grid lines
		float gridlineLength = gridlineInterval * (numGridlines + 2);
		this.horizontalGridlines = new ModelInstance[numGridlines];
		this.verticalGridlines = new ModelInstance[numGridlines];
		for (int i = 0; i < numGridlines; i++) {
			float x1 = (i * gridlineInterval) - ((numGridlines - 1) / 2.0f);
			float x2 = x1;
			float y1 = -gridlineLength / 2.0f;
			float y2 = gridlineLength / 2.0f;
			this.verticalGridlines[i] = Line.addLine(x1, 0, y1, x2, 0, y2, WORLD_SCENE);
			this.horizontalGridlines[i] = Line.addLine(y1, 0, x1, y2, 0, x2, WORLD_SCENE);

			this.verticalGridlines[i].setMaterial(new Material(new Vec3(102 / 255.0f)));
			this.horizontalGridlines[i].setMaterial(new Material(new Vec3(102 / 255.0f)));
		}

		this._resize();
	}

	@Override
	public String getDefaultTitle() {
		return "Model Viewer";
	}

	public void setModel(File file) {
		//try to load model from file
		Model model = Model.loadModelFile(file);
		this.setModel(model);
		this.shouldUnload = true; //we loaded it, we should probably unload it. 
	}

	public void setModel(Model model) {
		if (this.shouldUnload) {
			if (this.model != null) {
				this.model.kill();
			}
			this.model = null;
		}
		this.shouldUnload = false;
		this.modelInstance = null;

		if (this.model == null) {
			return;
		}

		this.model = model;
		this.modelInstance = new ModelInstance(this.model, WORLD_SCENE);
	}

	@Override
	protected void __kill() {
		this.perspectiveScreen.kill();
		this.uiScreen.kill();

		this.setModel((Model) null);

		Scene.removeScene(BACKGROUND_SCENE);
		Scene.removeScene(WORLD_SCENE);
		Scene.removeScene(UI_SCENE);
	}

	@Override
	protected void __resize() {
		this.perspectiveScreen.setScreenDimensions(this.getWidth(), this.getHeight());
		this.uiScreen.setScreenDimensions(this.getWidth(), this.getHeight());
	}

	@Override
	protected void _renderOverlay(Framebuffer outputBuffer) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _subtreeSelected() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _subtreeDeselected() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _update() {
		this.pic.update();

		this.perspectiveScreen.getCamera().setFacing(this.pic.getFacing());
		this.perspectiveScreen.getCamera().setPos(this.pic.getFacing().mul(-this.cameraDistFromCenter));
	}

	@Override
	protected void renderContent(Framebuffer outputBuffer) {
		this.uiScreen.setUIScene(BACKGROUND_SCENE);
		this.uiScreen.render(outputBuffer);

		this.perspectiveScreen.setWorldScene(WORLD_SCENE);
		this.perspectiveScreen.render(outputBuffer);

		this.uiScreen.setUIScene(UI_SCENE);
		this.uiScreen.render(outputBuffer);
	}

	@Override
	protected void selected() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void deselected() {
		this.pic.setAcceptPlayerInputs(false);
	}

	@Override
	protected void _mousePressed(int button) {
		this.pic.setAcceptPlayerInputs(true);
	}

	@Override
	protected void _mouseReleased(int button) {
		this.pic.setAcceptPlayerInputs(false);
	}

	@Override
	protected void _mouseScrolled(float wheelOffset, float smoothOffset) {
		if (smoothOffset < 0) {
			this.cameraDistFromCenter *= 1.1;
		}
		else {
			this.cameraDistFromCenter /= 1.1;
		}
	}

	@Override
	protected void _keyPressed(int key) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _keyReleased(int key) {
		// TODO Auto-generated method stub

	}

}
