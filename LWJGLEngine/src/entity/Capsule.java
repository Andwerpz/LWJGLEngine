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

	private Mat4 bottomSphereMat4, topSphereMat4, cylinderMat4;
	private long bottomSphereID, topSphereID, cylinderID;

	private boolean noUpdate; // if you just want a renderable capsule

	public Capsule(Vec3 pos, Vec3 vel, float radius, float height, int scene) {
		super();

		this.pos = new Vec3(pos);
		this.vel = new Vec3(vel);
		this.radius = radius;
		this.height = height;
		this.scene = scene;

		this.cylinderID = Model.addInstance(AssetManager.getModel("cylinder"), Mat4.identity(), this.scene);
		this.bottomSphereID = Model.addInstance(AssetManager.getModel("sphere"), Mat4.identity(), this.scene);
		this.topSphereID = Model.addInstance(AssetManager.getModel("sphere"), Mat4.identity(), this.scene);
		this.updateModelMats();
	}

	@Override
	protected void _kill() {
		Model.removeInstance(bottomSphereID);
		Model.removeInstance(topSphereID);
		Model.removeInstance(cylinderID);
	}

	public void setNoUpdate(boolean b) {
		this.noUpdate = b;
	}

	public void setPos(Vec3 pos) {
		this.pos = new Vec3(pos);
	}

	public Vec3 getBottom() {
		return new Vec3(this.pos);
	}

	public Vec3 getTop() {
		return this.pos.add(new Vec3(0, this.height, 0));
	}

	public float getRadius() {
		return this.radius;
	}

	public void updateModelMats() {
		Vec3 capsule_bottomSphere = pos.add(new Vec3(0, radius, 0));
		Vec3 capsule_topSphere = pos.add(new Vec3(0, height - radius, 0));
		Vec3 capsule_center = pos.add(new Vec3(0, height / 2f, 0));
		float cylinderHeight = height - radius * 2;

		this.cylinderMat4 = Mat4.scale(radius, cylinderHeight / 2f, radius).mul(Mat4.translate(capsule_center));
		this.bottomSphereMat4 = Mat4.scale(radius + 0.0036f).mul(Mat4.translate(capsule_bottomSphere));
		this.topSphereMat4 = Mat4.scale(radius + 0.0036f).mul(Mat4.translate(capsule_topSphere));

		Model.updateInstance(this.cylinderID, cylinderMat4);
		Model.updateInstance(this.bottomSphereID, bottomSphereMat4);
		Model.updateInstance(this.topSphereID, topSphereMat4);
	}

	@Override
	public void update() {
		if (this.noUpdate) {
			return;
		}

		this.vel.muli(0.99f);
		this.vel.addi(new Vec3(0, -0.005f, 0));
		this.pos.addi(vel);

		Vec3 capsule_bottom = pos.add(new Vec3(0, 0, 0));
		Vec3 capsule_top = pos.add(new Vec3(0, height, 0));

		Vec3 capsule_bottomSphere = pos.add(new Vec3(0, radius, 0));
		Vec3 capsule_topSphere = pos.add(new Vec3(0, height - radius, 0));

		// resolve intersections by applying a force to each one
		ArrayList<Vec3[]> intersections = Model.capsuleIntersect(scene, capsule_bottom, capsule_top, radius);
		for (Vec3[] a : intersections) {
			Vec3 v = a[0];
			Vec3 capsule_c = MathUtils.point_lineSegmentProjectClamped(v, capsule_bottomSphere, capsule_topSphere); // closest point on capsule midline
			Vec3 toCenter = new Vec3(v, capsule_c);

			Vec3 normToCenter = new Vec3(toCenter).normalize();
			toCenter.setLength(radius - toCenter.length());
			Vec3 impulse = this.vel.projectOnto(normToCenter).mul(-1f);

			if (this.vel.dot(toCenter) < 0) {
				this.vel.addi(impulse);
				this.pos.addi(toCenter.mul(1f + epsilon));
			}
		}

		this.updateModelMats();
	}

}
