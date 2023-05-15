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
import java.util.StringTokenizer;

public class Project {
	//stores information related to a project :D

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

		File projectDirectory = new File(parentDirectory.getPath() + File.separator +  projectName);

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

	private void init(File projectDirectory) throws IOException{
		this.projectDirectory = projectDirectory;
		
		this.assets = new HashMap<>();
		
		//look for project file
		this.projectFile = new File(this.projectDirectory.getPath() + File.separator + PROJECT_FILE_NAME);
		if(!this.projectFile.exists()) {
			throw new IOException("Project file does not exist");
		}
		
		//look for assets file
		this.assetsFile = new File(this.projectDirectory.getPath() + File.separator + ASSETS_FILE_NAME);
		if(!this.assetsFile.exists()) {
			throw new IOException("Assets file does not exist");
		}
		
		//look for assets directory
		this.assetsDirectory = new File(this.projectDirectory.getPath() + File.separator + ASSETS_DIRECTORY_NAME);
		if(!this.assetsDirectory.exists()) {
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
			for(int i = 0; i < numAssets; i++) {
				StringTokenizer st = new StringTokenizer(fin.readLine());
				long assetID = Long.parseLong(st.nextToken());
				String relativePath = st.nextToken();
				File f = new File(this.assetsDirectory.getPath() + File.separator + relativePath);
				Asset a = new Asset(f, assetID);
				this.assets.put(assetID, a);
			}
			fin.close();
		}
		
	}
	
	private long generateAssetID() {
		long newID = -1;
		while(newID == -1 || this.assets.keySet().contains(newID)) {
			newID = (long) (Math.random() * 1000000000);
		}
		return newID;
	}
	
	public void addAsset(File f) {
		//just make copy of given file into assets folder
		String filename = f.getName();
		File copy = new File(this.assetsDirectory.getPath() + File.separator + filename);
		
		try {
			Files.copy(f.toPath(), copy.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			System.err.println("Failed to copy file into asset folder : " + filename);
			e.printStackTrace();
		}
		
		Asset a = new Asset(copy, this.generateAssetID());
		this.assets.put(a.getID(), a);
	}
	
	public String getProjectName() {
		return this.projectName;
	}
	
	public ArrayList<Asset> getAssetList() {
		ArrayList<Asset> ret = new ArrayList<>();
		ret.addAll(this.assets.values());
		return ret;
	}
}
