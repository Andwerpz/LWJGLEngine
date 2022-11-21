package state;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import static org.lwjgl.openal.AL10.*;

import java.awt.Color;
import java.awt.Font;
import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;
import java.util.Stack;

import audio.Sound;
import entity.Capsule;
import entity.Entity;
import graphics.Framebuffer;
import graphics.Material;
import graphics.Texture;
import graphics.TextureMaterial;
import input.Button;
import input.Input;
import input.KeyboardInput;
import input.MouseInput;
import input.TextField;
import main.Main;
import model.AssetManager;
import model.Decal;
import model.FilledRectangle;
import model.Model;
import particle.Particle;
import player.Camera;
import player.Player;
import scene.DirLight;
import scene.Light;
import scene.Scene;
import screen.PerspectiveScreen;
import screen.Screen;
import screen.UIScreen;
import server.GameClient;
import server.GameServer;
import ui.Text;
import ui.UIElement;
import ui.UIFilledRectangle;
import util.BufferUtils;
import util.FontUtils;
import util.Mat4;
import util.MathUtils;
import util.NetworkingUtils;
import util.Pair;
import util.Vec3;
import util.Vec4;

public class GameState extends State {

	private static final int WORLD_SCENE = 0;
	private static final int DECAL_SCENE = 1; // screen space decals
	private static final int PARTICLE_SCENE = 2;

	private static final int UI_SCENE = 3;

	private static final int PAUSE_SCENE_STATIC = 4;
	private static final int PAUSE_SCENE_DYNAMIC = 5;

	private PerspectiveScreen perspectiveScreen;
	private UIScreen uiScreen;

	private Player player;

	private boolean pauseMenuActive = false;

	private boolean playerControlsDisabled = false;

	private boolean leftMouse = false;
	private boolean rightMouse = false;

	public GameState(StateManager sm) {
		super(sm);
	}

	@Override
	public void kill() {
		this.perspectiveScreen.kill();
		this.uiScreen.kill();
	}

