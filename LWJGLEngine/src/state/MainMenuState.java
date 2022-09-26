package state;

import java.awt.Font;

import entity.Entity;
import graphics.Framebuffer;
import graphics.Texture;
import graphics.TextureMaterial;
import input.Button;
import input.InputManager;
import input.TextField;
import main.Main;
import model.AssetManager;
import model.Model;
import player.Camera;
import scene.DirLight;
import scene.Light;
import scene.Scene;
import screen.PerspectiveScreen;
import screen.Screen;
import screen.UIScreen;
import ui.FilledRectangle;
import util.FontUtils;
import util.Mat4;
import util.MathUtils;
import util.Vec3;

public class MainMenuState extends State {

    private static final float PERSPECTIVE_FOV = (float) Math.toRadians(70f);
    private static final float ROTATION_TIME = 40f;

    private static final int BACKGROUND_SCENE = 0; // background, duh
    private static final int STATIC_UI_SCENE = 1; // for unchanging parts of the ui like the logo
    private static final int DYNAMIC_UI_SCENE = 2; // inputs and stuff

    private static Screen perspectiveScreen; // 3D background
    private static Camera perspectiveCamera;

    private static UIScreen uiScreen, uiScreenSelector; // Menu UI
    private static Camera uiScreenCamera;

    private long startTime;

    public MainMenuState(StateManager sm) {
	super(sm);

	this.startTime = System.currentTimeMillis();
    }

    @Override
    public void load() {
	if (perspectiveScreen == null) {
	    perspectiveCamera = new Camera(PERSPECTIVE_FOV, Main.windowWidth, Main.windowHeight, Main.NEAR, Main.FAR);
	    perspectiveScreen = new PerspectiveScreen();
	    perspectiveScreen.setCamera(perspectiveCamera);
	}
	perspectiveCamera.setPos(new Vec3(18.417412f, 1.7f, -29.812654f));

	if (uiScreen == null) {
	    uiScreenCamera = new Camera(Mat4.orthographic(0, Main.windowWidth, 0, Main.windowHeight, -10, 10));
	    uiScreen = new UIScreen();
	    uiScreen.setCamera(uiScreenCamera);
	}

	Main.unlockCursor();
	InputManager.removeAllInputs();
	Entity.killAll();

	// -- BACKGROUND --
	Model.removeInstancesFromScene(BACKGROUND_SCENE);
	Light.removeLightsFromScene(BACKGROUND_SCENE);
	Model.addInstance(AssetManager.getModel("dust2"), Mat4.rotateX((float) Math.toRadians(90)).mul(Mat4.scale((float) 0.05)), BACKGROUND_SCENE);
	Light.addLight(BACKGROUND_SCENE, new DirLight(new Vec3(0.3f, -1f, -0.5f), new Vec3(0.5f), 0.3f));
	Scene.skyboxes.put(BACKGROUND_SCENE, AssetManager.getSkybox("stars_skybox"));

	// -- DYNAMIC UI --
	Model.removeInstancesFromScene(DYNAMIC_UI_SCENE);
	Light.removeLightsFromScene(DYNAMIC_UI_SCENE);

	InputManager.addInput("btn_host_game", new Button(150, Main.windowHeight / 2 - 0, 200, 30, "Host Game", FontUtils.CSGOFont, 32, DYNAMIC_UI_SCENE));
	InputManager.addInput("btn_join_game", new Button(150, Main.windowHeight / 2 - 40, 200, 30, "Join Game", FontUtils.CSGOFont, 32, DYNAMIC_UI_SCENE));
	InputManager.addInput("btn_settings", new Button(150, Main.windowHeight / 2 - 80, 200, 30, "Settings", FontUtils.CSGOFont, 32, DYNAMIC_UI_SCENE));
	InputManager.addInput("btn_quit_game", new Button(150, Main.windowHeight / 2 - 120, 200, 30, "Quit Game", FontUtils.CSGOFont, 32, DYNAMIC_UI_SCENE));

	InputManager.addInput("tf_host_port", new TextField(370, Main.windowHeight / 2 - 0, 180, 30, "Port", new Font("Dialogue", Font.PLAIN, 1), 16, DYNAMIC_UI_SCENE));
	InputManager.addInput("tf_join_ip", new TextField(370, Main.windowHeight / 2 - 40, 180, 30, "IP", new Font("Dialogue", Font.PLAIN, 1), 16, DYNAMIC_UI_SCENE));
	InputManager.addInput("tf_join_port", new TextField(570, Main.windowHeight / 2 - 40, 180, 30, "Port", new Font("Dialogue", Font.PLAIN, 1), 16, DYNAMIC_UI_SCENE));

	// -- STATIC UI --
	Model.removeInstancesFromScene(STATIC_UI_SCENE);
	Light.removeLightsFromScene(STATIC_UI_SCENE);
	FilledRectangle csgoLogo = new FilledRectangle();
	csgoLogo.setTextureMaterial(new TextureMaterial(new Texture("/cs_go_logo.png", false, false, true)));
	Mat4 logoMat4 = Mat4.scale(700, 150, 1).mul(Mat4.translate(new Vec3(120, Main.windowHeight / 2, 0)));
	Model.addInstance(csgoLogo, logoMat4, STATIC_UI_SCENE);

    }

    @Override
    public void update() {
	InputManager.hovered(uiScreen.getEntityIDAtMouse());

	Entity.updateEntities();
	Model.updateModels();
	updateCamera();
    }

    @Override
    public void render(Framebuffer outputBuffer) {
	perspectiveScreen.render(outputBuffer, BACKGROUND_SCENE);
	uiScreen.render(outputBuffer, STATIC_UI_SCENE);
	uiScreen.render(outputBuffer, DYNAMIC_UI_SCENE);
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
	InputManager.released(uiScreen.getEntityIDAtMouse());
	if (InputManager.isClicked("btn_host_game")) {
	    try {
		int ip = Integer.parseInt(InputManager.getText("tf_host_port"));
		this.sm.switchState(new GameState(this.sm, null, ip, true));
	    } catch (NumberFormatException e) {
		System.err.println("BAD PORT");
	    }

	} else if (InputManager.isClicked("btn_join_game")) {
	    try {
		int ip = Integer.parseInt(InputManager.getText("tf_join_port"));
		String port = InputManager.getText("tf_join_ip");
		this.sm.switchState(new GameState(this.sm, port, ip, false));
	    } catch (NumberFormatException e) {
		System.err.println("BAD PORT");
	    }
	} else if (InputManager.isClicked("btn_quit_game")) {
	    Main.main.exit();
	}
    }

}
