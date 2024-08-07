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

import org.reflections.Reflections;

import java.awt.Color;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

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
import lwjglengine.ui.UILine;
import lwjglengine.ui.UISection;
import lwjglengine.window.NoiseGeneratorWindow.Node.NodeInput;
import lwjglengine.window.NoiseGeneratorWindow.Node.NodeOutput;
import myutils.math.MathUtils;
import myutils.math.Vec2;
import myutils.math.Vec3;
import myutils.noise.NoiseGenerator;
import myutils.noise.base.NoiseBase;
import myutils.noise.base.PerlinNoise;
import myutils.noise.binary.NoiseAdd;
import myutils.noise.binary.NoiseBinaryOperator;
import myutils.noise.binary.NoiseDomainWarp;
import myutils.noise.unary.NoiseUnaryOperator;

public class NoiseGeneratorWindow extends Window {
	//i'll call the nodes 'noise nodes'. 
	//each node should have exactly 1 output, which is the noise sampler. 

	//each node will have an ID, this is how we know what node we've selected. 
	//should also probably check for cycles. 

	//maybe add support for textures in the future. 

	//TODO
	// - check if setting an input will create a cycle. 
	// - allow user to drag around the view in a display. 
	//   - translating as well as scaling using scroll wheel. 
	//   - however, if the display is modified, then it won't exactly represent the output
	//   - maybe have it snap back to representing the output if the displays get updated. 
	//   - perhaps just have an affine transform noise generator that the user can modify instead. 
	// - add a background grid
	// - allow user to add noise nodes 
	// - WHY IS PERLIN NOISE SO SLOW (especially when you increase the number of octaves). 
	//   - in other words, optimize display generation. 
	//   - when generating displays, we can just use slow generators once, and repackage that into some sort of
	//     texture sampler for steps further down in the pipeline. 
	//   - of course, this may affect the result, and it should be an option to do the brute force method. 
	// - add color display node, allowing user to put different noise generators for color channels. 
	//   - or we could have a ternary noise sampler, and we can just check for it before rendering. 
	// - allow renaming of node titles
	//   - maybe this can be done by replacing the titles with text fields with transparent backgrounds. 
	// - allow exporting generated noise of nodes. 
	// - ability to remove nodes

	private UISection backgroundSection, nodeSection;

	private Vec2 viewportCenter = new Vec2(0);
	private Vec2 mousePos = new Vec2(0);
	private boolean draggingCamera = false;

	//node background entity id to node. 
	private HashMap<Long, Node> nodes;

	private boolean draggingNode = false;
	private long grabbedNodeID;

	private boolean shouldUpdateDisplays = false;
	
	private boolean draggingOutputHandle = false;
	private NodeOutput grabbedNodeOutput;
	private ModelInstance grabbedOutputHandleLine = null;
	
	//maybe input handle should map to a NoiseGenerator setter method?
	private HashMap<Long, NodeInput> nodeInputs;
	private HashMap<Long, NodeOutput> nodeOutputs;

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

		this.nodes = new HashMap<>();
		
		this.nodeInputs = new HashMap<>();
		this.nodeOutputs = new HashMap<>();
		
		this.setContextMenuRightClick(true);
		this.setContextMenuActions(new String[] {"Add Node"});

		Node tmp = new Node(new PerlinNoise());
		Node tmp2 = new Node(new PerlinNoise());
		Node tmp3 = new Node(new NoiseDomainWarp());
		tmp2.setPos(new Vec2(NODE_WIDTH + 10, 0));
		tmp3.setPos(new Vec2(NODE_WIDTH * 2 + 20, 0));

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
	
