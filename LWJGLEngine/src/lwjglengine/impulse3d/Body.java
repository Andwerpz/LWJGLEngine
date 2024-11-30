package lwjglengine.impulse3d;

import lwjglengine.impulse3d.bvh.KDOP;
import lwjglengine.impulse3d.shape.Shape;
import myutils.math.Mat3;
import myutils.math.MathUtils;
import myutils.math.Quaternion;
import myutils.math.Vec3;

public class Body {
	//for now, assume uniform density of 1, so mass = volume

	public Vec3 pos, vel;
	public float mass, inv_mass;
	public Quaternion orient;
	public Vec3 angvel;
	public Mat3 moment, inv_moment;

	public float static_friction, dynamic_friction;
	public float restitution;

	public boolean is_static = false;

	public Shape shape;

	public Body(Shape _shape, Vec3 _pos) {
		this.shape = _shape;
		this.pos = new Vec3(_pos);
		this.vel = new Vec3(0);
		this.orient = Quaternion.identity();
		this.angvel = new Vec3(0);

		this.mass = shape.mass;
		this.moment = shape.moment;

		this.inv_mass = 1.0f / shape.mass;
		this.inv_moment = shape.moment.inverse();

		this.static_friction = 0.5f;
		this.dynamic_friction = 0.4f;
		this.restitution = 0.1f;
	}

	public void setStatic() {
		this.is_static = true;
		this.inv_mass = 0;
	}
	
	/**
	 * Applies an impulse to body given world space impulse point and direction. 
	 * @param world_pt
	 * @param impulse
	 */
	public void applyImpulse(Vec3 world_pt, Vec3 impulse) {
		float impulse_mag = impulse.length();
		impulse.normalize();
		Vec3 rvec = new Vec3(world_pt, this.pos);
		Vec3 rotaxis = MathUtils.cross(impulse, rvec);
		float inv_moment = 0;
		if(rotaxis.lengthSq() != 0) {
			rotaxis.normalize();
			inv_moment = 1.0f / MathUtils.dot(rotaxis, this.calcITensorWorld().mul(rotaxis));
		}
		
		this.vel.addi(impulse.mul(impulse_mag * this.inv_mass));
		this.angvel.addi(MathUtils.cross(impulse, rvec).mul(impulse_mag * inv_moment));
	}
	
	public Vec3 calcBodyPtVel(Vec3 world_pt) {
		Vec3 ret = new Vec3(this.vel); //linear velocity
		ret.addi(MathUtils.cross(this.angvel, new Vec3(this.pos, world_pt))); //angular velocity
		return ret;
	}
	
	/**
	 * returns the inertia tensor in world space for the current orientation
	 * @return
	 */
	public Mat3 calcITensorWorld() {
		Mat3 rotmat = MathUtils.quaternionToRotationMat3(this.orient);
		Mat3 rotmat_t = (new Mat3(rotmat)).transpose();
		Mat3 itensor_world = rotmat.mul(this.moment).mul(rotmat_t);
		return itensor_world;
	}

	/**
	 * returns a bvh bounding box in world space
	 * @return
	 */
	public KDOP calcBoundingBox() {
		KDOP box = this.shape.calcBoundingBox(this.orient, this.pos);
		return box;
	}
}
