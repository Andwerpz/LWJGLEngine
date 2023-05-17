package lwjglengine.v10.window;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.graphics.Material;
import lwjglengine.v10.model.Line;
import lwjglengine.v10.model.Model;
import lwjglengine.v10.player.PlayerInputController;
import lwjglengine.v10.scene.DirLight;
import lwjglengine.v10.scene.Light;
import lwjglengine.v10.scene.Scene;
import lwjglengine.v10.screen.PerspectiveScreen;
import lwjglengine.v10.screen.UIScreen;
import lwjglengine.v10.ui.UIElement;
import lwjglengine.v10.ui.UIFilledRectangle;
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

	private Model model;
	private boolean hasModel;

	private long modelInstanceID;

	private static int numGridlines = 101;
	private float gridlineInterval = 1;
	private long[] horizontalGridlines, verticalGridlines;

	private PlayerInputController pic;
	private float cameraDistFromCenter = 1f;

	public ModelViewerWindow(int xOffset, int yOffset, int width, int height, Window parentWindow) {
		super(xOffset, yOffset, width, height, parentWindow);
		this.init(null);
	}

	public ModelViewerWindow(Model model) {
		super(0, 0, 300, 400, null);
		this.init(model);
	}
	
	public ModelViewerWindow() {
		super(0, 0, 300, 400, null);
		this.init(null);
	}

	private void init(Model model) {
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

		ArrayList<String> contextMenuOptions = new ArrayList<>();
		contextMenuOptions.add("Load Model");

		this.setContextMenuRightClick(true);
		this.setContextMenuOptions(contextMenuOptions);

		this.model = model;

		if (this.model != null) {
			this.hasModel = true;
			this.modelInstanceID = Model.addInstance(this.model, Mat4.identity(), WORLD_SCENE);
		}
		else {
			this.hasModel = false;
		}

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
		this.horizontalGridlines = new long[numGridlines];
		this.verticalGridlines = new long[numGridlines];
		for (int i = 0; i < numGridlines; i++) {
			float x1 = (i * gridlineInterval) - ((numGridlines - 1) / 2.0f);
			float x2 = x1;
			float y1 = -gridlineLength / 2.0f;
			float y2 = gridlineLength / 2.0f;
			this.verticalGridlines[i] = Line.addLine(x1, 0, y1, x2, 0, y2, WORLD_SCENE);
			this.horizontalGridlines[i] = Line.addLine(y1, 0, x1, y2, 0, x2, WORLD_SCENE);

			Model.updateInstance(this.verticalGridlines[i], new Material(new Vec3(102 / 255.0f)));
			Model.updateInstance(this.horizontalGridlines[i], new Material(new Vec3(102 / 255.0f)));
		}

		this._resize();
	}

	@Override
	public void handleContextMenuAction(String action) {
		switch (action) {
		case "Load Model":
			Window fileExplorer = new AdjustableWindow((int) this.getWindowMousePos().x, (int) this.getWindowMousePos().y, 400, 400, "File Explorer", new FileExplorerWindow(this), this);
			break;
		}
	}

	@Override
	public void handleFile(File file) {
		Model nextModel = null;
		try {
			nextModel = Model.loadModelFile(file);
		}
		catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		if (this.hasModel) {
			Model.removeInstance(this.modelInstanceID);
			this.modelInstanceID = -1;
			this.model.kill();
			this.model = null;
			this.hasModel = false;
		}

		this.model = nextModel;
		this.modelInstanceID = Model.addInstance(this.model, Mat4.identity(), WORLD_SCENE);
		this.hasModel = true;
	}

	@Override
	protected void __kill() {
		this.perspectiveScreen.kill();
		this.uiScreen.kill();

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
