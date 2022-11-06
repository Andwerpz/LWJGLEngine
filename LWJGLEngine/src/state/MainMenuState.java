package state;

import java.awt.Font;

import audio.Sound;
import entity.Entity;
import graphics.Framebuffer;
import graphics.Texture;
import graphics.TextureMaterial;
import input.Button;
import input.Input;
import input.TextField;
import main.Main;
import model.AssetManager;
import model.FilledRectangle;
import model.Model;
import player.Camera;
import scene.DirLight;
import scene.Light;
import scene.Scene;
import screen.PerspectiveScreen;
import screen.Screen;
import screen.UIScreen;
import ui.UIElement;
import util.FontUtils;
import util.Mat4;
import util.MathUtils;
import util.Vec3;

public class MainMenuState extends State {

	private static final float ROTATION_TIME = 40f;

	private static final int BACKGROUND_SCENE = 0; // background, duh
	private static final int STATIC_UI_SCENE = 1; // for unchanging parts of the ui like the logo
	private static final int DYNAMIC_UI_SCENE = 2; // inputs and stuff

	private PerspectiveScreen perspectiveScreen; // 3D background
	private UIScreen uiScreen; // Menu UI

	private Sound menuMusic;

	private long startTime;

	public MainMenuState(StateManager sm) {
		super(sm);

		this.startTime = System.currentTimeMillis();
	}

	@Override
	public void load() {
		this.perspectiveScreen = new PerspectiveScreen();
		this.perspectiveScreen.getCamera().setVerticalFOV((float) Math.toRadians(70f));
		this.perspectiveScreen.getCamera().setPos(new Vec3(18.417412f, 1.7f, -29.812654f));

		this.uiScreen = new UIScreen();

		Main.unlockCursor();

		Entity.killAll();

		// -- BACKGROUND --
		this.clearScene(BACKGROUND_SCENE);
		Model.addInstance(AssetManager.getModel("dust2"), Mat4.rotateX((float) Math.toRadians(90)).mul(Mat4.scale((float) 0.05)), BACKGROUND_SCENE);
		Light.addLight(BACKGROUND_SCENE, new DirLight(new Vec3(0.3f, -1f, -0.5f), new Vec3(0.5f), 0.3f));
		Scene.skyboxes.put(BACKGROUND_SCENE, AssetManager.getSkybox("stars_skybox"));

		this.drawMainMenu();

		menuMusic = new Sound("csgo_main_menu.ogg", true);
		int menuMusicID = menuMusic.addSource();
		Sound.setRelativePosition(menuMusicID, new Vec3(0));
		Sound.setGain(menuMusicID, 0.3f);
	}

	@Override
	public void kill() {
		this.perspectiveScreen.kill();
		this.uiScreen.kill();

		this.menuMusic.kill();
	}

