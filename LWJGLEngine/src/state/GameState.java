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
import weapon.AK47;
import weapon.AWP;
import weapon.Deagle;
import weapon.M4A4;
import weapon.Usps;
import weapon.Weapon;

public class GameState extends State {

	private static final int WORLD_SCENE = 0;
	private static final int PLAYERMODEL_SCENE = 1;
	private static final int DECAL_SCENE = 2; // screen space decals

	private static final int UI_SCENE = 3;

	private static final int PAUSE_SCENE_STATIC = 4;
	private static final int PAUSE_SCENE_DYNAMIC = 5;

	private static final int BUY_SCENE_STATIC = 6;
	private static final int BUY_SCENE_DYNAMIC = 7;

	private static final int WEAPON_PREVIEW_SCENE = 8;

	private static final int DECAL_LIMIT = 1000;
	private Queue<Long> decalIDs;

	private static final int FOOTSTEP_TYPE_STEP = 0;
	private static final int FOOTSTEP_TYPE_LANDING = 1;

	private String ip;
	private int port;

	private boolean hosting;

	private PerspectiveScreen perspectiveScreen;
	private UIScreen uiScreen;

	private boolean buyMenuActive = false;

	private PerspectiveScreen weaponPreviewScreen;
	private Framebuffer weaponPreviewBuffer;
	private Texture weaponPreviewColorMap;
	private FilledRectangle weaponPreviewRectangle;
	private String weaponPreviewModelName;
	private long weaponPreviewModelID;
	private float weaponPreviewYRotDegrees = 90f;
	private Text weaponPreviewDescription;

	private Player player;

	// -- NETWORKING --
	private GameClient client;
	private GameServer server;
	private HashMap<Integer, Capsule> otherPlayers;

	private long mapID;

	private Model bloodDecal, bulletHoleDecal;
	private ArrayList<Pair<Integer, Pair<Integer, Vec3[]>>> bulletRays;

	private boolean playerControlsDisabled = false;
	private boolean leftMouse = false;
	private boolean rightMouse = false;

	private boolean pauseMenuActive = false;

	private ArrayList<Pair<Text, Long>> killfeed; //text, start time millis
	private long killfeedActiveMillis = 5000;
	private int killfeedFontSize = 18;
	private int killfeedCellGap = 5;
	private int killfeedMargin = 10;

	private static final int MAX_HEALTH = 100;
	private Text healthText;
	private int health = 100;
	private boolean isDead = false;

	private Text reserveAmmoText, magazineAmmoText;

	private long playermodelID;
	private boolean playermodelLeftHanded = false;

	private ArrayList<Pair<Text, Long>> serverMessages;
	private long serverMessageActiveMillis = 10000;
	private int serverMessagesVerticalGap = 5;
	private int serverMessagesHorizontalGap = 20;

	private Weapon weapon;

	private static int numFootstepSounds = 16;
	private Sound[] footstepSounds;
	private Vec3 lastFootstepPos = new Vec3(0);

	private static int numLandingSounds = 4;
	private Sound[] landingSounds;

	public GameState(StateManager sm, String ip, int port, boolean hosting) {
		super(sm);
		this.ip = ip;
		this.port = port;
		this.hosting = hosting;

		if (this.hosting) {
			this.ip = NetworkingUtils.getLocalIP();
		}
	}

	@Override
	public void kill() {
		this.perspectiveScreen.kill();
		this.uiScreen.kill();
		this.weaponPreviewScreen.kill();

		this.weaponPreviewBuffer.kill();
		this.weaponPreviewRectangle.kill();

		this.disconnect();
		this.stopHosting();
	}

