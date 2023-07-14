package lwjglengine.player;

import static org.lwjgl.glfw.GLFW.*;

import java.util.ArrayList;

import lwjglengine.input.KeyboardInput;
import lwjglengine.input.MouseInput;
import lwjglengine.model.Model;
import myutils.v10.math.MathUtils;
import myutils.v10.math.Vec2;
import myutils.v10.math.Vec3;

public class PlayerInputController {

	public static float jumpVel = 0.12f;
	public static float airMoveSpeed = 0.0005f;
	public static float airFriction = 0.99f;
	public static Vec3 cameraVec = new Vec3(0f, 0f, 0f);

	public static float groundMoveSpeed = 0.012f;
	public static float groundFriction = 0.8f;
	public static float epsilon = 0.00001f;
	public static float gravity = 0.005f;

	private static float runningSpeed = 0.05f;
	private static float walkingSpeed = 0.02f; //TBD

	private static float noclipSpeed = 0.3f;
	private static float noclipFriction = 0.7f;

	private Vec3 pos, vel;
	private float radius = 0.33f;
	private float height = 1f;
	private boolean onGround = false;
	private Vec3 groundNormal = new Vec3(0);

	private static float landingSpeed = 0.085f;
	private boolean hasLanded = false;

	private boolean acceptPlayerInputs = true;

	Vec2 mouse;

	private float camXRot;
	private float camYRot;
	private float camZRot;

	public PlayerInputController(Vec3 pos) {
		super();
		this.pos = new Vec3(pos);
		this.vel = new Vec3(0);
		mouse = MouseInput.getMousePos();
	}

	public Vec3 getBottom() {
		return new Vec3(this.pos);
	}

	public Vec3 getTop() {
		return this.pos.add(new Vec3(0, this.height, 0));
	}

	public float getRadius() {
		return this.radius;
	}

	public float getCamXRot() {
		return this.camXRot;
	}

	public float getCamYRot() {
		return this.camYRot;
	}

	public float getCamZRot() {
		return this.camZRot;
	}

	public Vec3 getFacing() {
		Vec3 facing = new Vec3(0, 0, -1);
		facing.rotateX(this.camXRot);
		facing.rotateY(this.camYRot);
		return facing;
	}

	public void setCamXRot(float f) {
		this.camXRot = f;
	}

	public void setCamYRot(float f) {
		this.camYRot = f;
	}

	public void setCamZRot(float f) {
		this.camZRot = f;
	}

	public Vec3 getPos() {
		return this.pos;
	}

	public void setAcceptPlayerInputs(boolean b) {
		this.acceptPlayerInputs = b;
	}

	public void update() {
		// ROTATION
		Vec2 nextMouse = MouseInput.getMousePos();
		Vec2 delta = nextMouse.sub(mouse);
		this.mouse = nextMouse;

		if (this.acceptPlayerInputs) {
			this.camYRot += Math.toRadians(delta.x / 10f);
			this.camXRot -= Math.toRadians(delta.y / 10f);
		}

		this.camXRot = MathUtils.clamp((float) -(Math.PI - 0.01) / 2f, (float) (Math.PI - 0.01) / 2f, camXRot);

		// TRANSLATION
		move_noclip();
	}

	public void setPos(Vec3 v) {
		this.pos = new Vec3(v);
	}

	public void setVel(Vec3 v) {
		this.vel = new Vec3(v);
	}

	// ignores all collision
	private void move_noclip() {
		this.vel.x *= noclipFriction;
		this.vel.z *= noclipFriction;
		this.vel.y *= noclipFriction;

		this.pos.addi(this.vel);

		if (this.acceptPlayerInputs) {
			Vec3 forward = new Vec3(0, 0, -1);
			forward.rotateY(camYRot);
			Vec3 right = new Vec3(forward);
			right.rotateY((float) Math.toRadians(-90));

			Vec3 inputAccel = new Vec3(0);

			if (KeyboardInput.isKeyPressed(GLFW_KEY_W)) {
				inputAccel.addi(forward);
			}
			if (KeyboardInput.isKeyPressed(GLFW_KEY_S)) {
				inputAccel.subi(forward);
			}
			if (KeyboardInput.isKeyPressed(GLFW_KEY_D)) {
				inputAccel.subi(right);
			}
			if (KeyboardInput.isKeyPressed(GLFW_KEY_A)) {
				inputAccel.addi(right);
			}
			if (KeyboardInput.isKeyPressed(GLFW_KEY_SPACE)) {
				inputAccel.y += 1;
			}
			if (KeyboardInput.isKeyPressed(GLFW_KEY_LEFT_SHIFT)) {
				inputAccel.y -= 1;
			}

			inputAccel.setLength(noclipSpeed);

			this.vel.addi(inputAccel);
		}

	}

