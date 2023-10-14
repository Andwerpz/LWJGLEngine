package lwjglengine.asset;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import lwjglengine.graphics.Texture;
import lwjglengine.project.Project;
import myutils.file.FileUtils;
import myutils.file.TargaReader;

public class TextureAsset extends Asset {

	private Texture texture;

	public TextureAsset(File file, long id, String name, Project project) {
		super(file, id, name, project);
	}

	@Override
	protected void _load() throws IOException {
		String fileExtension = FileUtils.getFileExtension(this.getFile());
		BufferedImage img = null;

		switch (fileExtension) {
		case "png":
		case "jpg":
		case "jpeg":
			img = FileUtils.loadImage(this.getFile().getAbsolutePath());
			break;

		case "tga":
			img = TargaReader.getImage(this.getFile().getAbsolutePath());
			break;
		}

		this.texture = new Texture(img);
	}

	@Override
	protected void _unload() {
		this.texture.kill();
		this.texture = null;
	}

	@Override
	protected void _save() throws IOException {
		//shouldn't do anything
	}

	@Override
	protected void _computeDependencies() {
		//shouldn't have any dependencies
	}

	public Texture getTexture() {
		return this.texture;
	}

}
