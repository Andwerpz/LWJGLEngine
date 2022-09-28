package model;

import java.util.ArrayList;

import graphics.VertexArray;
import util.Mat4;
import util.MathUtils;
import util.Vec3;

public class CollisionMesh {

	// each model has a collision mesh.

	// to get the collision mesh of a model instance, should access it statically
	// through Model

	private Vec3[] vertices;
	private int[] indices;

	public CollisionMesh(float[] vertices, int[] indices) {
		this.vertices = new Vec3[vertices.length / 3];
		for (int i = 0; i < vertices.length; i += 3) {
			this.vertices[i / 3] = new Vec3(vertices[i + 0], vertices[i + 1], vertices[i + 2]);
		}
		this.indices = indices;
	}

	public CollisionMesh(VertexArray vao) {
		float[] vertices = vao.getVertices();
		int[] indices = vao.getIndices();

		this.vertices = new Vec3[vertices.length / 3];
		this.indices = new int[indices.length];
		for (int i = 0; i < vertices.length; i += 3) {
			this.vertices[i / 3] = new Vec3(vertices[i + 0], vertices[i + 1], vertices[i + 2]);
		}
		for (int i = 0; i < indices.length; i++) {
			this.indices[i] = indices[i];
		}
	}

	private Vec3[] transformVertices(Mat4 transform) {
		Vec3[] vTransformed = new Vec3[vertices.length];
		for (int i = 0; i < vertices.length; i++) {
			vTransformed[i] = transform.mul(vertices[i], 1f);
		}
		return vTransformed;
	}

	public ArrayList<Vec3[]> rayIntersect(Vec3 ray_origin, Vec3 ray_dir, Mat4 transform) {
		ArrayList<Vec3[]> result = new ArrayList<>();

		Vec3[] vTransformed = this.transformVertices(transform);

		for (int t = 0; t < this.indices.length; t += 3) {
			Vec3 t0 = new Vec3(vTransformed[indices[t + 0]]);
			Vec3 t1 = new Vec3(vTransformed[indices[t + 1]]);
			Vec3 t2 = new Vec3(vTransformed[indices[t + 2]]);

			Vec3 intersect = MathUtils.ray_triangleIntersect(ray_origin, ray_dir, t0, t1, t2);
			if (intersect != null) {
				Vec3 normal = MathUtils.computeTriangleNormal(t0, t1, t2);
				result.add(new Vec3[] { intersect, normal, t0, t1, t2 });
			}
		}
		return result;
	}

	public ArrayList<Vec3[]> sphereIntersect(Vec3 sphere_origin, float sphere_radius, Mat4 transform) {
		ArrayList<Vec3[]> result = new ArrayList<>();

		Vec3[] vTransformed = this.transformVertices(transform);

		for (int t = 0; t < this.indices.length; t += 3) {
			Vec3 t0 = new Vec3(vTransformed[indices[t + 0]]);
			Vec3 t1 = new Vec3(vTransformed[indices[t + 1]]);
			Vec3 t2 = new Vec3(vTransformed[indices[t + 2]]);

			Vec3 intersect = MathUtils.sphere_triangleIntersect(sphere_origin, sphere_radius, t0, t1, t2);
			if (intersect != null) {
				Vec3 normal = MathUtils.computeTriangleNormal(t0, t1, t2);
				result.add(new Vec3[] { intersect, normal, t0, t1, t2 });
			}
		}
		return result;
	}

	public ArrayList<Vec3[]> capsuleIntersect(Vec3 capsule_bottom, Vec3 capsule_top, float capsule_radius, Mat4 transform) {
		ArrayList<Vec3[]> result = new ArrayList<>();

		Vec3[] vTransformed = this.transformVertices(transform);

		for (int t = 0; t < this.indices.length; t += 3) {
			Vec3 t0 = new Vec3(vTransformed[indices[t + 0]]);
			Vec3 t1 = new Vec3(vTransformed[indices[t + 1]]);
			Vec3 t2 = new Vec3(vTransformed[indices[t + 2]]);

			Vec3 intersect = MathUtils.capsule_triangleIntersect(capsule_bottom, capsule_top, capsule_radius, t0, t1, t2);
			if (intersect != null) {
				Vec3 normal = MathUtils.computeTriangleNormal(t0, t1, t2);
				result.add(new Vec3[] { intersect, normal, t0, t1, t2 });
			}
		}
		return result;
	}

	public ArrayList<Vec3[]> lineSegmentIntersect(Vec3 p1, Vec3 p2, Mat4 transform) {
		Vec3 dir = new Vec3(p1, p2);
		float pDist = dir.length();
		dir.normalize(); // could divide by pDist to save another sqrt operation
		ArrayList<Vec3[]> rayPoints = rayIntersect(p1, dir, transform);
		ArrayList<Vec3[]> ans = new ArrayList<>();
		for (Vec3[] a : rayPoints) {
			Vec3 v = a[0];
			float vDist = new Vec3(p1, v).length();
			if (vDist < pDist) {
				ans.add(a);
			}
		}
		return ans;
	}

}
