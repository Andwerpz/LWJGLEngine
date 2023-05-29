package lwjglengine.v10.asset;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import lwjglengine.v10.graphics.Texture;
import lwjglengine.v10.project.Project;
import myutils.v10.file.TargaReader;
import myutils.v11.file.FileUtils;

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
			img = FileUtils.loadImage(this.getFile().getAbsolutePath());
			break;

		case "tga":
			img = TargaReader.getImage(this.getFile().getAbsolutePath());
			break;
		}

		this.texture = new Texture(img, Texture.VERTICAL_FLIP_BIT);
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
