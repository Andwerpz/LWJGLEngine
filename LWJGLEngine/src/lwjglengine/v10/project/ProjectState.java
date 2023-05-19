package lwjglengine.v10.project;

import java.io.File;

import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.state.State;

public class ProjectState extends State {
	//this is for running the state in production. 

	private File stateFile;

	public ProjectState(File stateFile) {
		super();

		this.stateFile = stateFile;
	}

	@Override
	public void load() {
		//load the state file
	}

	@Override
	public void buildBuffers() {
		// TODO Auto-generated method stub

	}

	@Override
	public void kill() {
		// TODO Auto-generated method stub

	}

	@Override
	public void update() {
		// TODO Auto-generated method stub

	}

	@Override
	public void render(Framebuffer outputBuffer) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mousePressed(int button) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseReleased(int button) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseScrolled(float wheelOffset, float smoothOffset) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyPressed(int key) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyReleased(int key) {
		// TODO Auto-generated method stub

	}

}
