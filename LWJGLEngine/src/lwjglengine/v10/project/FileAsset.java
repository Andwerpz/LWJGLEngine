package lwjglengine.v10.project;

import java.io.File;
import java.io.IOException;

import myutils.v10.file.FileUtils;

public class FileAsset extends Asset {

	public static final int FILE_TYPE_OTHER = -1;
	public static final int FILE_TYPE_MODEL = 0;
	public static final int FILE_TYPE_TEXTURE = 1;
	public static final int FILE_TYPE_SOUND = 2;

	private int fileType;

	public FileAsset(File file, long id, String name, Project project) {
		super(file, id, name, project);
		this.fileType = determineFileType(file);
	}

	public static int determineFileType(File f) {
		String extension = FileUtils.getFileExtension(f.getPath());
		switch (extension) {
		case "obj":
			return FILE_TYPE_MODEL;

		case "tga":
		case "png":
		case "jpg":
			return FILE_TYPE_TEXTURE;

		case "ogg":
		case "wav":
			return FILE_TYPE_SOUND;
		}

		return FILE_TYPE_OTHER;
	}

	public int getFileType() {
		return this.fileType;
	}

	@Override
	protected void _load() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _unload() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _save() throws IOException {
		//for now, we can just do nothing. 
	}

	@Override
	protected void computeDependencies() {
		//this type of asset should have no dependencies. 
	}

}
