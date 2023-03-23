package lwjglengine.v10.impulse2d;

import myutils.v10.math.Mat2;

public abstract class Shape {
	public enum Type {
		Circle, Poly, Count
	}

	public Body body;
	public float radius;
	public final Mat2 u = new Mat2();

	public float area;

	public Shape() {

	}

	public void initialize() {
		this.computeMass(body.density);
		this.setOrient(0);
	}

	@Override
	public abstract Shape clone();

	public abstract void computeMass(float density);

	public abstract void setOrient(float radians);

	public abstract Type getType();
}
