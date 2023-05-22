package lwjglengine.v10.project;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import myutils.v11.file.FileUtils;

public abstract class Asset {
	//asset class is only responsible for loading itself. 
	//loading of dependencies is done outside of asset class. 

	//however, this class is responsible for making sure that it's own dependencies are correct. 

	//TODO
	// - make sure that for each asset, only one instance of each thing is loaded. 
	//   - ex. for a 3D model file, don't have multiple models from the same asset. 
	//   - perhaps this shouldn't be the responsibility of asset? maybe we load stuff based on scene?
	//   - nah, i think the asset should have a load and unload function, and the state should be able to load whatever file it needs from the project object
	//   - this comes with added bonus that if the asset was not loaded, then we can give an error texture/model/sound or whatever. 

	public static final int TYPE_UNKNOWN = -1; //never used 
	public static final int TYPE_FILE = 0;
	public static final int TYPE_ENTITY = 1;
	public static final int TYPE_STATE = 2;

	private Project project;

	//assets that need to be loaded for this state to function. 
	//this includes the assets of dependencies, such as entities that are loaded within this state. 
	//all these assets will be loaded in the load function to improve the user experience :D
	private HashSet<Long> assetDependencies;

	private boolean loaded = false;

	private File file;
	private int type;

	private long id;

	protected String name;

	public Asset(File file, long id, String name, Project project) {
		this.file = file;
		this.id = id;
		this.type = determineType(this);
		this.name = name;
		this.assetDependencies = new HashSet<>();
	}

	//infer the type of asset from the file extension
	public static int determineType(File f) {
		String ext = FileUtils.getFileExtension(f);
		if (ext.equals(StateAsset.STATE_ASSET_FILE_EXT)) {
			return TYPE_STATE;
		}
		else if (ext.equals(EntityAsset.ENTITY_ASSET_FILE_EXT)) {
			return TYPE_ENTITY;
		}
		else {
			return TYPE_FILE;
		}
	}

	public static int determineType(Asset a) {
		if (a instanceof FileAsset) {
			return TYPE_FILE;
		}
		else if (a instanceof EntityAsset) {
			return TYPE_ENTITY;
		}
		else if (a instanceof StateAsset) {
			return TYPE_STATE;
		}
		return TYPE_UNKNOWN;
	}

	public static Asset createAsset(File f, long id, String name, Project project, int type) {
		switch (type) {
		case TYPE_FILE: {
			return new FileAsset(f, id, f.getName(), project);
		}
		case TYPE_ENTITY: {
			return new EntityAsset(f, id, name, project);
		}
		case TYPE_STATE: {
			return new StateAsset(f, id, name, project);
		}
		}
		return null;
	}

	public static Asset createAsset(File f, long id, String name, Project project) {
		return createAsset(f, id, name, project, determineType(f));
	}

	//load, and load any dependencies. 
	public void load() {
		if (this.loaded) {
			return;
		}

		this._load();
		this.loaded = true;
	}

	protected abstract void _load();

	//should unload if numLoadedDependents == 0. 
	public void unload() {
		if (!this.loaded) {
			this.loaded = false;
		}

		this._unload();
		this.loaded = false;
	}

	protected abstract void _unload();

	public void save() {
		this.computeDependencies();
		this.save();
	}

	protected abstract void _save() throws IOException;

	//dependencies shouldn't change while actually playing the game, so we shouldn't have to worry about loading or unloading when 
	//changing dependencies. 
	public void addDependency(long id) {
		if (id == this.id) {
			System.err.println("Asset Warning : Can't have self as dependency");
			//can't have self as a dependency. 
			return;
		}
		this.assetDependencies.add(id);
	}

	public void removeDependency(long id) {
		this.assetDependencies.remove(id);
	}

	public HashSet<Long> getDependencies() {
		return this.assetDependencies;
	}

	protected abstract void computeDependencies();

	public long getID() {
		return this.id;
	}

	public int getType() {
		return this.type;
	}

	public File getFile() {
		return this.file;
	}

	public String getFilename() {
		return this.file.getName();
	}

	public String getFilepath() {
		return this.file.getPath();
	}

	public String getName() {
		return this.name;
	}
}