	@Override
	public void load() {
		this.perspectiveScreen = new PerspectiveScreen();
		this.uiScreen = new UIScreen();
		this.weaponPreviewScreen = new PerspectiveScreen();

		this.bulletHoleDecal = new Decal();
		this.bulletHoleDecal.setTextureMaterial(new TextureMaterial(AssetManager.getTexture("bullet_hole_texture")));

		this.bloodDecal = new Decal();
		this.bloodDecal.setTextureMaterial(new TextureMaterial(AssetManager.getTexture("blood_splatter_texture")));

		this.decalIDs = new ArrayDeque<>();
		this.killfeed = new ArrayList<>();
		this.serverMessages = new ArrayList<>();

		this.footstepSounds = new Sound[numFootstepSounds];
		for (int i = 0; i < numFootstepSounds; i++) {
			this.footstepSounds[i] = new Sound("/footstep/concrete" + (i + 1) + ".ogg", false);
		}

		this.landingSounds = new Sound[numLandingSounds];
		for (int i = 0; i < numLandingSounds; i++) {
			this.landingSounds[i] = new Sound("/land/land" + (i + 1) + ".ogg", false);
		}

		Main.lockCursor();
		Entity.killAll();

		// -- WORLD SCENE --
		this.clearScene(WORLD_SCENE);
		this.mapID = Model.addInstance(AssetManager.getModel("dust2"), Mat4.rotateX((float) Math.toRadians(90)).mul(Mat4.scale((float) 0.05)), WORLD_SCENE);
		Model.activateCollisionMesh(this.mapID);
		Light.addLight(WORLD_SCENE, new DirLight(new Vec3(0.3f, -1f, -0.5f), new Vec3(0.8f), 0.3f));
		Scene.skyboxes.put(WORLD_SCENE, AssetManager.getSkybox("lake_skybox"));
		player = new Player(new Vec3(0), WORLD_SCENE);

		// -- PLAYERMODEL SCENE --
		this.clearScene(PLAYERMODEL_SCENE);
		Light.addLight(PLAYERMODEL_SCENE, new DirLight(new Vec3(0.3f, -1f, -0.5f), new Vec3(0.8f), 0.3f));
		this.weapon = new AWP();
		this.playermodelID = Model.addInstance(AssetManager.getModel(this.weapon.getModelName()), Mat4.identity(), PLAYERMODEL_SCENE);

		// -- DECAL SCENE --
		this.clearScene(DECAL_SCENE);
		this.bulletRays = new ArrayList<>();

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

		this.healthText = new Text(20, 20, this.health + "", FontUtils.segoe_ui.deriveFont(Font.BOLD, 36), new Material(new Vec4(1)), UI_SCENE);
		this.healthText.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_BOTTOM);
		this.healthText.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_BOTTOM);

		this.reserveAmmoText = new Text(20, 20, this.weapon.getReserveAmmo() + "", FontUtils.segoe_ui.deriveFont(Font.BOLD, 24), new Material(new Vec4(1)), UI_SCENE);
		this.reserveAmmoText.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_BOTTOM);
		this.reserveAmmoText.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_BOTTOM);

		this.magazineAmmoText = new Text(60, 20, this.weapon.getMagazineAmmo() + "", FontUtils.segoe_ui.deriveFont(Font.BOLD, 36), new Material(new Vec4(1)), UI_SCENE);
		this.magazineAmmoText.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_BOTTOM);
		this.magazineAmmoText.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_BOTTOM);

		// -- PAUSE SCENE --
		this.drawPauseMenu();

		// -- BUY MENU --
		this.drawBuyMenu();

		// -- WEAPON PREVIEW --
		this.clearScene(WEAPON_PREVIEW_SCENE);

		this.weaponPreviewBuffer = new Framebuffer(Main.windowWidth, Main.windowHeight);
		this.weaponPreviewColorMap = new Texture(GL_RGBA, Main.windowWidth, Main.windowHeight, GL_RGBA, GL_FLOAT);
		this.weaponPreviewBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.weaponPreviewColorMap.getID());
		this.weaponPreviewBuffer.setDrawBuffers(new int[] { GL_COLOR_ATTACHMENT0 });
		this.weaponPreviewBuffer.isComplete();

		this.weaponPreviewScreen.setWorldCameraFOV(15f);

		TextureMaterial weaponPreviewTexture = new TextureMaterial(weaponPreviewColorMap);
		this.weaponPreviewRectangle = new FilledRectangle();
		this.weaponPreviewRectangle.setTextureMaterial(weaponPreviewTexture);

		UIFilledRectangle weaponPreviewUIRectangle = new UIFilledRectangle(0, 0, 0, 640, 360, weaponPreviewRectangle, BUY_SCENE_DYNAMIC);
		weaponPreviewUIRectangle.setFrameAlignmentStyle(UIElement.FROM_CENTER_RIGHT, UIElement.FROM_CENTER_TOP);
		weaponPreviewUIRectangle.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_BOTTOM);

		this.weaponPreviewModelName = "";
		Light.addLight(WEAPON_PREVIEW_SCENE, new DirLight(new Vec3(0.3f, -1f, -0.5f), new Vec3(1f), 0.3f));

		// -- NETWORKING --
		this.client = new GameClient();
		this.otherPlayers = new HashMap<>();

		if (this.hosting) {
			this.startHosting();
		}

		for (int i = 0; i < 3; i++) {
			if (this.connect()) {
				break;
			}
		}
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

	private void toggleBuyMenu() {
		if (this.buyMenuActive) {
			this.buyMenuActive = false;
			Main.lockCursor();
			this.enablePlayerControls();
		}
		else {
			this.buyMenuActive = true;
			Main.unlockCursor();
			this.disablePlayerControls();
		}
	}

	private void drawBuyMenu() {
		// -- STATIC --
		this.clearScene(BUY_SCENE_STATIC);
		UIFilledRectangle backgroundRect = new UIFilledRectangle(0, 0, 0, 1920, 1080, BUY_SCENE_STATIC);
		backgroundRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_BOTTOM);
		backgroundRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_BOTTOM);
		backgroundRect.setMaterial(new Material(new Vec4(0, 0, 0, 0.5f)));

		// -- DYNAMIC --
		this.clearScene(BUY_SCENE_DYNAMIC);
		Button buyAK47Button = new Button(10, 10, 200, 30, "btn_buy_ak47", "AK47", FontUtils.CSGOFont, 32, BUY_SCENE_DYNAMIC);
		buyAK47Button.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_TOP);
		buyAK47Button.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_BOTTOM);

		Button buyM4A4Button = new Button(10, 50, 200, 30, "btn_buy_m4a4", "M4A4", FontUtils.CSGOFont, 32, BUY_SCENE_DYNAMIC);
		buyM4A4Button.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_TOP);
		buyM4A4Button.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_BOTTOM);

		Button buyUspsButton = new Button(10, 90, 200, 30, "btn_buy_usps", "USPS", FontUtils.CSGOFont, 32, BUY_SCENE_DYNAMIC);
		buyUspsButton.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_TOP);
		buyUspsButton.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_BOTTOM);

		Button buyAWPButton = new Button(10, 130, 200, 30, "btn_buy_awp", "AWP", FontUtils.CSGOFont, 32, BUY_SCENE_DYNAMIC);
		buyAWPButton.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_TOP);
		buyAWPButton.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_BOTTOM);

		Button buyDeagleButton = new Button(10, 170, 200, 30, "btn_buy_deagle", "Deagle", FontUtils.CSGOFont, 32, BUY_SCENE_DYNAMIC);
		buyDeagleButton.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_TOP);
		buyDeagleButton.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_BOTTOM);

		this.weaponPreviewDescription = new Text(10, 10, 0, 640 - 20, " ", FontUtils.segoe_ui, 20, Color.WHITE, BUY_SCENE_DYNAMIC);
		this.weaponPreviewDescription.setFrameAlignmentStyle(UIElement.FROM_CENTER_RIGHT, UIElement.FROM_CENTER_BOTTOM);
		this.weaponPreviewDescription.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		this.weaponPreviewDescription.setTextWrapping(true);
	}

	private void startHosting() {
		this.server = new GameServer(this.ip, this.port);
	}

	private void stopHosting() {
		if (this.server == null) {
			return;
		}
		this.server.exit();
	}

	private boolean connect() {
		return this.client.connect(this.ip, this.port);
	}

	private void disconnect() {
		this.client.disconnect();
	}

	private boolean canShoot() {
		return this.weapon.canShoot() && !this.pauseMenuActive && !this.isDead && !this.playerControlsDisabled;
	}

	private void shoot() {
		if (this.canShoot()) {
			Vec3 ray_dir = this.weapon.shoot(this.player);
			Vec3 ray_origin = perspectiveScreen.getCamera().getPos();

			this.client.addBulletRay(this.weapon, ray_origin, ray_dir);
			this.bulletRays.add(new Pair<Integer, Pair<Integer, Vec3[]>>(-1, new Pair<Integer, Vec3[]>(this.weapon.getWeaponID(), new Vec3[] { ray_origin, ray_dir })));
			this.computeBulletDamage(ray_origin, ray_dir);

			int firingSoundID = this.weapon.getFiringSound().addSource();
			Sound.setGain(firingSoundID, 3);
			Sound.setReferenceDistance(firingSoundID, 2);
			Sound.setRelativePosition(firingSoundID, new Vec3(0, 0, -1));
			Sound.setRolloffFactor(firingSoundID, 2f);
		}
	}

	private void updateHud() {
		this.magazineAmmoText.setText(this.weapon.getMagazineAmmo() + "");
		this.reserveAmmoText.setText(this.weapon.getReserveAmmo() + "");

		this.healthText.setText(this.health + "");

		while (this.killfeed.size() != 0 && System.currentTimeMillis() - this.killfeed.get(this.killfeed.size() - 1).second >= this.killfeedActiveMillis) {
			Text t = this.killfeed.remove(this.killfeed.size() - 1).first;
			t.kill();
		}

		//realign elements in killfeed
		int yPtr = this.killfeedCellGap;
		for (Pair<Text, Long> p : this.killfeed) {
			Text t = p.first;
			t.setFrameAlignmentOffset(this.killfeedCellGap, yPtr);
			t.align();
			yPtr += this.killfeedCellGap + t.getHeight();
		}

		while (this.serverMessages.size() != 0 && System.currentTimeMillis() - this.serverMessages.get(this.serverMessages.size() - 1).second >= this.serverMessageActiveMillis) {
			Text t = this.serverMessages.remove(this.serverMessages.size() - 1).first;
			t.kill();
		}

		//server messages
		yPtr = 60;
		for (Pair<Text, Long> p : this.serverMessages) {
			Text t = p.first;
			t.setFrameAlignmentOffset(this.serverMessagesHorizontalGap, yPtr);
			t.align();
			yPtr += this.serverMessagesVerticalGap + t.getHeight();
		}
	}

	private float computeClosestRayIntersectionDist(Vec3 ray_origin, Vec3 ray_dir, int scene) {
		float minDist = 0f;
		ArrayList<Vec3[]> intersect = Model.rayIntersect(scene, ray_origin, ray_dir);
		if (intersect.size() != 0) {
			minDist = -1f;
			for (Vec3[] a : intersect) {
				Vec3 v = a[0];
				float dist = v.sub(ray_origin).length();
				if (dist < minDist || minDist == -1f) {
					minDist = dist;
				}
			}
		}
		return minDist;
	}

	private void computeBulletDamage(Vec3 ray_origin, Vec3 ray_dir) {
		float mapIntersectDist = computeClosestRayIntersectionDist(ray_origin, ray_dir, WORLD_SCENE);

		for (int ID : this.otherPlayers.keySet()) {
			Capsule c = this.otherPlayers.get(ID);
			Vec3 intersect = MathUtils.ray_capsuleIntersect(ray_origin, ray_dir, c.getBottom(), c.getTop(), c.getRadius());
			if (intersect == null) {
				continue;
			}

			float capsuleIntersectDist = new Vec3(ray_origin, intersect).length();
			if (mapIntersectDist > capsuleIntersectDist) {
				//scale damage
				int damage = this.weapon.getDamage(capsuleIntersectDist);
				this.client.addDamageSource(ID, damage);
			}
		}
	}

	private void respawn() {
		this.player.setPos(this.client.getRespawnPos());
		this.player.setVel(new Vec3(0));
		this.health = MAX_HEALTH;
		this.isDead = false;

		this.weapon.resetAmmo();

		this.client.respawn(this.health);
		this.enablePlayerControls();
	}

	private void enablePlayerControls() {
		if (!this.isDead && !this.pauseMenuActive && !this.buyMenuActive) {
			this.player.setAcceptPlayerInputs(true);
			this.playerControlsDisabled = false;
		}
	}

	private void disablePlayerControls() {
		this.player.setAcceptPlayerInputs(false);
		this.playerControlsDisabled = true;
	}

	private void switchWeapon(Weapon w) {
		Model.removeInstance(this.playermodelID);
		this.weapon = w;
		this.playermodelID = Model.addInstance(AssetManager.getModel(w.getModelName()), Mat4.identity(), PLAYERMODEL_SCENE);
	}

	@Override
	public void update() {
		// -- AUDIO --
		Sound.cullAllStoppedSources();

		//self footsteps
		if (this.player.isOnGround() && this.player.isRunning()) {
			//check if previous footstep position is far away. 
			Vec3 toPrevFootstep = new Vec3(this.player.pos, this.lastFootstepPos);
			if (toPrevFootstep.length() > (0.05f * 20)) {
				this.lastFootstepPos.set(this.player.pos);
				int footstepSoundID = this.footstepSounds[(int) (Math.random() * numFootstepSounds)].addSource();
				Sound.setGain(footstepSoundID, 0.6f);
				Sound.setReferenceDistance(footstepSoundID, 2);
				Sound.setRelativePosition(footstepSoundID, new Vec3(0, -1.5f, 0));
				Sound.setRolloffFactor(footstepSoundID, 2f);

				this.client.addFootstep(FOOTSTEP_TYPE_STEP, this.player.pos.add(new Vec3(0, -1.5f, 0)));
			}
		}
		if (this.player.vel.length() < 0.0001f) {
			this.lastFootstepPos.set(this.player.pos);
		}

		if (this.player.hasLanded()) {
			int landingSoundID = this.landingSounds[(int) (Math.random() * numLandingSounds)].addSource();
			Sound.setGain(landingSoundID, 0.6f);
			Sound.setReferenceDistance(landingSoundID, 2);
			Sound.setRelativePosition(landingSoundID, new Vec3(0, -1.5f, 0));
			Sound.setRolloffFactor(landingSoundID, 2f);

			this.client.addFootstep(FOOTSTEP_TYPE_LANDING, this.player.pos.add(new Vec3(0, -1.5f, 0)));
		}

		//others footsteps
		ArrayList<Pair<Integer, Pair<Integer, float[]>>> footsteps = this.client.getFootsteps();
		for (Pair<Integer, Pair<Integer, float[]>> p : footsteps) {
			int sourceClientID = p.first;
			int footstepType = p.second.first;
			Vec3 footstepPos = new Vec3(p.second.second);

			if (sourceClientID == this.client.getID()) { //footstep made by self. 
				continue;
			}

			int footstepSoundID = -1;
			switch (footstepType) {
			case FOOTSTEP_TYPE_STEP:
				footstepSoundID = this.footstepSounds[(int) (Math.random() * numFootstepSounds)].addSource();
				break;

			case FOOTSTEP_TYPE_LANDING:
				footstepSoundID = this.landingSounds[(int) (Math.random() * numLandingSounds)].addSource();
				break;
			}

			if (footstepSoundID == -1) {
				continue;
			}

			Sound.setGain(footstepSoundID, 0.6f);
			Sound.setReferenceDistance(footstepSoundID, 2);
			Sound.setPosition(footstepSoundID, footstepPos);
			Sound.setRolloffFactor(footstepSoundID, 4f);
		}

		// -- MENU --
		Input.inputsHovered(uiScreen.getEntityIDAtMouse());

		// -- WEAPON PREVIEW --
		Weapon nextWeapon = null;
		switch (Input.getHovered()) {
		case "btn_buy_ak47":
			nextWeapon = new AK47();
			break;

		case "btn_buy_m4a4":
			nextWeapon = new M4A4();
			break;

		case "btn_buy_usps":
			nextWeapon = new Usps();
			break;

		case "btn_buy_awp":
			nextWeapon = new AWP();
			break;

		case "btn_buy_deagle":
			nextWeapon = new Deagle();
			break;
		}
		if (nextWeapon != null && !nextWeapon.getModelName().equals(this.weaponPreviewModelName)) {
			this.weaponPreviewModelName = nextWeapon.getModelName();
			if (this.weaponPreviewModelID != 0) {
				Model.removeInstance(this.weaponPreviewModelID);
			}

			this.weaponPreviewModelID = Model.addInstance(AssetManager.getModel(this.weaponPreviewModelName), Mat4.identity(), WEAPON_PREVIEW_SCENE);
			this.weaponPreviewDescription.setText(nextWeapon.getDescription());
		}

		this.weaponPreviewYRotDegrees += (360f / 10000f) * Main.main.deltaMillis;
		if (this.weaponPreviewModelID != 0) {
			Model.updateInstance(this.weaponPreviewModelID, Mat4.rotateY((float) Math.toRadians(this.weaponPreviewYRotDegrees)).mul(Mat4.translate(new Vec3(0, 0, -4f))));
		}

		// -- KILLFEED --
		ArrayList<Pair<String, String>> newKillfeed = this.client.getKillfeed();
		for (Pair<String, String> p : newKillfeed) {
			String aggressorNick = p.first;
			String receiverNick = p.second;
			Text nextText = new Text(0, 0, aggressorNick + " killed " + receiverNick, FontUtils.segoe_ui.deriveFont(Font.PLAIN, this.killfeedFontSize), new Material(new Vec4(1)), UI_SCENE);
			nextText.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_TOP);
			nextText.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_TOP);
			nextText.setDrawBackgroundRectangle(true);
			nextText.setMargin(this.killfeedMargin);
			nextText.setBackgroundMaterial(new Material(new Vec4(0, 0, 0, 0.3f)));

			this.killfeed.add(0, new Pair<Text, Long>(nextText, System.currentTimeMillis()));
		}

		// -- SERVER MESSAGES --
		ArrayList<String> serverMessages = this.client.getServerMessages();
		for (String s : serverMessages) {
			Text t = new Text(0, 0, s, new Font("Dialogue", Font.PLAIN, 12), new Material(new Vec4(1)), UI_SCENE);
			t.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_BOTTOM);
			t.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_BOTTOM);
			this.serverMessages.add(0, new Pair<Text, Long>(t, System.currentTimeMillis()));
		}

		// -- HEALTH --
		HashMap<Integer, Integer> playerHealths = client.getPlayerHealths();
		for (int ID : playerHealths.keySet()) {
			int health = playerHealths.get(ID);
			if (ID == this.client.getID()) {
				this.health = health;
			}
		}

		this.isDead = this.health <= 0;
		if (this.isDead) {
			this.disablePlayerControls();
		}
		if (this.client.shouldRespawn()) {
			this.respawn();
		}
		this.enablePlayerControls();

		// -- SHOOTING --
		if (this.leftMouse) {
			this.shoot();
		}
		this.weapon.update(this.leftMouse);

		// -- NETWORKING --

		// check for disconnected host
		if (!this.client.isConnected()) {
			if (this.server != null && this.server.isRunning()) {
				this.server.exit();
			}
			if (this.client.isRunning()) {
				this.client.exit();
			}

			this.sm.switchState(new MainMenuState(this.sm));
		}

		// update other player positions
		HashMap<Integer, Vec3> playerPositions = this.client.getPlayerPositions();
		for (int ID : playerPositions.keySet()) {
			if (ID == this.client.getID()) {
				continue;
			}
			Vec3 pos = playerPositions.get(ID);
			if (!this.otherPlayers.keySet().contains(ID)) {
				this.otherPlayers.put(ID, new Capsule(pos, new Vec3(0), 0.33f, 1f, WORLD_SCENE));
			}
			Capsule c = this.otherPlayers.get(ID);
			c.setPos(pos);
			c.updateModelMats();
		}

		ArrayList<Integer> disconnectedPlayers = this.client.getDisconnectedPlayers();
		for (int ID : disconnectedPlayers) {
			this.otherPlayers.get(ID).kill();
			this.otherPlayers.remove(ID);
		}

		// process decals and damage from bullets
		this.bulletRays.addAll(this.client.getBulletRays());
		for (Pair<Integer, Pair<Integer, Vec3[]>> p : this.bulletRays) {
			int clientID = p.first;
			int weaponID = p.second.first;
			Vec3 ray_origin = p.second.second[0];
			Vec3 ray_dir = p.second.second[1];

			if (clientID == this.client.getID()) { //already processed when it was initially shot
				continue;
			}

			if (clientID != -1) {
				//add firing sound effect
				int firingSoundID = Weapon.getFiringSound(weaponID).addSource();
				Sound.setPosition(firingSoundID, ray_origin);
				Sound.setGain(firingSoundID, 3);
				Sound.setReferenceDistance(firingSoundID, 2);
			}

			ArrayList<Vec3> playerIntersections = new ArrayList<>();
			//check against other players
			for (int ID : this.otherPlayers.keySet()) {
				if (clientID == ID) { //a player can't hit themselves with their own bullet
					continue;
				}
				Capsule c = this.otherPlayers.get(ID);
				Vec3 intersect = MathUtils.ray_capsuleIntersect(ray_origin, ray_dir, c.getBottom(), c.getTop(), c.getRadius());
				if (intersect != null) {
					playerIntersections.add(intersect);
				}
			}

			//did the bullet hit yourself?
			if (clientID != this.client.getID() && clientID != -1) {
				Vec3 intersect = MathUtils.ray_capsuleIntersect(ray_origin, ray_dir, player.getBottom(), player.getTop(), player.getRadius());
				if (intersect != null) {
					playerIntersections.add(intersect);
				}
			}

			ArrayList<Vec3[]> intersect = Model.rayIntersect(WORLD_SCENE, ray_origin, ray_dir);
			if (intersect.size() != 0) {
				float minDist = 0f;
				Vec3 minVec = null;
				Vec3 normal = null;
				for (Vec3[] a : intersect) {
					Vec3 v = a[0];
					float dist = v.sub(ray_origin).length();
					if (dist < minDist || minVec == null) {
						minDist = dist;
						minVec = v;
						normal = a[1];
					}
				}

				float yRot = (float) (Math.atan2(normal.z, normal.x) - Math.PI / 2f);
				normal.rotateY(-yRot);
				float xRot = (float) (Math.atan2(normal.y, normal.z));
				normal.rotateX(-xRot);

				Mat4 modelMat4 = null;

				//check if player intersection is close enough to the wall intersect to splat blood
				float maxBloodSplatterDist = 4f;
				float maxPlayerIntersectDist = -100f; //that is less than the minimum wall intersection dist, since wallbangs aren't a thing
				for (Vec3 v : playerIntersections) {
					float dist = new Vec3(ray_origin, v).length();
					if (dist < minDist) {
						maxPlayerIntersectDist = Math.max(dist, maxPlayerIntersectDist);
					}
				}

				if (minDist - maxPlayerIntersectDist < maxBloodSplatterDist) {
					//blood splatter decal
					modelMat4 = Mat4.translate(new Vec3(-0.5f, -0.5f, -0.9f));
					modelMat4.muli(Mat4.scale((float) (Math.random() * 2f + 1.5f)));
					modelMat4.muli(Mat4.translate(new Vec3(0, 0, 0.0001f)));
					modelMat4.muli(Mat4.rotateZ((float) (Math.random() * Math.PI * 2f)));
					modelMat4.muli(Mat4.rotateX(xRot + (float) (Math.random() * Math.PI / 6f)));
					modelMat4.muli(Mat4.rotateY(yRot + (float) (Math.random() * Math.PI / 6f)));
					modelMat4.muli(Mat4.translate(minVec));
					long bloodSplatterID = Model.addInstance(this.bloodDecal, modelMat4, DECAL_SCENE);
					Model.updateInstance(bloodSplatterID, new Material(new Vec4(1), new Vec4(0f), 256f));
					this.decalIDs.add(bloodSplatterID);
				}

				//bullet hole decal
				modelMat4 = Mat4.translate(new Vec3(-0.5f, -0.5f, -0.95f));
				modelMat4.muli(Mat4.scale((float) (Math.random() * 0.03f + 0.1f)));
				modelMat4.muli(Mat4.translate(new Vec3(0, 0, 0.0001f)));
				modelMat4.muli(Mat4.rotateZ((float) (Math.random() * Math.PI * 2f)));
				modelMat4.muli(Mat4.rotateX(xRot));
				modelMat4.muli(Mat4.rotateY(yRot));
				modelMat4.muli(Mat4.translate(minVec));
				long bulletHoleID = Model.addInstance(this.bulletHoleDecal, modelMat4, DECAL_SCENE);
				Model.updateInstance(bulletHoleID, new Material(new Vec4(1), new Vec4(0f), 64f));
				this.decalIDs.add(bulletHoleID);
			}
		}
		this.bulletRays.clear();

		//cull old decals
		while (this.decalIDs.size() > DECAL_LIMIT) {
			long ID = this.decalIDs.poll();
			Model.removeInstance(ID);
		}

		// -- UPDATES --
		this.updateHud();
		Entity.updateEntities();

		// -- PLAYERMODEL --
		Vec3 playermodelOffset = this.weapon.getGunOffset();
		if (this.playermodelLeftHanded) {
			playermodelOffset.x = -playermodelOffset.x;
		}

		float[] rot = this.weapon.getGunRot();
		float xRot = rot[0];
		float yRot = rot[1];

		Mat4 playermodelMat4 = Mat4.scale(1f);
		playermodelMat4.muli(Mat4.rotateX(xRot));
		playermodelMat4.muli(Mat4.rotateY(yRot));
		playermodelMat4.muli(Mat4.translate(playermodelOffset));
		playermodelMat4.muli(Mat4.rotateX(this.player.camXRot));
		playermodelMat4.muli(Mat4.rotateY(this.player.camYRot));
		playermodelMat4.muli(Mat4.translate(this.player.pos.add(this.player.cameraVec).sub(this.player.vel.mul(0.5f))));
		Model.updateInstance(this.playermodelID, playermodelMat4);

		Model.updateModels();
		updateCamera();

		this.client.setPos(this.player.pos);

		// -- AUDIO --
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
		perspectiveScreen.renderPlayermodel(true);
		perspectiveScreen.setWorldScene(WORLD_SCENE);
		perspectiveScreen.setDecalScene(DECAL_SCENE);
		perspectiveScreen.setPlayermodelScene(PLAYERMODEL_SCENE);
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

		if (this.buyMenuActive) {
			this.weaponPreviewBuffer.bind();
			glClear(GL_COLOR_BUFFER_BIT);
			this.weaponPreviewScreen.renderSkybox(false);
			this.weaponPreviewScreen.renderDecals(true);
			this.weaponPreviewScreen.renderPlayermodel(false);
			this.weaponPreviewScreen.setWorldScene(WEAPON_PREVIEW_SCENE);
			this.weaponPreviewScreen.render(this.weaponPreviewBuffer);

			uiScreen.setUIScene(BUY_SCENE_STATIC);
			uiScreen.render(outputBuffer);

			uiScreen.setUIScene(BUY_SCENE_DYNAMIC);
			uiScreen.render(outputBuffer);
		}

	}

	private void updateCamera() {
		float[] offset = this.weapon.getCameraRecoilOffset();
		float xRot = offset[0];
		float yRot = offset[1];

		perspectiveScreen.getCamera().setFacing(player.camXRot + xRot, player.camYRot + yRot);
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
		case "btn_return_to_menu":
			this.disconnect();
			this.stopHosting();
			this.sm.switchState(new MainMenuState(this.sm));
			break;

		case "btn_toggle_playermodel_side":
			this.playermodelLeftHanded = !this.playermodelLeftHanded;
			break;

		case "btn_set_nickname":
			String nickname = Input.getText("tf_set_nickname");
			if (nickname.length() != 0) {
				this.client.setNickname(nickname);
			}
			break;

		case "btn_respawn":
			this.respawn();
			break;

		case "btn_buy_ak47":
			this.switchWeapon(new AK47());
			break;

		case "btn_buy_m4a4":
			this.switchWeapon(new M4A4());
			break;

		case "btn_buy_usps":
			this.switchWeapon(new Usps());
			break;

		case "btn_buy_awp":
			this.switchWeapon(new AWP());
			break;

		case "btn_buy_deagle":
			this.switchWeapon(new Deagle());
			break;

		}
	}

	@Override
	public void keyPressed(int key) {
		switch (key) {
		case GLFW_KEY_R:
			this.weapon.startReloading();
			break;

		case GLFW_KEY_ESCAPE:
			if (this.buyMenuActive) {
				this.toggleBuyMenu();
			}
			else {
				this.togglePauseMenu();
			}
			break;

		case GLFW_KEY_B:
			if (!this.pauseMenuActive) {
				this.toggleBuyMenu();
			}
			break;
		}
	}

	@Override
	public void keyReleased(int key) {

	}

}