package lwjglengine.v10.project;

import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.screen.PerspectiveScreen;
import lwjglengine.v10.screen.UIScreen;
import lwjglengine.v10.window.Window;

public class ProjectStateEditorWindow extends Window {

	//somehow, i want to be able to run the project state in this window. 
	//ideally using the actual project state...

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
	}

	@Override
	protected void _kill() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _resize() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _update() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void renderContent(Framebuffer outputBuffer) {
		// TODO Auto-generated method stub

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
		// TODO Auto-generated method stub

	}

	@Override
	protected void _keyReleased(int key) {
		// TODO Auto-generated method stub

	}

}
