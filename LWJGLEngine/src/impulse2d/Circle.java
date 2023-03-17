package impulse2d;

public class Circle extends Shape {

	public Circle(float r) {
		radius = r;
	}

	@Override
	public Shape clone() {
		return new Circle(radius);
	}

	@Override
	public void computeMass(float density) {
		this.area = ImpulseMath.PI * radius * radius;
		body.mass = this.area * density;
		body.invMass = (body.mass != 0.0f) ? 1.0f / body.mass : 0.0f;
		body.inertia = body.mass * radius * radius;
		body.invInertia = (body.inertia != 0.0f) ? 1.0f / body.inertia : 0.0f;
	}

	@Override
	public void setOrient(float radians) {
	}

	@Override
	public Type getType() {
		return Type.Circle;
	}
}