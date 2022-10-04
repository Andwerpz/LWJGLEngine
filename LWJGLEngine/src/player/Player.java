package player;

import static org.lwjgl.glfw.GLFW.*;

import java.util.ArrayList;

import entity.Capsule;
import entity.Entity;
import input.KeyboardInput;
import input.MouseInput;
import main.Main;
import model.AssetManager;
import model.Hitbox;
import model.Model;
import scene.Light;
import scene.PointLight;
import scene.Scene;
import scene.SpotLight;
import util.Mat4;
import util.MathUtils;
import util.Vec2;
import util.Vec3;

public class Player extends Entity {

	public static float jumpVel = 0.12f;
	public static float airMoveSpeed = 0.0005f;
	public static float airFriction = 0.99f;
	public static Vec3 cameraVec = new Vec3(0f, 0.9f, 0f);

	public static float groundMoveSpeed = 0.012f;
	public static float groundFriction = 0.8f;
	public static float epsilon = 0.00001f;
	public static float gravity = 0.005f;

	public Vec3 pos, vel;
	public float radius = 0.33f;
	public float height = 1f;
	public int scene;
	private boolean onGround = false;
	private Vec3 groundNormal = new Vec3(0);

	private boolean acceptPlayerInputs = true;

	Vec2 mouse;

	public float camXRot;
	public float camYRot;
	public float camZRot;

	public Player(Vec3 pos, int scene) {
		super();
		this.scene = scene;
		this.pos = new Vec3(pos);
		this.vel = new Vec3(0);
		mouse = MouseInput.getMousePos();
	}

	@Override
	protected void _kill() {
	};

	public Vec3 getBottom() {
		return new Vec3(this.pos);
	}

	public Vec3 getTop() {
		return this.pos.add(new Vec3(0, this.height, 0));
	}

	public float getRadius() {
		return this.radius;
	}

	public void setAcceptPlayerInputs(boolean b) {
		this.acceptPlayerInputs = b;
	}

	@Override
	public void update() {
		// ROTATION
		Vec2 nextMouse = MouseInput.getMousePos();
		Vec2 delta = nextMouse.sub(mouse);

		if (this.acceptPlayerInputs) {
			camYRot += Math.toRadians(delta.x / 10f);
			camXRot += Math.toRadians(delta.y / 10f);
			mouse = nextMouse;
		}

		camXRot = MathUtils.clamp((float) -(Math.PI - 0.01) / 2f, (float) (Math.PI - 0.01) / 2f, camXRot);

		// TRANSLATION
		move();
	}

	public void setPos(Vec3 v) {
		this.pos = new Vec3(v);
	}

	public void setVel(Vec3 v) {
		this.vel = new Vec3(v);
	}

	// ignores all collision
	private void move_noclip() {
		this.vel.x *= airFriction;
		this.vel.z *= airFriction;
		this.vel.y *= airFriction;

		this.pos.add(this.vel);
	}

