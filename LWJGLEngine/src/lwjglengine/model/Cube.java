package lwjglengine.model;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;

import myutils.math.Mat4;
import myutils.math.MathUtils;
import myutils.math.Quaternion;
import myutils.math.Vec3;

public class Cube extends Model {
	//unit cube centered at origin

	public static final Cube DEFAULT_CUBE = new Cube();

	public Cube() {
		super(createMesh());
	}

	public static VertexArray createMesh() {
		// @formatter:off		
		float[] vertices = new float[] {
			1, 1, 1,	//+x
			1, -1, 1,
			1, -1, -1,
			1, 1, -1,
			-1, 1, 1,	//-x
			-1, 1, -1,
			-1, -1, -1,
			-1, -1, 1,
			1, 1, 1,	//+y
			1, 1, -1,
			-1, 1, -1,
			-1, 1, 1,
			1, -1, 1,	//-y
			-1, -1, 1,
			-1, -1, -1,
			1, -1, -1,
			1, 1, 1,	//+z
			-1, 1, 1,
			-1, -1, 1,
			1, -1, 1,
			1, 1, -1,	//-z
			1, -1, -1,
			-1, -1, -1,
			-1, 1, -1,
		};
		
		float[] uvs = new float[] {
			0, 0, 0, 1, 1, 1, 1, 0,
			0, 0, 0, 1, 1, 1, 1, 0,
			0, 0, 0, 1, 1, 1, 1, 0,
			0, 0, 0, 1, 1, 1, 1, 0,
			0, 0, 0, 1, 1, 1, 1, 0,
			0, 0, 0, 1, 1, 1, 1, 0,
		};
		
		int[] indices = new int[] {
			0, 1, 2, 0, 2, 3,
			4, 5, 6, 4, 6, 7,
			8, 9, 10, 8, 10, 11,
			12, 13, 14, 12, 14, 15,
			16, 17, 18, 16, 18, 19,
			20, 21, 22, 20, 22, 23,
		};
		// @formatter:on

		VertexArray va = new VertexArray(vertices, uvs, indices, GL_TRIANGLES);
		return va;
	}

	public ModelInstance addCube(Vec3 pos, Vec3 dim, Quaternion orient, int scene) {
		return new ModelInstance(this, generateCubeModelTransform(pos, dim, orient), scene);
	}

	public static ModelTransform generateCubeModelTransform(Vec3 pos, Vec3 dim, Quaternion orient) {
		Mat4 transform = Mat4.scale(dim.div(2.0f));
		transform.muli(MathUtils.quaternionToRotationMat4(orient));
		transform.muli(Mat4.translate(pos));
		return new ModelTransform(transform);
	}

	public static ModelInstance addDefaultCube(Vec3 pos, Vec3 dim, Quaternion orient, int scene) {
		return DEFAULT_CUBE.addCube(pos, dim, orient, scene);
	}

	public static ModelInstance addDefaultCube(Vec3 pos, Vec3 dim, int scene) {
		return addDefaultCube(pos, dim, Quaternion.identity(), scene);
	}

}
