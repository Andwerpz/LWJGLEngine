package state;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_M;

import java.util.ArrayList;
import java.util.HashMap;

import entity.Capsule;
import entity.Entity;
import graphics.Framebuffer;
import graphics.Material;
import graphics.TextureMaterial;
import input.InputManager;
import input.KeyboardInput;
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
import server.GameClient;
import server.GameServer;
import util.Mat4;
import util.MathUtils;
import util.NetworkingUtils;
import util.Pair;
import util.Vec3;
import util.Vec4;

public class GameState extends State {

	private static final int WORLD_SCENE = 0;

	private static final int DECAL_SCENE = 1; // screen space decals

	private String ip;
	private int port;

	private boolean hosting;

	private static PerspectiveScreen perspectiveScreen;
	private static Camera perspectiveCamera;

	private Player player;

	// -- NETWORKING --
	private GameClient client;
	private GameServer server;
	private HashMap<Integer, Capsule> otherPlayers;

	private long mapID;

	private Model bloodDecal, bulletHoleDecal;

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
			perspectiveCamera = new Camera(Main.FOV, Main.windowWidth, Main.windowHeight, Main.NEAR, Main.FAR);
			perspectiveScreen = new PerspectiveScreen();
			perspectiveScreen.setCamera(perspectiveCamera);
		}

		this.bulletHoleDecal = new Decal();
		this.bulletHoleDecal.setTextureMaterial(new TextureMaterial(AssetManager.getTexture("bullet_hole_texture")));

		this.bloodDecal = new Decal();
		this.bloodDecal.setTextureMaterial(new TextureMaterial(AssetManager.getTexture("blood_splatter_texture")));

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

	@Override
	public void update() {
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
		ArrayList<Pair<Integer, Vec3[]>> bulletRays = this.client.getBulletRays();
		for (Pair<Integer, Vec3[]> p : bulletRays) {
			int clientID = p.first;
			Vec3 ray_origin = p.second[0];
			Vec3 ray_dir = p.second[1];

			boolean playerIntersect = false;
			//check against other players
			for (int ID : this.otherPlayers.keySet()) {
				System.out.println(ID);
				if (clientID == ID) { //a player can't hit themselves with their own bullet
					System.out.println("OWN BULLET");
					continue;
				}
				Capsule c = this.otherPlayers.get(ID);
				if (MathUtils.ray_capsuleIntersect(ray_origin, ray_dir, c.getBottom(), c.getTop(), c.getRadius()) != null) {
					playerIntersect = true;
					break;
				}
			}

			//did the bullet hit yourself?
			if (clientID != this.client.getID()) {
				if (MathUtils.ray_capsuleIntersect(ray_origin, ray_dir, player.getBottom(), player.getTop(), player.getRadius()) != null) {
					playerIntersect = true;
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

				if (playerIntersect) {
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

			}

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
	}

	private void updateCamera() {
		perspectiveCamera.setFacing(player.camXRot, player.camYRot);
		perspectiveCamera.setPos(player.pos.add(Player.cameraVec).sub(perspectiveCamera.getFacing().mul(0f)));
		perspectiveCamera.setUp(new Vec3(0, 1, 0));
	}

	@Override
	public void mousePressed(int button) {
		// shoot ray in direction of camera
		Vec3 ray_origin = perspectiveCamera.getPos();
		Vec3 ray_dir = perspectiveCamera.getFacing();
		this.client.addBulletRay(ray_origin, ray_dir);

		//		ArrayList<Vec3[]> intersect = Model.rayIntersect(WORLD_SCENE, ray_origin, ray_dir);
		//		if (intersect.size() != 0) {
		//			float minDist = 0f;
		//			Vec3 minVec = null;
		//			Vec3 normal = null;
		//			for (Vec3[] a : intersect) {
		//				Vec3 v = a[0];
		//				float dist = v.sub(ray_origin).length();
		//				if (dist < minDist || minVec == null) {
		//					minDist = dist;
		//					minVec = v;
		//					normal = a[1];
		//				}
		//			}
		//
		//			float yRot = (float) (Math.atan2(normal.z, normal.x) - Math.PI / 2f);
		//			normal.rotateY(-yRot);
		//			float xRot = (float) (Math.atan2(normal.y, normal.z));
		//			normal.rotateX(-xRot);
		//
		//			System.out.println(normal);
		//
		//			Mat4 modelMat4 = Mat4.scale((float) (Math.random() * 1f + 0.5f));
		//			modelMat4.muli(Mat4.translate(new Vec3(0, 0, 0.001f)));
		//			modelMat4.muli(Mat4.rotateZ((float) (Math.random() * Math.PI * 2f)));
		//			modelMat4.muli(Mat4.rotateX(xRot));
		//			modelMat4.muli(Mat4.rotateY(yRot));
		//			modelMat4.muli(Mat4.translate(minVec));
		//			long id = Model.addInstance(this.bloodDecal, modelMat4, WORLD_SCENE);
		//			Model.updateInstance(id, new Material(new Vec4(1), new Vec4(0.7f), 64f));
		//		}

		//		Vec3 cam_pos = new Vec3(perspectiveCamera.getPos());
		//		Vec3 cam_dir = new Vec3(perspectiveCamera.getFacing());
		//		
		//		//Capsule c = new Capsule(cam_pos, cam_dir.mul(0.3f), 0.25f, 1f, WORLD_SCENE);
		//		Ball b = new Ball(cam_pos, cam_dir.mul(0.3f), 0.3f, WORLD_SCENE);
		//		System.out.println(cam_pos + " " + cam_dir.mul(0.6f));
	}

	@Override
	public void mouseReleased(int button) {
		// TODO Auto-generated method stub

	}

}