	@Override
	public void load() {
		Main.lockCursor();
		Entity.killAll();

		this.perspectiveScreen = new PerspectiveScreen();
		this.uiScreen = new UIScreen();

		// -- WORLD SCENE --
		this.clearScene(WORLD_SCENE);
		long mapID = Model.addInstance(AssetManager.getModel("dust2"), Mat4.rotateX((float) Math.toRadians(90)).mul(Mat4.scale((float) 0.05)), WORLD_SCENE);
		Model.activateCollisionMesh(mapID);
		Light.addLight(WORLD_SCENE, new DirLight(new Vec3(0.3f, -1f, -0.5f), new Vec3(0.8f), 0.3f));
		Scene.skyboxes.put(WORLD_SCENE, AssetManager.getSkybox("lake_skybox"));
		player = new Player(new Vec3(0), WORLD_SCENE);

		// -- DECAL SCENE --
		this.clearScene(DECAL_SCENE);

		// -- UI SCENE --
		this.clearScene(UI_SCENE);
		UIFilledRectangle crosshairRect1 = new UIFilledRectangle(0, 0, 0, 12, 2, UI_SCENE);
		UIFilledRectangle crosshairRect2 = new UIFilledRectangle(0, 0, 0, 2, 12, UI_SCENE);
		crosshairRect1.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_BOTTOM);
		crosshairRect2.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_BOTTOM);
		crosshairRect1.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_CENTER);
		crosshairRect2.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_CENTER);

		Material crosshairMaterial = new Material(new Vec4(0, 1, 0, 0.5f));
		crosshairRect1.setMaterial(crosshairMaterial);
		crosshairRect2.setMaterial(crosshairMaterial);

		// -- PAUSE SCENE --
		this.drawPauseMenu();
	}

	private void togglePauseMenu() {
		if (this.pauseMenuActive) {
			this.pauseMenuActive = false;
			Main.lockCursor();
			this.enablePlayerControls();
		}
		else {
			this.pauseMenuActive = true;
			Main.unlockCursor();
			this.disablePlayerControls();
		}
	}

	private void drawPauseMenu() {
		// -- STATIC --
		this.clearScene(PAUSE_SCENE_STATIC);
		UIFilledRectangle backgroundRect = new UIFilledRectangle(0, 0, 0, 400, 350, PAUSE_SCENE_STATIC);
		backgroundRect.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_BOTTOM);
		backgroundRect.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_CENTER);
		backgroundRect.setMaterial(new Material(new Vec4(0, 0, 0, 0.25f)));

		// -- DYNAMIC --
		this.clearScene(PAUSE_SCENE_DYNAMIC);
		Button setNicknameButton = new Button(5, -40, 145, 30, "btn_set_nickname", "Set Nick", FontUtils.CSGOFont, 32, PAUSE_SCENE_DYNAMIC);
		setNicknameButton.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_BOTTOM);
		setNicknameButton.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);

		TextField setNicknameTextField = new TextField(5, -40, 145, 30, "tf_set_nickname", "Nickname", new Font("Dialogue", Font.PLAIN, 1), 16, PAUSE_SCENE_DYNAMIC);
		setNicknameTextField.setFrameAlignmentStyle(UIElement.FROM_CENTER_RIGHT, UIElement.FROM_CENTER_BOTTOM);
		setNicknameTextField.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);

		Button returnToMenu = new Button(0, 0, 300, 30, "btn_return_to_menu", "Return to Menu", FontUtils.CSGOFont, 32, PAUSE_SCENE_DYNAMIC);
		returnToMenu.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_BOTTOM);
		returnToMenu.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_CENTER);

		Button togglePlayermodelSide = new Button(0, 40, 300, 30, "btn_toggle_playermodel_side", "Toggle Handedness", FontUtils.CSGOFont, 32, PAUSE_SCENE_DYNAMIC);
		togglePlayermodelSide.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_BOTTOM);
		togglePlayermodelSide.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_CENTER);

		Button respawn = new Button(0, 80, 300, 30, "btn_respawn", "Respawn", FontUtils.CSGOFont, 32, PAUSE_SCENE_DYNAMIC);
		respawn.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_BOTTOM);
		respawn.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_CENTER);
	}

	private void enablePlayerControls() {
		if (!this.pauseMenuActive) {
			this.player.setAcceptPlayerInputs(true);
			this.playerControlsDisabled = false;
		}
	}

	private void disablePlayerControls() {
		this.player.setAcceptPlayerInputs(false);
		this.playerControlsDisabled = true;
	}

	@Override
	public void update() {

		// -- MENU --
		Input.inputsHovered(uiScreen.getEntityIDAtMouse());

		// -- UPDATES --
		Entity.updateEntities();
		Model.updateModels();
		updateCamera();

		// -- AUDIO --
		Sound.cullAllStoppedSources();
		Vec3 cameraPos = this.perspectiveScreen.getCamera().getPos();
		alListener3f(AL_POSITION, cameraPos.x, cameraPos.y, cameraPos.z);
		Vec3 cameraFacing = this.perspectiveScreen.getCamera().getFacing();
		Vec3 cameraUp = new Vec3(0, 1, 0);
		FloatBuffer cameraOrientation = BufferUtils.createFloatBuffer(new float[] { cameraFacing.x, cameraFacing.y, cameraFacing.z, cameraUp.x, cameraUp.y, cameraUp.z });
		alListenerfv(AL_ORIENTATION, cameraOrientation);

	}

	@Override
	public void render(Framebuffer outputBuffer) {
		//world
		perspectiveScreen.renderSkybox(true);
		perspectiveScreen.renderDecals(true);
		perspectiveScreen.renderPlayermodel(false);
		perspectiveScreen.renderParticles(true);
		perspectiveScreen.setWorldScene(WORLD_SCENE);
		perspectiveScreen.setDecalScene(DECAL_SCENE);
		perspectiveScreen.setParticleScene(PARTICLE_SCENE);
		perspectiveScreen.render(outputBuffer);

		//ui
		uiScreen.setUIScene(UI_SCENE);
		uiScreen.render(outputBuffer);

		if (this.pauseMenuActive) {
			uiScreen.setUIScene(PAUSE_SCENE_STATIC);
			uiScreen.render(outputBuffer);

			uiScreen.setUIScene(PAUSE_SCENE_DYNAMIC);
			uiScreen.render(outputBuffer);
		}

	}

	private void updateCamera() {
		perspectiveScreen.getCamera().setFacing(player.camXRot, player.camYRot);
		perspectiveScreen.getCamera().setPos(player.pos.add(Player.cameraVec).sub(perspectiveScreen.getCamera().getFacing().mul(0f))); //last part is for third person
		perspectiveScreen.getCamera().setUp(new Vec3(0, 1, 0));
	}

	@Override
	public void mousePressed(int button) {
		Input.inputsPressed(uiScreen.getEntityIDAtMouse());
		if (button == MouseInput.LEFT_MOUSE_BUTTON) {
			this.leftMouse = true;
		}
		else if (button == MouseInput.RIGHT_MOUSE_BUTTON) {
			this.rightMouse = true;
		}
	}

	@Override
	public void mouseReleased(int button) {
		Input.inputsReleased(uiScreen.getEntityIDAtMouse());
		if (button == MouseInput.LEFT_MOUSE_BUTTON) {
			this.leftMouse = false;
		}
		else if (button == MouseInput.RIGHT_MOUSE_BUTTON) {
			this.rightMouse = false;
		}

		String clickedButton = Input.getClicked();
		switch (clickedButton) {

		}
	}

	@Override
	public void keyPressed(int key) {
		switch (key) {
		case GLFW_KEY_ESCAPE:
			this.togglePauseMenu();
			break;
		}
	}

	@Override
	public void keyReleased(int key) {

	}

}