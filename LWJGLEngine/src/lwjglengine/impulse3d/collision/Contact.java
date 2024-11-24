package lwjglengine.impulse3d.collision;

import myutils.math.Vec3;

public class Contact {
	//pos should be in world space?
	//penetration is distance from pos to the contact surface

	public Vec3 pos, norm;
	public float penetration;

	public Contact(Vec3 _pos, Vec3 _norm, float _penetration) {
		this.pos = new Vec3(_pos);
		this.norm = new Vec3(_norm);
		this.penetration = _penetration;
	}
}
