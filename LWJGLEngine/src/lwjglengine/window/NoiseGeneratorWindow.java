package lwjglengine.window;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.GL42.glTexStorage2D;
import static org.lwjgl.opengl.GL46.*;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;

import lwjglengine.graphics.Framebuffer;
import lwjglengine.graphics.Material;
import lwjglengine.graphics.Texture;
import lwjglengine.graphics.TextureMaterial;
import lwjglengine.input.Input;
import lwjglengine.model.FilledRectangle;
import lwjglengine.model.Line;
import lwjglengine.model.Model;
import lwjglengine.model.ModelInstance;
import lwjglengine.ui.ObjectEditor;
import lwjglengine.ui.ObjectEditor.ObjectEditorCallback;
import lwjglengine.ui.Text;
import lwjglengine.ui.UIElement;
import lwjglengine.ui.UIFilledRectangle;
import lwjglengine.ui.UISection;
import myutils.math.MathUtils;
import myutils.math.Vec2;
import myutils.math.Vec3;
import myutils.noise.NoiseGenerator;
import myutils.noise.base.NoiseBase;
import myutils.noise.base.PerlinNoise;
import myutils.noise.binary.NoiseBinaryOperator;
import myutils.noise.unary.NoiseUnaryOperator;

public class NoiseGeneratorWindow extends Window {
	//i'll call the nodes 'noise nodes'. 
	//each node should have exactly 1 output, which is the noise sampler. 

	//each node will have an ID, this is how we know what node we've selected. 
	//should also probably check for cycles. 

	//maybe add support for textures in the future. 

	//TODO
	// - create input and output handles for nodes.
	// - check if setting an input will create a cycle. 
	// - allow user to drag around the view in a display. 

	private UISection backgroundSection, nodeSection;

	private Vec2 viewportCenter = new Vec2(0);
	private Vec2 mousePos = new Vec2(0);
	private boolean draggingCamera = false;

	//node background entity id to node. 
	private HashMap<Long, Node> nodes;

	private boolean draggingNode = false;
	private long grabbedNodeID;

	private boolean shouldUpdateDisplays = false;

	public NoiseGeneratorWindow(Window parentWindow) {
		super(parentWindow);
		this.init();
	}

	private void init() {
		this.backgroundSection = new UISection();
		this.backgroundSection.getBackgroundRect().setFillWidth(true);
		this.backgroundSection.getBackgroundRect().setFillHeight(true);
		this.backgroundSection.getBackgroundRect().setMaterial(new Material(new Vec3(0.1)));
		this.backgroundSection.getBackgroundRect().bind(this.rootUIElement);

		this.nodeSection = new UISection();
		this.nodeSection.getBackgroundRect().setDimensions(1, 1);
		this.nodeSection.getBackgroundRect().setFrameAlignmentOffset(0, 0);
		this.nodeSection.getBackgroundRect().setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		this.nodeSection.getBackgroundRect().setMaterial(Material.transparent());
		this.nodeSection.getBackgroundRect().setZ(this.backgroundSection.getBackgroundRect().getZ() + 1);
		this.nodeSection.setAllowInputWhenSectionNotHovered(true);
		this.nodeSection.getBackgroundScreen().setReverseDepthColorID(true);
		this.nodeSection.getBackgroundScreen().setRenderColorID(true);

		this.nodes = new HashMap<>();

		Node tmp = new Node(new PerlinNoise());

		this._resize();
	}

	@Override
	protected void _kill() {
		for (Node n : nodes.values()) {
			n.kill();
		}

		this.backgroundSection.kill();
		this.nodeSection.kill();
	}

	@Override
	protected void _resize() {
		this.backgroundSection.setScreenDimensions(this.getWidth(), this.getHeight());
		this.nodeSection.setScreenDimensions(this.getWidth(), this.getHeight());
	}

	@Override
	public String getDefaultTitle() {
		return "Noise Generator";
	}

	private void updateDisplays() {
		this.shouldUpdateDisplays = true;
	}

	private Vec2 getWorldMousePos() {
		Vec2 ans = new Vec2(0);
		ans.addi(this.viewportCenter);
		ans.subi(this.getWidth() / 2, this.getHeight() / 2);
		ans.addi(this.getWindowMousePos());
		return ans;
	}

