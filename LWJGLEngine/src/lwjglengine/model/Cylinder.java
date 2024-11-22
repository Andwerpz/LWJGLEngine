package lwjglengine.model;

import static org.lwjgl.opengl.GL11.*;

import java.util.ArrayList;

import myutils.math.Mat4;
import myutils.math.MathUtils;
import myutils.math.Quaternion;
import myutils.math.Vec3;

public class Cylinder extends Model {
	//cylinder with radius 1, and height 2 centered at origin. 
	//cylinder is made by first making a unit square, then normalizing the vertices, then extruding it in the z axis. 
	//we construct the cylinder this way to make it perfectly fit with the cube sphere. 

	public static final Cylinder DEFAULT_CYLINDER = new Cylinder();

	public Cylinder() {
		super(createMesh(20));
	}

	public Cylinder(int res) {
		super(createMesh(res));
	}

	public static VertexArray createMesh(int res) {
		//create square and then normalize
		Vec3[] loop = new Vec3[res * 4];
		for (int i = 0; i < res; i++) {
			loop[i + res * 0] = new Vec3(-1.0f + 2.0f * i / res, 1, 0);
		}
		for (int i = 0; i < res; i++) {
			loop[i + res * 1] = new Vec3(1, 1.0f - 2.0f * i / res, 0);
		}
		for (int i = 0; i < res; i++) {
			loop[i + res * 2] = new Vec3(1.0f - 2.0f * i / res, -1, 0);
		}
		for (int i = 0; i < res; i++) {
			loop[i + res * 3] = new Vec3(-1, -1.0f + 2.0f * i / res, 0);
		}
		for (int i = 0; i < loop.length; i++) {
			loop[i].normalize();
		}

		ArrayList<Vec3> vertex_list = new ArrayList<>();
		ArrayList<Integer> ind_list = new ArrayList<>();

		//extrude along z axis
		int indptr = 0;
		int[] top_inds = new int[res * 4];
		int[] bot_inds = new int[res * 4];
		for (int i = 0; i < loop.length; i++) {
			top_inds[i] = indptr++;
			vertex_list.add(loop[i].add(new Vec3(0, 0, 1)));
			bot_inds[i] = indptr++;
			vertex_list.add(loop[i].add(new Vec3(0, 0, -1)));
		}
		for (int i = 0; i < loop.length; i++) {
			ind_list.add(top_inds[i]);
			ind_list.add(top_inds[(i + 1) % loop.length]);
			ind_list.add(bot_inds[(i + 1) % loop.length]);

			ind_list.add(top_inds[i]);
			ind_list.add(bot_inds[(i + 1) % loop.length]);
			ind_list.add(bot_inds[i]);
		}

		//top and bottom caps
		int top_center_ind = indptr ++;
		vertex_list.add(new Vec3(0, 0, 1));
		int bot_center_ind = indptr ++;
		vertex_list.add(new Vec3(0, 0, -1));
		for (int i = 0; i < loop.length; i++) {
			top_inds[i] = indptr++;
			vertex_list.add(loop[i].add(new Vec3(0, 0, 1)));
			bot_inds[i] = indptr++;
			vertex_list.add(loop[i].add(new Vec3(0, 0, -1)));
		}
		for(int i = 0; i < loop.length; i++) {
			ind_list.add(top_center_ind);
			ind_list.add(top_inds[(i + 1) % loop.length]);
			ind_list.add(top_inds[i]);
			
			ind_list.add(bot_center_ind);
			ind_list.add(bot_inds[i]);
			ind_list.add(bot_inds[(i + 1) % loop.length]);
		}

		VertexArray va = new VertexArray(vertex_list, ind_list, GL_TRIANGLES);
		return va;
	}
	
	public ModelInstance addCylinder(ModelTransform transform, int scene) {
		return new ModelInstance(this, transform, scene);
	}
	
	public static ModelTransform generateCylinderModelTransform(float length, float radius, Vec3 pos, Vec3 facing) {
		Mat4 transform = Mat4.scale(new Vec3(radius, radius, length / 2.0));
		transform.muli(Mat4.rotateAToB(new Vec3(0, 0, 1), facing));
		transform.muli(Mat4.translate(pos));
		return new ModelTransform(transform);
	}
	
	public static ModelInstance addDefaultCylinder(float length, float radius, Vec3 pos, Vec3 facing, int scene) {
		ModelTransform transform = generateCylinderModelTransform(length, radius, pos, facing);
		return DEFAULT_CYLINDER.addCylinder(transform, scene);
	}
	
	public static ModelInstance addDefaultCylinder(float length, float radius, Vec3 pos, Quaternion orient, int scene) {
		Vec3 facing = MathUtils.quaternionRotateVec3(orient, new Vec3(0, 0, 1));
		ModelTransform transform = generateCylinderModelTransform(length, radius, pos, facing);
		return DEFAULT_CYLINDER.addCylinder(transform, scene);
	}

}
