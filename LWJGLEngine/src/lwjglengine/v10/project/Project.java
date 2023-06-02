package lwjglengine.v10.project;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;
import java.util.StringTokenizer;

import lwjglengine.v10.asset.Asset;
import lwjglengine.v10.asset.AssetDependencyNode;
import lwjglengine.v10.asset.EntityAsset;
import lwjglengine.v10.asset.ModelAsset;
import lwjglengine.v10.asset.StateAsset;
import lwjglengine.v10.asset.TextureAsset;
import lwjglengine.v10.graphics.Texture;
import lwjglengine.v10.model.Model;
import myutils.v10.algorithm.GraphUtils;
import myutils.v10.misc.BiMap;
import myutils.v11.file.FileUtils;

public class Project {
	//stores information related to a project :D

	//TODO
	// - process dependencies when saving. 
	//   - generate dependency tree
	//   - combine assets into strongly connected components, and load all assets in one component at once. 

	private static final String PROJECT_FILE_NAME = "project.dat";
	private static final String ASSETS_FILE_NAME = "assets.dat";
	private static final String ASSETS_DIRECTORY_NAME = "assets";

	private static final String VERSION_HEADER_10 = "Project v1.0";

	private File projectDirectory;

	private File projectFile;
	private File assetsFile;

	private File assetsDirectory;

	private String projectName;

	//list of all assets within this project. 
	//each asset gets an id used by this project. 
	private HashMap<Long, Asset> assets;

	private HashMap<String, Long> relativeFilepathToAsset;

	//list of dependency nodes for all the current assets. 
	//each dependency node represents one scc in the asset dependency graph. 
	//it is guaranteed that the graph formed by the asset dependency nodes is a DAG/tree. 
	private ArrayList<AssetDependencyNode> assetDependencyNodes;

	//maps asset id to dependency node
	//when loading assets, load by loading the asset dependency node associated with the asset. 
	private HashMap<Long, AssetDependencyNode> assetToDependencyNode;

	//assets that have been loaded from the outside, along with a count of how many times they were loaded. 
	private HashMap<Long, Integer> externallyLoadedAssets;

	private boolean isEditing = false;

	public Project(File projectDirectory) throws IOException {
		this.init(projectDirectory);
	}

	public void setIsEditing(boolean b) {
		this.isEditing = b;
	}

	public boolean isEditing() {
		return this.isEditing;
	}

	/**
	 * Creates a new folder within the given directory to put the new project in. 
	 * @param directory
	 * @return
	 */
	public static Project createNewProject(File parentDirectory, String projectName) throws IOException {
		if (!parentDirectory.exists()) {
			throw new IOException("When creating a project, the given parent directory must exist");
		}
		if (!parentDirectory.isDirectory()) {
			throw new IOException("When creating a project, the given filepath should be a directory");
		}

		System.out.println("PARENT DIRECTORY : " + parentDirectory.getPath());

		File projectDirectory = new File(parentDirectory.getPath() + File.separator + projectName);

		if (!projectDirectory.exists()) {
			projectDirectory.mkdir();
		}

		//create project.dat
		File projectFile = new File(projectDirectory.getPath() + File.separator + PROJECT_FILE_NAME);
		projectFile.createNewFile();
		{
			FileWriter fout = new FileWriter(projectFile);
			fout.write(VERSION_HEADER_10 + "\n");
			fout.write(projectName + "\n");
			fout.close();
		}

		//create assets.dat
		File assetsFile = new File(projectDirectory.getPath() + File.separator + ASSETS_FILE_NAME);
		assetsFile.createNewFile();
		{
			FileWriter fout = new FileWriter(assetsFile);
			fout.write(0 + "\n");
			fout.close();
		}

		//create assets folder
		File assetsFolder = new File(projectDirectory.getPath() + File.separator + ASSETS_DIRECTORY_NAME);
		assetsFolder.mkdir();

		Project p = new Project(projectDirectory);

		return p;
	}

	private void init(File projectDirectory) throws IOException {
		this.projectDirectory = projectDirectory;

		this.assets = new HashMap<>();
		this.relativeFilepathToAsset = new HashMap<>();

		this.assetDependencyNodes = new ArrayList<>();
		this.assetToDependencyNode = new HashMap<>();

		this.externallyLoadedAssets = new HashMap<>();

		//look for project file
		this.projectFile = new File(this.projectDirectory.getPath() + File.separator + PROJECT_FILE_NAME);
		if (!this.projectFile.exists()) {
			throw new IOException("Project file does not exist");
		}

		//look for assets file
		this.assetsFile = new File(this.projectDirectory.getPath() + File.separator + ASSETS_FILE_NAME);
		if (!this.assetsFile.exists()) {
			throw new IOException("Assets file does not exist");
		}

		//look for assets directory
		this.assetsDirectory = new File(this.projectDirectory.getPath() + File.separator + ASSETS_DIRECTORY_NAME);
		if (!this.assetsDirectory.exists()) {
			throw new IOException("Assets directory does not exist");
		}

		this.loadProject();
	}