	class NoiseGeneratorSelectorCallback implements ListViewerWindow.ListViewerCallback {
		@Override
		public void handleListViewerCallback(Object[] contents) {
			assert contents.length == 1;
			Class<? extends NoiseGenerator> c = (Class<? extends NoiseGenerator>) contents[0];
			try {
				NoiseGenerator gen = c.newInstance();
				Node node = new Node(gen);
				node.setPos(getWorldMousePos());
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	
	@Override
	public void handleContextMenuAction(String action) {
		switch(action) {
		case "Add Node":{
			//retrieve entire list of non-abstract classes under NoiseGenerator
			Reflections r = new Reflections("myutils.noise");
	        Set<Class<? extends NoiseGenerator>> all_classes = r.getSubTypesOf(NoiseGenerator.class);
	        Set<Class<? extends NoiseGenerator>> non_abstract = all_classes.stream()
	                .filter(subclass -> !Modifier.isAbstract(subclass.getModifiers()))
	                .collect(Collectors.toSet());
	        
	        ListViewerWindow noise_selector = new ListViewerWindow(new NoiseGeneratorSelectorCallback());
	        noise_selector.setSingleEntrySelection(true);
	        noise_selector.setSortEntries(true);
	        noise_selector.setRenderBottomBar(false);
	        noise_selector.setSubmitOnClickingSelectedListEntry(true);
	        for (Class<? extends NoiseGenerator> c : non_abstract) {
	        	noise_selector.addToList(c, c.getSimpleName());
	        }
	        this.addChildAdjWindow(noise_selector);
	        
			break;
		}
		}
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
		
		if(this.draggingOutputHandle) {
			if(this.grabbedOutputHandleLine != null) {
				this.grabbedOutputHandleLine.kill();
				this.grabbedOutputHandleLine = null;
			}
			
			UIFilledRectangle output_handle = this.grabbedNodeOutput.outputHandle;
			Vec2 handle_pos = new Vec2(output_handle.getGlobalAlignedX(), output_handle.getGlobalAlignedY());
			handle_pos.addi(output_handle.getWidth() / 2, output_handle.getHeight() / 2);
			Vec2 mouse_pos = this.getWorldMousePos();
			this.grabbedOutputHandleLine = Line.addDefaultLine(handle_pos, mouse_pos, this.nodeSection.getBackgroundScene());
			this.grabbedOutputHandleLine.setMaterial(CONNECTION_MATERIAL);
		}

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

		long hovered_background_id = this.nodeSection.getHoveredBackgroundID();
		long hovered_selection_id = this.nodeSection.getHoveredSelectionID();
		
		if(hovered_selection_id != 0) {	//check if an input is hovered over
			//see if the hovered thing is an output handle. 
			if(this.nodeOutputs.containsKey(hovered_selection_id)) {
				this.draggingOutputHandle = true;
				this.grabbedNodeOutput = this.nodeOutputs.get(hovered_selection_id);
			}
			
			//see if the hovered thing is an input handle
			else if(this.nodeInputs.containsKey(hovered_selection_id)) {
				NodeInput input = this.nodeInputs.get(hovered_selection_id);
				if(input.connectedOutput != null) {
					this.draggingOutputHandle = true;
					this.grabbedNodeOutput = input.connectedOutput;
					input.setConnection(null);
				}
			}
		}
		else if (this.nodes.containsKey(hovered_background_id)) {	//otherwise, check if we're hovering over a node
			if (this.nodeSection.getHoveredSelectionID() == 0) {
				this.draggingNode = true;
				this.grabbedNodeID = hovered_background_id;
			}
		}
		else {	//otherwise, drag the camera. 
			this.draggingCamera = true;
		}
	}

	@Override
	protected void _mouseReleased(int button) {
		this.nodeSection.mouseReleased(button);
		
		long hovered_background_id = this.nodeSection.getHoveredBackgroundID();
		long hovered_selection_id = this.nodeSection.getHoveredSelectionID();

		this.draggingNode = false;
		this.draggingCamera = false;
		
		if(this.draggingOutputHandle) {
			this.draggingOutputHandle = false;
			if(this.grabbedOutputHandleLine != null) {
				this.grabbedOutputHandleLine.kill();
				this.grabbedOutputHandleLine = null;
			}
			
			//check if we released the mouse over some input handle. 
			if(this.nodeInputs.containsKey(hovered_selection_id)) {
				NodeInput input = this.nodeInputs.get(hovered_selection_id);
				input.setConnection(this.grabbedNodeOutput);
			}
		}
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
	private static final int NODE_COMPONENT_SPACING = 4;
	private static final int NODE_HORIZONTAL_MARGIN = 8;

	private static final int DISPLAY_RESOLUTION = NODE_WIDTH - 2 * NODE_COMPONENT_SPACING;
	
	private static final int NODE_HANDLE_SIZE = 8;
	
	private static final Material NODE_BACKGROUND_MATERIAL = Material.TOP_BAR_DEFAULT_MATERIAL;
	
	private static final Material CONNECTION_MATERIAL = Material.CONTENT_SELECTED_MATERIAL;

	class Node implements ObjectEditorCallback {
		//should use input listeners to know when to update stuff. 
		//if it takes in another node as input, should keep track of them. 

		//also should allow to display noise. 

		public long id;
		public NoiseGenerator generator;

		public Vec2 pos;
		public UIFilledRectangle backgroundRect;

		public Texture displayTexture;

		public ModelInstance[] borderLines;
		
		public UIFilledRectangle[] inputHandles;
		public UIFilledRectangle outputHandle;
		
		public NodeInput[] inputs;
		public NodeOutput output;

		public Node(NoiseGenerator _generator) {
			this.pos = new Vec2(0);

			this.generator = _generator;
			this.generateUI();
			this.setPos(this.pos);

			this.id = this.backgroundRect.getID();
			nodes.put(id, this);
			
			//generate node inputs and outputs
			this.output = new NodeOutput(this.outputHandle, this.generator);
			HashMap<String, Method> method_map = new HashMap<>();
			{
				Method[] methods = this.generator.getClass().getMethods();
				for(Method m : methods) {
					method_map.put(m.getName(), m);
				}
			}
			
			if(this.generator instanceof NoiseUnaryOperator) {
				this.inputs = new NodeInput[1];
				this.inputs[0] = new NodeInput(this.inputHandles[0], method_map.get("setA"));
			}
			else if(this.generator instanceof NoiseBinaryOperator) {
				this.inputs = new NodeInput[2];
				this.inputs[0] = new NodeInput(this.inputHandles[0], method_map.get("setA"));
				this.inputs[1] = new NodeInput(this.inputHandles[1], method_map.get("setB"));
			}
			else {
				this.inputs = new NodeInput[0];
			}
			
			updateDisplays();
		}

		private int height;

		//should be called once at startup
		private void generateUI() {
			this.height = 0;
			this.backgroundRect = new UIFilledRectangle(0, 0, 0, NODE_WIDTH, 0, nodeSection.getBackgroundScene());
			this.backgroundRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
			this.backgroundRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
			this.backgroundRect.setMaterial(NODE_BACKGROUND_MATERIAL);
			this.backgroundRect.bind(nodeSection.getBackgroundRect());

			this.generateTitleBar();
			this.generateDisplay();
			this.outputHandle = this.generateOutputHandle();
			
			if(this.generator instanceof NoiseBase) {
				this.inputHandles = new UIFilledRectangle[0];
			}
			else if(this.generator instanceof NoiseUnaryOperator) {
				this.inputHandles = new UIFilledRectangle[1];
				this.inputHandles[0] = this.generateInputHandle("Input A");
			}
			else if(this.generator instanceof NoiseBinaryOperator) {
				this.inputHandles = new UIFilledRectangle[2];
				this.inputHandles[0] = this.generateInputHandle("Input A");
				this.inputHandles[1] = this.generateInputHandle("Input B");
			}
			
			this.generateObjectEditor();

			this.backgroundRect.setHeight(this.height);
		}
		
		private UIFilledRectangle generateOutputHandle() {
			UIFilledRectangle backing_rect = new UIFilledRectangle(0, this.height, 0, NODE_WIDTH, 20, nodeSection.getBackgroundScene());
			backing_rect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
			backing_rect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
			backing_rect.setMaterial(NODE_BACKGROUND_MATERIAL);
			backing_rect.bind(this.backgroundRect);
			
			Text output_text = new Text("Output", nodeSection.getTextScene());
			output_text.setFrameAlignmentOffset(NODE_HORIZONTAL_MARGIN, 0);
			output_text.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_CENTER_TOP);
			output_text.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
			output_text.setMaterial(new Material(Color.WHITE));
			output_text.bind(backing_rect);
			
			UIFilledRectangle output_handle = new UIFilledRectangle(0, 0, 0, NODE_HANDLE_SIZE, NODE_HANDLE_SIZE, nodeSection.getSelectionScene());
			output_handle.setMaterial(Material.CONTENT_SELECTED_MATERIAL);
			output_handle.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_CENTER_TOP);
			output_handle.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_CENTER);
			output_handle.bind(backing_rect);
			
			this.height += 20 + NODE_COMPONENT_SPACING;
			
			return output_handle;
		}
		
		private UIFilledRectangle generateInputHandle(String name) {
			UIFilledRectangle backing_rect = new UIFilledRectangle(0, this.height, 0, NODE_WIDTH, 20, nodeSection.getBackgroundScene());
			backing_rect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
			backing_rect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
			backing_rect.setMaterial(NODE_BACKGROUND_MATERIAL);
			backing_rect.bind(this.backgroundRect);
			
			Text output_text = new Text(name, nodeSection.getTextScene());
			output_text.setFrameAlignmentOffset(NODE_HORIZONTAL_MARGIN, 0);
			output_text.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_TOP);
			output_text.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
			output_text.setMaterial(new Material(Color.WHITE));
			output_text.bind(backing_rect);
			
			UIFilledRectangle input_handle = new UIFilledRectangle(0, 0, 0, NODE_HANDLE_SIZE, NODE_HANDLE_SIZE, nodeSection.getSelectionScene());
			input_handle.setMaterial(Material.CONTENT_DEFAULT_MATERIAL);
			input_handle.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_TOP);
			input_handle.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_CENTER);
			input_handle.bind(backing_rect);
			
