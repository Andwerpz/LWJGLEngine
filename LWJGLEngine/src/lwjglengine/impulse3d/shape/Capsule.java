package lwjglengine.impulse3d.shape;

import lwjglengine.impulse3d.bvh.KDOP;
import myutils.math.Mat3;
import myutils.math.MathUtils;
import myutils.math.Quaternion;
import myutils.math.Vec3;
import myutils.misc.Pair;

public class Capsule extends Shape {
	//capsule defined by radius and half_length
	//by default, capsule is laying along the z axis

	public float radius, length;

	public Capsule(float _radius, float _length) {
		this.type = Type.CAPSULE;
		this.radius = _radius;
		this.length = _length;

		float cylinder_vol = (float) Math.PI * radius * radius * length;
		float sphere_vol = (float) Math.PI * radius * radius * radius * (4.0f / 3.0f);
		this.mass = (cylinder_vol + sphere_vol);

		Mat3 cylinder_moment = new Mat3();
		{
			float[][] mat = new float[3][3];
			mat[0][0] = (this.mass * (3.0f * this.radius * this.radius + this.length * this.length)) / 12.0f;
			mat[1][1] = (this.mass * (3.0f * this.radius * this.radius + this.length * this.length)) / 12.0f;
			mat[2][2] = (this.mass * this.radius * this.radius) / 2.0f;
			cylinder_moment.set(mat);
		}

		float ecap_mass = (sphere_vol) / 2.0f;
		Mat3 ecap_moment = new Mat3();
		{
			float[][] mat = new float[3][3];
			mat[0][0] = (this.mass * this.radius * this.radius) / 5.0f;
			mat[1][1] = (this.mass * this.radius * this.radius) / 5.0f;
			mat[2][2] = (this.mass * this.radius * this.radius) / 5.0f;

			mat[0][0] += ecap_mass * Math.pow(_length / 2.0f, 2);
			mat[1][1] += ecap_mass * Math.pow(_length / 2.0f, 2);
			ecap_moment.set(mat);
			ecap_moment.muli(2.0f); //two endcaps
		}

		this.moment = new Mat3();
		this.moment.addi(cylinder_moment);
		this.moment.addi(ecap_moment);
	}

	public Pair<Vec3, Vec3> getEnds(Quaternion orient, Vec3 pos) {
		Vec3 u1 = new Vec3(0, 0, this.length / 2);
		Vec3 u2 = new Vec3(0, 0, -this.length / 2);
		u1 = MathUtils.quaternionRotateVec3(orient, u1);
		u2 = MathUtils.quaternionRotateVec3(orient, u2);
		u1.addi(pos);
		u2.addi(pos);
		return new Pair<>(u1, u2);
	}

	@Override
	public KDOP calcBoundingBox(Quaternion orient, Vec3 pos) {
		Pair<Vec3, Vec3> ends = this.getEnds(orient, pos);
		Vec3 e1 = ends.first;
		Vec3 e2 = ends.second;
		float[] bmin = new float[KDOP.axes.length];
		float[] bmax = new float[KDOP.axes.length];
		for (int i = 0; i < KDOP.axes.length; i++) {
			bmin[i] = MathUtils.dot(e1, KDOP.axes[i]) - this.radius;
			bmax[i] = MathUtils.dot(e1, KDOP.axes[i]) + this.radius;
		}
		for (int i = 0; i < KDOP.axes.length; i++) {
			bmin[i] = Math.min(bmin[i], MathUtils.dot(e2, KDOP.axes[i]) - this.radius);
			bmax[i] = Math.max(bmax[i], MathUtils.dot(e2, KDOP.axes[i]) + this.radius);
		}
		return new KDOP(bmin, bmax);
	}

}
