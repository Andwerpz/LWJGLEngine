package lwjglengine.impulse3d;

import static org.lwjgl.opengl.GL11.GL_LINES;

import java.util.ArrayList;

import lwjglengine.impulse3d.Body;
import lwjglengine.impulse3d.shape.AABB;
import lwjglengine.impulse3d.shape.Capsule;
import lwjglengine.impulse3d.shape.Shape;
import lwjglengine.impulse3d.shape.KDOP;
import lwjglengine.impulse3d.bvh.BVH;
import lwjglengine.impulse3d.collision.Manifold;
import lwjglengine.model.Cube;
import lwjglengine.model.CubeSphere;
import lwjglengine.model.Cylinder;
import lwjglengine.model.Model;
import lwjglengine.model.ModelInstance;
import lwjglengine.model.ModelTransform;
import lwjglengine.model.VertexArray;
import lwjglengine.scene.Scene;
import myutils.math.Mat4;
import myutils.math.MathUtils;
import myutils.math.Quaternion;
import myutils.math.Vec2;
import myutils.math.Vec3;

public class ImpulseScene {

	//maybe useful: https://box2d.org/files/ErinCatto_IterativeDynamics_GDC2005.pdf

	//TODO
	// - figure out tetrahedron moment of inertia
	// - properly solve for friction in Manifold. 
	// - still some weird stuff going on with moment of inertia of thin objects
	// - optimize kdop-kdop collision narrow phase

	private final int WORLD_SCENE;
	private final int WIREFRAME_SCENE = Scene.generateScene();

	private ArrayList<Body> bodies;
	private boolean collisionOccurred = false;

	private ArrayList<DisplayBody> displayBodies;

	//generating wireframes is very expensive, as they are custom generated to fit the collision shape
	//probably only use for debug
	private boolean generateWireframes = false;

	private Vec3 gravity = new Vec3(0, -10, 0);

	public ImpulseScene(int WORLD_SCENE) {
		this.WORLD_SCENE = WORLD_SCENE;
		this.bodies = new ArrayList<>();
		this.displayBodies = new ArrayList<>();
	}

	public ArrayList<Body> getBodies() {
		return this.bodies;
	}

	private Body addBody(Body b, DisplayBody d) {
		this.bodies.add(b);
		this.displayBodies.add(d);
		return b;
	}

	public Body addAABB(Vec3 pos, Vec3 dim) {
		ModelInstance mi = Cube.addDefaultCube(new Vec3(0), dim, WORLD_SCENE);

		Shape s = new AABB(dim);
		Body b = new Body(s, pos);
		DisplayBody d = new DisplayBody(b, this.generateWireframes, WIREFRAME_SCENE, mi);
		return this.addBody(b, d);
	}

	public Body addCapsule(Vec3 pos, float radius, float length) {
		Capsule s = new Capsule(radius, length);
		Body b = new Body(s, pos);
		DisplayBody d = null;

		if (length == 0) {
			ModelInstance e1 = CubeSphere.addDefaultSphere(new Vec3(0, 0, length / 2), radius, WORLD_SCENE);
			d = new DisplayBody(b, this.generateWireframes, WIREFRAME_SCENE, e1);
		}
		else {
			ModelInstance e1 = CubeSphere.addDefaultSphere(new Vec3(0, 0, length / 2), radius, WORLD_SCENE);
			ModelInstance e2 = CubeSphere.addDefaultSphere(new Vec3(0, 0, -length / 2), radius, WORLD_SCENE);
			ModelInstance mid = Cylinder.addDefaultCylinder(length, radius, new Vec3(0), Quaternion.identity(), WORLD_SCENE);
			d = new DisplayBody(b, this.generateWireframes, WIREFRAME_SCENE, e1, e2, mid);
		}

		return this.addBody(b, d);
	}

	public Body addSphere(Vec3 pos, float radius) {
		return this.addCapsule(pos, radius, 0);
	}

	public Body addKDOP(Vec3 pos, Model m, Mat4 base_transform) {
		Shape s = this.generateKDOP(m, base_transform);
		Body b = new Body(s, pos);

		Mat4 transform = new Mat4(base_transform);
		transform.muli(Mat4.translate(((KDOP) s).getCOMCorrection().mul(-1)));
		ModelInstance mi = new ModelInstance(m, new ModelTransform(transform), WORLD_SCENE);

		DisplayBody d = new DisplayBody(b, this.generateWireframes, WIREFRAME_SCENE, mi);
		this.addBody(b, d);
		return b;
	}

