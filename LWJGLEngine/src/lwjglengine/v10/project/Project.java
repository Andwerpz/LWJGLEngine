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
import java.util.StringTokenizer;

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

	//list of dependency nodes for all the current assets. 
	//each dependency node represents one scc in the asset dependency graph. 
	//it is guaranteed that the graph formed by the asset dependency nodes is a DAG/tree. 
	private ArrayList<AssetDependencyNode> assetDependencyNodes;

	//maps asset id to dependency node
	//when loading assets, load by loading the asset dependency node associated with the asset. 
	private HashMap<Long, AssetDependencyNode> assetToDependencyNode;

	public Project(File projectDirectory) throws IOException {
		this.init(projectDirectory);
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

		this.assetDependencyNodes = new ArrayList<>();
		this.assetToDependencyNode = new HashMap<>();

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

				//dependencies
				int numDependencies = Integer.parseInt(fin.readLine());
				StringTokenizer st = new StringTokenizer(fin.readLine());
				for (int j = 0; j < numDependencies; j++) {
					long dID = Long.parseLong(st.nextToken());
					a.addDependency(dID);
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
	private void saveProject() {
		try {
			//update all assets
			{
				for (long id : this.assets.keySet()) {
					Asset a = this.assets.get(id);
					a._save();
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

	//if there is a file that is outside of the current assets folder, then we can copy it in. 
	public long addAsset(File f) {
		//just make copy of given file into assets folder
		String filename = f.getName();
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
		String assetName = "";
		if (assetType == Asset.TYPE_FILE) {
			assetName = copy.getName();
		}
		else {
			String ext = FileUtils.getFileExtension(assetName);
			assetName = copy.getName().substring(0, copy.getName().length() - (ext.length() + 1));
		}

		long assetID = this.generateAssetID();
		Asset a = Asset.createAsset(copy, assetID, assetName, this, assetType);
		this.assets.put(a.getID(), a);

		//TODO figure out a better place to do this. 
		//perhaps autosave?
		this.saveProject();

		return assetID;
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

		Asset a = new EntityAsset(entityFile, this.generateAssetID(), name, this);
		this.assets.put(a.getID(), a);

		this.saveProject();

		return a.getID();
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

		Asset a = new StateAsset(stateFile, this.generateAssetID(), name, this);
		this.assets.put(a.getID(), a);

		this.saveProject();

		return a.getID();
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
}
