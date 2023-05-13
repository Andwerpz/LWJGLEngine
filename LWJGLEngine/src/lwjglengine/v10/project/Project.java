package lwjglengine.v10.project;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class Project {
	//stores information related to a project :D

	private static final String PROJECT_FILE_NAME = "project.dat";

	private static final String VERSION_HEADER_10 = "Halfcup Project v1.0";

	private File projectFile;

	//list of all assets within this thing. 
	private ArrayList<String> assetNames;

	public Project(File file) {
		this.projectFile = file;
		this.init();
	}

	/**
	 * Creates a new folder within the given directory to put the new project in. 
	 * @param directory
	 * @return
	 */
	public static Project createNewProject(File directory, String projectName) throws IOException {
		if (!directory.exists()) {
			throw new IOException("When creating a project, the given parent directory must exist");
		}
		if (!directory.isDirectory()) {
			throw new IOException("When creating a project, the given filepath should be a directory");
		}

		File projectDirectory = new File(directory.getPath() + projectName + "\\");

		if (!projectDirectory.exists()) {
			projectDirectory.mkdir();
		}

		File projectFile = new File(projectDirectory.getPath() + PROJECT_FILE_NAME);
		projectFile.createNewFile();

		FileWriter fout = new FileWriter(projectFile);

		fout.write(VERSION_HEADER_10 + "\n");

		fout.write("0\n");

		fout.close();

		Project p = new Project(projectFile);

		return p;
	}

	private void init() {
		this.assetNames = new ArrayList<String>();

		try {
			this.loadProject(this.projectFile);
		}
		catch (IOException e) {
			System.err.println("Failed to load project file " + this.projectFile.getName());
			e.printStackTrace();
		}
	}

	private void loadProject(File file) throws IOException {
		BufferedReader fin = new BufferedReader(new FileReader(file));
		String versionHeader = fin.readLine();
		switch (versionHeader) {
		case VERSION_HEADER_10:
			this.loadProject10(fin);
			break;
		}
	}

	private void loadProject10(BufferedReader fin) throws IOException {
		int numAssets = Integer.parseInt(fin.readLine());
		for (int i = 0; i < numAssets; i++) {
			this.assetNames.add(fin.readLine());
		}
		Collections.sort(this.assetNames);
	}

	/**
	 * Updates the contents of the current project file to match whatever is stored by this object. 
	 */
	public void updateProjectFile() {
		//TODO
	}
}
