package state;

import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_P;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;

import java.awt.Color;
import java.util.HashSet;

import entity.Entity;
import graphics.Framebuffer;
import graphics.Texture;
import input.Button;
import input.InputManager;
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
import screen.UIScreen;
import ui.FilledRectangle;
import ui.Line;
import ui.Rectangle;
import ui.Text;
import util.Mat4;
import util.MathUtils;
import util.Vec3;

public class MainMenuState extends State {
	
	private static final float PERSPECTIVE_FOV = (float) Math.toRadians(70f);
	private static final float ROTATION_TIME = 40f;

	private static Screen perspectiveScreen;	//3D background
	private static Camera perspectiveCamera;
	
	private static UIScreen uiScreen;	//Menu UI
	private static Camera uiCamera;
	
	private long startTime;
	
	public MainMenuState(StateManager sm) {
		super(sm);
		
		this.startTime = System.currentTimeMillis();
	}
	
	@Override
	public void load() {
		if(perspectiveScreen == null) {
			perspectiveCamera = new Camera(PERSPECTIVE_FOV, (float) Main.windowWidth, (float) Main.windowHeight, Main.NEAR, Main.FAR);
			perspectiveScreen = new PerspectiveScreen();
			perspectiveScreen.setCamera(perspectiveCamera);
		}
		perspectiveCamera.setPos(new Vec3(18.417412f, 1.7f, -29.812654f));
		
		if(uiScreen == null) {
			uiCamera = new Camera(Mat4.orthographic(0, (float) Main.windowWidth, 0, (float) Main.windowHeight, 1, -1));
			uiScreen = new UIScreen();
			uiScreen.setCamera(uiCamera);
		}
		
		Main.unlockCursor();
		InputManager.removeAllInputs();
		Entity.killAll();
		
		// -- WORLD SCENE --
		Model.removeInstancesFromScene(Scene.WORLD_SCENE);
		Light.removeLightsFromScene(Scene.WORLD_SCENE);
		Model.addInstance(AssetManager.getModel("dust2"), Mat4.rotateX((float) Math.toRadians(90)).mul(Mat4.scale((float) 0.05)), Scene.WORLD_SCENE);
		Light.addLight(Scene.WORLD_SCENE, new DirLight(new Vec3(0.3f, -1f, -0.5f), new Vec3(0.5f), 0.3f));
		Scene.skyboxes.put(Scene.WORLD_SCENE, AssetManager.getSkybox("stars_skybox")); 
		
		// -- UI SCENE --
		Model.removeInstancesFromScene(Scene.UI_SCENE);
		Light.removeLightsFromScene(Scene.UI_SCENE);
		InputManager.addInput("btn_logo", new Button(120, Main.windowHeight / 2, 700, 150, new Texture("/cs_go_logo.png", false, false, true), new Texture("/hitbox.png"), Scene.UI_SCENE));
		InputManager.addInput("btn_new_game", new Button(120, Main.windowHeight / 2 - 50, 200, 100, new Texture("/ui/main_menu/new_game.png", false, false, true), new Texture("/hitbox.png"), Scene.UI_SCENE));
		InputManager.addInput("btn_settings", new Button(120, Main.windowHeight / 2 - 100, 200, 100, new Texture("/ui/main_menu/settings.png", false, false, true), new Texture("/hitbox.png"), Scene.UI_SCENE));
		InputManager.addInput("btn_quit_game", new Button(120, Main.windowHeight / 2 - 150, 200, 100, new Texture("/ui/main_menu/quit_game.png", false, false, true), new Texture("/hitbox.png"), Scene.UI_SCENE));
		
		Rectangle.addRectangle(100, 100, 100, 100, Scene.UI_SCENE);
		FilledRectangle.addRectangle(200, 200, 100, 100, Scene.UI_SCENE);
		Line.addLine(300, 100, 300, 200, Scene.UI_SCENE);
		
		Text text = new Text("Test text", 36, Color.WHITE, 1000, 500, Scene.UI_SCENE);
	}

	@Override
	public void update() {
		Entity.updateEntities();
		Model.updateModels();
		updateCamera();
	}

	@Override
	public void render(Framebuffer outputBuffer) {
		perspectiveScreen.render(outputBuffer, Scene.WORLD_SCENE);
		uiScreen.render(outputBuffer, Scene.UI_SCENE);
	}
	
	private void updateCamera() {
		perspectiveCamera.setFacing(0, MathUtils.interpolate(0, 0, (float) Math.PI * 2f, ROTATION_TIME * 1000f, System.currentTimeMillis() - startTime));
		perspectiveCamera.setUp(new Vec3(0, 1, 0));
	}

	@Override
	public void mousePressed(int button) {
		InputManager.pressed(uiScreen.getEntityIDAtMouse());
	}

	@Override
	public void mouseReleased(int button) {
		InputManager.released();
		if(InputManager.isClicked("btn_logo")) {
			this.sm.switchState(new SplashState(this.sm));
		}
		else if(InputManager.isClicked("btn_new_game")) {
			this.sm.switchState(new GameState(this.sm));
		}
		else if(InputManager.isClicked("btn_quit_game")) {
			Main.main.exit();
		}
	}

}