	private void move_collision(int scene) {
		// -- UPDATE POSITON --
		if (onGround) {
			this.vel.x *= groundFriction;
			this.vel.z *= groundFriction;
			this.vel.y *= airFriction;
		}
		else {
			this.vel.mul(airFriction);
		}
		this.pos.addi(vel);

		this.groundCheck(scene);

		// -- GRAVITY --
		if (!onGround) {
			this.vel.addi(new Vec3(0, -gravity, 0));
		}
		resolveCollisions(scene);

		// -- PLAYER INPUTS --
		if (this.acceptPlayerInputs) {
			Vec3 forward = new Vec3(0, 0, -1);
			forward.rotateY(camYRot);
			Vec3 right = new Vec3(forward);
			right.rotateY((float) Math.toRadians(-90));

			Vec3 inputAccel = new Vec3(0);

			if (KeyboardInput.isKeyPressed(GLFW_KEY_W)) {
				inputAccel.addi(forward);
			}
			if (KeyboardInput.isKeyPressed(GLFW_KEY_S)) {
				inputAccel.subi(forward);
			}
			if (KeyboardInput.isKeyPressed(GLFW_KEY_D)) {
				inputAccel.subi(right);
			}
			if (KeyboardInput.isKeyPressed(GLFW_KEY_A)) {
				inputAccel.addi(right);
			}
			if (onGround) {
				Vec3 groundCorrection = inputAccel.projectOnto(groundNormal);
				groundCorrection.y = -(Math.abs(groundCorrection.y));
				inputAccel.addi(groundCorrection);
			}
			inputAccel.setLength(this.onGround ? groundMoveSpeed : airMoveSpeed);
			if (KeyboardInput.isKeyPressed(GLFW_KEY_SPACE)) {
				if (onGround) {
					this.vel.y = jumpVel;
					this.vel.x *= groundFriction;
					this.vel.y *= groundFriction;
				}
			}
			this.vel.addi(inputAccel);
			resolveCollisions(scene);
		}

	}

	// check if on the ground, and if so, then compute the ground normal
	private void groundCheck(int scene) {
		this.hasLanded = true;
		if (-this.vel.y < landingSpeed) {
			this.hasLanded = false;
		}
		onGround = false;
		groundNormal = new Vec3(0);
		Vec3 capsule_bottomSphere = pos.add(new Vec3(0, radius, 0));
		ArrayList<Vec3[]> intersections = Model.sphereIntersect(scene, capsule_bottomSphere, this.radius + epsilon);
		for (Vec3[] a : intersections) {
			Vec3 v = a[0];
			Vec3 toCenter = new Vec3(v, capsule_bottomSphere);
			toCenter.normalize();
			if (toCenter.dot(new Vec3(0, 1, 0)) > 0.5) {
				groundNormal.addi(toCenter.normalize());
				onGround = true;
			}
		}
		if (!onGround) {
			this.hasLanded = false;
		}
		groundNormal.normalize();
	}

	private void resolveCollisions(int scene) {
		Vec3 capsule_bottom = pos.add(new Vec3(0, 0, 0));
		Vec3 capsule_top = pos.add(new Vec3(0, height, 0));

		Vec3 capsule_bottomSphere = pos.add(new Vec3(0, radius, 0));
		Vec3 capsule_topSphere = pos.add(new Vec3(0, height - radius, 0));

		// resolve intersections by applying a force to each one
		ArrayList<Vec3[]> intersections = Model.capsuleIntersect(scene, capsule_bottom, capsule_top, radius - 0.01f);
		for (Vec3[] a : intersections) {
			Vec3 v = a[0];
			Vec3 capsule_c = MathUtils.point_lineSegmentProjectClamped(v, capsule_bottomSphere, capsule_topSphere); // closest point on capsule midline
			Vec3 toCenter = new Vec3(v, capsule_c);

			Vec3 normToCenter = new Vec3(toCenter).normalize();
			toCenter.setLength(radius - toCenter.length());
			Vec3 impulse = this.vel.projectOnto(normToCenter).mul(-1f);

			if (this.vel.dot(toCenter) < 0) {
				this.vel.addi(impulse);
				this.pos.addi(toCenter.mul(1f + epsilon));
			}
		}
	}

	public boolean isOnGround() {
		return this.onGround;
	}

	public boolean isRunning() {
		return this.vel.length() > runningSpeed && this.isOnGround();
	}

	public boolean hasLanded() {
		return this.hasLanded;
	}

}
