package lwjglengine.v10.asset;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import lwjglengine.v10.project.Project;
import myutils.v11.file.FileUtils;

public abstract class Asset {
	//asset class is only responsible for loading itself. 
	//loading of dependencies is done outside of asset class. 

	//however, this class is responsible for making sure that it's own dependencies are correct. 

	//assets will update their dependencies when unloading, if the project is currently in edit mode. 

	//TODO
	// - rework saving info in text files, it would be nice to have a format that is like json or something. 

	public static final int TYPE_UNKNOWN = -1; //never used 
	public static final int TYPE_FILE = 0;
	public static final int TYPE_ENTITY = 1;
	public static final int TYPE_STATE = 2;

	protected Project project;

	//assets that need to be loaded for this state to function. 
	//this includes the assets of dependencies, such as entities that are loaded within this state. 
	//all these assets will be loaded in the load function to improve the user experience :D
	private HashSet<Long> assetDependencies;

	protected boolean loaded = false;

	protected File file;
	protected int type;

	protected long id;

	protected String name;

	public Asset(File file, long id, String name, Project project) {
		this.file = file;
		this.id = id;
		this.type = determineType(this);
		this.name = name;
		this.project = project;
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
			return FileAsset.createFileAsset(f, id, name, project);
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

	//load yourself
	//only called from assetDependencyNode
	protected void load() {
		if (this.loaded) {
			return;
		}

		try {
			this._load();
		}
		catch (IOException e) {
			System.err.println("Failed to load asset : " + this.name);
			e.printStackTrace();
			return;
		}
		this.loaded = true;
	}

	protected abstract void _load() throws IOException;

	//unload yourself
	//only called from assetDependencyNode
	protected void unload() {
		if (!this.loaded) {
			return;
		}

		if (this.project.isEditing()) {
			this.save();
		}

		this._unload();
		this.loaded = false;
	}

	protected abstract void _unload();

	//we must load the asset in order to save it. 
	//if we don't load it, then how is the asset going to know what to save?
	public void save() {
		if (!this.isLoaded()) {
			System.err.println("Asset Warning : Tried to save unloaded asset named : " + this.getName());
			return;
		}

		this.computeDependencies();
		try {
			this._save();
		}
		catch (IOException e) {
			System.out.println("Asset failed to save : " + this.getName());
			e.printStackTrace();
		}
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

	private void computeDependencies() {
		this.assetDependencies.clear();
		this._computeDependencies();
	}

	//should tally up all of the dependencies from this asset. 
	protected abstract void _computeDependencies();

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

	public Project getProject() {
		return this.project;
	}

	public boolean isLoaded() {
		return this.loaded;
	}
}
