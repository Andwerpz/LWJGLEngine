package lwjglengine.model;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;

import lwjglengine.graphics.Material;
import lwjglengine.graphics.TextureMaterial;
import myutils.math.Mat4;
import myutils.math.Vec3;

public class Triangle extends Model {
	//triangle with points at (1, 0, 0), (0, 1, 0), (0, 0, 1)
	//can apply a linear transformation to map these three into an arbitrary set of 3 other points. 

	private static Triangle triangle = new Triangle();

	public Triangle() {
		super();
	}

	@Override
	public void create() {
		float[] vertices = new float[] { 1, 0, 0, 0, 1, 0, 0, 0, 1 };
		float[] uvs = new float[] { 0, 0, 1, 0, 0, 1 };
		int[] indices = new int[] { 1, 0, 2 };

		this.meshes.add(new VertexArray(vertices, uvs, indices, GL_TRIANGLES));
		this.defaultMaterials.add(Material.defaultMaterial());
		this.textureMaterials.add(TextureMaterial.defaultTextureMaterial());
	}

	public static ModelInstance addTriangle(Vec3 a, Vec3 b, Vec3 c, int scene) {
		Mat4 mat = Mat4.identity();

		mat.mat[0][0] = a.x;
		mat.mat[1][0] = a.y;
		mat.mat[2][0] = a.z;
		mat.mat[0][1] = b.x;
		mat.mat[1][1] = b.y;
		mat.mat[2][1] = b.z;
		mat.mat[0][2] = c.x;
		mat.mat[1][2] = c.y;
		mat.mat[2][2] = c.z;

		ModelTransform transform = new ModelTransform(mat);
		return new ModelInstance(triangle, transform, scene);
	}

}
