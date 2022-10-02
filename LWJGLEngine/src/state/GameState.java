package state;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_M;

import java.awt.Font;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;
import java.util.Stack;

import entity.Capsule;
import entity.Entity;
import graphics.Framebuffer;
import graphics.Material;
import graphics.TextureMaterial;
import input.InputManager;
import input.KeyboardInput;
import input.MouseInput;
import main.Main;
import model.AssetManager;
import model.Decal;
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
import ui.FilledRectangle;
import ui.Text;
import ui.UIElement;
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
	private static final int UI_SCENE = 2;

	private static final int DECAL_LIMIT = 1000;
	private Queue<Long> decalIDs;

	private String ip;
	private int port;

	private boolean hosting;

	private static PerspectiveScreen perspectiveScreen;
	private static UIScreen uiScreen;

	private Player player;

	// -- NETWORKING --
	private GameClient client;
	private GameServer server;
	private HashMap<Integer, Capsule> otherPlayers;

	private long mapID;

	private Model bloodDecal, bulletHoleDecal;
	private ArrayList<Pair<Integer, Vec3[]>> bulletRays;

	private boolean leftMouse = false;
	private boolean rightMouse = false;

	private static final int MAX_HEALTH = 100;
	private Text healthText;
	private int health = 100;

	private Text magazineAmmoText, reserveAmmoText;
	private int magazineSize = 30;
	private int magazineAmmo = 30;
	private int reserveAmmo = 90;

	private long fireDelayMillis = 100;
	private long fireMillisCounter = 0;

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
	public void load() {
		if (perspectiveScreen == null) {
			perspectiveScreen = new PerspectiveScreen();
		}

		if (uiScreen == null) {
			uiScreen = new UIScreen();
		}

		this.bulletHoleDecal = new Decal();
		this.bulletHoleDecal.setTextureMaterial(new TextureMaterial(AssetManager.getTexture("bullet_hole_texture")));

		this.bloodDecal = new Decal();
		this.bloodDecal.setTextureMaterial(new TextureMaterial(AssetManager.getTexture("blood_splatter_texture")));

		this.decalIDs = new ArrayDeque<>();

		Main.lockCursor();
		InputManager.removeAllInputs();
		Entity.killAll();

		// -- WORLD SCENE --
		Model.removeInstancesFromScene(WORLD_SCENE);
		Light.removeLightsFromScene(WORLD_SCENE);
		this.mapID = Model.addInstance(AssetManager.getModel("dust2"), Mat4.rotateX((float) Math.toRadians(90)).mul(Mat4.scale((float) 0.05)), WORLD_SCENE);
		Model.activateCollisionMesh(this.mapID);
		Light.addLight(WORLD_SCENE, new DirLight(new Vec3(0.3f, -1f, -0.5f), new Vec3(0.8f), 0.3f));
		Scene.skyboxes.put(WORLD_SCENE, AssetManager.getSkybox("lake_skybox"));
		player = new Player(new Vec3(18.417412f, 0.7f, -29.812654f), WORLD_SCENE);

		// -- DECAL SCENE --
		Model.removeInstancesFromScene(DECAL_SCENE);
		Light.removeLightsFromScene(DECAL_SCENE);
		this.bulletRays = new ArrayList<>();

		// -- UI SCENE --
		Model.removeInstancesFromScene(UI_SCENE);
		Light.removeLightsFromScene(UI_SCENE);

		//crosshair
		long crosshairRect1ID = FilledRectangle.addRectangle(Main.windowWidth / 2 - 1, Main.windowHeight / 2 - 6, 2, 12, UI_SCENE);
		long crosshairRect2ID = FilledRectangle.addRectangle(Main.windowWidth / 2 - 6, Main.windowHeight / 2 - 1, 12, 2, UI_SCENE);
		Material crosshairMaterial = new Material(new Vec4(0, 1, 0, 0.5f));
		Model.updateInstance(crosshairRect1ID, crosshairMaterial);
		Model.updateInstance(crosshairRect2ID, crosshairMaterial);

		this.healthText = new Text(20, 20, this.health + "", FontUtils.segoe_ui.deriveFont(Font.BOLD, 36), new Material(new Vec4(1)), UI_SCENE);
		this.healthText.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_BOTTOM);

		this.reserveAmmoText = new Text(Main.windowWidth - 20, 20, this.reserveAmmo + "", FontUtils.segoe_ui.deriveFont(Font.BOLD, 24), new Material(new Vec4(1)), UI_SCENE);
		this.reserveAmmoText.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_BOTTOM);

		this.magazineAmmoText = new Text(Main.windowWidth - 60, 20, this.magazineAmmo + "", FontUtils.segoe_ui.deriveFont(Font.BOLD, 36), new Material(new Vec4(1)), UI_SCENE);
		this.magazineAmmoText.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_BOTTOM);

		UIElement.alignAllUIElements();

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
		return this.fireMillisCounter > this.fireDelayMillis && this.magazineAmmo > 0;
	}

	private void shoot() {
		if (this.canShoot()) {
			this.fireMillisCounter %= this.fireDelayMillis;

			// shoot ray in direction of camera
			Vec3 ray_origin = perspectiveScreen.getCamera().getPos();
			Vec3 ray_dir = perspectiveScreen.getCamera().getFacing();
			this.client.addBulletRay(ray_origin, ray_dir);
			this.bulletRays.add(new Pair<Integer, Vec3[]>(-1, new Vec3[] { ray_origin, ray_dir }));

			//update ammo info
			this.magazineAmmo--;
			this.magazineAmmoText.setText(this.magazineAmmo + "");
		}
	}

	@Override
	public void update() {
		// -- SHOOTING --
		this.fireMillisCounter += Main.main.deltaMillis;
		if (this.leftMouse) {
			this.shoot();
		}

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

		// update player information
		HashMap<Integer, Vec3> otherPlayerPositions = this.client.getOtherPlayerPositions();
		for (int ID : otherPlayerPositions.keySet()) {
			Vec3 pos = otherPlayerPositions.get(ID);
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

		// process decals from bullets
		this.bulletRays.addAll(this.client.getBulletRays());
		for (Pair<Integer, Vec3[]> p : this.bulletRays) {
			int clientID = p.first;
			Vec3 ray_origin = p.second[0];
			Vec3 ray_dir = p.second[1];

			if (clientID == this.client.getID()) { //already processed when it was initially shot
				continue;
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
				float maxBloodSplatterDist = 1f;
				float maxPlayerIntersectDist = -100f; //that is less than the minimum wall intersection dist, since wallbangs aren't a thing
				for (Vec3 v : playerIntersections) {
					float dist = new Vec3(ray_origin, v).length();
					if (dist < minDist) {
						maxPlayerIntersectDist = Math.max(minDist, maxPlayerIntersectDist);
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
					Model.updateInstance(bloodSplatterID, new Material(new Vec4(1), new Vec4(0.7f), 256f));
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
		Entity.updateEntities();
		Model.updateModels();
		updateCamera();

		this.client.setPos(this.player.pos);

		// input
		if (KeyboardInput.isKeyPressed(GLFW_KEY_M)) {
			this.disconnect();
			this.stopHosting();
			this.sm.switchState(new MainMenuState(this.sm));
		}
	}

	@Override
	public void render(Framebuffer outputBuffer) {
		perspectiveScreen.renderSkybox(true);
		perspectiveScreen.setWorldScene(WORLD_SCENE);
		perspectiveScreen.setDecalScene(DECAL_SCENE);
		perspectiveScreen.render(outputBuffer);

		uiScreen.setUIScene(UI_SCENE);
		uiScreen.render(outputBuffer);
	}

	private void updateCamera() {
		perspectiveScreen.getCamera().setFacing(player.camXRot, player.camYRot);
		perspectiveScreen.getCamera().setPos(player.pos.add(Player.cameraVec).sub(perspectiveScreen.getCamera().getFacing().mul(0f))); //last part is for third person
		perspectiveScreen.getCamera().setUp(new Vec3(0, 1, 0));
	}

	@Override
	public void mousePressed(int button) {
		if (button == MouseInput.LEFT_MOUSE_BUTTON) {
			this.leftMouse = true;
		}
		else if (button == MouseInput.RIGHT_MOUSE_BUTTON) {
			this.rightMouse = true;
		}
	}

	@Override
	public void mouseReleased(int button) {
		if (button == MouseInput.LEFT_MOUSE_BUTTON) {
			this.leftMouse = false;
		}
		else if (button == MouseInput.RIGHT_MOUSE_BUTTON) {
			this.rightMouse = false;
		}
	}

}