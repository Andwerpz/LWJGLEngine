package lwjglengine.v10.asset;

import java.io.File;
import java.io.IOException;

import lwjglengine.v10.graphics.Texture;
import lwjglengine.v10.project.Project;
import myutils.v11.file.FileUtils;

public class TextureAsset extends FileAsset {

	private Texture texture;

	public TextureAsset(File file, long id, String name, Project project) {
		super(file, id, name, project);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void _load() throws IOException {
		this.texture = new Texture(FileUtils.loadImage(this.file.getPath()), Texture.VERTICAL_FLIP_BIT);
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
