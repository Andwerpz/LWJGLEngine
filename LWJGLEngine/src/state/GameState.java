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
	
	private Player player;
	
	private long mapID;
	
	public GameState(StateManager sm) {
		super(sm);
	}
	
	@Override
	public void load() {
		if(perspectiveScreen == null) {
			perspectiveCamera = new Camera(Main.FOV, (float) Main.windowWidth, (float) Main.windowHeight, Main.NEAR, Main.FAR);
			perspectiveScreen = new PerspectiveScreen();
			perspectiveScreen.setCamera(perspectiveCamera);
		}
		player = new Player(new Vec3(18.417412f, 0.7f, -29.812654f));
		
		Main.lockCursor();
		
		AssetManager.loadModel("sphere");
		
		// -- WORLD SCENE --
		Model.removeInstancesFromScene(Scene.WORLD_SCENE);
		Light.removeLightsFromScene(Scene.WORLD_SCENE);
		this.mapID = Model.addInstance(AssetManager.getModel("dust2"), Mat4.rotateX((float) Math.toRadians(90)).mul(Mat4.scale((float) 0.05)), Scene.WORLD_SCENE);
		Light.addLight(Scene.WORLD_SCENE, new DirLight(new Vec3(0.3f, -1f, -0.5f), new Vec3(0.8f), 0.3f));
		Scene.skyboxes.put(Scene.WORLD_SCENE, AssetManager.getSkybox("lake_skybox")); 
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
		perspectiveCamera.setPos(player.pos.add(Player.cameraVec));
		perspectiveCamera.setFacing(player.camXRot, player.camYRot);
		perspectiveCamera.setUp(new Vec3(0, 1, 0));
	}

	@Override
	public void mousePressed(int button) {
		//shoot ray in direction of camera
		Vec3 ray_origin = perspectiveCamera.getPos();
		Vec3 ray_dir = perspectiveCamera.getFacing();
		ArrayList<Vec3> intersect = Model.rayIntersect(mapID, ray_origin, ray_dir);
		if(intersect.size() != 0) {
			float minDist = 0f;
			Vec3 minVec = null;
			for(Vec3 v : intersect) {
				float dist = v.sub(ray_origin).length();
				if(dist < minDist || minVec == null) {
					minDist = dist;
					minVec = v;
				}
			}
			Model.addInstance(AssetManager.getModel("sphere"), Mat4.scale(0.1f).mul(Mat4.translate(minVec)), Scene.WORLD_SCENE);
		}
	}

	@Override
	public void mouseReleased(int button) {
		// TODO Auto-generated method stub
		
	}

}
