package lwjglengine.v10.entity;

import lwjglengine.v10.asset.EntityAsset;
import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.project.Project;
import lwjglengine.v10.window.Window;

public class ProjectEntityEditorWindow extends Window {

	private EntityAsset entity;

	private Project project;

	public ProjectEntityEditorWindow(int xOffset, int yOffset, int width, int height, Project project, EntityAsset entity, Window parentWindow) {
		super(xOffset, yOffset, width, height, parentWindow);
		this.init(project, entity);
	}

	private void init(Project project, EntityAsset entity) {
		this.project = project;
		this.entity = entity;

		this.project.loadAsset(this.entity.getID());
	}

	@Override
	protected void _kill() {
		this.project.unloadAsset(this.entity.getID());
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
