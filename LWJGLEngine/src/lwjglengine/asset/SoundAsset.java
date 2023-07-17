package lwjglengine.asset;

import java.io.File;
import java.io.IOException;

import lwjglengine.project.Project;

public class SoundAsset extends Asset {
	
	//TODO make this work

	public SoundAsset(File file, long id, String name, Project project) {
		super(file, id, name, project);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void _load() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _unload() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _save() throws IOException {
		//shouldn't do anything
	}

	@Override
	protected void _computeDependencies() {
		//shouldn't have any dependencies
	}

}
