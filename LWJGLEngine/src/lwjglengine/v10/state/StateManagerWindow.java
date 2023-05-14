package lwjglengine.v10.state;

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

import lwjglengine.v10.entity.Entity;
import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.graphics.Material;
import lwjglengine.v10.graphics.Shader;
import lwjglengine.v10.graphics.Texture;
import lwjglengine.v10.graphics.TextureMaterial;
import lwjglengine.v10.main.Main;
import lwjglengine.v10.model.FilledRectangle;
import lwjglengine.v10.model.Model;
import lwjglengine.v10.project.ProjectManagerWindow;
import lwjglengine.v10.scene.Scene;
import lwjglengine.v10.screen.ScreenQuad;
import lwjglengine.v10.screen.UIScreen;
import lwjglengine.v10.ui.UIElement;
import lwjglengine.v10.ui.UIFilledRectangle;
import lwjglengine.v10.window.AdjustableWindow;
import lwjglengine.v10.window.FileExplorerWindow;
import lwjglengine.v10.window.ModelViewerWindow;
import lwjglengine.v10.window.RaytracingWindow;
import lwjglengine.v10.window.Window;
import myutils.v10.math.Vec3;
import myutils.v10.math.Vec4;

public class StateManagerWindow extends Window {
	//this window is it's own root window?

	private final int BACKGROUND_SCENE = Scene.generateScene();
	private final int LOGO_SCENE = Scene.generateScene();

	private UIScreen uiScreen;

	private UIFilledRectangle backgroundRect;
	private UIFilledRectangle logoIconRect;

	private boolean renderLogo = true;

	public State activeState;
	public LoadState loadState;

	public StateManagerWindow(int xOffset, int yOffset, int contentWidth, int contentHeight, Window parentWindow) {
		super(xOffset, yOffset, contentWidth, contentHeight, parentWindow);
		this.init();
	}

	public StateManagerWindow() {
		super(0, 0, Main.windowWidth, Main.windowHeight, null);
		this.init();
	}

	private void init() {
		this.activeState = null;
		this.loadState = new LoadState(new SplashState());
		this.loadState.setStateManager(this);

		this.uiScreen = new UIScreen();

		this.backgroundRect = new UIFilledRectangle(0, 0, 0, this.getWidth(), this.getHeight(), BACKGROUND_SCENE);
		this.backgroundRect.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_TOP);
		this.backgroundRect.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_CENTER);
		this.backgroundRect.setMaterial(new Material(new Vec4(0, 0, 0, 1)));
		this.backgroundRect.bind(this.rootUIElement);

		TextureMaterial logoIconTexture = new TextureMaterial(new Texture("Halfcup_icon_white.png", Texture.VERTICAL_FLIP_BIT));
		TextureMaterial logoTexture = new TextureMaterial(new Texture("Halfcup_logo_v2.png", Texture.VERTICAL_FLIP_BIT));

		this.logoIconRect = new UIFilledRectangle(0, 0, 0, 600, 200, new FilledRectangle(), LOGO_SCENE);
		this.logoIconRect.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_TOP);
		this.logoIconRect.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_CENTER);
		this.logoIconRect.setMaterial(new Material(new Vec3(30 / 255.0f)));
		this.logoIconRect.setTextureMaterial(logoTexture);
		this.logoIconRect.bind(this.rootUIElement);

		this.setContextMenuRightClick(true);

		ArrayList<String> contextMenuOptions = new ArrayList<>();
		contextMenuOptions.add("New File Explorer Window");
		contextMenuOptions.add("New Raytracing Window");
		contextMenuOptions.add("New Model Viewer Window");
		contextMenuOptions.add("New Project Manager Window");

		this.setContextMenuOptions(contextMenuOptions);

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

		case "New Model Viewer Window":
			Window modelViewer = new AdjustableWindow((int) this.getWindowMousePos().x, (int) this.getWindowMousePos().y, 400, 400, "Model Viewer", new ModelViewerWindow(null), this);
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

	public void killAllChildren() {
		for (Window w : this.childWindows) {
			w.kill();
		}
		this.childWindows.clear();
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

		if (this.loadState != null) {
			this.loadState.render(outputBuffer);
		}
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
