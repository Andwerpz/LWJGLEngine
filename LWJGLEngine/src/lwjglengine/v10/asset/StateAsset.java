package lwjglengine.v10.asset;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import lwjglengine.v10.model.ModelTransform;
import lwjglengine.v10.project.Project;
import myutils.v10.misc.Pair;

public class StateAsset extends Asset {

	public static final String STATE_ASSET_FILE_EXT = "sta";

	private static final String HEADER_STATIC_MODELS = "static models";

	//for now, everything is in one scene
	//TODO implement scenes

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
				long modelID = Long.parseLong(fin.readLine());
				String tmp = "";
				tmp += fin.readLine() + "\n";
				tmp += fin.readLine() + "\n";
				tmp += fin.readLine();
				ModelTransform transform = ModelTransform.parseModelTransform(tmp);
				this.staticModels.add(new Pair<Long, ModelTransform>(modelID, transform));
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
		fout.write(this.staticModels.size());
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

}
