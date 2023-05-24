package lwjglengine.v10.asset;

import java.io.File;
import java.io.IOException;

import lwjglengine.v10.project.Project;
import myutils.v10.file.FileUtils;

public abstract class FileAsset extends Asset {

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

	public static FileAsset createFileAsset(File f, long id, String name, Project project, int type) {
		switch (type) {
		case FILE_TYPE_MODEL:
			return new ModelAsset(f, id, name, project);

		case FILE_TYPE_TEXTURE:
			return new TextureAsset(f, id, name, project);

		case FILE_TYPE_SOUND:
			return new SoundAsset(f, id, name, project);

		case FILE_TYPE_OTHER:
			return new OtherAsset(f, id, name, project);
		}

		return null;
	}

	public static FileAsset createFileAsset(File f, long id, String name, Project project) {
		return createFileAsset(f, id, name, project, FileAsset.determineFileType(f));
	}

	public int getFileType() {
		return this.fileType;
	}

}
