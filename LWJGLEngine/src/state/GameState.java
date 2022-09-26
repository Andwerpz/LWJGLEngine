package state;

import static org.lwjgl.glfw.GLFW.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import entity.Ball;
import entity.Capsule;
import entity.Entity;
import graphics.Framebuffer;
import graphics.Texture;
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
import server.GameClient;
import server.GameServer;
import util.Mat4;
import util.NetworkingUtils;
import util.Vec3;

public class GameState extends State {

    private String ip;
    private int port;
    
    private boolean hosting;

    private static Screen perspectiveScreen;
    private static Camera perspectiveCamera;

    private Player player;

    // -- NETWORKING --
    private GameClient client;
    private GameServer server;

    private HashMap<Integer, Capsule> otherPlayers;

    private long mapID;

    public GameState(StateManager sm, String ip, int port, boolean hosting) {
	super(sm);
	this.ip = ip;
	this.port = port;
	this.hosting = hosting;
	
	if(this.hosting) {
	    this.ip = NetworkingUtils.getLocalIP();
	}
    }

    @Override
    public void load() {
	if (perspectiveScreen == null) {
	    perspectiveCamera = new Camera(Main.FOV, (float) Main.windowWidth, (float) Main.windowHeight, Main.NEAR, Main.FAR);
	    perspectiveScreen = new PerspectiveScreen();
	    perspectiveScreen.setCamera(perspectiveCamera);
	}

	Main.lockCursor();
	InputManager.removeAllInputs();
	Entity.killAll();

	// -- WORLD SCENE --
	Model.removeInstancesFromScene(Scene.WORLD_SCENE);
	Light.removeLightsFromScene(Scene.WORLD_SCENE);
	this.mapID = Model.addInstance(AssetManager.getModel("dust2"), Mat4.rotateX((float) Math.toRadians(90)).mul(Mat4.scale((float) 0.05)), Scene.WORLD_SCENE);
	Model.activateCollisionMesh(this.mapID);
	Light.addLight(Scene.WORLD_SCENE, new DirLight(new Vec3(0.3f, -1f, -0.5f), new Vec3(0.8f), 0.3f));
	Scene.skyboxes.put(Scene.WORLD_SCENE, AssetManager.getSkybox("lake_skybox"));
	player = new Player(new Vec3(18.417412f, 0.7f, -29.812654f), Scene.WORLD_SCENE);

	// -- NETWORKING --
	this.client = new GameClient();
	this.otherPlayers = new HashMap<>();
	
	if(this.hosting) {
	    this.server = new GameServer(this.ip, this.port);
	}
	
	for(int i = 0; i < 3; i++) {
	    if(this.client.connect(this.ip, this.port)) {
		break;
	    }
	}
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
		this.otherPlayers.put(ID, new Capsule(pos, new Vec3(0), 0.33f, 1f, Scene.WORLD_SCENE));
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

	// -- UPDATES --
	Entity.updateEntities();
	Model.updateModels();
	updateCamera();

	this.client.setPos(this.player.pos);

	// input
	if (KeyboardInput.isKeyPressed(GLFW_KEY_M)) {
	    this.sm.switchState(new MainMenuState(this.sm));
	}
    }

    @Override
    public void render(Framebuffer outputBuffer) {
	perspectiveScreen.render(outputBuffer, Scene.WORLD_SCENE);
    }

    private void updateCamera() {
	perspectiveCamera.setFacing(player.camXRot, player.camYRot);
	perspectiveCamera.setPos(player.pos.add(Player.cameraVec).sub(perspectiveCamera.getFacing().mul(0f)));
	perspectiveCamera.setUp(new Vec3(0, 1, 0));
    }

    @Override
    public void mousePressed(int button) {
	// shoot ray in direction of camera
//		Vec3 ray_origin = perspectiveCamera.getPos();
//		Vec3 ray_dir = perspectiveCamera.getFacing();
//		ArrayList<Vec3> intersect = Model.rayIntersect(Scene.WORLD_SCENE, ray_origin, ray_dir);
//		if(intersect.size() != 0) {
//			float minDist = 0f;
//			Vec3 minVec = null;
//			for(Vec3 v : intersect) {
//				float dist = v.sub(ray_origin).length();
//				if(dist < minDist || minVec == null) {
//					minDist = dist;
//					minVec = v;
//				}
//			}
//			Model.addInstance(AssetManager.getModel("sphere"), Mat4.scale(0.1f).mul(Mat4.translate(minVec)), Scene.WORLD_SCENE);
//		}

//		Vec3 cam_pos = new Vec3(perspectiveCamera.getPos());
//		Vec3 cam_dir = new Vec3(perspectiveCamera.getFacing());
//		
//		//Capsule c = new Capsule(cam_pos, cam_dir.mul(0.3f), 0.25f, 1f, Scene.WORLD_SCENE);
//		Ball b = new Ball(cam_pos, cam_dir.mul(0.3f), 0.3f, Scene.WORLD_SCENE);
//		System.out.println(cam_pos + " " + cam_dir.mul(0.6f));
    }

    @Override
    public void mouseReleased(int button) {
	// TODO Auto-generated method stub

    }

}
