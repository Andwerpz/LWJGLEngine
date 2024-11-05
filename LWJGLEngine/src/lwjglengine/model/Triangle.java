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
		int[] indices = new int[] { 0, 1, 2 };

		this.meshes.add(new VertexArray(vertices, uvs, indices, GL_TRIANGLES));
		this.defaultMaterials.add(Material.defaultMaterial());
		this.textureMaterials.add(TextureMaterial.defaultTextureMaterial());
	}

	public static ModelTransform generateTriangleModelTransform(Vec3 a, Vec3 b, Vec3 c) {
		//avoid degenerate cases where all three points only span a coordinate plane
		float xadd = (a.x == 0 && b.x == 0 && c.x == 0) ? 1 : 0;
		float yadd = (a.y == 0 && b.y == 0 && c.y == 0) ? 1 : 0;
		float zadd = (a.z == 0 && b.z == 0 && c.z == 0) ? 1 : 0;

		Mat4 mat1 = Mat4.identity();
		mat1.mat[0][0] = a.x + xadd;
		mat1.mat[1][0] = a.y + yadd;
		mat1.mat[2][0] = a.z + zadd;
		mat1.mat[0][1] = b.x + xadd;
		mat1.mat[1][1] = b.y + yadd;
		mat1.mat[2][1] = b.z + zadd;
		mat1.mat[0][2] = c.x + xadd;
		mat1.mat[1][2] = c.y + yadd;
		mat1.mat[2][2] = c.z + zadd;

		mat1.muli(Mat4.translate(-xadd, -yadd, -zadd));

		ModelTransform transform = new ModelTransform(mat1);
		return transform;
	}

	public static ModelInstance addTriangle(Vec3 a, Vec3 b, Vec3 c, int scene) {
		return new ModelInstance(triangle, generateTriangleModelTransform(a, b, c), scene);
	}

	public static ModelInstance addTriangle(int scene) {
		return Triangle.addTriangle(new Vec3(1, 0, 0), new Vec3(0, 1, 0), new Vec3(0, 0, 1), scene);
	}

}
