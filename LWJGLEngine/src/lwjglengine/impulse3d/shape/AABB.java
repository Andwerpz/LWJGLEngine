package lwjglengine.impulse3d.shape;

import myutils.math.Mat3;
import myutils.math.MathUtils;
import myutils.math.Vec3;

public class AABB extends KDOP {

	public Vec3 half_dim;

	public AABB(Vec3 dimensions) {
		this.type = Type.KDOP;
		this.axes = new Vec3[] { new Vec3(1, 0, 0), new Vec3(0, 1, 0), new Vec3(0, 0, 1) };
		this.half_dim = dimensions.mul(0.5f);

		Vec3[] init_pts = new Vec3[8];
		init_pts[0] = new Vec3(half_dim.x, half_dim.y, half_dim.z);
		init_pts[1] = new Vec3(-half_dim.x, half_dim.y, half_dim.z);
		init_pts[2] = new Vec3(-half_dim.x, -half_dim.y, half_dim.z);
		init_pts[3] = new Vec3(half_dim.x, -half_dim.y, half_dim.z);
		init_pts[4] = new Vec3(half_dim.x, half_dim.y, -half_dim.z);
		init_pts[5] = new Vec3(-half_dim.x, half_dim.y, -half_dim.z);
		init_pts[6] = new Vec3(-half_dim.x, -half_dim.y, -half_dim.z);
		init_pts[7] = new Vec3(half_dim.x, -half_dim.y, -half_dim.z);
		this.computeBVE(init_pts);
		this.computeFaces();

		float w = half_dim.x * 2;
		float h = half_dim.y * 2;
		float d = half_dim.z * 2;
		this.mass = w * h * d;
		this.moment = Mat3.identity();
		this.moment.mat[0][0] = (this.mass * (h * h + d * d)) / 12.0f;
		this.moment.mat[1][1] = (this.mass * (w * w + d * d)) / 12.0f;
		this.moment.mat[2][2] = (this.mass * (w * w + h * h)) / 12.0f;

	}
}
