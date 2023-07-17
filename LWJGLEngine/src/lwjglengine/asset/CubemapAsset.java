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
	
	//i think i'm just going to put 6 asset ids in the cubemap file. 
	
	//i'll have to write an editor for this asset. By default, all the textures on the cubemap are
	//going to be error textures. 

	public static final String CUBEMAP_ASSET_FILE_EXT = "cube";

	private Cubemap cubemap;
	
	private int[] sideIDs;

	public CubemapAsset(File file, long id, String name, Project project) {
		super(file, id, name, project);
		
		this.sideIDs = new int[6];
		for(int i = 0; i < 6; i++) {
			this.sideIDs[i] = -1;
		}
	}

	@Override
	protected void _load() throws IOException {
		BufferedReader fin = new BufferedReader(new FileReader(this.getFile()));
		
		Texture[] textures = new Texture[6];
		int[] sideIDs = new int[6];
		for (int i = 0; i < 6; i++) {
			int id = Integer.parseInt(fin.readLine());
			sideIDs[i] = id;
			Texture texture = this.project.getTexture(id);
			textures[i] = texture;
		}
		
		// idk if this works lol
		this.cubemap = new Cubemap(textures);

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

		for (int i = 0; i < 6; i++) {
			fout.write(this.sideIDs[i] + "\n");
		}

		fout.close();
	}

	@Override
	protected void _computeDependencies() {
		for(int i = 0; i < 6; i++) {
			this.addDependency(this.sideIDs[i]);
		}
	}

	public Cubemap getCubemap() {
		return this.cubemap;
	}

}
