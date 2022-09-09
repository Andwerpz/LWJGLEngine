package model;

import java.util.ArrayList;

import util.MathTools;
import util.Vec3;

public class CollisionMesh {

	//each model has a collision mesh. 
	
	//to get the collision mesh of a model instance, should access it statically through Model
	
	private Vec3[] vertices;
	private int[] indices;
	
	public CollisionMesh(float[] vertices, int[] indices) {
		this.vertices = new Vec3[vertices.length / 3];
		for(int i = 0; i < vertices.length; i += 3) {
			this.vertices[i / 3] = new Vec3(vertices[i + 0], vertices[i + 1], vertices[i + 2]);
		}
		this.indices = indices;
	}
	
	//ray collision
	public ArrayList<Vec3> ray_collision(Vec3 ray_origin, Vec3 ray_dir){
		//process each triangle individually
		for(int t = 0; t < this.indices.length; t += 3) {
			Vec3 t0 = new Vec3(vertices[indices[t + 0]]);
			Vec3 t1 = new Vec3(vertices[indices[t + 1]]);
			Vec3 t2 = new Vec3(vertices[indices[t + 2]]);
			
			Vec3 d0 = new Vec3(t0, t1).normalize();
			Vec3 d1 = new Vec3(t1, t2).normalize();
			Vec3 d2 = new Vec3(t2, t0).normalize();
			
			Vec3 plane_origin = new Vec3(t0);
			Vec3 plane_normal = MathTools.crossProduct(d0, d1);
			
			float ray_dirStepRatio = (float) MathTools.dotProduct(plane_normal, ray_dir); 
			if(ray_dirStepRatio == 0) {
				//ray_dir is parallel to the plane, intersection impossible
				continue;
			}
			
		}
	}
	
	//line segment collision
	public ArrayList<Vec3> lineSegment_collision(Vec3 p1, Vec3 p2) {
		Vec3 dir = new Vec3(p1, p2);
		float pDist = dir.length();
		dir.normalize();	//could divide by pDist to save another sqrt operation
		ArrayList<Vec3> rayPoints = ray_collision(p1, dir);
		ArrayList<Vec3> ans = new ArrayList<>();
		for(Vec3 v : rayPoints) {
			float vDist = new Vec3(p1, v).length();
			if(vDist < pDist) {
				ans.add(v);
			}
		}
		return ans;
	}
	
}
