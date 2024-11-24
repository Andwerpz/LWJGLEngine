package lwjglengine.impulse3d.shape;

import myutils.math.Mat3;
import myutils.math.Quaternion;
import myutils.math.Vec3;

public abstract class Shape {
	//should always try to store the shape with center of mass at origin. 

	public enum Type {
		KDOP, CAPSULE
	}

	public Type type;
	public float mass; //for shape, mass = volume
	public Mat3 moment;

	public abstract lwjglengine.impulse3d.bvh.KDOP calcBoundingBox(Quaternion orient, Vec3 pos);
}
