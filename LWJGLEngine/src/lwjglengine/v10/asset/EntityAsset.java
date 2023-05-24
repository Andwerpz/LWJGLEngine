package lwjglengine.v10.asset;

import java.io.File;
import java.io.IOException;

import lwjglengine.v10.project.Project;

public class EntityAsset extends Asset {

	public static final String ENTITY_ASSET_FILE_EXT = "ent";

	public EntityAsset(File file, long id, String name, Project project) {
		super(file, id, name, project);
	}

	@Override
	protected void _load() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _unload() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _save() throws IOException {

	}

	@Override
	protected void _computeDependencies() {

	}

}
