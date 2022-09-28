package entity;

import java.util.ArrayList;

import model.AssetManager;
import model.Model;
import util.Mat4;
import util.Vec3;

public class Ball extends Entity {

	private static float epsilon = 0.0001f;

	private int scene;
	private Vec3 pos, vel;
	private Mat4 transformMat;
	private float radius;

	private float elasticity = 1.5f; // 2 for complete bounce, 1 for no bounce

	private long modelInstanceID;

	public Ball(Vec3 pos, Vec3 vel, float radius, int scene) {
		super();

		this.pos = new Vec3(pos);
		this.vel = new Vec3(vel);
		this.radius = radius;
		this.transformMat = Mat4.scale(this.radius).mul(Mat4.translate(this.pos));
		this.scene = scene;

		this.modelInstanceID = addModelInstance(AssetManager.getModel("sphere"), this.transformMat, this.scene);
	}

	@Override
	protected void _kill() {

	}

	@Override
	public void update() {

		this.vel.muli(0.99f);
		this.vel.addi(new Vec3(0, -0.005f, 0));
		this.pos.addi(vel);

		// resolve intersections by applying a force
		ArrayList<Vec3[]> intersections = Model.sphereIntersect(this.scene, this.pos, this.radius);
		Vec3 closestPoint = null;
		float minDist = -1f;
		for (Vec3[] a : intersections) {
			Vec3 v = a[0];
			float dist = new Vec3(v, this.pos).length();
			if (closestPoint == null || dist < minDist) {
				minDist = dist;
				closestPoint = v;
			}
		}

		if (closestPoint != null) {
			Vec3 toCenter = new Vec3(closestPoint, pos);
			Vec3 normToCenter = new Vec3(toCenter).normalize();
			toCenter.setLength(radius - toCenter.length());
			Vec3 impulse = this.vel.projectOnto(normToCenter);

			if (this.vel.dot(impulse) > 0) {
				this.vel.subi(impulse.mul(elasticity));
				this.pos.addi(toCenter.muli(1f + epsilon));
			}
		}

		this.transformMat = Mat4.scale(radius).mul(Mat4.translate(pos));
		this.updateModelInstance(modelInstanceID, transformMat);
	}

}
