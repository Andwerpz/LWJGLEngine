package lwjglengine.v10.asset;

import java.io.File;
import java.io.IOException;

import lwjglengine.v10.project.Project;

public class UnknownAsset extends Asset {

	public UnknownAsset(File file, long id, String name, Project project) {
		super(file, id, name, project);
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
		// TODO Auto-generated method stub

	}

	@Override
	protected void _computeDependencies() {
		// TODO Auto-generated method stub

	}

}