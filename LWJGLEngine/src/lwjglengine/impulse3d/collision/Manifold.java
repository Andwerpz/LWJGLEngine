package lwjglengine.impulse3d.collision;

import java.util.ArrayList;

import lwjglengine.impulse3d.Body;
import lwjglengine.impulse2d.ImpulseMath;
import myutils.math.Mat3;
import myutils.math.MathUtils;
import myutils.math.Vec2;
import myutils.math.Vec3;

public class Manifold {
	public Body a, b;
	public ArrayList<Contact> contacts = null;

	public Vec3 separating_axis = null; //should be pointing towards A
	public float penetration;

	public boolean didCollide = false;

	public Manifold(Body _a, Body _b) {
		this.a = _a;
		this.b = _b;
	}

	public void apply() {
		if (a.is_static && b.is_static) {
			//early out if both are supposed to be unmoving
			return;
		}

		int ta = a.shape.type.ordinal();
		int tb = b.shape.type.ordinal();
		if (ta < tb) {
			Body tmp = a;
			a = b;
			b = tmp;

			int tmpi = ta;
			ta = tb;
			tb = tmpi;
		}

		Collisions.dispatch[ta][tb].handleCollision(this);

		//no collision
		if (!this.didCollide) {
			return;
		}
		this.separating_axis.normalize();
		//		System.out.println("Found " + a.shape.type.name() + " " + b.shape.type.name() + " collision");
		//		System.out.println("PENETRATION : " + this.penetration);

		if (this.contacts.size() != 0) {
			this.doCollisionResolution();
		}

		//static correction
		if (a.is_static) {
			a.vel.muli(0);
			a.angvel.muli(0);
		}
		if (b.is_static) {
			b.vel.muli(0);
			b.angvel.muli(0);
		}

		//position correction
		float slop = 0.01f; //allow penetrating objects to keep penetrating slightly 
		float correction = Math.max(this.penetration - slop, 0);
		correction *= 0.9f;
		Vec3 poscorr_a = this.separating_axis.mul(correction * a.inv_mass / (a.inv_mass + b.inv_mass));
		Vec3 poscorr_b = this.separating_axis.mul(-correction * b.inv_mass / (a.inv_mass + b.inv_mass));

		a.pos.addi(poscorr_a);
		b.pos.addi(poscorr_b);

	}

