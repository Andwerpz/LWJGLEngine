package impulse2d;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;

import util.Vec2;

public class ImpulseScene {
	private float dt = 1f / 60f;
	private int iterations = 4;
	private ArrayList<Body> bodies;
	private ArrayList<Manifold> contacts;

	//adds a force in the -y direction every iteration
	private boolean doGravity = true;

	//allows things to collide with each other; pretty much the whole point of this object
	private boolean doCollision = true;

	//simulates the scene as if it were placed on top of some surface, like a table. 
	//this means that all objects passively have friction applied to their velocities. 
	// TODO for now, static friction is not accounted for, only dynamic friction. 
	private boolean simulateOnSurface = false;
	private float surfaceFrictionCoefficient = 1;

	public ImpulseScene() {
		bodies = new ArrayList<>();
		contacts = new ArrayList<>();
	}

	public void setDoGravity(boolean b) {
		this.doGravity = b;
	}

	public void setDoCollision(boolean b) {
		this.doCollision = b;
	}

	public void setSimulateOnSurface(boolean b) {
		this.simulateOnSurface = b;
	}

	public void setSurfaceFrictionCoefficient(float f) {
		this.surfaceFrictionCoefficient = f;
	}

	public void setIterations(int iterations) {
		this.iterations = iterations;
	}

	public void tick() {
		if (doCollision) {
			// Generate new collision info
			contacts.clear();
			for (int i = 0; i < bodies.size(); ++i) {
				Body A = bodies.get(i);

				for (int j = i + 1; j < bodies.size(); ++j) {
					Body B = bodies.get(j);

					if (A.invMass == 0 && B.invMass == 0) {
						continue;
					}

					Manifold m = new Manifold(A, B);
					m.solve();

					if (m.contactCount > 0) {
						contacts.add(m);
					}
				}
			}
		}

		// Integrate forces
		for (int i = 0; i < bodies.size(); ++i) {
			integrateForces(bodies.get(i), dt);
		}

		// Initialize collision
		for (int i = 0; i < contacts.size(); ++i) {
			contacts.get(i).initialize();
		}
		// Solve collisions
		for (int j = 0; j < iterations; ++j) {
			for (int i = 0; i < contacts.size(); ++i) {
				contacts.get(i).applyImpulse();
			}
		}

		// Integrate velocities
		for (int i = 0; i < bodies.size(); ++i) {
			integrateVelocity(bodies.get(i), dt);
		}

		// Correct positions
		for (int i = 0; i < contacts.size(); ++i) {
			contacts.get(i).positionalCorrection();
		}

		// Clear all forces
		for (int i = 0; i < bodies.size(); ++i) {
			Body b = bodies.get(i);
			b.force.set(0, 0);
			b.torque = 0;
		}
	}

	public Body addBody(Shape s, float x, float y) {
		Body b = new Body(s, x, y);
		this.bodies.add(b);
		return b;
	}

	public void addBody(Body b) {
		this.bodies.add(b);
	}

	public void removeBody(Body b) {
		this.bodies.remove(b);
	}

	public void integrateForces(Body b, float dt) {
		//		if(b->im == 0.0f)
		//			return;
		//		b->velocity += (b->force * b->im + gravity) * (dt / 2.0f);
		//		b->angularVelocity += b->torque * b->iI * (dt / 2.0f);

		if (b.invMass == 0.0f) {
			return;
		}

		float dts = dt * 0.5f;

		b.velocity.addsi(b.force, b.invMass * dts);
		if (doGravity) {
			b.velocity.addsi(ImpulseMath.GRAVITY, dts);
		}
		b.angularVelocity += b.torque * b.invInertia * dts;

		if (this.simulateOnSurface) {
			//apply friction to velocity and angular velocity.
			if (b.velocity.lengthSq() > ImpulseMath.EPSILON) {
				Vec2 friction = new Vec2(b.velocity);
				friction.setLength(-this.surfaceFrictionCoefficient);
				b.velocity.addsi(friction, dts);

				if (b.velocity.dot(friction) > 0) {
					//velocity has reversed direction, just set velocity to 0
					b.velocity.set(0, 0);
				}
			}

			if (Math.abs(b.angularVelocity) > ImpulseMath.EPSILON) {
				float friction = b.angularVelocity / Math.abs(b.angularVelocity);
				//i have no idea how to do angular friction, i'll just bs it here, but TODO, fix. 
				b.angularVelocity -= friction * this.surfaceFrictionCoefficient * dts;
				if (b.angularVelocity * friction < 0) {
					b.angularVelocity = 0;
				}
			}
		}

	}

	public void integrateVelocity(Body b, float dt) {
		//		if(b->im == 0.0f)
		//			return;
		//		b->position += b->velocity * dt;
		//		b->orient += b->angularVelocity * dt;
		//		b->SetOrient( b->orient );
		//		IntegrateForces( b, dt );

		if (b.invMass == 0.0f) {
			return;
		}

		b.position.addsi(b.velocity, dt);
		b.orient += b.angularVelocity * dt;
		b.setOrient(b.orient);

		integrateForces(b, dt);
	}
}