	private void move() {
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

		this.groundCheck();

		// -- GRAVITY --
		if (!onGround) {
			this.vel.addi(new Vec3(0, -gravity, 0));
		}
		resolveCollisions();

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
			resolveCollisions();
		}

	}

	// check if on the ground, and if so, then compute the ground normal
	private void groundCheck() {
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
		groundNormal.normalize();
	}

	private void resolveCollisions() {
		Vec3 capsule_bottom = pos.add(new Vec3(0, 0, 0));
		Vec3 capsule_top = pos.add(new Vec3(0, height, 0));

		Vec3 capsule_bottomSphere = pos.add(new Vec3(0, radius, 0));
		Vec3 capsule_topSphere = pos.add(new Vec3(0, height - radius, 0));

		// resolve intersections by applying a force to each one
		ArrayList<Vec3[]> intersections = Model.capsuleIntersect(scene, capsule_bottom, capsule_top, radius - 0.01f);
		for (Vec3[] a : intersections) {
			Vec3 v = a[0];
			Vec3 capsule_c = MathUtils.point_lineSegmentProjectClamped(v, capsule_bottomSphere, capsule_topSphere); // closest
			// point
			// on
			// capsule
			// midline
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

	// WIP
	// assumes that objects in the world are arranged in a gridlike fashion
	/*
	 * public void move_grid() { this.vel.x *= friction; this.vel.z *= friction; this.vel.y -= gravity;
	 * 
	 * this.pos = nextPos_grid(); }
	 * 
	 * public Vec3 nextPos_grid() { Hitbox h = this.getHitbox(); Vec3 nextPos = new Vec3(this.pos);
	 * 
	 * //north / south movement nextPos.z += vel.z; if(vel.z < 0) { boolean collision = false; for(int x = (int) Math.floor(nextPos.x); x <= (int) Math.floor(nextPos.x + h.getWidth()); x++) { for(int y = (int) Math.floor(nextPos.y); y <= (int) Math.floor(nextPos.y + h.getHeight()); y++) { for(int z
	 * = (int) Math.floor(nextPos.z); z <= (int) Math.floor(nextPos.z + h.getDepth()); z++) { Block b = World.getBlock(x, y, z); if(b.collision(nextPos, h)) { collision = true; nextPos.z = Math.max(nextPos.z, b.getHitbox().max.z + z + cushion); } } } } if(collision) { vel.z = 0; } } else if(vel.z >
	 * 0) { boolean collision = false; for(int x = (int) Math.floor(nextPos.x); x <= (int) Math.floor(nextPos.x + h.getWidth()); x++) { for(int y = (int) Math.floor(nextPos.y); y <= (int) Math.floor(nextPos.y + h.getHeight()); y++) { for(int z = (int) Math.floor(nextPos.z); z <= (int)
	 * Math.floor(nextPos.z + h.getDepth()); z++) { Block b = World.getBlock(x, y, z); if(b.collision(nextPos, h)) { collision = true; nextPos.z = Math.min(nextPos.z, b.getHitbox().min.z + z - h.getDepth() - cushion); } } } } if(collision) { vel.z = 0; } }
	 * 
	 * //east / west movement nextPos.x += vel.x; if(vel.x < 0) { boolean collision = false; for(int x = (int) Math.floor(nextPos.x); x <= (int) Math.floor(nextPos.x + h.getWidth()); x++) { for(int y = (int) Math.floor(nextPos.y); y <= (int) Math.floor(nextPos.y + h.getHeight()); y++) { for(int z =
	 * (int) Math.floor(nextPos.z); z <= (int) Math.floor(nextPos.z + h.getDepth()); z++) { Block b = World.getBlock(x, y, z); if(b.collision(nextPos, h)) { collision = true; nextPos.x = Math.max(nextPos.x, b.getHitbox().max.x + x + cushion); } } } } if(collision) { vel.x = 0; } } else if(vel.x > 0)
	 * { boolean collision = false; for(int x = (int) Math.floor(nextPos.x); x <= (int) Math.floor(nextPos.x + h.getWidth()); x++) { for(int y = (int) Math.floor(nextPos.y); y <= (int) Math.floor(nextPos.y + h.getHeight()); y++) { for(int z = (int) Math.floor(nextPos.z); z <= (int)
	 * Math.floor(nextPos.z + h.getDepth()); z++) { Block b = World.getBlock(x, y, z); if(b.collision(nextPos, h)) { collision = true; nextPos.x = Math.min(nextPos.x, b.getHitbox().min.x + x - h.getWidth() - cushion); } } } } if(collision) { vel.x = 0; } }
	 * 
	 * //up / down movement nextPos.y += vel.y; if(vel.y < 0) { boolean collision = false; for(int x = (int) Math.floor(nextPos.x); x <= (int) Math.floor(nextPos.x + h.getWidth()); x++) { for(int y = (int) Math.floor(nextPos.y); y <= (int) Math.floor(nextPos.y + h.getHeight()); y++) { for(int z =
	 * (int) Math.floor(nextPos.z); z <= (int) Math.floor(nextPos.z + h.getDepth()); z++) { Block b = World.getBlock(x, y, z); if(b.collision(nextPos, h)) { collision = true; nextPos.y = Math.max(nextPos.y, b.getHitbox().max.y + y + cushion); } } } } if(collision) { onGround = true; vel.y = 0; } }
	 * else if(vel.y > 0) { onGround = false; boolean collision = false; for(int x = (int) Math.floor(nextPos.x); x <= (int) Math.floor(nextPos.x + h.getWidth()); x++) { for(int y = (int) Math.floor(nextPos.y); y <= (int) Math.floor(nextPos.y + h.getHeight()); y++) { for(int z = (int)
	 * Math.floor(nextPos.z); z <= (int) Math.floor(nextPos.z + h.getDepth()); z++) { Block b = World.getBlock(x, y, z); if(b.collision(nextPos, h)) { collision = true; nextPos.y = Math.min(nextPos.y, b.getHitbox().min.y + y - h.getHeight() - cushion); } } } } if(collision) { vel.y = 0; } } }
	 */

}