	public void kill() {
		//save project
		this.saveProject();

		//unload all assets
		//this is going to get quite a few warnings D:
		for (AssetDependencyNode i : this.assetDependencyNodes) {
			i.setNumLoadedDependents(1); //trick into thinking an unload is necessary
			i.unload();
		}
	}

	private void loadProject() throws IOException {
		BufferedReader fin = new BufferedReader(new FileReader(this.projectFile));
		String versionHeader = fin.readLine();
		fin.close();
		switch (versionHeader) {
		case VERSION_HEADER_10:
			this.loadProject10();
			break;
		}

		this.generateAssetDependencyNodes();
	}

	private void loadProject10() throws IOException {
		//load project.dat
		{
			BufferedReader fin = new BufferedReader(new FileReader(this.projectFile));
			String header = fin.readLine();
			this.projectName = fin.readLine();
			fin.close();
		}

		//load assets.dat
		{
			BufferedReader fin = new BufferedReader(new FileReader(this.assetsFile));
			int numAssets = Integer.parseInt(fin.readLine());
			//each asset will have relative filepath
			for (int i = 0; i < numAssets; i++) {
				//asset details
				long assetID = Long.parseLong(fin.readLine());
				String relativePath = fin.readLine();
				String assetName = fin.readLine();
				int assetType = Integer.parseInt(fin.readLine());

				File f = new File(this.assetsDirectory.getPath() + relativePath);
				Asset a = Asset.createAsset(f, assetID, assetName, this, assetType);
				this.assets.put(assetID, a);
				this.relativeFilepathToAsset.put(relativePath, assetID);

				//dependencies
				System.out.println("Asset : " + assetName);
				int numDependencies = Integer.parseInt(fin.readLine());
				StringTokenizer st = new StringTokenizer(fin.readLine());
				for (int j = 0; j < numDependencies; j++) {
					long dID = Long.parseLong(st.nextToken());
					System.out.println(dID);
					a.getDependencies().add(dID); //this is jank
				}
			}
			fin.close();
		}

	}

	/**
	 * Updates the .dat files associated with the project in order to save. 
	 * Also updates the assets. 
	 * @throws IOException
	 */
	public void saveProject() {
		try {
			//update all assets
			{
				for (long id : this.assets.keySet()) {
					Asset a = this.assets.get(id);
					if (a.isLoaded()) {
						a.save();
					}
				}
			}

			//update project.dat
			{
				FileWriter fout = new FileWriter(this.projectFile);
				fout.write(VERSION_HEADER_10 + "\n");
				fout.write(this.projectName + "\n");
				fout.close();
			}

			//update assets.dat
			{
				FileWriter fout = new FileWriter(this.assetsFile);
				fout.write(this.assets.size() + "\n");
				for (long id : this.assets.keySet()) {
					Asset a = this.assets.get(id);

					//asset details
					String absolutePath = a.getFilepath();
					String relativePath = absolutePath.substring(this.assetsDirectory.getPath().length());
					String assetName = a.getName();
					int assetType = a.getType();
					fout.write(id + "\n" + relativePath + "\n" + assetName + "\n" + assetType + "\n");

					//dependencies
					HashSet<Long> dependencies = a.getDependencies();
					fout.write(dependencies.size() + "\n");
					for (long i : dependencies) {
						fout.write(i + " ");
					}
					fout.write("\n");
				}
				fout.close();
			}

			//after updating the assets, generate new dependency nodes. 
			this.generateAssetDependencyNodes();
		}
		catch (IOException e) {
			System.err.println("Failed to save project");
			e.printStackTrace();
		}
	}

	private long generateAssetID() {
		long newID = -1;
		while (newID == -1 || this.assets.keySet().contains(newID)) {
			newID = (long) (Math.random() * 1000000000);
		}
		return newID;
	}

	private long addAsset(File f, String name, int type) {
		String absolutePath = f.getAbsolutePath();
		String relativePath = absolutePath.substring(this.assetsDirectory.getPath().length());

		if (this.relativeFilepathToAsset.containsKey(relativePath)) {
			return -1;
		}

		Asset a = Asset.createAsset(f, this.generateAssetID(), name, this, type);

		this.relativeFilepathToAsset.put(relativePath, a.getID());

		this.assets.put(a.getID(), a);

		this.saveProject();

		return a.getID();
	}

