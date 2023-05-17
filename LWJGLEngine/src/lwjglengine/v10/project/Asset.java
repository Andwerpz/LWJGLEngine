package lwjglengine.v10.project;

import java.io.File;

import myutils.v10.file.FileUtils;

public class Asset {
	
	//TODO
	// - make sure that for each asset, only one instance of each thing is loaded. 
	//   - ex. for a 3D model file, don't have multiple models from the same asset. 
	//   - perhaps this shouldn't be the responsibility of asset? maybe we load stuff based on scene?
	
	public static final int TYPE_OTHER = -1;
	public static final int TYPE_MODEL = 0;
	public static final int TYPE_TEXTURE = 1;
	public static final int TYPE_SOUND = 2;

	private File file;
	
	private int type;
	
	private long id;
	
	public Asset(File file, long id) {
		this.file = file;
		this.id = id;
		this.type = determineType(file);
	}
	
	public static int determineType(File f) {
		String extension = FileUtils.getFileExtension(f.getPath());
		switch(extension) {
		case "obj":
			return TYPE_MODEL;
			
		case "tga":
		case "png":
		case "jpg":
			return TYPE_TEXTURE;
			
		case "ogg":
		case "wav":
			return TYPE_SOUND;
		}
		
		return TYPE_OTHER;
	}
	
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
}
