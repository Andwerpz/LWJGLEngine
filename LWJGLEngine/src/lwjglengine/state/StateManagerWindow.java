package lwjglengine.state;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

import java.util.ArrayList;

import lwjglengine.entity.Entity;
import lwjglengine.graphics.Framebuffer;
import lwjglengine.graphics.Material;
import lwjglengine.graphics.Shader;
import lwjglengine.graphics.Texture;
import lwjglengine.graphics.TextureMaterial;
import lwjglengine.main.Main;
import lwjglengine.model.FilledRectangle;
import lwjglengine.model.Model;
import lwjglengine.project.ProjectManagerWindow;
import lwjglengine.scene.Scene;
import lwjglengine.screen.ScreenQuad;
import lwjglengine.screen.UIScreen;
import lwjglengine.ui.UIElement;
import lwjglengine.ui.UIFilledRectangle;
import lwjglengine.window.AdjustableWindow;
import lwjglengine.window.FileExplorerWindow;
import lwjglengine.window.ModelAssetViewerWindow;
import lwjglengine.window.RaytracingWindow;
import lwjglengine.window.Window;
import myutils.v10.math.Vec3;
import myutils.v10.math.Vec4;
import myutils.v11.file.FileUtils;
import myutils.v11.file.JarUtils;

public class StateManagerWindow extends Window {
	//this window is it's own root window?

	private final int BACKGROUND_SCENE = Scene.generateScene();
	private final int LOGO_SCENE = Scene.generateScene();

	private UIScreen uiScreen;

	private UIFilledRectangle backgroundRect;
	private UIFilledRectangle logoIconRect;

	private boolean renderLogo = true;

	private StateFactory initialStateFactory;

	public State activeState;
	public LoadState loadState;

	public StateManagerWindow(int xOffset, int yOffset, int contentWidth, int contentHeight, StateFactory initialStateFactory, Window parentWindow) {
		super(xOffset, yOffset, contentWidth, contentHeight, parentWindow);
		this.init(initialStateFactory);
	}

	public StateManagerWindow(StateFactory initialStateFactory) {
		super(0, 0, Main.windowWidth, Main.windowHeight, null);
		this.init(initialStateFactory);
	}

	private void init(StateFactory initialStateFactory) {
		this.initialStateFactory = initialStateFactory;

		this.activeState = null;
		this.loadState = new LoadState(new SplashState(this.initialStateFactory.createState()));
		this.loadState.setStateManager(this);
		this.setAllowInput(false);

		this.uiScreen = new UIScreen();

		this.backgroundRect = new UIFilledRectangle(0, 0, 0, this.getWidth(), this.getHeight(), BACKGROUND_SCENE);
		this.backgroundRect.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_TOP);
		this.backgroundRect.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_CENTER);
		this.backgroundRect.setMaterial(new Material(new Vec4(0, 0, 0, 1)));
		this.backgroundRect.bind(this.rootUIElement);

		TextureMaterial logoIconTexture = new TextureMaterial(new Texture(JarUtils.loadImage("/Halfcup_icon_white.png"), Texture.VERTICAL_FLIP_BIT));
		TextureMaterial logoTexture = new TextureMaterial(new Texture(JarUtils.loadImage("/Halfcup_logo_v2.png"), Texture.VERTICAL_FLIP_BIT));

		this.logoIconRect = new UIFilledRectangle(0, 0, 0, 600, 200, new FilledRectangle(), LOGO_SCENE);
		this.logoIconRect.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_TOP);
		this.logoIconRect.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_CENTER);
		this.logoIconRect.setMaterial(new Material(new Vec3(30 / 255.0f)));
		this.logoIconRect.setTextureMaterial(logoTexture);
		this.logoIconRect.bind(this.rootUIElement);

		//this.setContextMenuRightClick(true);

		ArrayList<String> contextMenuOptions = new ArrayList<>();
		contextMenuOptions.add("New File Explorer Window");
		contextMenuOptions.add("New Raytracing Window");
		contextMenuOptions.add("New Project Manager Window");

		this.setContextMenuActions(contextMenuOptions);

	}

	@Override
	public void handleContextMenuAction(String action) {
		switch (action) {
		case "New File Explorer Window":
			Window fileExplorer = new AdjustableWindow((int) this.getWindowMousePos().x, (int) this.getWindowMousePos().y, 400, 400, "File Explorer", new FileExplorerWindow(this), this);
			break;

		case "New Raytracing Window":
			Window raytracer = new AdjustableWindow((int) this.getWindowMousePos().x, (int) this.getWindowMousePos().y, 400, 400, "Raytracing", new RaytracingWindow(), this);
			break;

		case "New Project Manager Window":
			Window projectManager = new AdjustableWindow((int) this.getWindowMousePos().x, (int) this.getWindowMousePos().y, 400, 400, "Project Manager", new ProjectManagerWindow(), this);
			break;
		}
	}

	// trigger a load screen
	public void switchState(State nextState) {
		if (this.loadState == null || this.loadState.isFinishedLoading()) {
			this.loadState = new LoadState(nextState);
			this.loadState.setStateManager(this);
			this.setAllowInput(false);
		}
	}

	@Override
	protected void _kill() {
		this.activeState.kill();
		this.loadState.kill();

		Scene.removeScene(BACKGROUND_SCENE);
		Scene.removeScene(LOGO_SCENE);
		this.uiScreen.kill();
	}

	//used during the load screen to clear all windows
	public void killAllChildren() {
		while (this.childWindows.size() != 0) {
			Window w = this.childWindows.get(0);
			w.kill();
		}
	}

	@Override
	protected void _resize() {
		this.uiScreen.setScreenDimensions(this.getWidth(), this.getHeight());
	}

	public void setRenderLogo(boolean b) {
		this.renderLogo = b;
	}

	@Override
	protected void _update() {
		if (this.activeState != null) {
			this.activeState.update();
		}

		if (this.loadState != null) {
			this.loadState.update();
		}

		if (this.loadState != null && this.loadState.isFinishedLoading() && this.activeState != this.loadState.getNextState()) {
			if (this.activeState != null) {
				this.activeState.kill();
			}
			this.activeState = this.loadState.getNextState();
			this.activeState.setStateManager(this);

			this.setAllowInput(true);
		}
	}

	@Override
	protected void renderContent(Framebuffer outputBuffer) {
		this.uiScreen.setUIScene(BACKGROUND_SCENE);
		this.uiScreen.render(outputBuffer);

		if (this.renderLogo) {
			this.uiScreen.setUIScene(LOGO_SCENE);
			this.uiScreen.render(outputBuffer);
		}

		if (this.activeState != null) {
			this.activeState.render(outputBuffer);
		}
	}

	@Override
	protected void renderOverlay(Framebuffer outputBuffer) {
		if (this.loadState != null) {
			this.loadState.render(outputBuffer);
		}
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
		if (this.activeState != null) {
			this.activeState.mousePressed(button);
		}
	}

	@Override
	protected void _mouseReleased(int button) {
		if (this.activeState != null) {
			this.activeState.mouseReleased(button);
		}
	}

	@Override
	protected void _mouseScrolled(float wheelOffset, float smoothOffset) {
		if (this.activeState != null) {
			this.activeState.mouseScrolled(wheelOffset, smoothOffset);
		}
	}

	@Override
	protected void _keyPressed(int key) {
		if (this.activeState != null) {
			this.activeState.keyPressed(key);
		}
	}

	@Override
	protected void _keyReleased(int key) {
		if (this.activeState != null) {
			this.activeState.keyReleased(key);
		}
	}

}
