package lwjglengine.asset;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import lwjglengine.graphics.Framebuffer;
import lwjglengine.graphics.Material;
import lwjglengine.input.MouseInput;
import lwjglengine.model.Line;
import lwjglengine.model.ModelInstance;
import lwjglengine.project.Project;
import lwjglengine.scene.Scene;
import lwjglengine.screen.UIScreen;
import lwjglengine.ui.Text;
import lwjglengine.ui.UIElement;
import lwjglengine.ui.UIFilledRectangle;
import lwjglengine.ui.UISection;
import lwjglengine.window.Window;
import myutils.v10.algorithm.Graph;
import myutils.v10.graphics.GraphicsTools;
import myutils.v10.math.MathUtils;
import myutils.v10.math.Vec2;

public class DependencyGraphViewerWindow extends Window {

	//TODO
	// - use a force directed graph layout. 
	// - perhaps also within each scc, if there are multiple nodes, render all of the nodes within it using a subwindow. 

	private Project project;

	private UIFilledRectangle baseRect;

	private Graph<Node> graph;

	private ArrayList<Node> nodes;
	private ArrayList<Edge> edges;

	private float dt = 1f;
	private int iterations = 10;
	private float repulsion = 0.5f;
	private float centerStr = 0.5f;
	private float edgeLength = 100f;
	private float edgeStrength = 2f;

	private UIScreen uiScreen;
	private UISection uiSection;

	private final int LINE_SCENE = Scene.generateScene();

	private Vec2 cameraPos = new Vec2(0);
	private float screenScale = 1f; //TODO implement

	private Vec2 prevMouse = MouseInput.getMousePos();
	private boolean draggingCamera = false;

	private long hoveredNodeID;
	private boolean grabbedNode = false;
	private int grabbedNodeIndex = -1;

	public DependencyGraphViewerWindow(int xOffset, int yOffset, int width, int height, Project project, Window parentWindow) {
		super(xOffset, yOffset, width, height, parentWindow);
		this.init(project);
	}

	private void init(Project project) {
		this.project = project;

		this.uiScreen = new UIScreen();
		this.uiScreen.setClearColorIDBufferOnRender(true);

		this.uiSection = new UISection(0, 0, this.getWidth(), this.getHeight(), this.uiScreen);

		UIFilledRectangle backgroundRect = this.uiSection.getBackgroundRect();
		backgroundRect.setFillWidth(true);
		backgroundRect.setFillHeight(true);
		backgroundRect.setMaterial(Material.transparent());
		backgroundRect.bind(this.rootUIElement);

		this.nodes = new ArrayList<>();
		this.edges = new ArrayList<>();

		this.generateNodes();
	}

	private void clear() {
		if (this.baseRect != null) {
			this.baseRect.kill();
		}

		this.nodes.clear();

		for (Edge e : this.edges) {
			e.line.kill();
		}
		this.edges.clear();
	}

