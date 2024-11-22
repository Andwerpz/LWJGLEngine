package lwjglengine.model;

import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;

import java.util.ArrayList;

import lwjglengine.graphics.TextureMaterial;
import myutils.math.Mat4;
import myutils.math.Vec3;

public class CubeSphere extends Model {
	//unit circle centered at origin. 
	//creates sphere by first making unit cube, then normalizing all the vertices. 
	//TODO remove smooth shading artifacts by un-duplicating the vertices at cube edges. 

	public static final CubeSphere DEFAULT_CUBE_SPHERE = new CubeSphere();

	public CubeSphere() {
		super(createMesh(20));
	}

	public CubeSphere(int grid_res) {
		super(createMesh(grid_res));
	}

	public static VertexArray createMesh(int grid_res) {
		Vec3[][] grid = new Vec3[grid_res + 1][grid_res + 1];
		for (int i = 0; i <= grid_res; i++) {
			float x = -1.0f + 2.0f * i / grid_res;
			for (int j = 0; j <= grid_res; j++) {
				float y = -1.0f + 2.0f * j / grid_res;
				grid[i][j] = new Vec3(x, y, 1);
			}
		}

		Mat4[] side_transforms = new Mat4[6];
		side_transforms[0] = Mat4.rotateX((float) Math.PI / 2.0f * 0);
		side_transforms[1] = Mat4.rotateX((float) Math.PI / 2.0f * 1);
		side_transforms[2] = Mat4.rotateX((float) Math.PI / 2.0f * 2);
		side_transforms[3] = Mat4.rotateX((float) Math.PI / 2.0f * 3);
		side_transforms[4] = Mat4.rotateY((float) Math.PI / 2.0f * 1);
		side_transforms[5] = Mat4.rotateY((float) Math.PI / 2.0f * -1);

		ArrayList<Vec3> vertex_list = new ArrayList<>();
		ArrayList<Integer> index_list = new ArrayList<>();

		int indptr = 0;
		for (int i = 0; i < 6; i++) {
			Vec3[][] cgrid = new Vec3[grid_res + 1][grid_res + 1];
			int[][] inds = new int[grid_res + 1][grid_res + 1];
			for (int j = 0; j <= grid_res; j++) {
				for (int k = 0; k <= grid_res; k++) {
					cgrid[j][k] = side_transforms[i].mul(grid[j][k], 0);
					cgrid[j][k].normalize();
					inds[j][k] = indptr++;
					vertex_list.add(cgrid[j][k]);
				}
			}
			for (int j = 0; j < grid_res; j++) {
				for (int k = 0; k < grid_res; k++) {
					index_list.add(inds[j][k + 1]);
					index_list.add(inds[j][k]);
					index_list.add(inds[j + 1][k + 1]);

					index_list.add(inds[j + 1][k + 1]);
					index_list.add(inds[j][k]);
					index_list.add(inds[j + 1][k]);
				}
			}
		}

		VertexArray va = new VertexArray(vertex_list, index_list, GL_TRIANGLES);
		return va;
	}

	public ModelInstance addSphere(Vec3 pos, float radius, int scene) {
		return new ModelInstance(this, generateSphereModelTransform(pos, radius), scene);
	}

	public static ModelTransform generateSphereModelTransform(Vec3 pos, float radius) {
		Mat4 transform = Mat4.scale(radius);
		transform.muli(Mat4.translate(pos));
		return new ModelTransform(transform);
	}

	public static ModelInstance addDefaultSphere(Vec3 pos, float radius, int scene) {
		return DEFAULT_CUBE_SPHERE.addSphere(pos, radius, scene);
	}

}
