package lwjglengine.v10.project;

import java.awt.Color;
import java.io.File;

import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.graphics.Material;
import lwjglengine.v10.input.Button;
import lwjglengine.v10.input.Input;
import lwjglengine.v10.scene.Scene;
import lwjglengine.v10.screen.UIScreen;
import lwjglengine.v10.ui.Text;
import lwjglengine.v10.ui.UIElement;
import lwjglengine.v10.ui.UIFilledRectangle;
import lwjglengine.v10.window.AdjustableWindow;
import lwjglengine.v10.window.FileExplorerWindow;
import lwjglengine.v10.window.Window;
import myutils.v10.math.Vec3;

public class ProjectImportAssetWindow extends Window {
	
	//allows user to import assets to the selected project. 
	
	private final int CONTENT_BACKGROUND_SCENE = Scene.generateScene();
	private final int CONTENT_SELECTION_SCENE = Scene.generateScene();
	private final int CONTENT_TEXT_SCENE = Scene.generateScene();
	
	private UIScreen uiScreen;
	
	private UIFilledRectangle contentBackgroundRect;
	private Text contentImportPromptText;
	private Button contentImportButton;
	
	private Project project;
	
	private long hoveredSectionID;
	private long hoveredContentID;

	public ProjectImportAssetWindow(int xOffset, int yOffset, int width, int height, Project project, Window parentWindow) {
		super(xOffset, yOffset, width, height, parentWindow);
		this.init(project);
	}
	
	public ProjectImportAssetWindow(Project project, Window parentWindow) {
		super(0, 0, 300, 300, parentWindow);
		this.init(project);
	}
	
	private void init(Project project) {
		this.project = project;
		
		this.uiScreen = new UIScreen();
		
		this.contentBackgroundRect = new UIFilledRectangle(0, 0, 0, this.getWidth(), this.getHeight(), CONTENT_BACKGROUND_SCENE);
		this.contentBackgroundRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_BOTTOM);
		this.contentBackgroundRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_BOTTOM);
		this.contentBackgroundRect.setMaterial(new Material(new Vec3(40 / 255.0f)));
		this.contentBackgroundRect.setFillWidth(true);
		this.contentBackgroundRect.setFillHeight(true);
		this.contentBackgroundRect.bind(this.rootUIElement);
		
		this.contentImportPromptText = new Text(0, 10, "Import file to project: " + project.getProjectName(), 12, Color.WHITE, CONTENT_TEXT_SCENE);
		this.contentImportPromptText.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_TOP);
		this.contentImportPromptText.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_BOTTOM);
		this.contentImportPromptText.setDoAntialiasing(false);
		this.contentImportPromptText.bind(this.contentBackgroundRect);
		
		this.contentImportButton = new Button(0, 0, 100, 20, "btn_import", "Select File", 12, CONTENT_SELECTION_SCENE, CONTENT_TEXT_SCENE);
		this.contentImportButton.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_BOTTOM);
		this.contentImportButton.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_TOP);
		this.contentImportButton.getButtonText().setDoAntialiasing(false);
		this.contentImportButton.bind(this.contentBackgroundRect);
		
		this._resize();
	}
	
	@Override
	public void handleFile(File file) {
		this.project.addAsset(file);
	}

	@Override
	protected void _kill() {
		this.uiScreen.kill();
		
		Scene.removeScene(CONTENT_BACKGROUND_SCENE);
		Scene.removeScene(CONTENT_SELECTION_SCENE);
		Scene.removeScene(CONTENT_TEXT_SCENE);
	}

	@Override
	protected void _resize() {
		this.uiScreen.setScreenDimensions(this.getWidth(), this.getHeight());
	}

	@Override
	protected void _update() {
		Input.inputsHovered(this.hoveredContentID, CONTENT_SELECTION_SCENE);
	}

	@Override
	protected void renderContent(Framebuffer outputBuffer) {
		int mouseX = (int) this.getWindowMousePos().x;
		int mouseY = (int) this.getWindowMousePos().y;
		
		this.uiScreen.setUIScene(CONTENT_BACKGROUND_SCENE);
		this.uiScreen.render(outputBuffer);		
		if(this.uiScreen.getEntityIDAtCoord(mouseX, mouseY) == this.contentBackgroundRect.getID()) {
			this.hoveredSectionID = this.contentBackgroundRect.getID();
		}
		this.uiScreen.setUIScene(CONTENT_SELECTION_SCENE);
		this.uiScreen.render(outputBuffer);
		this.hoveredContentID = this.uiScreen.getEntityIDAtCoord(mouseX, mouseY);
		this.uiScreen.setUIScene(CONTENT_TEXT_SCENE);
		this.uiScreen.render(outputBuffer);
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
		Input.inputsPressed(this.hoveredContentID, CONTENT_SELECTION_SCENE);
	}

	@Override
	protected void _mouseReleased(int button) {
		Input.inputsReleased(this.hoveredContentID, CONTENT_SELECTION_SCENE);
		switch(Input.getClicked(CONTENT_SELECTION_SCENE)) {
		case "btn_import":
			Window fileExplorer = new AdjustableWindow((int) this.getWindowMousePos().x, (int) this.getWindowMousePos().y, 400, 400, "File Explorer", new FileExplorerWindow(this), this);
			break;
		}
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

}