	private void generateNodes() {
		this.clear();

		this.baseRect = new UIFilledRectangle(0, 0, 1, 1, 1, this.uiSection.getBackgroundScene());
		this.baseRect.setMaterial(Material.transparent());

		//placing nodes into layers that depend on the topological sort of the graph. 
		this.graph = new Graph<>();
		ArrayList<AssetDependencyNode> assetNodes = this.project.getAssetDependencyNodeList();
		HashMap<AssetDependencyNode, Node> assetToNode = new HashMap<>();

		for (AssetDependencyNode i : assetNodes) {
			Node n = new Node(0, 0);
			n.assetNode = i;
			this.graph.addNode(n);
			assetToNode.put(i, n);

			this.nodes.add(n);
		}

		for (AssetDependencyNode i : assetNodes) {
			Node a = assetToNode.get(i);
			for (AssetDependencyNode j : i.getDependencies()) {
				Node b = assetToNode.get(j);
				this.graph.addEdge(a, b);
			}
		}

		ArrayList<Node> topologicalSort = this.graph.topologicalSort();

		//the 'layer' of a node is simply the maximum layer of its parents + 1, or 0 in the case where it doesn't have any parents. 
		ArrayList<Integer> nodeLayer = new ArrayList<>();
		for (int i = 0; i < topologicalSort.size(); i++) {
			int layer = 0;
			for (int j = 0; j < i; j++) {
				if (this.graph.doesEdgeExist(topologicalSort.get(j), topologicalSort.get(i))) {
					layer = Math.max(layer, nodeLayer.get(j) + 1);
				}
			}
			nodeLayer.add(layer);
		}

		ArrayList<ArrayList<Node>> layers = new ArrayList<>();
		for (int i = 0; i < nodeLayer.size(); i++) {
			int layer = nodeLayer.get(i);
			if (layer == layers.size()) {
				layers.add(new ArrayList<>());
			}

			layers.get(layer).add(topologicalSort.get(i));
		}

		//place nodes
		for (int i = 0; i < layers.size(); i++) {
			System.out.println("LAYER : " + i);
			for (int j = 0; j < layers.get(i).size(); j++) {
				System.out.println("NODE : " + j);

				Node node = layers.get(i).get(j);

				float nodeHorizontalMargin = 5;
				float nodeVerticalMargin = 5;

				float nodeTextEntryGap = 3;

				float rectWidth = 0;
				float rectHeight = nodeVerticalMargin;

				UIFilledRectangle nodeBackgroundRect = new UIFilledRectangle(0, 0, 0, 70, 70, this.uiSection.getSelectionScene());
				nodeBackgroundRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_BOTTOM);
				nodeBackgroundRect.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_CENTER);
				nodeBackgroundRect.setMaterial(this.topBarDefaultMaterial);
				nodeBackgroundRect.bind(this.baseRect);
				node.rect = nodeBackgroundRect;
				node.pos.set(new Vec2(i * 100, j * 100));

				AssetDependencyNode assetNode = node.assetNode;
				HashSet<Asset> assets = assetNode.getAssets();
				int k = 0;
				for (Asset a : assets) {
					System.out.println(a.getName());

					Text assetText = new Text(nodeHorizontalMargin, rectHeight, a.getName(), 12, Color.WHITE, this.uiSection.getTextScene());
					assetText.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
					assetText.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
					assetText.setDoAntialiasing(false);
					assetText.bind(nodeBackgroundRect);

					rectWidth = Math.max(rectWidth, assetText.getWidth() + nodeHorizontalMargin * 2);
					rectHeight += assetText.getHeight() + nodeTextEntryGap;

					k++;
				}

				rectHeight -= nodeTextEntryGap;
				rectHeight += nodeVerticalMargin;

				nodeBackgroundRect.setDimensions(rectWidth, rectHeight);

				System.out.println();
			}
		}

		//place edges
		for (int i = 0; i < this.nodes.size(); i++) {
			for (int j = 0; j < this.nodes.size(); j++) {
				Node a = this.nodes.get(i);
				Node b = this.nodes.get(j);
				if (this.graph.doesEdgeExist(a, b)) {
					Edge e = new Edge(this.edgeStrength, this.edgeLength, a, b, LINE_SCENE);
					e.line.setMaterial(this.contentHoveredMaterial);
					this.edges.add(e);
				}
			}
		}

	}

	@Override
	protected void _kill() {
		this.clear();

		Scene.removeScene(LINE_SCENE);

		this.uiSection.kill();
		this.uiScreen.kill();
	}

	@Override
	protected void _resize() {
		this.uiScreen.setScreenDimensions(this.getWidth(), this.getHeight());
	}

	@Override
	protected void _update() {

		//mouse input
		Vec2 curMouse = this.getWindowMousePos();
		Vec2 diff = new Vec2(this.prevMouse, curMouse);

		Vec2 worldMouse = this.cameraPos.sub(new Vec2(this.getWidth() / 2, this.getHeight() / 2)).add(curMouse);

		//grabbed node
		if (this.grabbedNode) {
			Node n = this.nodes.get(this.grabbedNodeIndex);
			n.pos.set(worldMouse);
		}

		//update camera position
		else if (this.draggingCamera) {
			this.cameraPos.subi(diff);
		}
		this.uiScreen.setViewportOffset(this.cameraPos.sub(new Vec2(this.getWidth() / 2, this.getHeight() / 2)));

		this.prevMouse.set(curMouse);

		//update graph
		for (int i = 0; i < 10; i++) {
			this.iterateGraph();
		}

		for (Edge e : this.edges) {
			e.align();
		}

		for (Node n : this.nodes) {
			n.align();
		}
	}

	private void iterateGraph() {
		for (Edge e : this.edges) {
			e.update();
		}

		//each node repels every other non adjacent node
		for (int i = 0; i < this.nodes.size(); i++) {
			for (int j = 0; j < i; j++) {
				Node a = this.nodes.get(i);
				Node b = this.nodes.get(j);

				if (this.graph.doesEdgeExist(a, b)) {
					continue;
				}

				Vec2 ab = new Vec2(a.pos, b.pos);

				float dist = ab.length() / this.edgeLength;

				float force = this.repulsion / (dist * dist);
				force = Math.min(this.repulsion, force);

				ab.normalize();
				a.applyImpulse(ab.mul(-force));
				b.applyImpulse(ab.mul(force));
			}
		}

		//attract all nodes to the center of the screen
		for (int i = 0; i < this.nodes.size(); i++) {
			Node a = this.nodes.get(i);
			Vec2 toCenter = new Vec2(a.pos, new Vec2(0, 0));
			if (toCenter.length() > this.centerStr) {
				toCenter.setLength(this.centerStr);
			}

			a.applyImpulse(toCenter);
		}

		for (Node n : this.nodes) {
			n.update(this.dt);
		}
	}

	@Override
	protected void renderContent(Framebuffer outputBuffer) {
		this.uiScreen.setUIScene(LINE_SCENE);
		this.uiScreen.render(outputBuffer);

		this.uiSection.render(outputBuffer, getWindowMousePos());
		this.hoveredNodeID = this.uiSection.getHoveredEntityID();
	}

	@Override
	protected void renderOverlay(Framebuffer outputBuffer) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void selected() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void deselected() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void subtreeSelected() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void subtreeDeselected() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _mousePressed(int button) {
		this.draggingCamera = true;

		this.grabbedNodeIndex = -1;
		for (int i = 0; i < this.nodes.size(); i++) {
			Node n = this.nodes.get(i);
			if (n.rect.getID() == this.hoveredNodeID) {
				this.grabbedNodeIndex = i;
				this.grabbedNode = true;
				break;
			}
		}
	}

	@Override
	protected void _mouseReleased(int button) {
		this.draggingCamera = false;
		this.grabbedNode = false;
	}

	@Override
	protected void _mouseScrolled(float wheelOffset, float smoothOffset) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _keyPressed(int key) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _keyReleased(int key) {
		// TODO Auto-generated method stub

	}

	class Node {

		public AssetDependencyNode assetNode;
		public UIFilledRectangle rect;

		public Vec2 pos, acc;

		public float size = 20f;

		public boolean pinned = false;

		public Node(float x, float y) {
			this.pos = new Vec2(x, y);
			this.acc = new Vec2(0);
		}

		public void update(float dt) {
			Vec2 next_pos = this.pos.add(this.acc.mul(dt));
			this.pos.set(next_pos);

			this.acc.set(0, 0);
		}

		public void align() {
			rect.setFrameAlignmentOffset(this.pos.x, this.pos.y);
		}

		public void applyImpulse(Vec2 impulse) {
			this.acc.addi(impulse);
		}

	}

	class Edge {
		ModelInstance line;

		double strength; //pretty much spring coeff
		double length; //rest length
		Node a, b;

		public Edge(float strength, double length, Node a, Node b, int scene) {
			this.a = a;
			this.b = b;
			this.strength = strength;
			this.length = length;

			this.line = Line.addLine(a.pos.x, a.pos.y, b.pos.x, b.pos.y, scene);
		}

		public void update() {
			Vec2 ab = new Vec2(this.a.pos, this.b.pos);
			double dist = ab.length();
			double diff = dist - length;

			double force = this.strength * Math.log(this.length / dist);

			ab.normalize();

			this.a.applyImpulse(ab.mul(-force));
			this.b.applyImpulse(ab.mul(force));
		}

		public void align() {
			Line.setLineModelTransform(this.a.pos.x, this.a.pos.y, this.b.pos.x, this.b.pos.y, line);
		}
	}

}
