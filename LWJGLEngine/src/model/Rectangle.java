package model;

import static org.lwjgl.opengl.GL11.*;

import graphics.Material;
import graphics.TextureMaterial;
import graphics.VertexArray;
import util.Mat4;
import util.Vec3;

public class Rectangle extends Model {
	// this rectangle is not filled, it's just a wireframe.
	// you can render any rectangle with the transformation of the rectangle (0, 0)
	// to (1, 1).

	private static Rectangle rectangle = new Rectangle();

	public Rectangle() {
		super();
	}

	@Override
	public void create() {
		float[] vertices = new float[] { 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, };

		float[] uvs = new float[] { 0, 0, 1, 0, 1, 1, 0, 1, };

		int[] indices = new int[] { 1, 0, 1, 2, 1, 2, 3, 2, 3, 0, 3, 0, };

		this.meshes.add(new VertexArray(vertices, uvs, indices, GL_LINES));
		this.defaultMaterials.add(Material.defaultMaterial());
		this.textureMaterials.add(TextureMaterial.defaultTextureMaterial());
	}

	public static long addRectangle(float x, float y, float width, float height, int scene) {
		Mat4 modelMat4 = Mat4.scale(width, height, 1).mul(Mat4.translate(new Vec3(x, y, 0)));
		return Model.addInstance(Rectangle.rectangle, modelMat4, scene);
	}

}
