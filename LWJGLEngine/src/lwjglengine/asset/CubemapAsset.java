package lwjglengine.asset;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import lwjglengine.graphics.Cubemap;
import lwjglengine.graphics.Texture;
import lwjglengine.project.Project;

public class CubemapAsset extends Asset {

	//TODO figure out what to put in the cubemap file. 
	// - best way would be to use the texture asset, but the problem there is that I don't know how to link up
	//   multiple textures to form one cubemap
	// - current method is to save relative filepaths to the textures, but then how will we do error textures when
	//   they inevitably go missing?

	public static final String CUBEMAP_ASSET_FILE_EXT = "cube";

	private Cubemap cubemap;

	private String[] sides;

	public CubemapAsset(File file, long id, String name, Project project) {
		super(file, id, name, project);
	}

	@Override
	protected void _load() throws IOException {
		this.sides = new String[6];

		BufferedReader fin = new BufferedReader(new FileReader(this.getFile()));

		for (int i = 0; i < 6; i++) {
			this.sides[i] = fin.readLine();
		}

		fin.close();
	}

	@Override
	protected void _unload() {
		this.cubemap.kill();
		this.cubemap = null;
	}

	@Override
	protected void _save() throws IOException {
		FileWriter fout = new FileWriter(this.getFile());

		//		for (int i = 0; i < 6; i++) {
		//			if (this.sides[i] == -1) {
		//				continue;
		//			}
		//			fout.write(this.sides[i] + "\n");
		//		}

		fout.close();
	}

	@Override
	protected void _computeDependencies() {
		//no dependencies.
	}

	public Cubemap getCubemap() {
		return this.cubemap;
	}

}