	private KDOP generateKDOP(Model m, Mat4 transform) {
		ArrayList<Vec3> pts_list = new ArrayList<>();
		for (VertexArray va : m.getMeshes()) {
			for (int i = 0; i < va.getVertices().length / 3; i++) {
				pts_list.add(new Vec3(va.getVertices()[i * 3 + 0], va.getVertices()[i * 3 + 1], va.getVertices()[i * 3 + 2]));
			}
		}
		Vec3[] pts = new Vec3[pts_list.size()];
		for (int i = 0; i < pts.length; i++) {
			pts[i] = transform.mul(pts_list.get(i), 1);
		}
		KDOP kdop = new KDOP(pts);
		return kdop;
	}

	public void removeBody(Body b) {
		this.bodies.remove(b);
		for (int i = 0; i < this.displayBodies.size(); i++) {
			if (this.displayBodies.get(i).body == b) {
				this.displayBodies.remove(i);
				break;
			}
		}
	}

	public void clearScene() {
		this.bodies.clear();
		for (DisplayBody d : this.displayBodies) {
			d.kill();
		}
		this.displayBodies.clear();
	}

	public void kill() {
		this.clearScene();
		Scene.removeScene(WIREFRAME_SCENE);
	}

	private void handleCollisions() {
		this.collisionOccurred = false;

		ArrayList<lwjglengine.impulse3d.bvh.KDOP> aabb_list = new ArrayList<>();
		for (int i = 0; i < this.bodies.size(); i++) {
			aabb_list.add(this.bodies.get(i).calcBoundingBox());
		}

		BVH bvh = new BVH(aabb_list);

		int coll_cnt = 0;
		for (int i = 0; i < this.bodies.size(); i++) {
			ArrayList<Integer> isect = bvh.getIntersections(aabb_list.get(i));
			for (int j = 0; j < isect.size(); j++) {
				int next = isect.get(j);
				if (next >= i) { //already should've considered this collision
					continue;
				}
				coll_cnt++;
				Manifold m = new Manifold(this.bodies.get(i), this.bodies.get(next));
				m.apply();
				if (m.didCollide) {
					this.collisionOccurred = true;
				}
			}
		}

		int naive_cnt = (this.bodies.size() * (this.bodies.size() - 1)) / 2;
		//		System.out.println("COLL CNT : " + coll_cnt + " NAIVE CNT : " + naive_cnt);
	}

	public void update(float dt) {
		Vec3[] accel = new Vec3[this.bodies.size()];
		for (int i = 0; i < this.bodies.size(); i++) {
			accel[i] = new Vec3(0);
		}

		//gravity
		for (int i = 0; i < this.bodies.size(); i++) {
			accel[i].addi(this.gravity);
		}

		//euler step
		for (int i = 0; i < this.bodies.size(); i++) {
			Body b = this.bodies.get(i);
			if (b.is_static) {
				continue;
			}

			Vec3 npos = b.pos.add(b.vel.mul(dt));
			Vec3 nvel = b.vel.add(accel[i].mul(dt));

			Vec3 axis = new Vec3(b.angvel);
			float omega = axis.length();
			axis = axis.normalize();
			Quaternion quat_rot = MathUtils.quaternionRotationAxisAngle(omega * dt, axis.x, axis.y, axis.z);
			Quaternion norient = quat_rot.mul(b.orient);
			norient.normalize();

			b.pos.set(npos);
			b.vel.set(nvel);
			b.orient.set(norient);
		}

		this.handleCollisions();
	}

	public void updateDisplayBodies() {
		for (DisplayBody d : this.displayBodies) {
			d.updateModelInstance();
		}
	}

	public void setGravity(Vec3 g) {
		this.gravity.set(g);
	}

	public Vec3 getGravity() {
		return this.gravity;
	}

	public boolean getCollisionOccurred() {
		return this.collisionOccurred;
	}

	public void setGenerateWireframes(boolean b) {
		this.generateWireframes = b;
	}

}
