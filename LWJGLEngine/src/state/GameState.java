package state;

import static org.lwjgl.glfw.GLFW.*;

import java.util.ArrayList;

import entity.Entity;
import graphics.Framebuffer;
import graphics.Texture;
import input.KeyboardInput;
import main.Main;
import model.AssetManager;
import model.Model;
import player.Camera;
import player.Player;
import scene.DirLight;
import scene.Light;
import scene.Scene;
import screen.PerspectiveScreen;
import screen.Screen;
import util.Mat4;
import util.Vec3;

public class GameState extends State {
	
	private static Screen perspectiveScreen;
	private static Camera perspectiveCamera;
	
	public GameState(StateManager sm) {
		super(sm);
	}
	
	@Override
	public void load() {
		//model and scene instance relations
		//before we can add a model instance to a scene, we first must load that model. 
		//load state should load all models associated with the current state, unload any others, and reset scene instancing
		
		// -- WORLD SCENE --
		Model.removeInstancesFromScene(Scene.WORLD_SCENE);
		Light.removeLightsFromScene(Scene.WORLD_SCENE);
		AssetManager.getModel("dust2").addInstance(Mat4.rotateX((float) Math.toRadians(90)).mul(Mat4.scale((float) 0.05)), Scene.WORLD_SCENE);
		Light.addLight(Scene.WORLD_SCENE, new DirLight(new Vec3(0.3f, -1f, -0.5f), new Vec3(0.8f), 0.3f));
		Scene.skyboxes.put(Scene.WORLD_SCENE, AssetManager.getSkybox("lake_skybox")); 
		
		if(perspectiveScreen == null) {
			perspectiveCamera = new Camera(Main.FOV, (float) Main.windowWidth, (float) Main.windowHeight, Main.NEAR, Main.FAR);
			perspectiveScreen = new PerspectiveScreen();
			perspectiveScreen.setCamera(perspectiveCamera);
		}
		
	}

	@Override
	public void update() {
		Entity.updateEntities();
		Model.updateModels();
		updateCamera();
		
		//input
		if(KeyboardInput.isKeyPressed(GLFW_KEY_P)) {
			this.sm.switchState(new SplashState(this.sm));
		}
	}

	@Override
	public void render(Framebuffer outputBuffer) {
		perspectiveScreen.render(outputBuffer, Scene.WORLD_SCENE);
	}
	
	private void updateCamera() {
		Player p = StateManager.player;
		perspectiveCamera.setPos(p.pos.add(Player.cameraVec));
		perspectiveCamera.setFacing(p.camXRot, p.camYRot);
		perspectiveCamera.setUp(new Vec3(0, 1, 0));
	}

}
