package player;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;

import entity.Entity;
import input.KeyboardInput;
import input.MouseInput;
import model.Hitbox;
import util.MathTools;
import util.Vec2;
import util.Vec3;

public class Player extends Entity{
	
	public static Hitbox h = new Hitbox(new Vec3(0), new Vec3(0.6f, 1.8f, 0.6f));
	public static float moveSpeed = 0.05f;
	public static float jumpVel = 0.25f;
	public static Vec3 cameraVec = new Vec3(0.3f, 1.6f, 0.3f);
	
	Vec2 mouse;
	public Camera camera;
	
	public Player(Vec3 pos) {
		super(pos);
		camera = new Camera();
		mouse = MouseInput.getMousePos();
	}

	@Override
	public Hitbox getHitbox() {
		return h;
	}

	@Override
	public void update() {
		// ROTATION
		Vec2 nextMouse = MouseInput.getMousePos();
		Vec2 delta = nextMouse.sub(mouse);
		camera.yRot += Math.toRadians(delta.x / 10f); 
		camera.xRot += Math.toRadians(delta.y / 10f);
		mouse = nextMouse;
		
		camera.xRot = (float) MathTools.clamp(-Math.PI / 2f, Math.PI / 2f, camera.xRot);

		// TRANSLATION
		Vec3 forward = new Vec3(0, 0, -moveSpeed);
		forward.rotateY(camera.yRot);
		Vec3 right = new Vec3(forward);
		right.rotateY((float) Math.toRadians(-90));

		if (KeyboardInput.isKeyPressed(GLFW_KEY_W)) {
			vel.addi(forward);
		}
		if (KeyboardInput.isKeyPressed(GLFW_KEY_S)) {
			vel.subi(forward);
		}
		if (KeyboardInput.isKeyPressed(GLFW_KEY_D)) {
			vel.subi(right);
		}
		if (KeyboardInput.isKeyPressed(GLFW_KEY_A)) {
			vel.addi(right);
		}
		if (KeyboardInput.isKeyPressed(GLFW_KEY_SPACE)) {
			vel.y += moveSpeed;
		}
		if (KeyboardInput.isKeyPressed(GLFW_KEY_LEFT_SHIFT)) {
			vel.y -= moveSpeed;
		}
		
		move();
		
		camera.pos = this.pos.add(cameraVec);
	}
	
}
