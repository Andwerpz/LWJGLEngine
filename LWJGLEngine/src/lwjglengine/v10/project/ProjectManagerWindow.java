package lwjglengine.v10.project;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import lwjglengine.v10.entity.ProjectEntityViewerWindow;
import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.graphics.Material;
import lwjglengine.v10.input.Button;
import lwjglengine.v10.input.Input;
import lwjglengine.v10.input.TextField;
import lwjglengine.v10.scene.Scene;
import lwjglengine.v10.screen.UIScreen;
import lwjglengine.v10.ui.Text;
import lwjglengine.v10.ui.UIElement;
import lwjglengine.v10.ui.UIFilledRectangle;
import lwjglengine.v10.window.AdjustableWindow;
import lwjglengine.v10.window.FileExplorerWindow;
import lwjglengine.v10.window.Window;
import myutils.v10.math.Vec3;

public class ProjectManagerWindow extends Window {

	//TODO
	// - should offer to create a new project or open one if a project isn't currently selected. 	

	private final int SELECT_PROJECT_BACKGROUND_SCENE = Scene.generateScene();
	private final int SELECT_PROJECT_SELECTION_SCENE = Scene.generateScene();
	private final int SELECT_PROJECT_TEXT_SCENE = Scene.generateScene();

	private UIFilledRectangle selectProjectBackgroundRect;
	private Text selectProjectText;
	private Button selectProjectImportButton, selectProjectCreateNewButton;
	private TextField selectProjectProjectNameTf;

	private String newProjectName = null;

	private final int TOP_BAR_BACKGROUND_SCENE = Scene.generateScene();
	private final int TOP_BAR_SELECTION_SCENE = Scene.generateScene();
	private final int TOP_BAR_TEXT_SCENE = Scene.generateScene();

	private Window contentWindow = null;

	private UIScreen uiScreen;

	private Project project = null;
	private boolean hasProject = false;

	public static int topBarHeightPx = 20;
	private UIFilledRectangle topBarBackgroundRect;
	public static Material topBarMaterial = new Material(new Vec3((float) (20 / 255.0)));
	public static Material topBarHoveredMaterial = new Material(new Vec3((float) (30 / 255.0)));
	public static Material topBarSelectedMaterial = new Material(new Vec3((float) (40 / 255.0)));

	public static Material selectProjectBackgroundMaterial = new Material(new Vec3((float) (40 / 255.0)));

	private ArrayList<TopBarEntry> topBarEntries;

	private long hoveredSectionID;
	private long hoveredTopBarID, hoveredSelectProjectID;

	private TopBarEntry selectedTopBarEntry = null;

	public ProjectManagerWindow(int xOffset, int yOffset, int width, int height, Window parentWindow) {
		super(xOffset, yOffset, width, height, parentWindow);
		this.init();
	}

	public ProjectManagerWindow() {
		super(0, 0, 300, 300, null);
		this.init();
	}