	private void doCollisionResolution() {
		//		System.out.println("----- START COLLISION RESOLUTION -----");
		//		System.out.println("CONTACTS : " + this.contacts.size());
		//		System.out.println("ANGVEL A B : " + this.a.angvel + " " + this.b.angvel);

		float coll_restitution = Math.min(a.restitution, b.restitution);
		float static_friction = (float) Math.sqrt(a.static_friction * a.static_friction + b.static_friction * b.static_friction);
		float dynamic_friction = (float) Math.sqrt(a.dynamic_friction * a.dynamic_friction + b.dynamic_friction * b.dynamic_friction);

		//determine if we should do resting collision
		for (Contact c : this.contacts) {
			Vec3 cpt = c.pos;
			Vec3 rvel = calcBodyPtVel(cpt, this.b);
			rvel.subi(calcBodyPtVel(cpt, this.a));
			if (rvel.length() < 0.1) {
				coll_restitution = 0;
			}
		}

		//rotation matrices for the bodies
		Mat3 rotmat_a = MathUtils.quaternionToRotationMat3(a.orient);
		Mat3 rotmat_b = MathUtils.quaternionToRotationMat3(b.orient);
		Mat3 rotmat_at = (new Mat3(rotmat_a)).transpose();
		Mat3 rotmat_bt = (new Mat3(rotmat_b)).transpose();

		//inertia tensor transformed to world space
		Mat3 itensorworld_a = rotmat_a.mul(a.moment).mul(rotmat_at);
		Mat3 itensorworld_b = rotmat_b.mul(b.moment).mul(rotmat_bt);

		//look at contacts, and apply them to bodies. 
		Vec3 accel_a = new Vec3(0);
		Vec3 accel_b = new Vec3(0);
		Vec3 angaccel_a = new Vec3(0);
		Vec3 angaccel_b = new Vec3(0);
		for (Contact c : this.contacts) {
			Vec3 cpt = c.pos; //in world space
			Vec3 cnorm = new Vec3(this.separating_axis); //should be facing towards a
			cnorm.normalize(); //just in case

			//compute relative velocity from a's perspective. 
			//this means rvel should be pointing in the same direction as cnorm if there is a collision
			Vec3 rvel = calcBodyPtVel(cpt, this.b);
			rvel.subi(calcBodyPtVel(cpt, this.a));

			float norm_mag = MathUtils.dot(cnorm, rvel);
			if (norm_mag < 0) {
				//no collision, they are travelling away
				return;
			}
			Vec3 norm = cnorm.mul(MathUtils.dot(cnorm, rvel));
			Vec3 tang = rvel.sub(norm);
			float tang_mag = tang.length();

			Vec3 unit_norm = new Vec3(norm).normalize();
			Vec3 unit_tang = new Vec3(tang).normalize();

			//vector from center of shape to collision point
			Vec3 radiusvec_a = new Vec3(a.pos, cpt);
			Vec3 radiusvec_b = new Vec3(b.pos, cpt);

			//axis of rotation that the normal component will apply torque to
			Vec3 rotaxis_a = MathUtils.cross(cnorm, radiusvec_a);
			Vec3 rotaxis_b = MathUtils.cross(radiusvec_b, cnorm);

			//moment of inertia around axes in question
			float inv_moment_a = 0, inv_moment_b = 0;
			if (rotaxis_a.lengthSq() != 0) {
				rotaxis_a.normalize();
				inv_moment_a = 1.0f / calcMomentAroundAxis(itensorworld_a, rotaxis_a);
			}
			if (rotaxis_b.lengthSq() != 0) {
				rotaxis_b.normalize();
				inv_moment_b = 1.0f / calcMomentAroundAxis(itensorworld_b, rotaxis_b);
			}

			//compute normal scalar. 
			float inv_mass_sum = a.inv_mass + b.inv_mass;
			inv_mass_sum += MathUtils.cross(radiusvec_a, cnorm).lengthSq() * inv_moment_a;
			inv_mass_sum += MathUtils.cross(radiusvec_b, cnorm).lengthSq() * inv_moment_b;
			float norm_scalar = norm_mag * (1.0f + coll_restitution) / inv_mass_sum;

			//compute accelerations due to force
			accel_a.addi(unit_norm.mul(norm_scalar * a.inv_mass));
			accel_b.addi(unit_norm.mul(-norm_scalar * b.inv_mass));
			angaccel_a.addi(MathUtils.cross(unit_norm, radiusvec_a).mul(-norm_scalar * inv_moment_a));
			angaccel_b.addi(MathUtils.cross(unit_norm, radiusvec_b).mul(norm_scalar * inv_moment_b));

			//friction
			float tang_scalar = tang_mag / inv_mass_sum; //static friction
			if (tang_scalar > norm_scalar * static_friction) {
				//if the force is too great for static friction, switch to dynamic friction
				tang_scalar = norm_scalar * dynamic_friction;
			}

			Vec3 tang_rotaxis_a = MathUtils.cross(unit_tang, radiusvec_a);
			Vec3 tang_rotaxis_b = MathUtils.cross(unit_tang, radiusvec_b);

			float tang_inv_moment_a = 0, tang_inv_moment_b = 0;
			if (tang_rotaxis_a.lengthSq() != 0) {
				tang_rotaxis_a.normalize();
				tang_inv_moment_a = 1.0f / calcMomentAroundAxis(itensorworld_a, tang_rotaxis_a);
			}
			if (tang_rotaxis_b.lengthSq() != 0) {
				tang_rotaxis_b.normalize();
				tang_inv_moment_b = 1.0f / calcMomentAroundAxis(itensorworld_b, tang_rotaxis_b);
			}

			accel_a.addi(unit_tang.mul(tang_scalar * a.inv_mass));
			accel_b.addi(unit_tang.mul(-tang_scalar * b.inv_mass));
			angaccel_a.addi(MathUtils.cross(unit_tang, radiusvec_a).mul(-tang_scalar * tang_inv_moment_a));
			angaccel_b.addi(MathUtils.cross(unit_tang, radiusvec_b).mul(tang_scalar * tang_inv_moment_b));

			//			System.out.println("ROTAXIS A B : " + rotaxis_a + " " + rotaxis_b);
			//			System.out.println("INV MOMENT A B : " + inv_moment_a + " " + inv_moment_b);
			//			System.out.println("INV MASS SUM : " + inv_mass_sum);
			//			System.out.println("COLL RESTITUTION : " + coll_restitution);
			//			System.out.println("NORM MAG : " + norm_mag);
			//			System.out.println("INV MASS : " + a.inv_mass + " " + b.inv_mass);
			//			System.out.println("RVEL : " + rvel);
			//			System.out.println("NORM SCALAR : " + norm_scalar);
			//			System.out.println("CPT : " + cpt);
			//			System.out.println("CONTACT ANGACCEL B : " + angaccel_b);
			//			System.out.println("CROSS : " + MathUtils.cross(cnorm, radiusvec_b));
			//			System.out.println("TANG MAG : " + tang_mag + " " + tang_scalar);
		}

		//take average force, and apply it to bodies
		accel_a.divi(this.contacts.size());
		accel_b.divi(this.contacts.size());
		angaccel_a.divi(this.contacts.size());
		angaccel_b.divi(this.contacts.size());

		a.vel.addi(accel_a);
		b.vel.addi(accel_b);
		a.angvel.addi(angaccel_a);
		b.angvel.addi(angaccel_b);

		//		System.out.println("SEP AXIS : " + this.separating_axis);
		//		System.out.println("ACCEL B : " + accel_b);
		//		System.out.println("ANG ACCEL B : " + angaccel_b);
		//		System.out.println("ACCEL A : " + accel_a);
		//		System.out.println("ANG ACCEL A : " + angaccel_a);
		//		System.out.println("----- DONE COLLISION RESOLUTION -----");
	}

	private Vec3 calcBodyPtVel(Vec3 pt, Body body) {
		Vec3 ret = new Vec3(body.vel); //linear velocity
		ret.addi(MathUtils.cross(body.angvel, new Vec3(body.pos, pt))); //angular velocity
		return ret;
	}

	private float calcMomentAroundAxis(Mat3 itensorworld, Vec3 axis) {
		axis.normalize();
		return MathUtils.dot(axis, itensorworld.mul(axis));
	}
}