	private void drawMainMenu() {
		// -- DYNAMIC UI --
		this.clearScene(DYNAMIC_UI_SCENE);
		Button hostGame = new Button(150, 0, 200, 30, "btn_host_game", "Host Game", FontUtils.CSGOFont, 32, DYNAMIC_UI_SCENE);
		hostGame.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_BOTTOM);
		hostGame.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_BOTTOM);

		Button joinGame = new Button(150, 40, 200, 30, "btn_join_game", "Join Game", FontUtils.CSGOFont, 32, DYNAMIC_UI_SCENE);
		joinGame.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_BOTTOM);
		joinGame.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_BOTTOM);

		Button settings = new Button(150, 80, 200, 30, "btn_settings", "Settings", FontUtils.CSGOFont, 32, DYNAMIC_UI_SCENE);
		settings.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_BOTTOM);
		settings.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_BOTTOM);

		Button quitGame = new Button(150, 120, 200, 30, "btn_quit_game", "Quit Game", FontUtils.CSGOFont, 32, DYNAMIC_UI_SCENE);
		quitGame.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_BOTTOM);
		quitGame.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_BOTTOM);

		TextField tfHostPort = new TextField(370, 0, 180, 30, "tf_host_port", "Port", new Font("Dialogue", Font.PLAIN, 1), 16, DYNAMIC_UI_SCENE);
		tfHostPort.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_BOTTOM);
		tfHostPort.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_BOTTOM);

		TextField tfJoinIP = new TextField(370, 40, 180, 30, "tf_join_ip", "IP", new Font("Dialogue", Font.PLAIN, 1), 16, DYNAMIC_UI_SCENE);
		tfJoinIP.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_BOTTOM);
		tfJoinIP.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_BOTTOM);

		TextField tfJoinPort = new TextField(570, 40, 180, 30, "tf_join_port", "Port", new Font("Dialogue", Font.PLAIN, 1), 16, DYNAMIC_UI_SCENE);
		tfJoinPort.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_BOTTOM);
		tfJoinPort.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_BOTTOM);

		// -- STATIC UI --
		this.clearScene(STATIC_UI_SCENE);
		FilledRectangle csgoLogo = new FilledRectangle();
		csgoLogo.setTextureMaterial(new TextureMaterial(new Texture("/cs_go_logo.png", Texture.VERTICAL_FLIP_BIT)));
		Mat4 logoMat4 = Mat4.scale(700, 150, 1).mul(Mat4.translate(new Vec3(120, Main.windowHeight / 2, 0)));
		Model.addInstance(csgoLogo, logoMat4, STATIC_UI_SCENE);
	}

	private void drawSettingsMenu() {
		// -- DYNAMIC UI --
		this.clearScene(DYNAMIC_UI_SCENE);
		Button settingsExit = new Button(100, 50, 200, 30, "btn_settings_exit", "Back", FontUtils.CSGOFont, 32, DYNAMIC_UI_SCENE);
		settingsExit.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		settingsExit.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		//InputManager.addInput("btn_settings_exit", settingsExit);

		Button settingsToggleFullscreen = new Button(100, 100, 400, 30, "btn_settings_toggle_fullscreen", "Toggle Fullscreen", FontUtils.CSGOFont, 32, DYNAMIC_UI_SCENE);
		settingsToggleFullscreen.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		settingsToggleFullscreen.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		//InputManager.addInput("btn_settings_toggle_fullscreen", settingsToggleFullscreen);

		// -- STATIC UI --
		this.clearScene(STATIC_UI_SCENE);
	}

	@Override
	public void update() {
		Input.inputsHovered(uiScreen.getEntityIDAtMouse());

		Entity.updateEntities();
		Model.updateModels();
		updateCamera();
	}

	@Override
	public void render(Framebuffer outputBuffer) {
		perspectiveScreen.renderSkybox(true);
		perspectiveScreen.renderDecals(false);
		perspectiveScreen.renderPlayermodel(false);
		perspectiveScreen.setWorldScene(BACKGROUND_SCENE);
		perspectiveScreen.render(outputBuffer);

		uiScreen.setUIScene(STATIC_UI_SCENE);
		uiScreen.render(outputBuffer);
		uiScreen.setUIScene(DYNAMIC_UI_SCENE);
		uiScreen.render(outputBuffer);
	}

	private void updateCamera() {
		perspectiveScreen.getCamera().setFacing(0, MathUtils.interpolate(0, 0, (float) Math.PI * 2f, ROTATION_TIME * 1000f, System.currentTimeMillis() - startTime));
		perspectiveScreen.getCamera().setUp(new Vec3(0, 1, 0));
	}

	@Override
	public void mousePressed(int button) {
		Input.inputsPressed(uiScreen.getEntityIDAtMouse());
	}

	@Override
	public void mouseReleased(int button) {
		Input.inputsReleased(uiScreen.getEntityIDAtMouse());
		String clickedButton = Input.getClicked();
		switch (clickedButton) {
		case "btn_host_game":
			try {
				int ip = Integer.parseInt(Input.getText("tf_host_port"));
				this.sm.switchState(new GameState(this.sm));
			}
			catch (NumberFormatException e) {
				System.err.println("BAD PORT");
			}
			break;

		case "btn_join_game":
			try {
				int ip = Integer.parseInt(Input.getText("tf_join_port"));
				String port = Input.getText("tf_join_ip");
				this.sm.switchState(new GameState(this.sm));
			}
			catch (NumberFormatException e) {
				System.err.println("BAD PORT");
			}
			break;

		case "btn_quit_game":
			Main.main.exit();
			break;

		case "btn_settings":
			this.drawSettingsMenu();
			break;

		case "btn_settings_exit":
			this.drawMainMenu();
			break;

		case "btn_settings_toggle_fullscreen":
			Main.main.toggleFullscreen();
			break;

		}

	}

	@Override
	public void keyPressed(int key) {

	}

	@Override
	public void keyReleased(int key) {

	}

}
