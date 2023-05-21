package lwjglengine.v10.project;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class StateAsset extends Asset {

	public static final String STATE_ASSET_FILE_EXT = "sta";

	public ArrayList<Long> staticModels;

	public StateAsset(File file, long id, String name, Project project) {
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
	protected void save() throws IOException {

	}

}
