package player;

import static org.lwjgl.glfw.GLFW.*;

import entity.Entity;
import input.KeyboardInput;
import input.MouseInput;
import main.Main;
import model.Hitbox;
import model.Model;
import scene.Light;
import scene.PointLight;
import scene.Scene;
import scene.SpotLight;
import util.Mat4;
import util.MathTools;
import util.Vec2;
import util.Vec3;

public class Player extends Entity {

	public static Hitbox h = new Hitbox(new Vec3(0), new Vec3(0.6f, 1.8f, 0.6f));
	public static float moveSpeed = 0.05f;
	public static float jumpVel = 0.25f;
	public static Vec3 cameraVec = new Vec3(0.3f, 1.6f, 0.3f);

	Vec2 mouse;
	public Camera camera;
	
	public float camXRot;
	public float camYRot;
	public float camZRot;

	public PointLight flashlight;
	public boolean flashlightOn = false;
	int flashlightToggleDelay = 15;
	int flashlightToggleCounter = 0;
	
	public boolean moveFlashlightWithPlayer = true;
	int moveFlashlightToggleDelay = 15;
	int moveFlashlightToggleCounter = 0;

	public Player(Vec3 pos) {
		super(pos, 0, 0, 0, new Model(), Scene.WORLD_SCENE);
		camera = new Camera(Main.FOV, (float) Main.windowWidth, (float) Main.windowHeight, Main.NEAR, Main.FAR);
		mouse = MouseInput.getMousePos();
		flashlight = new PointLight(this.camera.getPos(), new Vec3(1), 0f, 1.5f, 0.022f, 0.0019f);
	}
	
	@Override
	public Model getModel() {
		return null;
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
		camYRot += Math.toRadians(delta.x / 10f);
		camXRot += Math.toRadians(delta.y / 10f);
		mouse = nextMouse;

		camXRot = (float) MathTools.clamp(-(Math.PI - 0.01) / 2f, (Math.PI - 0.01) / 2f, camXRot);
		
		camera.setFacing(camXRot, camYRot);
		camera.setUp(camZRot);

		// TRANSLATION
		Vec3 forward = new Vec3(0, 0, -moveSpeed);
		forward.rotateY(camYRot);
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

		camera.setPos(this.pos.add(cameraVec));

		// flashlight
		if(moveFlashlightWithPlayer) {
			this.flashlight.pos = this.camera.getPos();
			this.flashlight.dir = this.camera.getFacing();
		}
		
		if (KeyboardInput.isKeyPressed(GLFW_KEY_E) && flashlightToggleCounter >= flashlightToggleDelay) {
			flashlightToggleCounter = 0;
			if (flashlightOn) {
				Light.lights.get(Scene.WORLD_SCENE).remove(flashlight);
			} else {
				Light.lights.get(Scene.WORLD_SCENE).add(flashlight);
			}
			flashlightOn = !flashlightOn;
		}
		flashlightToggleCounter = Math.min(flashlightToggleCounter + 1, flashlightToggleDelay);
		
		if(KeyboardInput.isKeyPressed(GLFW_KEY_L) && moveFlashlightToggleCounter >= moveFlashlightToggleDelay) {
			moveFlashlightToggleCounter = 0;
			this.moveFlashlightWithPlayer = !moveFlashlightWithPlayer;
		}
		moveFlashlightToggleCounter ++;
	}

}
