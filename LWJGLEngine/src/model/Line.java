package model;

import static org.lwjgl.opengl.GL11.*;

import graphics.TextureMaterial;
import graphics.VertexArray;
import util.Mat4;
import util.Vec3;

public class Line extends Model {
	// you can render any line with a linear scaling of the line (0, 0, 0) to (1, 1,
	// 1).

	public static Line line = new Line();

	public Line() {
		super();
	}

	@Override
	public void create() {
		float[] vertices = new float[] { 0, 0, 0, 1, 1, 1, };

		float[] uvs = new float[] { 0, 0, 1, 1, };

		int[] indices = new int[] { 0, 1, 0, // 3 points since everything has to be a triangle
		};

		this.meshes.add(new VertexArray(vertices, uvs, indices, GL_LINES));
		this.defaultMaterials.add(DEFAULT_MATERIAL);
		this.textureMaterials.add(TextureMaterial.defaultTextureMaterial());
	}

	public static long addLine(float x1, float y1, float z1, float x2, float y2, float z2, int scene) {
		float dx = x2 - x1;
		float dy = y2 - y1;
		float dz = z2 - z1;
		Mat4 modelMat4 = Mat4.scale(dx, dy, dz).mul(Mat4.translate(new Vec3(x1, y1, z1)));
		return Model.addInstance(Line.line, modelMat4, scene);
	}

	public static long addLine(float x1, float y1, float x2, float y2, int scene) {
		float dx = x2 - x1;
		float dy = y2 - y1;
		Mat4 modelMat4 = Mat4.scale(dx, dy, 0).mul(Mat4.translate(new Vec3(x1, y1, 0)));
		return Model.addInstance(Line.line, modelMat4, scene);
	}

}