	private void init() {
		this.uiScreen = new UIScreen();

		this.selectProjectBackgroundRect = new UIFilledRectangle(0, 0, 0, this.getWidth(), this.getHeight(), SELECT_PROJECT_BACKGROUND_SCENE);
		this.selectProjectBackgroundRect.setFillWidth(true);
		this.selectProjectBackgroundRect.setFillHeight(true);
		this.selectProjectBackgroundRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_BOTTOM);
		this.selectProjectBackgroundRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_BOTTOM);
		this.selectProjectBackgroundRect.setMaterial(selectProjectBackgroundMaterial);
		this.selectProjectBackgroundRect.bind(this.rootUIElement);

		this.selectProjectText = new Text(0, 10, "Create a new project, or import an existing one", 12, Color.WHITE, SELECT_PROJECT_TEXT_SCENE);
		this.selectProjectText.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_TOP);
		this.selectProjectText.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_BOTTOM);
		this.selectProjectText.setDoAntialiasing(false);
		this.selectProjectText.bind(this.selectProjectBackgroundRect);

		this.selectProjectCreateNewButton = new Button(10, 0, 100, 20, "btn_create_new_project", "New Project", 12, SELECT_PROJECT_SELECTION_SCENE, SELECT_PROJECT_TEXT_SCENE);
		this.selectProjectCreateNewButton.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_BOTTOM);
		this.selectProjectCreateNewButton.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_TOP);
		this.selectProjectCreateNewButton.getButtonText().setDoAntialiasing(false);
		this.selectProjectCreateNewButton.bind(this.selectProjectBackgroundRect);

		this.selectProjectImportButton = new Button(10, 0, 100, 20, "btn_import_project", "Import Project", 12, SELECT_PROJECT_SELECTION_SCENE, SELECT_PROJECT_TEXT_SCENE);
		this.selectProjectImportButton.setFrameAlignmentStyle(UIElement.FROM_CENTER_RIGHT, UIElement.FROM_CENTER_BOTTOM);
		this.selectProjectImportButton.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		this.selectProjectImportButton.getButtonText().setDoAntialiasing(false);
		this.selectProjectImportButton.bind(this.selectProjectBackgroundRect);

		this.selectProjectProjectNameTf = new TextField(10, 30, 100, 20, "tf_project_name", "Project Name", 12, SELECT_PROJECT_SELECTION_SCENE, SELECT_PROJECT_TEXT_SCENE);
		this.selectProjectProjectNameTf.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_BOTTOM);
		this.selectProjectProjectNameTf.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_TOP);
		this.selectProjectProjectNameTf.getTextUIElement().setDoAntialiasing(false);
		this.selectProjectProjectNameTf.bind(this.selectProjectBackgroundRect);

		this.topBarBackgroundRect = new UIFilledRectangle(0, 0, 0, this.getWidth(), topBarHeightPx, TOP_BAR_BACKGROUND_SCENE);
		this.topBarBackgroundRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.topBarBackgroundRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		this.topBarBackgroundRect.setFillWidth(true);
		this.topBarBackgroundRect.setFillWidthMargin(0);
		this.topBarBackgroundRect.setMaterial(topBarMaterial);
		this.topBarBackgroundRect.bind(this.rootUIElement);

		this.topBarEntries = new ArrayList<>();
		this.topBarEntries.add(new TopBarEntry("Assets", this.topBarBackgroundRect, TOP_BAR_SELECTION_SCENE, TOP_BAR_TEXT_SCENE));
		this.topBarEntries.add(new TopBarEntry("States", this.topBarBackgroundRect, TOP_BAR_SELECTION_SCENE, TOP_BAR_TEXT_SCENE));
		this.topBarEntries.add(new TopBarEntry("Entities", this.topBarBackgroundRect, TOP_BAR_SELECTION_SCENE, TOP_BAR_TEXT_SCENE));
		this.topBarEntries.add(new TopBarEntry("Import", this.topBarBackgroundRect, TOP_BAR_SELECTION_SCENE, TOP_BAR_TEXT_SCENE));
		this.topBarEntries.add(new TopBarEntry("Dependency Graph", this.topBarBackgroundRect, TOP_BAR_SELECTION_SCENE, TOP_BAR_TEXT_SCENE));

		int xOffset = 0;
		for (int i = 0; i < this.topBarEntries.size(); i++) {
			this.topBarEntries.get(i).align(xOffset);
			xOffset += this.topBarEntries.get(i).getWidth();
		}

		this._resize();
	}

	@Override
	protected void _kill() {
		if (this.project != null) {
			this.project.kill();
		}

		this.uiScreen.kill();

		Scene.removeScene(TOP_BAR_BACKGROUND_SCENE);
		Scene.removeScene(TOP_BAR_SELECTION_SCENE);
		Scene.removeScene(TOP_BAR_TEXT_SCENE);
	}

	@Override
	protected void _resize() {
		this.uiScreen.setScreenDimensions(this.getWidth(), this.getHeight());

		if (this.contentWindow != null) {
			this.contentWindow.setDimensions(this.getWidth(), this.getHeight() - topBarHeightPx);
		}
	}

	@Override
	protected void _update() {
		Input.inputsHovered(this.hoveredSelectProjectID, SELECT_PROJECT_SELECTION_SCENE);

		for (int i = 0; i < this.topBarEntries.size(); i++) {
			this.topBarEntries.get(i).hovered(this.hoveredTopBarID);
			this.topBarEntries.get(i).update();
		}
	}

	@Override
	protected void renderContent(Framebuffer outputBuffer) {
		int mouseX = (int) this.getWindowMousePos().x;
		int mouseY = (int) this.getWindowMousePos().y;

		this.uiScreen.setUIScene(TOP_BAR_BACKGROUND_SCENE);
		this.uiScreen.render(outputBuffer);
		if (this.uiScreen.getEntityIDAtCoord(mouseX, mouseY) == this.topBarBackgroundRect.getID()) {
			this.hoveredSectionID = this.topBarBackgroundRect.getID();
		}
		this.uiScreen.setUIScene(TOP_BAR_SELECTION_SCENE);
		this.uiScreen.render(outputBuffer);
		this.hoveredTopBarID = this.uiScreen.getEntityIDAtCoord(mouseX, mouseY);
		this.uiScreen.setUIScene(TOP_BAR_TEXT_SCENE);
		this.uiScreen.render(outputBuffer);

		if (!this.hasProject) {
			this.uiScreen.setUIScene(SELECT_PROJECT_BACKGROUND_SCENE);
			this.uiScreen.render(outputBuffer);
			if (this.uiScreen.getEntityIDAtCoord(mouseX, mouseY) == this.selectProjectBackgroundRect.getID()) {
				this.hoveredSectionID = this.selectProjectBackgroundRect.getID();
			}
			this.uiScreen.setUIScene(SELECT_PROJECT_SELECTION_SCENE);
			this.uiScreen.render(outputBuffer);
			this.hoveredSelectProjectID = this.uiScreen.getEntityIDAtCoord(mouseX, mouseY);
			this.uiScreen.setUIScene(SELECT_PROJECT_TEXT_SCENE);
			this.uiScreen.render(outputBuffer);
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
	public void handleFiles(File[] files) {
		if (files.length != 1) {
			return;
		}

		if (!this.hasProject) {
			boolean failedToLoad = false;
			Project project = null;

			if (this.newProjectName != null) {
				//create a new project in this directory. 
				try {
					project = Project.createNewProject(files[0], this.newProjectName);
				}
				catch (IOException e) {
					e.printStackTrace();
					failedToLoad = true;
					return;
				}
			}
			else {
				//load the project
				try {
					project = new Project(files[0]);
				}
				catch (IOException e) {
					e.printStackTrace();
					failedToLoad = true;
					return;
				}
			}

			if (failedToLoad) {
				System.err.println("Project failed to load");
				return;
			}

			this.hasProject = true;
			this.project = project;
			this.project.setIsEditing(true);
		}
	}

	@Override
	protected void _mousePressed(int button) {
		Input.inputsPressed(this.hoveredTopBarID, TOP_BAR_SELECTION_SCENE);
		Input.inputsPressed(this.hoveredSelectProjectID, SELECT_PROJECT_SELECTION_SCENE);

		if (this.hoveredSectionID == this.topBarBackgroundRect.getID()) {
			if (this.hoveredTopBarID != this.topBarBackgroundRect.getID()) {
				TopBarEntry nextEntry = null;

				for (int i = 0; i < this.topBarEntries.size(); i++) {
					this.topBarEntries.get(i).select(this.hoveredTopBarID);
					if (this.topBarEntries.get(i).isSelected()) {
						nextEntry = this.topBarEntries.get(i);
					}
				}

				if (this.selectedTopBarEntry != nextEntry) {
					this.selectedTopBarEntry = nextEntry;

					//switch the current content window
					if (this.contentWindow != null) {
						this.contentWindow.kill();
						this.contentWindow = null;
					}

					if (this.selectedTopBarEntry != null) {
						switch (this.selectedTopBarEntry.getText()) {
						case "Assets":
							this.contentWindow = new ProjectAssetViewerWindow(0, 0, this.getWidth(), this.getHeight() - topBarHeightPx, this.project, this);
							break;

						case "States":
							ProjectStateViewerWindow psv = new ProjectStateViewerWindow(0, 0, this.getWidth(), this.getHeight() - topBarHeightPx, this.project, this, this);
							psv.setCloseOnSubmit(false);
							this.contentWindow = psv;
							break;
							
						case "Entities":
							ProjectEntityViewerWindow pev = new ProjectEntityViewerWindow(0, 0, this.getWidth(), this.getHeight() - topBarHeightPx, this.project, this, this);
							pev.setCloseOnSubmit(false);
							this.contentWindow = pev;
							break;

						case "Import":
							this.contentWindow = new ProjectImportAssetWindow(0, 0, this.getWidth(), this.getHeight() - topBarHeightPx, this.project, this);
							break;
						}
					}
				}
			}
		}
	}

	@Override
	protected void _mouseReleased(int button) {
		Input.inputsReleased(this.hoveredSelectProjectID, SELECT_PROJECT_SELECTION_SCENE);
		System.out.println(Input.getClicked(SELECT_PROJECT_SELECTION_SCENE));
		switch (Input.getClicked(SELECT_PROJECT_SELECTION_SCENE)) {
		case "btn_create_new_project": {
			String newProjectName = Input.getText("tf_project_name");
			if (newProjectName.length() == 0) {
				return;
			}
			this.newProjectName = newProjectName;
			AdjustableWindow fileExplorer = new AdjustableWindow((int) this.getWindowMousePos().x, (int) this.getWindowMousePos().y, 400, 400, "Select New Project Directory", new FileExplorerWindow(this), this);
			FileExplorerWindow w = (FileExplorerWindow) fileExplorer.getContentWindow();
			w.setSingleEntrySelection(true);
			break;
		}

		case "btn_import_project": {
			this.newProjectName = null;
			AdjustableWindow fileExplorer = new AdjustableWindow((int) this.getWindowMousePos().x, (int) this.getWindowMousePos().y, 400, 400, "Select Project Directory", new FileExplorerWindow(this), this);
			FileExplorerWindow w = (FileExplorerWindow) fileExplorer.getContentWindow();
			w.setSingleEntrySelection(true);
			break;
		}
		}

		Input.inputsReleased(this.hoveredTopBarID, TOP_BAR_SELECTION_SCENE);
	}

	@Override
	protected void _mouseScrolled(float wheelOffset, float smoothOffset) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _keyPressed(int key) {
		Input.inputsKeyPressed(key, SELECT_PROJECT_SELECTION_SCENE);
	}

	@Override
	protected void _keyReleased(int key) {
		Input.inputsKeyReleased(key, SELECT_PROJECT_SELECTION_SCENE);
	}

}

class TopBarEntry {

	private static int entryHorizontalMargin = 5;

	private int selectionScene, textScene;

	private UIFilledRectangle entryRect;
	private Text entryText;

	private boolean isHovered = false;
	private boolean isSelected = false;

	public TopBarEntry(String text, UIElement rootUIElement, int selectionScene, int textScene) {
		this.selectionScene = selectionScene;
		this.textScene = textScene;

		this.entryRect = new UIFilledRectangle(0, 0, 0, 10, ProjectManagerWindow.topBarHeightPx, this.selectionScene);
		this.entryRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_BOTTOM);
		this.entryRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_BOTTOM);
		this.entryRect.setMaterial(ProjectManagerWindow.topBarMaterial);
		this.entryRect.bind(rootUIElement);

		this.entryText = new Text(0, 0, text, 12, Color.WHITE, this.textScene);
		this.entryText.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_TOP);
		this.entryText.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_CENTER);
		this.entryText.setDoAntialiasing(false);
		this.entryText.bind(this.entryRect);

		this.entryRect.setWidth(this.entryText.getTextWidth() + entryHorizontalMargin * 2);
	}

	public String getText() {
		return this.entryText.getText();
	}

	public int getWidth() {
		return (int) this.entryRect.getWidth();
	}

	public void align(int xOffset) {
		this.entryRect.setXOffset(xOffset);
	}

	public void select(long entityID) {
		this.isSelected = this.entryRect.getID() == entityID;
	}

	public void hovered(long entityID) {
		this.isHovered = this.entryRect.getID() == entityID;
	}

	public void update() {
		if (this.isSelected) {
			this.entryRect.setMaterial(ProjectManagerWindow.topBarSelectedMaterial);
		}
		else if (this.isHovered) {
			this.entryRect.setMaterial(ProjectManagerWindow.topBarHoveredMaterial);
		}
		else {
			this.entryRect.setMaterial(ProjectManagerWindow.topBarMaterial);
		}
	}

	public boolean isHovered() {
		return this.isHovered;
	}

	public boolean isSelected() {
		return this.isSelected;
	}

}
