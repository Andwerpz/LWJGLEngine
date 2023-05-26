package lwjglengine.v10.project;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;

import lwjglengine.v10.asset.StateAsset;
import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.main.Main;
import lwjglengine.v10.model.Model;
import lwjglengine.v10.model.ModelTransform;
import lwjglengine.v10.player.PlayerInputController;
import lwjglengine.v10.scene.DirLight;
import lwjglengine.v10.scene.Light;
import lwjglengine.v10.scene.Scene;
import lwjglengine.v10.screen.PerspectiveScreen;
import lwjglengine.v10.screen.UIScreen;
import lwjglengine.v10.window.Window;
import myutils.v10.math.Vec3;
import myutils.v10.misc.Pair;

public class ProjectStateEditorWindow extends Window {

	//project state viewer window, and project state component editor. 

	//somehow, i want to be able to run the project state in this window. 
	//ideally using the actual project state...

	//one idea to accomplish this is to perhaps have a toggle from freecam, to the project state dictating camera motion. 

	private final int PERSPECTIVE_WORLD_SCENE = Scene.generateScene();

	private PlayerInputController pic;

	private PerspectiveScreen perspectiveScreen;
	private UIScreen uiScreen;

	private Project project;
	private StateAsset state;

	public ProjectStateEditorWindow(int xOffset, int yOffset, int width, int height, Project project, StateAsset state, Window parentWindow) {
		super(xOffset, yOffset, width, height, parentWindow);
		this.init(project, state);
	}

	private void init(Project project, StateAsset state) {
		this.project = project;
		this.state = state;

		this.project.loadAsset(this.state.getID());

		this.uiScreen = new UIScreen();
		this.perspectiveScreen = new PerspectiveScreen();
		this.perspectiveScreen.setWorldScene(PERSPECTIVE_WORLD_SCENE);

		this.pic = new PlayerInputController(new Vec3(0, 0, 0));

		//place static models
		for (Pair<Long, ModelTransform> i : this.state.getStaticModels()) {
			Model m = this.project.getModel(i.first);
			Model.addInstance(m, i.second, PERSPECTIVE_WORLD_SCENE);
		}

		//'sun'
		DirLight sun = new DirLight(new Vec3(1, -1, 1), new Vec3(1), 0.3f);
		Light.addLight(PERSPECTIVE_WORLD_SCENE, sun);
	}

	@Override
	protected void _kill() {
		this.perspectiveScreen.kill();
		this.uiScreen.kill();

		this.project.unloadAsset(this.state.getID());
	}

	@Override
	protected void _resize() {
		this.perspectiveScreen.setScreenDimensions(this.getWidth(), this.getHeight());
		this.uiScreen.setScreenDimensions(this.getWidth(), this.getHeight());
	}

	@Override
	protected void _update() {
		if (this.isSelected()) {
			this.pic.update();

			//update camera
			this.perspectiveScreen.getCamera().setFacing(this.pic.getFacing());
			this.perspectiveScreen.getCamera().setPos(this.pic.getPos());
		}
	}

	@Override
	protected void renderContent(Framebuffer outputBuffer) {
		this.perspectiveScreen.render(outputBuffer);
	}

	@Override
	protected void renderOverlay(Framebuffer outputBuffer) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void selected() {
		Main.lockCursor();
	}

	@Override
	protected void deselected() {
		Main.unlockCursor();
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
		// TODO Auto-generated method stub

	}

	@Override
	protected void _mouseReleased(int button) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _mouseScrolled(float wheelOffset, float smoothOffset) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _keyPressed(int key) {
		switch (key) {
		case GLFW_KEY_ESCAPE:
			this.deselect();
			break;
		}
	}

	@Override
	protected void _keyReleased(int key) {
		// TODO Auto-generated method stub

	}

}
