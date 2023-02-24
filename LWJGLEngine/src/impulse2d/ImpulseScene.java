package impulse2d;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;

import util.Vec2;

public class ImpulseScene {
	public float dt = 1f / 60f;
	public int iterations = 4;
	public ArrayList<Body> bodies;
	public ArrayList<Manifold> contacts;

	public boolean fillBodies = true;
	public boolean doGravity = true;
	public boolean doCollision = true;

	public ImpulseScene() {
		bodies = new ArrayList<>();
		contacts = new ArrayList<>();
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

	public Body add(Shape shape, int x, int y) {
		Body b = new Body(shape, x, y);
		bodies.add(b);
		return b;
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

	//	public void draw(Graphics g) {
	//		Graphics2D g2d = (Graphics2D) g;
	//
	//		// transform to real space
	//		AffineTransform realSpaceTransform = new AffineTransform();
	//		realSpaceTransform.scale(1.0, -1.0);
	//		realSpaceTransform.translate(0, -MainPanel.HEIGHT);
	//
	//		g2d.transform(realSpaceTransform);
	//
	//		for (Body b : bodies) {
	//			Vec2 pos = b.position;
	//			if(b.shape instanceof Circle) {
	//				if(this.fillBodies) {
	//					g.fillOval((int) (pos.x - b.shape.radius), (int) (pos.y - b.shape.radius),
	//							(int) (b.shape.radius * 2), (int) (b.shape.radius * 2));
	//				}
	//				else {
	//					g.drawOval((int) (pos.x - b.shape.radius), (int) (pos.y - b.shape.radius),
	//							(int) (b.shape.radius * 2), (int) (b.shape.radius * 2));
	//
	//					Vec2 facing = new Vec2(0, b.shape.radius);
	//					facing.rotate(b.orient);
	//
	//					Vec2 a = new Vec2(b.position.x, b.position.y);
	//					Vec2 aFacing = a.add(facing);
	//
	//					g.drawLine((int) a.x, (int) a.y, (int) aFacing.x, (int) aFacing.y);
	//				}
	//			}
	//			else if(b.shape instanceof Polygon) {
	//				Polygon p = (Polygon) b.shape;
	//
	//				int[] cx = new int[p.vertexCount];
	//				int[] cy = new int[p.vertexCount];
	//				for (int i = 0; i < p.vertexCount; i++) {
	//					Vec2 v = new Vec2(p.vertices[i]);
	//					b.shape.u.muli(v);
	//					v.addi(b.position);
	//					cx[i] = (int) v.x;
	//					cy[i] = (int) v.y;
	//				}
	//
	//				if(this.fillBodies) {
	//					g.fillPolygon(cx, cy, p.vertexCount);
	//				}
	//				else {
	//					g.drawOval((int) (b.position.x - 3), (int) (b.position.y - 3), 6, 6);
	//					g.drawPolygon(cx, cy, p.vertexCount);
	//				}
	//			}
	//		}
	//
	//		// transform back to screen space
	//		try {
	//			g2d.transform(realSpaceTransform.createInverse());
	//		} catch (NoninvertibleTransformException e) {
	//			// TODO Auto-generated catch block
	//			e.printStackTrace();
	//		}
	//	}
}