	@Override
	protected void _update() {
		this.backgroundSection.update();
		this.nodeSection.update();

		Vec2 next_mouse_pos = this.getWindowMousePos();
		Vec2 mouse_diff = next_mouse_pos.sub(this.mousePos);
		if (this.draggingCamera) {
			this.viewportCenter.subi(mouse_diff);
		}
		else if (this.draggingNode) {
			Node n = this.nodes.get(this.grabbedNodeID);
			n.setPos(n.pos.add(mouse_diff));
		}
		this.mousePos.set(next_mouse_pos);

		//goes through all nodes and updates all of their displays. 
		//TODO make it so that we only update displays that need updating. 
		if (this.shouldUpdateDisplays) {
			this.shouldUpdateDisplays = false;

			for (Node n : this.nodes.values()) {
				NoiseGenerator gen = n.generator;
				int[] data = new int[DISPLAY_RESOLUTION * DISPLAY_RESOLUTION];
				for (int i = 0; i < DISPLAY_RESOLUTION; i++) {
					for (int j = 0; j < DISPLAY_RESOLUTION; j++) {
						float x = (float) i / DISPLAY_RESOLUTION;
						float y = (float) j / DISPLAY_RESOLUTION;
						float value = gen.sampleNoise(x, y);
						value = MathUtils.clamp(0, 1, MathUtils.lerp(0, -1, 1, 1, value));

						int r = (int) (value * 255);
						int g = (int) (value * 255);
						int b = (int) (value * 255);

						data[i * DISPLAY_RESOLUTION + j] = (r << 0) + (g << 8) + (b << 16) + (255 << 24);
					}
				}

				glBindTexture(GL_TEXTURE_2D, n.displayTexture.getID());
				glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, DISPLAY_RESOLUTION, DISPLAY_RESOLUTION, GL_RGBA, GL_UNSIGNED_BYTE, data);
			}
		}
	}

	@Override
	protected void renderContent(Framebuffer outputBuffer) {
		this.backgroundSection.render(outputBuffer, this.getWindowMousePos());

		Vec2 viewport_offset = new Vec2(this.viewportCenter);
		viewport_offset.subi(this.getWidth() / 2, this.getHeight() / 2);
		this.nodeSection.setViewportOffset(viewport_offset);
		this.nodeSection.render(outputBuffer, this.getWindowMousePos());
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
		this.nodeSection.mousePressed(button);

		long hovered_node_id = this.nodeSection.getHoveredBackgroundID();
		if (this.nodes.containsKey(hovered_node_id)) {
			if (this.nodeSection.getHoveredSelectionID() == 0) {
				this.draggingNode = true;
				this.grabbedNodeID = hovered_node_id;
			}
		}
		else {
			this.draggingCamera = true;
		}
	}

	@Override
	protected void _mouseReleased(int button) {
		this.nodeSection.mouseReleased(button);

		this.draggingNode = false;
		this.draggingCamera = false;
	}

	@Override
	protected void _mouseScrolled(float wheelOffset, float smoothOffset) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _keyPressed(int key) {
		this.nodeSection.keyPressed(key);
	}

	@Override
	protected void _keyReleased(int key) {
		this.nodeSection.keyReleased(key);
	}

	private static final int NODE_WIDTH = 200;
	private static final int NODE_COMPONENT_SPACING = 5;

	private static final int DISPLAY_RESOLUTION = NODE_WIDTH - 2 * NODE_COMPONENT_SPACING;

	class Node implements ObjectEditorCallback {
		//should use input listeners to know when to update stuff. 
		//if it takes in another node as input, should keep track of them. 

		//also should allow to display noise. 

		public long id;
		public NoiseGenerator generator;

		public Vec2 pos;
		public UIFilledRectangle backgroundRect;
		public ArrayList<Input> inputs;

		public Texture displayTexture;

		public ModelInstance[] borderLines;

		public Node(NoiseGenerator _generator) {
			this.pos = new Vec2(0);

			this.generator = _generator;
			this.generateUI();
			this.setPos(this.pos);

			this.id = this.backgroundRect.getID();
			nodes.put(id, this);
		}

		private int height;

		//should be called once at startup
		private void generateUI() {
			this.height = 0;
			this.backgroundRect = new UIFilledRectangle(0, 0, 0, NODE_WIDTH, 0, nodeSection.getBackgroundScene());
			this.backgroundRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
			this.backgroundRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
			this.backgroundRect.setMaterial(Material.TOP_BAR_DEFAULT_MATERIAL);
			this.backgroundRect.bind(nodeSection.getBackgroundRect());

			this.generateTitleBar();
			this.generateDisplay();
			this.generateObjectEditor();

			this.backgroundRect.setHeight(this.height);
		}

		private void generateTitleBar() {
			UIFilledRectangle title_rect = new UIFilledRectangle(0, this.height, 0, NODE_WIDTH, 24, nodeSection.getBackgroundScene());
			title_rect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
			title_rect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
			title_rect.setMaterial(Material.TOP_BAR_DEFAULT_MATERIAL);
			title_rect.bind(this.backgroundRect);

			String generator_name = this.generator.getClass().getSimpleName();
			Text title_text = new Text(generator_name, nodeSection.getTextScene());
			title_text.setFrameAlignmentOffset(0, 0);
			title_text.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_TOP);
			title_text.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_CENTER);
			title_text.setMaterial(new Material(Color.WHITE));
			title_text.bind(title_rect);

			this.height += 24 + NODE_COMPONENT_SPACING;
		}

		private void generateDisplay() {
			this.displayTexture = new Texture(DISPLAY_RESOLUTION, DISPLAY_RESOLUTION);
			FilledRectangle texture_rect = new FilledRectangle();
			texture_rect.setTextureMaterial(new TextureMaterial(this.displayTexture));

			int display_size = NODE_WIDTH - NODE_COMPONENT_SPACING * 2;
			UIFilledRectangle display_rect = new UIFilledRectangle(0, this.height, 0, display_size, display_size, texture_rect, nodeSection.getSelectionScene());
			display_rect.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_TOP);
			display_rect.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_TOP);
			display_rect.setMaterial(new Material(Color.WHITE));
			display_rect.bind(this.backgroundRect);

			this.height += display_size + NODE_COMPONENT_SPACING;
		}

		private void generateObjectEditor() {
			ObjectEditor editor = new ObjectEditor(this.generator, nodeSection);
			editor.setFrameAlignmentOffset(0, this.height);
			editor.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
			editor.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
			editor.setFillWidth(true);
			editor.setInputWidth(80);
			editor.bind(this.backgroundRect);
			editor.addCallback(this);

			this.height += editor.getHeight() + NODE_COMPONENT_SPACING;
		}

		public void kill() {
			this.backgroundRect.kill();
		}

		private void generateBorderLines() {
			if (this.borderLines != null) {
				for (int i = 0; i < 4; i++) {
					this.borderLines[i].kill();
				}
			}

			Vec2 p = new Vec2(this.pos.x, this.pos.y);
			Vec2 p0 = p.add(0, 1);
			Vec2 p1 = p.add(0, -this.height);
			Vec2 p2 = p.add(NODE_WIDTH + 1, -this.height);
			Vec2 p3 = p.add(NODE_WIDTH + 1, 1);

			this.borderLines = new ModelInstance[4];
			this.borderLines[0] = Line.addDefaultLine(p0, p1.add(0, -1), nodeSection.getSelectionScene());
			this.borderLines[1] = Line.addDefaultLine(p1, p2, nodeSection.getSelectionScene());
			this.borderLines[2] = Line.addDefaultLine(p2, p3, nodeSection.getSelectionScene());
			this.borderLines[3] = Line.addDefaultLine(p3, p0, nodeSection.getSelectionScene());
			for (int i = 0; i < 4; i++) {
				this.borderLines[i].setMaterial(Material.CONTENT_SELECTED_MATERIAL);
			}
		}

		public void setPos(Vec2 _pos) {
			this.pos.set(_pos);
			this.backgroundRect.setFrameAlignmentOffset(this.pos.x, -this.pos.y);
			this.generateBorderLines();
		}

		@Override
		public void objectModified(Object o) {
			updateDisplays();
		}

	}

}