			this.height += 20 + NODE_COMPONENT_SPACING;
			
			return input_handle;
		}

		private void generateTitleBar() {
			UIFilledRectangle title_rect = new UIFilledRectangle(0, this.height, 0, NODE_WIDTH, 24, nodeSection.getBackgroundScene());
			title_rect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
			title_rect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
			title_rect.setMaterial(NODE_BACKGROUND_MATERIAL);
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
			editor.setMaterial(NODE_BACKGROUND_MATERIAL);
			editor.setFillWidth(true);
			editor.setInputWidth(80);
			editor.setHorizontalMargin(NODE_HORIZONTAL_MARGIN);
			editor.setVerticalMargin(0);
			editor.setVerticalPadding(NODE_COMPONENT_SPACING);
			editor.bind(this.backgroundRect);
			editor.addCallback(this);

			this.height += editor.getHeight() + NODE_COMPONENT_SPACING;
		}

		public void kill() {
			this.backgroundRect.kill();
			
			nodes.remove(this.id);
			this.output.kill();
			for(NodeInput i : this.inputs) {
				i.kill();
			}
			
			updateDisplays();
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
			this.borderLines[0] = Line.addDefaultLine(p0, p1.add(0, -1), nodeSection.getBackgroundScene());
			this.borderLines[1] = Line.addDefaultLine(p1, p2, nodeSection.getBackgroundScene());
			this.borderLines[2] = Line.addDefaultLine(p2, p3, nodeSection.getBackgroundScene());
			this.borderLines[3] = Line.addDefaultLine(p3, p0, nodeSection.getBackgroundScene());
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
		
		class NodeInput {
			//also responsible for drawing the line between this and whatever connected output
			
			public long id;
			public UIFilledRectangle inputHandle;
			
			public Method setter;
			
			public NodeOutput connectedOutput = null;
			
			public UILine line = null;
			
			public NodeInput(UIFilledRectangle input_handle, Method setter) {
				this.id = input_handle.getID();
				this.setter = setter;
				this.inputHandle = input_handle;
				
				nodeInputs.put(this.id, this);
			}
			
			public void setConnection(NodeOutput o) {
				this.connectedOutput = o;
				if(this.line != null) {
					this.line.kill();
					this.line = null;
				}
				
				if(this.connectedOutput == null) {
					try {
						setter.invoke(generator, (NoiseGenerator) null);
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else {			
					try {
						setter.invoke(generator, this.connectedOutput.generator);
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					//create line
					UIFilledRectangle output_handle = this.connectedOutput.outputHandle;
					UIFilledRectangle input_handle = this.inputHandle;
					
					this.line = new UILine(nodeSection.getBackgroundScene());
					this.line.getE1().setFrameAlignmentOffset(0, 0);
					this.line.getE1().setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_TOP);
					this.line.getE1().setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_CENTER);
					this.line.getE1().bind(output_handle);
					this.line.getE2().setFrameAlignmentOffset(0, 0);
					this.line.getE2().setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_TOP);
					this.line.getE2().setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_CENTER);
					this.line.getE2().bind(input_handle);
					this.line.setMaterial(CONNECTION_MATERIAL);
				}
				
				updateDisplays();
			}
			
			public void kill() {
				nodeInputs.remove(this.id);
				
				if(this.line != null) {
					this.line.kill();
				}
			}
		}
		
		class NodeOutput {
			
			public long id;
			public UIFilledRectangle outputHandle;
			public NoiseGenerator generator;
			
			public NodeOutput(UIFilledRectangle output_handle, NoiseGenerator generator) {
				this.id = output_handle.getID();
				this.outputHandle = output_handle;
				this.generator = generator;
				nodeOutputs.put(this.id, this);
			}
			
			public void kill() {
				nodeOutputs.remove(this.id);
			}
		}
	}

}
