package lwjglengine.asset;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import lwjglengine.model.ModelTransform;
import lwjglengine.project.Project;
import myutils.misc.Pair;

public class StateAsset extends Asset {

	public static final String STATE_ASSET_FILE_EXT = "sta";

	private static final String HEADER_STATIC_MODELS = "static models";

	//for now, everything is in one scene
	//TODO implement scenes
	
	//TODO some form of scripting, for example, when to transition to other scenes?
	// - like, when the player touches some sort of load trigger, a scene transition is played. 

	public ArrayList<Pair<Long, ModelTransform>> staticModels;

	public StateAsset(File file, long id, String name, Project project) {
		super(file, id, name, project);
	}

	@Override
	protected void _load() throws IOException {
		this.staticModels = new ArrayList<>();

		BufferedReader fin = new BufferedReader(new FileReader(this.getFile()));
		String header = fin.readLine();
		while (header != null) {
			switch (header) {
			case HEADER_STATIC_MODELS: {
				int nrModels = Integer.parseInt(fin.readLine());
				for (int i = 0; i < nrModels; i++) {
					long modelID = Long.parseLong(fin.readLine());
					String tmp = "";
					tmp += fin.readLine() + "\n";
					tmp += fin.readLine() + "\n";
					tmp += fin.readLine();
					ModelTransform transform = ModelTransform.parseModelTransform(tmp);
					this.staticModels.add(new Pair<Long, ModelTransform>(modelID, transform));
				}

				break;
			}
			}
			header = fin.readLine();
		}
		fin.close();
	}

	@Override
	protected void _unload() {
		this.staticModels = null;
	}

	@Override
	protected void _save() throws IOException {
		FileWriter fout = new FileWriter(this.getFile());

		//static models
		fout.write(HEADER_STATIC_MODELS + "\n");
		fout.write(this.staticModels.size() + "\n");
		for (int i = 0; i < this.staticModels.size(); i++) {
			fout.write(this.staticModels.get(i).first + "\n");
			fout.write(this.staticModels.get(i).second + "\n");
		}

		fout.close();
	}

	@Override
	protected void _computeDependencies() {
		//static models
		for (int i = 0; i < this.staticModels.size(); i++) {
			this.addDependency(this.staticModels.get(i).first);
		}
	}

	public ArrayList<Pair<Long, ModelTransform>> getStaticModels() {
		return this.staticModels;
	}

	public void addStaticModel(Long assetID, ModelTransform transform) {
		if (!(this.project.getAsset(assetID) instanceof ModelAsset)) {
			return;
		}

		this.addDependency(assetID);
		this.staticModels.add(new Pair<Long, ModelTransform>(assetID, transform));
	}

}
