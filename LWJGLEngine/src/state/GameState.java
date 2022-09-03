package state;

import entity.Entity;
import graphics.Framebuffer;
import graphics.Texture;
import main.Main;
import model.Model;
import player.Camera;
import player.Player;
import scene.Scene;
import screen.PerspectiveScreen;
import screen.Screen;
import util.Vec3;

public class GameState extends State {
	
	private Screen perspectiveScreen;
	private Camera perspectiveCamera;
	
	public GameState(StateManager sm) {
		super(sm);
	}
	
	@Override
	public void load() {
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
