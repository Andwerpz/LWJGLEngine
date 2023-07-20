package lwjglengine.project;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;

import java.util.ArrayList;

import lwjglengine.asset.Asset;
import lwjglengine.asset.StateAsset;
import lwjglengine.graphics.Framebuffer;
import lwjglengine.main.Main;
import lwjglengine.model.Model;
import lwjglengine.model.ModelInstance;
import lwjglengine.model.ModelTransform;
import lwjglengine.player.PlayerInputController;
import lwjglengine.scene.DirLight;
import lwjglengine.scene.Light;
import lwjglengine.scene.Scene;
import lwjglengine.screen.PerspectiveScreen;
import lwjglengine.screen.UIScreen;
import lwjglengine.window.AdjustableWindow;
import lwjglengine.window.ListViewerWindow;
import lwjglengine.window.Window;
import myutils.v10.math.Vec3;
import myutils.v10.misc.Pair;
import myutils.v10.misc.Triple;

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

	private ArrayList<ModelInstance> staticModelInstances;

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
		this.staticModelInstances = new ArrayList<>();
		for (Pair<Long, ModelTransform> i : this.state.getStaticModels()) {
			Model m = this.project.getModel(i.first);
			ModelTransform transform = i.second;
			ModelInstance instance = new ModelInstance(m, transform, PERSPECTIVE_WORLD_SCENE);
			this.staticModelInstances.add(instance);
		}

		//'sun'
		DirLight sun = new DirLight(new Vec3(1, -1, 1), new Vec3(1), 0.3f);
		Light.addLight(PERSPECTIVE_WORLD_SCENE, sun);

		//context menu
		ArrayList<String> contextOptions = new ArrayList<>();
		contextOptions.add("Static Models");
		contextOptions.add("New Static Model");

		this.setContextMenuActions(contextOptions);

		this.setContextMenuRightClick(true);
		this.setUnlockCursorOnEscPressed(true);
		this.setLockCursorOnSelect(true);
	}

	@Override
	public String getDefaultTitle() {
		return "Project State Editor";
	}

	@Override
	public void handleContextMenuAction(String action) {
		switch (action) {
		case "Static Models": {
			ArrayList<Pair<Long, ModelTransform>> staticModels = this.state.getStaticModels();
			ArrayList<String> staticModelStrings = new ArrayList<>();
			ArrayList<String> staticModelSelObjects = new ArrayList<>();
			for (int i = 0; i < staticModels.size(); i++) {
				long assetID = staticModels.get(i).first;
				Asset asset = this.project.getAsset(assetID);
				ModelTransform transform = staticModels.get(i).second;
				String desc = asset.getName() + " Instance : " + transform.translate;
				staticModelStrings.add(desc);

				staticModelSelObjects.add("static_model " + this.staticModelInstances.get(i).getID());
			}
			AdjustableWindow staticModelListWindow = new AdjustableWindow("Static Models", new ListViewerWindow(this, null), this);
			ListViewerWindow contentWindow = (ListViewerWindow) staticModelListWindow.getContentWindow();
			contentWindow.setCloseOnSubmit(false);
			contentWindow.setRenderBottomBar(false);
			contentWindow.setSubmitOnClickingSelectedListEntry(true);
			contentWindow.setSingleEntrySelection(true);
			contentWindow.setList(staticModelSelObjects, staticModelStrings);
			break;
		}

		case "New Static Model": {
			Window newStaticModelWindow = new AdjustableWindow("New Static Model", new NewStaticModelWindow(this.project, this, null), this);
			break;
		}
		}
	}

	@Override
	public void handleObjects(Object[] objects) {
		Object o = objects[0];

		if (o instanceof String) {
			//strings should have the form 'type id'. 
			String[] arr = ((String) o).split(" ");
			String type = arr[0];
			long id = Long.parseLong(arr[1]);

			switch (type) {
			case "static_model": {
				//look for the static model with the given id
				int modelIndex = -1;
				for (int i = 0; i < this.staticModelInstances.size(); i++) {
					if (this.staticModelInstances.get(i).getID() == id) {
						modelIndex = i;
						break;
					}
				}

				ModelTransform transform = this.state.getStaticModels().get(modelIndex).second;

				//open a model transform editor window
				//TODO make it into a general model instance editor, so we can edit materials. 
				Window modelTransformEditor = new AdjustableWindow("Editing Static Model : " + id, new ModelTransformEditorWindow(this.staticModelInstances.get(modelIndex), transform, null), this);
				break;
			}
			}
		}
		else if (o instanceof Pair) {
			if (((Pair<?, ?>) o).first instanceof Long && ((Pair<?, ?>) o).second instanceof ModelTransform) {
				//we have a new static model. 

				Pair<Long, ModelTransform> p = (Pair<Long, ModelTransform>) o;

				Long assetID = p.first;
				ModelTransform transform = p.second;

				//add to project, and register as dependency
				this.state.addStaticModel(assetID, transform);

				//add the static model to the scene
				Model m = this.project.getModel(assetID);
				ModelInstance instance = new ModelInstance(m, transform, PERSPECTIVE_WORLD_SCENE);
				this.staticModelInstances.add(instance);
			}
		}
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
		if (this.isCursorLocked()) {
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

	}

	@Override
	protected void deselected() {

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

		}
	}

	@Override
	protected void _keyReleased(int key) {
		// TODO Auto-generated method stub

	}

}
