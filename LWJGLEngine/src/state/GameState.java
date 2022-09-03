package state;

import java.util.ArrayList;

import entity.Entity;
import graphics.Framebuffer;
import graphics.Texture;
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
	
	private Screen perspectiveScreen;
	private Camera perspectiveCamera;
	
	public GameState(StateManager sm) {
		super(sm);
	}
	
	@Override
	public void load() {
		// -- WORLD SCENE --
		AssetManager.getModel("dust2").addInstance(Mat4.rotateX((float) Math.toRadians(90)).mul(Mat4.scale((float) 0.05)), Scene.WORLD_SCENE);
		Light.lights.put(Scene.WORLD_SCENE, new ArrayList<>());
		Light.lights.get(Scene.WORLD_SCENE).add(new DirLight(new Vec3(0.3f, -1f, -0.5f), new Vec3(0.8f), 0.3f));
		Scene.skyboxes.put(Scene.WORLD_SCENE, AssetManager.getSkybox("lake_skybox"));
		
		// -- TEST SCENE --
		AssetManager.getModel("dust2").addInstance(Mat4.rotateX((float) Math.toRadians(90)).mul(Mat4.scale((float) 0.05)), Scene.TEST_SCENE);
		Light.lights.put(Scene.TEST_SCENE, new ArrayList<>());
		Light.lights.get(Scene.TEST_SCENE).add(new DirLight(new Vec3(0.3f, -1f, -0.5f), new Vec3(0.3f), 0.8f));
		Scene.skyboxes.put(Scene.TEST_SCENE, AssetManager.getSkybox("stars_skybox"));
		
		this.perspectiveCamera = new Camera(Main.FOV, (float) Main.windowWidth, (float) Main.windowHeight, Main.NEAR, Main.FAR);
		this.perspectiveScreen = new PerspectiveScreen();
		this.perspectiveScreen.setCamera(perspectiveCamera);
	}

	@Override
	public void update() {
		Entity.updateEntities();
		Model.updateModels();
		updateCamera();
	}

	@Override
	public void render(Framebuffer outputBuffer) {
		this.perspectiveScreen.render(outputBuffer, Scene.TEST_SCENE);
	}
	
	private void updateCamera() {
		Player p = StateManager.player;
		this.perspectiveCamera.setPos(p.pos.add(Player.cameraVec));
		this.perspectiveCamera.setFacing(p.camXRot, p.camYRot);
		this.perspectiveCamera.setUp(new Vec3(0, 1, 0));
	}

}
