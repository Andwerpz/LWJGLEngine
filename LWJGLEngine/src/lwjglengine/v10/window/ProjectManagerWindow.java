package lwjglengine.v10.window;

import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.project.Project;

public class ProjectManagerWindow extends Window {

	//TODO
	// - should offer to create a new project or open one if a project isn't currently selected. 	

	private Project project;

	public ProjectManagerWindow(int xOffset, int yOffset, int width, int height, Window parentWindow) {
		super(xOffset, yOffset, width, height, parentWindow);
		// TODO Auto-generated constructor stub
	}

	public ProjectManagerWindow() {
		super(0, 0, 300, 300, null);
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
