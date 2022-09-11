package entity;

import java.util.ArrayList;

import model.AssetManager;
import model.Model;
import util.Mat4;
import util.MathUtils;
import util.Vec3;

public class Capsule extends Entity {

	private static float epsilon = 0.0001f;

	private int scene;
	private Vec3 pos, vel; // pos refers to the very bottom of the capsule
	private float radius, height;

	private Mat4 bottomMat, topMat;
	private long bottomID, topID;

	public Capsule(Vec3 pos, Vec3 vel, float radius, float height, int scene) {
		super();

		this.pos = new Vec3(pos);
		this.vel = new Vec3(vel);
		this.radius = radius;
		this.height = height;
		this.scene = scene;

		this.bottomID = addModelInstance(AssetManager.getModel("sphere"), Mat4.identity(), this.scene);
		this.topID = addModelInstance(AssetManager.getModel("sphere"), Mat4.identity(), this.scene);
	}

	@Override
	protected void _kill() {

	}

	private void updateModelMats() {
		Vec3 capsule_bottom = pos.add(new Vec3(0, radius, 0));
		Vec3 capsule_top = pos.add(new Vec3(0, height - radius, 0));

		bottomMat = Mat4.scale(radius).mul(Mat4.translate(capsule_bottom));
		topMat = Mat4.scale(radius).mul(Mat4.translate(capsule_top));

		updateModelInstance(bottomID, bottomMat);
		updateModelInstance(topID, topMat);
	}

	@Override
	public void update() {
		
		this.vel.muli(0.99f);
		this.vel.addi(new Vec3(0, -0.005f, 0));
		this.pos.addi(vel);
		
		Vec3 capsule_bottom = pos.add(new Vec3(0, 0, 0));
		Vec3 capsule_top = pos.add(new Vec3(0, height, 0));
		
		Vec3 capsule_bottomSphere = pos.add(new Vec3(0, radius, 0));
		Vec3 capsule_topSphere = pos.add(new Vec3(0, height - radius, 0));
		
		//resolve intersections by applying a force to each one
		ArrayList<Vec3> intersections = Model.capsuleIntersect(scene, capsule_bottom, capsule_top, radius);
		for(Vec3 v : intersections) {
			Vec3 capsule_c = MathUtils.point_lineSegmentProjectClamped(v, capsule_bottomSphere, capsule_topSphere);	//closest point on capsule midline
			Vec3 toCenter = new Vec3(v, capsule_c);
			
			Vec3 normToCenter = new Vec3(toCenter).normalize();
			toCenter.setLength(radius - toCenter.length());
			Vec3 impulse = this.vel.projectOnto(normToCenter).mul(-1f);
			
			if(this.vel.dot(toCenter) < 0) {
				this.vel.addi(impulse);
				this.pos.addi(toCenter.mul(1f + epsilon));
			}
		}
		
		this.updateModelMats();
	}

}
