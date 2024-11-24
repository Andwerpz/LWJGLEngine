package lwjglengine.impulse3d;

import lwjglengine.impulse3d.bvh.KDOP;
import lwjglengine.impulse3d.shape.Shape;
import myutils.math.Mat3;
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

	//returns a bounding box in world space
	public KDOP calcBoundingBox() {
		KDOP box = this.shape.calcBoundingBox(this.orient, this.pos);
		return box;
	}
}
