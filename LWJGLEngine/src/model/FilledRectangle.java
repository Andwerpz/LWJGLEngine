package model;

import static org.lwjgl.opengl.GL11.*;

import graphics.TextureMaterial;
import graphics.VertexArray;
import util.Mat4;
import util.Vec3;

public class FilledRectangle extends Model {
	// as the name suggests, this rectangle is indeed filled
	// you can render any rectangle with the transformation of the rectangle (0, 0, 0) to (1, 1, 0).

	public static final FilledRectangle DEFAULT_RECTANGLE = new FilledRectangle();

	public FilledRectangle() {
		super();
	}

	@Override
	public void create() {
		float[] vertices = new float[] { 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, };

		float[] uvs = new float[] { 0, 0, 1, 0, 1, 1, 0, 1, };

		int[] indices = new int[] { 0, 1, 2, 0, 2, 3, };

		this.meshes.add(new VertexArray(vertices, uvs, indices, GL_TRIANGLES));
		this.defaultMaterials.add(DEFAULT_MATERIAL);
		this.textureMaterials.add(TextureMaterial.defaultTextureMaterial());
	}

	public long addRectangle(float x, float y, float width, float height, int scene) {
		Mat4 modelMat4 = Mat4.scale(width, height, 1).mul(Mat4.translate(new Vec3(x, y, 0)));
		return Model.addInstance(this, modelMat4, scene);
	}

	public long addRectangle(float x, float y, float z, float width, float height, int scene) {
		Mat4 modelMat4 = Mat4.scale(width, height, 1).mul(Mat4.translate(new Vec3(x, y, z)));
		return Model.addInstance(this, modelMat4, scene);
	}

}