	//if there is a file that is outside of the current assets folder, then we can copy it in. 
	public long addAsset(File f) {
		if (f.isDirectory()) {
			return -1; //can't have a directory as an asset. 
		}

		//just make copy of given file into assets folder
		String filename = f.getName();
		String relFilepath = File.separator + filename;
		String assetFilepath = this.assetsDirectory.getPath() + File.separator + filename;

		if (this.findAssetFromRelativeFilepath(relFilepath) != -1) {
			return -1; //asset already exists. 
		}

		File copy = new File(this.assetsDirectory.getPath() + File.separator + filename);

		try {
			Files.copy(f.toPath(), copy.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		catch (IOException e) {
			System.err.println("Failed to copy file into asset folder : " + filename);
			e.printStackTrace();
		}

		System.out.println("FILEPATH : " + copy.getPath());

		int assetType = Asset.determineType(copy);
		String assetName = copy.getName();

		return this.addAsset(copy, assetName, assetType);
	}

	public long createEntityAsset(String name) {
		File entityFile = new File(this.assetsDirectory.getPath() + File.separator + name + "." + EntityAsset.ENTITY_ASSET_FILE_EXT);
		try {
			entityFile.createNewFile();
		}
		catch (IOException e) {
			System.err.println("Failed to create entity asset");
			e.printStackTrace();
			return -1;
		}

		return this.addAsset(entityFile, name, Asset.TYPE_ENTITY);
	}

	public long createStateAsset(String name) {
		File stateFile = new File(this.assetsDirectory.getPath() + File.separator + name + "." + StateAsset.STATE_ASSET_FILE_EXT);
		try {
			stateFile.createNewFile();
		}
		catch (IOException e) {
			System.err.println("Failed to create state asset");
			e.printStackTrace();
			return -1;
		}

		return this.addAsset(stateFile, name, Asset.TYPE_STATE);
	}

	public String getProjectName() {
		return this.projectName;
	}

	public ArrayList<Asset> getAssetList() {
		ArrayList<Asset> ret = new ArrayList<>();
		ret.addAll(this.assets.values());
		return ret;
	}

	public Asset getAsset(long id) {
		return this.assets.get(id);
	}

	public boolean isAssetLoaded(long id) {
		Asset a = this.getAsset(id);
		return a != null && a.isLoaded();
	}

	public long findAssetFromRelativeFilepath(String relFilepath) {
		if (this.relativeFilepathToAsset.get(relFilepath) != null) {
			return this.relativeFilepathToAsset.get(relFilepath);
		}
		return -1;
	}

	public void loadAsset(long id) {
		if (!this.assets.containsKey(id)) {
			return;
		}

		this.externallyLoadedAssets.put(id, this.externallyLoadedAssets.getOrDefault(id, 0) + 1);

		AssetDependencyNode n = this.assetToDependencyNode.get(id);
		n.load();

		//System.err.println("LOADING ASSET VIA PROJECT : " + id);
	}

	public void unloadAsset(long id) {
		if (!this.assets.containsKey(id)) {
			return;
		}

		this.externallyLoadedAssets.put(id, this.externallyLoadedAssets.get(id) - 1);
		if (this.externallyLoadedAssets.get(id) == 0) {
			this.externallyLoadedAssets.remove(id);
		}

		AssetDependencyNode n = this.assetToDependencyNode.get(id);
		n.unload();

		//System.err.println("UNLOADING ASSET VIA PROJECT : " + id);
	}

	private boolean updatingDependencyGraph = false;
	private boolean shouldUpdateDependencyGraph = false;

	public void updateDependencyGraph() {
		//we should probably have a seperate thing for this, like project.addAssetDependency
		//and in that function, do this. 
		//when modifying the assets, we also probably want to update the dependency graph in realtime as well.

		//make sure that all dependencies are satisfied. 
		// - create a list of all externally loaded dependencies
		// - for each scc, if it includes an externally loaded dependency, then it should remain loaded, else it should be unloaded. 

		if (this.updatingDependencyGraph) {
			this.shouldUpdateDependencyGraph = true;
			return;
		}

		this.updatingDependencyGraph = true;
		this.shouldUpdateDependencyGraph = true;

		while (this.shouldUpdateDependencyGraph) {
			this.shouldUpdateDependencyGraph = false;

			this.generateAssetDependencyNodes();

			//find all dependency nodes that are loaded externally
			HashSet<AssetDependencyNode> shouldBeLoaded = new HashSet<>();
			Stack<AssetDependencyNode> dfsStack = new Stack<>();
			for (AssetDependencyNode i : this.assetDependencyNodes) {
				for (Asset j : i.getAssets()) {
					if (this.externallyLoadedAssets.containsKey(j.getID())) {
						dfsStack.push(i);
						shouldBeLoaded.add(i);
						break;
					}
				}
			}
			while (dfsStack.size() != 0) {
				AssetDependencyNode next = dfsStack.pop();
				for (AssetDependencyNode i : next.getDependencies()) {
					if (shouldBeLoaded.contains(i)) {
						continue;
					}
					shouldBeLoaded.add(i);
					dfsStack.push(i);
				}
			}

			//go through all nodes. If the node should not be loaded, then forcefully unload all the assets
			//we do this in the first pass because loading assets lower in the tree will interfere with unloading. 
			for (AssetDependencyNode i : this.assetDependencyNodes) {
				if (!shouldBeLoaded.contains(i)) {
					i.setNumLoadedDependents(1); //trick it into thinking it needs unloading
					//System.err.println("Force unloading asset dependency node");
					i.unload();
				}
			}

			//if it should be, then check to see if there are any external dependencies. If there are, you can go ahead and load it. 
			for (AssetDependencyNode i : this.assetDependencyNodes) {
				if (shouldBeLoaded.contains(i)) {
					//find sum of external dependents
					int sum = 0;
					for (Asset j : i.getAssets()) {
						if (this.externallyLoadedAssets.get(j.getID()) != null) {
							sum += this.externallyLoadedAssets.get(j.getID());
						}
					}

					if (sum != 0) {
						//this thing is loaded externally. run load once, then add to the number of external dependents sum - 1. 
						i.load(); //this might trigger assets to realize that they need to update their dependencies. 
						i.setNumLoadedDependents(i.getNumLoadedDependents() + sum - 1);
					}
					else {
						//this thing should eventually be loaded because it is a dependency of an externally loaded node
						//we don't have to do anything. 
					}
				}
			}
		}

		this.updatingDependencyGraph = false;

		this.saveProject();
	}

	private void generateAssetDependencyNodes() {
		//clear current info on scc
		this.assetDependencyNodes.clear();
		this.assetToDependencyNode.clear();

		//first, we run kosajaru's algorithm on the dependency graph to determine the strongly connected components. 
		BiMap<Long, Integer> idToIndex = new BiMap<>();
		ArrayList<ArrayList<Integer>> c = new ArrayList<>();
		for (long id : this.assets.keySet()) {
			int ind = c.size();
			idToIndex.put(id, ind);
			c.add(new ArrayList<Integer>());
		}
		for (long i : this.assets.keySet()) {
			int a = idToIndex.getValue(i);
			HashSet<Long> d = this.assets.get(i).getDependencies();
			for (long j : d) {
				int b = idToIndex.getValue(j);
				c.get(a).add(b);
			}
		}
		ArrayList<ArrayList<Integer>> scc = GraphUtils.kosajaru(c);

		//once sccs have been determined, we then figure out all the connections between the sccs. 
		ArrayList<AssetDependencyNode> a = new ArrayList<>();
		HashMap<Integer, Integer> indexToSCC = new HashMap<>();
		for (int i = 0; i < scc.size(); i++) {
			AssetDependencyNode node = new AssetDependencyNode(this);
			for (int j = 0; j < scc.get(i).size(); j++) {
				indexToSCC.put(scc.get(i).get(j), i);
				long assetID = idToIndex.getKey(scc.get(i).get(j));
				node.addAsset(this.assets.get(assetID));
				this.assetToDependencyNode.put(assetID, node);
			}
			a.add(node);
		}
		for (int i = 0; i < a.size(); i++) {
			HashSet<Asset> assets = a.get(i).getAssets();
			for (Asset j : assets) {
				HashSet<Long> dependencies = j.getDependencies();
				for (long k : dependencies) {
					int ind = idToIndex.getValue(k);
					int sccInd = indexToSCC.get(ind);
					a.get(i).addDependency(a.get(sccInd));
				}
			}
		}
		this.assetDependencyNodes = a;

		//for debug later
		//		int cnt = 0;
		//		for (AssetDependencyNode n : this.assetDependencyNodes) {
		//			System.out.println("ASSET DEPENDENCY NODE : " + (cnt++));
		//			HashSet<Asset> assets = n.getAssets();
		//			for (Asset asset : assets) {
		//				System.out.println(asset.getName());
		//			}
		//			System.out.println();
		//		}
	}

	public Texture getTexture(long id) {
		if (!isAssetLoaded(id)) {
			return null;
		}

		Asset a = this.getAsset(id);

		if (!(a instanceof TextureAsset)) {
			return null;
		}

		return ((TextureAsset) a).getTexture();
	}

	public Model getModel(long id) {
		if (!isAssetLoaded(id)) {
			return null;
		}

		Asset a = this.getAsset(id);

		if (!(a instanceof ModelAsset)) {
			return null;
		}

		return ((ModelAsset) a).getModel();
	}
}
