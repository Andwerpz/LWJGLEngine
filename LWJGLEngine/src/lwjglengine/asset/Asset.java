package lwjglengine.asset;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import lwjglengine.project.Project;
import myutils.v11.file.FileUtils;

public abstract class Asset {
	//asset class is only responsible for loading itself. 
	//loading of dependencies is done outside of asset class. 

	//however, this class is responsible for making sure that it's own dependencies are correct. 

	//assets will update their dependencies when unloading, if the project is currently in edit mode. 

	//TODO
	// - rework saving info in text files, it would be nice to have a format that is like json or something. 

	public static final int TYPE_UNKNOWN = -1; //never used 
	public static final int TYPE_ENTITY = 0;
	public static final int TYPE_STATE = 1;
	public static final int TYPE_MODEL = 2;
	public static final int TYPE_TEXTURE = 3;
	public static final int TYPE_SOUND = 4;
	public static final int TYPE_CUBEMAP = 5;

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

	private boolean firstLoad = true;

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
		switch (ext) {
		case StateAsset.STATE_ASSET_FILE_EXT:
			return TYPE_STATE;

		case EntityAsset.ENTITY_ASSET_FILE_EXT:
			return TYPE_ENTITY;

		case "obj":
			return TYPE_MODEL;

		case "tga":
		case "png":
		case "jpg":
		case "jpeg":
			return TYPE_TEXTURE;

		case "ogg":
		case "wav":
			return TYPE_SOUND;

		case CubemapAsset.CUBEMAP_ASSET_FILE_EXT:
			return TYPE_CUBEMAP;
		}
		return TYPE_UNKNOWN;
	}

	public static int determineType(Asset a) {
		if (a instanceof EntityAsset) {
			return TYPE_ENTITY;
		}
		else if (a instanceof StateAsset) {
			return TYPE_STATE;
		}
		else if (a instanceof ModelAsset) {
			return TYPE_MODEL;
		}
		else if (a instanceof TextureAsset) {
			return TYPE_TEXTURE;
		}
		else if (a instanceof SoundAsset) {
			return TYPE_SOUND;
		}
		else if (a instanceof CubemapAsset) {
			return TYPE_CUBEMAP;
		}
		return TYPE_UNKNOWN;
	}

	public static Asset createAsset(File f, long id, String name, Project project, int type) {
		switch (type) {
		case TYPE_ENTITY:
			return new EntityAsset(f, id, name, project);
		case TYPE_STATE:
			return new StateAsset(f, id, name, project);
		case TYPE_MODEL:
			return new ModelAsset(f, id, name, project);
		case TYPE_TEXTURE:
			return new TextureAsset(f, id, name, project);
		case TYPE_SOUND:
			return new SoundAsset(f, id, name, project);
		case TYPE_CUBEMAP:
			return new CubemapAsset(f, id, name, project);
		}
		return new UnknownAsset(f, id, name, project);
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

		System.out.println("LOADING ASSET : " + this.getName());

		//try to compute dependencies
		if (this.project.isEditing()) {
			try {
				this._load();
			}
			catch (IOException e) {
				System.err.println("Failed to load asset : " + this.name);
				e.printStackTrace();
				return;
			}

			if (this.project.isEditing()) {
				this.computeDependencies();
			}
			this._unload();
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

		System.out.println("UNLOADING ASSET : " + this.getName());

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
		if (!this.project.isEditing()) {
			System.err.println("Asset Warning : Trying to change dependencies when project is not in edit mode");
		}
		if (!this.isLoaded()) {
			System.err.println("Asset Warning : Probably shouldn't try to modify dependency of unloaded asset");
		}
		if (id == this.id) {
			System.err.println("Asset Warning : Can't have self as dependency");
			return;
		}

		if (!this.assetDependencies.contains(id)) {
			this.assetDependencies.add(id);

			if (!this.computingDependencies) {
				this.project.updateDependencyGraph();
			}
		}
	}

	public HashSet<Long> getDependencies() {
		return this.assetDependencies;
	}

	private boolean computingDependencies = false;

	protected void computeDependencies() {
		System.out.println("COMPUTING DEPENDENCIES : " + this.getName());

		HashSet<Long> oldDependencies = new HashSet<>();
		oldDependencies.addAll(this.assetDependencies);

		this.assetDependencies.clear();

		this.computingDependencies = true;
		this._computeDependencies();
		this.computingDependencies = false;

		if (!oldDependencies.equals(this.assetDependencies)) {
			this.project.updateDependencyGraph();
		}
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
