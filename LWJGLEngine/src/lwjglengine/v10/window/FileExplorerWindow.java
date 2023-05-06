package lwjglengine.v10.window;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.filechooser.FileSystemView;

import org.lwjgl.glfw.GLFW;

import lwjglengine.v10.entity.Entity;
import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.graphics.Material;
import lwjglengine.v10.input.Input;
import lwjglengine.v10.input.Button;
import lwjglengine.v10.input.TextField;
import lwjglengine.v10.model.Model;
import lwjglengine.v10.scene.Scene;
import lwjglengine.v10.screen.UIScreen;
import lwjglengine.v10.ui.Text;
import lwjglengine.v10.ui.UIElement;
import lwjglengine.v10.ui.UIFilledRectangle;
import myutils.v11.file.FileUtils;
import myutils.v10.math.MathUtils;
import myutils.v10.math.Vec3;
import myutils.v10.math.Vec4;

public class FileExplorerWindow extends Window {
	//TODO 
	// - figure out how other things are going to call this one, and retrieve the submitted file
	// - qol
	//   - icons? maybe just icons in the folder rect to tell the user which ones are folders
	//   - text indication when you didn't select a directory in the text rect?
	//   - text indication when the filter filters out all files in folder

	private final int DIRECTORY_BACKGROUND_SCENE = Scene.generateScene();
	private final int DIRECTORY_SELECTION_SCENE = Scene.generateScene();
	private final int DIRECTORY_TEXT_SCENE = Scene.generateScene();

	private final int FOLDER_SCENE = Scene.generateScene();
	private final int FOLDER_SELECTION_SCENE = Scene.generateScene();
	private final int FOLDER_TEXT_SCENE = Scene.generateScene();

	private final int TOP_BAR_SCENE = Scene.generateScene();
	private final int TOP_BAR_SELECTION_SCENE = Scene.generateScene();
	private final int TOP_BAR_TEXT_SCENE = Scene.generateScene();

	private final int BOTTOM_BAR_SCENE = Scene.generateScene();
	private final int BOTTOM_BAR_SELECTION_SCENE = Scene.generateScene();
	private final int BOTTOM_BAR_TEXT_SCENE = Scene.generateScene();

	public static int entryHeight = 16;
	public static int entryXOffsetInterval = 10;
	public static int entryXOffsetBase = 5;

	private UIScreen uiScreen;

	public static int topBarHeight = 24;
	private UIFilledRectangle topBarRect;
	public static Material topBarMaterial = new Material(new Vec3((float) (20 / 255.0)));

	private Button topBarBackButton;
	private Text topBarPathText;
	private TextField topBarFilterTextField;

	private static int topBarFilterTextFieldWidth = 200;

	private static int directoryMinWidth = 75;
	private int directoryWidth = 200;
	private UIFilledRectangle directoryRect;
	public static Material directoryMaterial = new Material(new Vec3((float) (20 / 255.0)));

	public static int directoryGrabTolerancePx = 4;
	private boolean directoryGrabbed = false;

	private UIFilledRectangle folderRect;
	public static Material folderMaterial = new Material(new Vec3((float) (40 / 255.0)));

	private static int bottomBarHeight = 24;
	private UIFilledRectangle bottomBarRect;
	public static Material bottomBarMaterial = new Material(new Vec3((float) (20 / 255.0)));
	private Text bottomBarSelectedFileText;
	private Button bottomBarSubmitFileButton;

	private DirectoryEntry rootDirectoryEntry;
	private int directoryYOffset = 0;

	private DirectoryEntry selectedDirectoryEntry = null;

	private ArrayList<FolderEntry> folderEntries;
	private int folderYOffset = 0;
	private FolderEntry selectedFolderEntry = null;
	private String folderEntryFilter = "";

	private long hoveredDirectoryEntryID = -1;
	private long hoveredFolderEntryID = -1;
	private long hoveredSectionID = -1;

	private long hoveredTopBarID = -1;
	private long hoveredBottomBarID = -1;

	private File submittedFile = null;

	public FileExplorerWindow() {
		super(0, 0, 300, 400, null);
		this.init();
	}

	private void init() {
		this.uiScreen = new UIScreen();

		this.directoryRect = new UIFilledRectangle(0, topBarHeight, 0, this.directoryWidth, this.getHeight() - topBarHeight - bottomBarHeight, DIRECTORY_BACKGROUND_SCENE);
		this.directoryRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.directoryRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		this.directoryRect.setMaterial(directoryMaterial);
		this.directoryRect.bind(this.rootUIElement);

		this.folderRect = new UIFilledRectangle(0, topBarHeight, 0, this.getWidth() - this.directoryWidth, this.getHeight() - topBarHeight - bottomBarHeight, FOLDER_SCENE);
		this.folderRect.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_TOP);
		this.folderRect.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_TOP);
		this.folderRect.setMaterial(folderMaterial);
		this.folderRect.bind(this.rootUIElement);

		this.topBarRect = new UIFilledRectangle(0, 0, 0, this.getWidth(), topBarHeight, TOP_BAR_SCENE);
		this.topBarRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.topBarRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		this.topBarRect.setFillWidth(true);
		this.topBarRect.setFillWidthMargin(0);
		this.topBarRect.setMaterial(topBarMaterial);
		this.topBarRect.bind(this.rootUIElement);

		this.bottomBarRect = new UIFilledRectangle(0, 0, 0, this.getWidth(), topBarHeight, BOTTOM_BAR_SCENE);
		this.bottomBarRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_BOTTOM);
		this.bottomBarRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_BOTTOM);
		this.bottomBarRect.setFillWidth(true);
		this.bottomBarRect.setFillWidthMargin(0);
		this.bottomBarRect.setMaterial(bottomBarMaterial);
		this.bottomBarRect.bind(this.rootUIElement);

		this.rootDirectoryEntry = new DirectoryEntry(null, "", "", this.directoryRect, DIRECTORY_SELECTION_SCENE, DIRECTORY_TEXT_SCENE);
		this.rootDirectoryEntry.display();
		this.rootDirectoryEntry.expand();
		this.rootDirectoryEntry.align(this.directoryYOffset);

		this.folderEntries = new ArrayList<>();

		this.topBarBackButton = new Button(3, 0, 20, 20, "btn_directory_back", "          ", new Font("Dialog", Font.PLAIN, 12), 12, TOP_BAR_SELECTION_SCENE, TOP_BAR_TEXT_SCENE);
		this.topBarBackButton.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_TOP);
		this.topBarBackButton.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
		this.topBarBackButton.setReleasedMaterial(new Material(new Vec3(100 / 255.0f)));
		this.topBarBackButton.setHoveredMaterial(new Material(new Vec3(150 / 255.0f)));
		this.topBarBackButton.setPressedMaterial(new Material(new Vec3(200 / 255.0f)));
		this.topBarBackButton.bind(this.topBarRect);

		this.topBarFilterTextField = new TextField(3, 0, topBarFilterTextFieldWidth, 20, "tf_filter", "Search Folder", new Font("Dialog", Font.PLAIN, 12), 12, TOP_BAR_SELECTION_SCENE, TOP_BAR_TEXT_SCENE);
		this.topBarFilterTextField.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_CENTER_TOP);
		this.topBarFilterTextField.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
		this.topBarFilterTextField.getTextUIElement().setDoAntialiasing(false);
		this.topBarFilterTextField.setReleasedMaterial(new Material(new Vec3((float) (15 / 255.0))));
		this.topBarFilterTextField.setHoveredMaterial(new Material(new Vec3((float) (10 / 255.0))));
		this.topBarFilterTextField.setPressedMaterial(new Material(new Vec3((float) (5 / 255.0))));
		this.topBarFilterTextField.setSelectedMaterial(new Material(new Vec3((float) (5 / 255.0))));
		this.topBarFilterTextField.bind(this.topBarRect);

		this.topBarPathText = new Text(this.topBarBackButton.getRightBorder() + 5, 0, "          ", 12, Color.WHITE, TOP_BAR_TEXT_SCENE);
		this.topBarPathText.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_TOP);
		this.topBarPathText.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
		this.topBarPathText.setDoAntialiasing(false);
		this.topBarPathText.bind(this.topBarRect);

		this.bottomBarSelectedFileText = new Text(3, 0, "            ", 12, Color.WHITE, BOTTOM_BAR_TEXT_SCENE);
		this.bottomBarSelectedFileText.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_TOP);
		this.bottomBarSelectedFileText.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
		this.bottomBarSelectedFileText.setDoAntialiasing(false);
		this.bottomBarSelectedFileText.bind(this.bottomBarRect);

		this.bottomBarSubmitFileButton = new Button(3, 0, 100, 20, "btn_submit_file", "Select File", new Font("Dialog", Font.PLAIN, 12), 12, BOTTOM_BAR_SELECTION_SCENE, BOTTOM_BAR_TEXT_SCENE);
		this.bottomBarSubmitFileButton.getButtonText().setDoAntialiasing(false);
		this.bottomBarSubmitFileButton.setReleasedMaterial(new Material(new Vec3((float) (15 / 255.0))));
		this.bottomBarSubmitFileButton.setHoveredMaterial(new Material(new Vec3((float) (10 / 255.0))));
		this.bottomBarSubmitFileButton.setPressedMaterial(new Material(new Vec3((float) (5 / 255.0))));
		this.bottomBarSubmitFileButton.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_CENTER_TOP);
		this.bottomBarSubmitFileButton.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
		this.bottomBarSubmitFileButton.bind(this.bottomBarRect);

		this._resize();
	}

	@Override
	protected void _kill() {
		this.uiScreen.kill();

		this.rootDirectoryEntry.kill();

		Scene.removeScene(DIRECTORY_BACKGROUND_SCENE);
		Scene.removeScene(DIRECTORY_SELECTION_SCENE);
		Scene.removeScene(DIRECTORY_TEXT_SCENE);
		Scene.removeScene(FOLDER_SCENE);
		Scene.removeScene(FOLDER_SELECTION_SCENE);
		Scene.removeScene(FOLDER_TEXT_SCENE);
		Scene.removeScene(TOP_BAR_SCENE);
		Scene.removeScene(TOP_BAR_SELECTION_SCENE);
		Scene.removeScene(TOP_BAR_TEXT_SCENE);
		Scene.removeScene(BOTTOM_BAR_SCENE);
		Scene.removeScene(BOTTOM_BAR_SELECTION_SCENE);
		Scene.removeScene(BOTTOM_BAR_TEXT_SCENE);
	}

	@Override
	protected void _resize() {
		if (this.uiScreen != null) {
			this.uiScreen.setScreenDimensions(this.getWidth(), this.getHeight());
		}

		if (this.folderRect != null) {
			this.folderRect.setWidth(this.getWidth() - this.directoryWidth);
			this.folderRect.setHeight(this.getHeight() - topBarHeight - bottomBarHeight);

			this.setFolderYOffset(this.folderYOffset);
		}

		if (this.directoryRect != null) {
			this.directoryRect.setHeight(this.getHeight() - topBarHeight - bottomBarHeight);

			//realign entries
			this.setDirectoryYOffset(this.directoryYOffset);
		}

		if (this.topBarRect != null) {
			this.topBarPathText.setWidth(this.topBarRect.getWidth() - topBarFilterTextFieldWidth - this.topBarBackButton.getWidth() - 10);
		}

		if (this.bottomBarRect != null) {
			this.bottomBarSelectedFileText.setWidth(this.bottomBarRect.getWidth() - this.bottomBarSubmitFileButton.getWidth() - 6);
		}
	}

	@Override
	protected void _update() {
		Input.inputsHovered(this.hoveredTopBarID, TOP_BAR_SELECTION_SCENE);
		Input.inputsHovered(this.hoveredBottomBarID, BOTTOM_BAR_SELECTION_SCENE);

		for (FolderEntry e : this.folderEntries) {
			e.hovered(this.hoveredFolderEntryID);
			e.update();
		}

		this.rootDirectoryEntry.hovered(this.hoveredDirectoryEntryID);
		this.rootDirectoryEntry.update();

		//update directory width
		if (this.directoryGrabbed) {
			int newDirectoryWidth = (int) this.getWindowMousePos().x;
			newDirectoryWidth = Math.max(newDirectoryWidth, directoryMinWidth);
			this.directoryWidth = newDirectoryWidth;
			this.directoryRect.setWidth(this.directoryWidth);
			this.folderRect.setWidth(this.getWidth() - this.directoryWidth);
		}
	}

	@Override
	protected void renderContent(Framebuffer outputBuffer) {
		int mouseX = (int) this.getWindowMousePos().x;
		int mouseY = (int) this.getWindowMousePos().y;

		this.uiScreen.setUIScene(DIRECTORY_BACKGROUND_SCENE);
		this.uiScreen.render(outputBuffer);
		if (this.uiScreen.getEntityIDAtCoord(mouseX, mouseY) == this.directoryRect.getID()) {
			this.hoveredSectionID = this.directoryRect.getID();
		}

		this.uiScreen.setUIScene(DIRECTORY_SELECTION_SCENE);
		this.uiScreen.render(outputBuffer);
		this.hoveredDirectoryEntryID = this.uiScreen.getEntityIDAtCoord(mouseX, mouseY);
		this.uiScreen.setUIScene(DIRECTORY_TEXT_SCENE);
		this.uiScreen.render(outputBuffer);

		this.uiScreen.setUIScene(FOLDER_SCENE);
		this.uiScreen.render(outputBuffer);
		if (this.uiScreen.getEntityIDAtCoord(mouseX, mouseY) == this.folderRect.getID()) {
			this.hoveredSectionID = this.folderRect.getID();
		}

		this.uiScreen.setUIScene(FOLDER_SELECTION_SCENE);
		this.uiScreen.render(outputBuffer);
		this.hoveredFolderEntryID = this.uiScreen.getEntityIDAtCoord(mouseX, mouseY);
		this.uiScreen.setUIScene(FOLDER_TEXT_SCENE);
		this.uiScreen.render(outputBuffer);

		this.uiScreen.setUIScene(TOP_BAR_SCENE);
		this.uiScreen.render(outputBuffer);
		if (this.uiScreen.getEntityIDAtCoord(mouseX, mouseY) == this.topBarRect.getID()) {
			this.hoveredSectionID = this.topBarRect.getID();
		}

		this.uiScreen.setUIScene(TOP_BAR_SELECTION_SCENE);
		this.uiScreen.render(outputBuffer);
		this.hoveredTopBarID = this.uiScreen.getEntityIDAtCoord(mouseX, mouseY);
		this.uiScreen.setUIScene(TOP_BAR_TEXT_SCENE);
		this.uiScreen.render(outputBuffer);

		this.uiScreen.setUIScene(BOTTOM_BAR_SCENE);
		this.uiScreen.render(outputBuffer);
		if (this.uiScreen.getEntityIDAtCoord(mouseX, mouseY) == this.bottomBarRect.getID()) {
			this.hoveredSectionID = this.bottomBarRect.getID();
		}

		this.uiScreen.setUIScene(BOTTOM_BAR_SELECTION_SCENE);
		this.uiScreen.render(outputBuffer);
		this.hoveredBottomBarID = this.uiScreen.getEntityIDAtCoord(mouseX, mouseY);
		this.uiScreen.setUIScene(BOTTOM_BAR_TEXT_SCENE);
		this.uiScreen.render(outputBuffer);
	}

	@Override
	protected void renderOverlay(Framebuffer outputBuffer) {

	}

	public File getSubmittedFile() {
		return this.submittedFile;
	}

	private void setDirectoryYOffset(int newYOffset) {
		int directoryEntryTotalHeight = this.rootDirectoryEntry.countDisplayed() * entryHeight;

		int minYOffset = 0;
		int maxYOffset = (int) (directoryEntryTotalHeight - this.directoryRect.getHeight());
		maxYOffset = Math.max(maxYOffset, 0);
		this.directoryYOffset = (int) MathUtils.clamp(minYOffset, maxYOffset, newYOffset);

		this.rootDirectoryEntry.align(-this.directoryYOffset);
	}

	private void setSelectedDirectoryEntry(DirectoryEntry e) {
		if (e == this.selectedDirectoryEntry) {
			if (e == null) {
				return;
			}
			if (e.isExpanded()) {
				e.collapse();
			}
			else {
				e.expand();
			}
			this.setDirectoryYOffset(this.directoryYOffset);
			return;
		}

		//delete all old ui elements from folder section. 
		for (FolderEntry entry : this.folderEntries) {
			entry.kill();
		}
		this.folderEntries.clear();

		this.selectedDirectoryEntry = e;

		//reset the filter
		this.folderEntryFilter = "";
		this.topBarFilterTextField.setText("");

		if (e == null) {
			this.topBarPathText.setText("          ");
			return;
		}

		if (!this.selectedDirectoryEntry.isSelected()) {
			this.rootDirectoryEntry.selected(this.selectedDirectoryEntry.getPath());
		}

		this.topBarPathText.setText(e.getPath() + "          ");

		//if parents aren't expanded, go ahead and expand them
		ArrayList<DirectoryEntry> unexpandedParents = new ArrayList<>();
		DirectoryEntry ptr = this.selectedDirectoryEntry;
		while (!ptr.getParent().isExpanded()) {
			unexpandedParents.add(ptr.getParent());
			ptr = ptr.getParent();
		}
		for (int i = unexpandedParents.size() - 1; i >= 0; i--) {
			unexpandedParents.get(i).expand();
		}

		//make sure to generate children
		if (!this.selectedDirectoryEntry.childrenGenerated()) {
			this.selectedDirectoryEntry.generateChildren();
		}

		//make sure this directory entry is visible
		this.rootDirectoryEntry.computeOrder(-1);
		int numAbove = this.selectedDirectoryEntry.getOrder();
		int maxYOffset = numAbove * entryHeight;
		int minYOffset = maxYOffset - (int) this.directoryRect.getHeight() + entryHeight;
		System.out.println("MIN : " + minYOffset + " MAX : " + maxYOffset + " NUM ABOVE : " + numAbove);
		System.out.println(this.selectedDirectoryEntry.getFilename());
		this.setDirectoryYOffset((int) (MathUtils.clamp(minYOffset, maxYOffset, this.directoryYOffset)));

		String[] fileNames = FileUtils.getAllFilenamesFromDirectory(e.getPath());
		Arrays.sort(fileNames);
		for (int i = 0; i < fileNames.length; i++) {
			FolderEntry entry = new FolderEntry(fileNames[i], this.selectedDirectoryEntry.getPath(), this.folderRect, FOLDER_SELECTION_SCENE, FOLDER_TEXT_SCENE);
			this.folderEntries.add(entry);
		}

		this.setFolderEntryFilter("");

		this.setFolderYOffset(0);
	}

	private void setFolderEntryFilter(String filter) {
		this.folderEntryFilter = filter;

		this.setFolderYOffset(this.folderYOffset);
	}

	private void setFolderYOffset(int newYOffset) {
		ArrayList<FolderEntry> filtered = new ArrayList<>();
		for (int i = 0; i < this.folderEntries.size(); i++) {
			//check if folder entry is filtered out
			if (this.folderEntryFilter == "" || this.folderEntries.get(i).getFilename().toLowerCase().contains(this.folderEntryFilter.toLowerCase())) {
				filtered.add(this.folderEntries.get(i));
			}
			else {
				this.folderEntries.get(i).align(-entryHeight * 10);
			}
		}

		int folderEntryTotalHeight = filtered.size() * entryHeight;

		int minYOffset = 0;
		int maxYOffset = (int) (folderEntryTotalHeight - this.folderRect.getHeight());
		maxYOffset = Math.max(maxYOffset, 0);
		this.folderYOffset = (int) (MathUtils.clamp(minYOffset, maxYOffset, newYOffset));

		for (int i = 0; i < filtered.size(); i++) {
			filtered.get(i).align(i * entryHeight - this.folderYOffset);
		}
	}

	@Override
	protected void selected() {

	}

	@Override
	protected void deselected() {

	}

	@Override
	protected void _mousePressed(int button) {
		int mouseX = (int) this.getWindowMousePos().x;
		int mouseY = (int) this.getWindowMousePos().y;

		//see if user is trying to drag the directory window
		if (Math.abs(this.directoryWidth - mouseX) <= directoryGrabTolerancePx) {
			this.directoryGrabbed = true;
			return;
		}

		if (this.hoveredSectionID == this.directoryRect.getID()) {
			//select the directory entry
			this.rootDirectoryEntry.selected(this.hoveredDirectoryEntryID);

			DirectoryEntry e = this.rootDirectoryEntry.getSelected();
			this.setSelectedDirectoryEntry(e);

			this.setDirectoryYOffset(this.directoryYOffset);
		}
		else if (this.hoveredSectionID == this.folderRect.getID()) {
			FolderEntry selectedEntry = null;
			for (FolderEntry e : this.folderEntries) {
				e.selected(this.hoveredFolderEntryID);
				if (e.isSelected()) {
					selectedEntry = e;
				}
			}

			//check if we clicked on the selected entry twice
			if (selectedEntry != null && selectedEntry == this.selectedFolderEntry) {
				String filepath = this.selectedFolderEntry.getPath() + this.selectedFolderEntry.getFilename() + "\\";
				if (selectedEntry.isDirectory()) {
					//this is a folder. Open the directory entry that corresponds to this folder. 
					this.rootDirectoryEntry.selected(filepath);
					this.setSelectedDirectoryEntry(this.rootDirectoryEntry.getSelected());
					selectedEntry = null; //obviously, we won't have anything in the new folder selected
				}
			}

			this.selectedFolderEntry = selectedEntry;

			if (this.selectedFolderEntry != null && !this.selectedFolderEntry.isDirectory()) {
				this.bottomBarSelectedFileText.setText(this.selectedFolderEntry.getFilename() + "         ");
			}
			else {
				this.bottomBarSelectedFileText.setText("          ");
			}
		}
		else if (this.hoveredSectionID == this.topBarRect.getID()) {
			Input.inputsPressed(this.hoveredTopBarID);
		}
		else if (this.hoveredSectionID == this.bottomBarRect.getID()) {
			Input.inputsPressed(this.hoveredBottomBarID);
		}
	}

	@Override
	protected void _mouseReleased(int button) {
		Input.inputsReleased(this.hoveredTopBarID, TOP_BAR_SELECTION_SCENE);
		Input.inputsReleased(this.hoveredBottomBarID, BOTTOM_BAR_SELECTION_SCENE);

		switch (Input.getClicked(TOP_BAR_SELECTION_SCENE)) {
		case "btn_directory_back":
			if (this.selectedDirectoryEntry != null && this.selectedDirectoryEntry.getParent() != this.rootDirectoryEntry) {
				this.setSelectedDirectoryEntry(this.selectedDirectoryEntry.getParent());
			}
			break;
		}

		switch (Input.getClicked(BOTTOM_BAR_SELECTION_SCENE)) {
		case "btn_submit_file":
			if (this.selectedFolderEntry != null && !this.selectedFolderEntry.isDirectory()) {
				this.submittedFile = this.selectedFolderEntry.loadFile();
			}
			break;
		}

		this.directoryGrabbed = false;
	}

	@Override
	protected void _mouseScrolled(float wheelOffset, float smoothOffset) {
		if (this.hoveredSectionID == this.directoryRect.getID()) {
			this.setDirectoryYOffset(this.directoryYOffset - (int) (smoothOffset) * entryHeight);
		}
		else if (this.hoveredSectionID == this.folderRect.getID()) {
			this.setFolderYOffset(this.folderYOffset - (int) (smoothOffset) * entryHeight);
		}
	}

	@Override
	protected void _keyPressed(int key) {
		Input.inputsKeyPressed(key, TOP_BAR_SELECTION_SCENE);

		if (this.topBarFilterTextField.isClicked()) {
			//update the filter
			this.setFolderEntryFilter(this.topBarFilterTextField.getText());
		}
	}

	@Override
	protected void _keyReleased(int key) {
		Input.inputsKeyReleased(key, TOP_BAR_SELECTION_SCENE);
	}

}

class FolderEntry {

	public static Material defaultFolderEntryMaterial = new Material(new Vec3((float) (40 / 255.0)));
	public static Material hoveredFolderEntryMaterial = new Material(new Vec3((float) (50 / 255.0)));
	public static Material selectedFolderEntryMaterial = new Material(new Vec3((float) (60 / 255.0)));

	private boolean isVisible = true;

	private boolean isHovered = false;
	private boolean isSelected = false;

	private UIElement rootUIElement;
	private UIElement entryBaseUIElement = null;

	private String filename;
	private String path;
	private Text entryText = null;

	private int selectionScene, textScene;

	public FolderEntry(String filename, String path, UIElement rootUIElement, int selectionScene, int textScene) {
		this.filename = filename;
		this.path = path;
		this.rootUIElement = rootUIElement;
		this.selectionScene = selectionScene;
		this.textScene = textScene;
	}

	public void kill() {
		if (this.entryBaseUIElement != null) {
			this.entryBaseUIElement.kill();
			this.entryText.kill();
		}
	}

	public void update() {
		if (this.isVisible) {
			if (this.isSelected) {
				this.entryBaseUIElement.setMaterial(selectedFolderEntryMaterial);
			}
			else if (this.isHovered) {
				this.entryBaseUIElement.setMaterial(hoveredFolderEntryMaterial);
			}
			else {
				this.entryBaseUIElement.setMaterial(defaultFolderEntryMaterial);
			}
		}
	}

	public void hovered(long entityID) {
		if (!this.isVisible) {
			this.isHovered = false;
			return;
		}

		if (this.entryBaseUIElement.getID() == entityID) {
			this.isHovered = true;
		}
		else {
			this.isHovered = false;
		}
	}

	public void selected(long entityID) {
		if (!this.isVisible) {
			this.isSelected = false;
			return;
		}

		if (this.entryBaseUIElement.getID() == entityID) {
			this.isSelected = true;
		}
		else {
			this.isSelected = false;
		}
	}

	public boolean isSelected() {
		return this.isSelected;
	}

	public String getFilename() {
		return this.filename;
	}

	public String getPath() {
		return this.path;
	}

	public boolean isDirectory() {
		return this.loadFile().isDirectory();
	}

	public File loadFile() {
		File file = new File(this.getPath() + this.getFilename() + "\\");
		return file;
	}

	public void align(int yOffset) {
		this.isVisible = false;
		if (-FileExplorerWindow.entryHeight <= yOffset && yOffset <= this.rootUIElement.getHeight()) {
			this.isVisible = true;
		}

		if (this.isVisible) {
			if (this.entryBaseUIElement == null) {
				this.entryBaseUIElement = new UIFilledRectangle(0, 0, 0, this.rootUIElement.getWidth(), FileExplorerWindow.entryHeight, this.selectionScene);
				this.entryBaseUIElement.setFillWidth(true);
				this.entryBaseUIElement.setFillWidthMargin(0);
				this.entryBaseUIElement.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
				this.entryBaseUIElement.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
				this.entryBaseUIElement.setMaterial(defaultFolderEntryMaterial);
				this.entryBaseUIElement.bind(this.rootUIElement);

				this.entryText = new Text(FileExplorerWindow.entryXOffsetBase, 0, this.filename + "         ", 12, Color.WHITE, this.textScene);
				this.entryText.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_TOP);
				this.entryText.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
				this.entryText.setDoAntialiasing(false);
				this.entryText.bind(this.entryBaseUIElement);
			}

			this.entryBaseUIElement.setYOffset(yOffset);
		}
		else {
			if (this.entryBaseUIElement != null) {
				this.entryText.kill();
				this.entryBaseUIElement.kill();
				this.entryText = null;
				this.entryBaseUIElement = null;
			}
		}
	}

}

class DirectoryEntry {
	public static Material defaultDirectoryEntryMaterial = new Material(new Vec3((float) (20 / 255.0)));
	public static Material hoveredDirectoryEntryMaterial = new Material(new Vec3((float) (30 / 255.0)));
	public static Material selectedDirectoryEntryMaterial = new Material(new Vec3((float) (40 / 255.0)));

	private boolean isHovered = false;
	private boolean isSelected = false;

	//if this is null, then it means that we still need to find this entry's children. 
	//one special case is where this entry has no children, then we just set it to an empty arraylist. 
	private ArrayList<DirectoryEntry> children = null;
	private boolean childrenGenerated = false;

	private DirectoryEntry parent;

	private int selectionScene, textScene;

	private UIElement rootUIElement;

	private boolean isExpanded = false;
	private boolean isDisplayed = false;
	private boolean isVisible = false; //every time we align, we check if this entry is visible. 

	private UIElement entryBaseUIElement = null;

	private String path;

	private String filename;
	private Text entryText = null;

	private int xOffset, yOffset;

	private int order = -1;

	public DirectoryEntry(DirectoryEntry parent, String path, String filename, UIElement rootUIElement, int selectionScene, int textScene) {
		this.parent = parent;

		this.xOffset = 0;
		this.yOffset = 0;

		if (this.parent != null) {
			this.xOffset = this.parent.xOffset + FileExplorerWindow.entryXOffsetInterval;
		}
		else {
			this.xOffset = FileExplorerWindow.entryXOffsetBase - FileExplorerWindow.entryXOffsetInterval;
		}

		this.children = new ArrayList<>();

		this.path = path;
		this.filename = filename;
		if (path != "" && FileUtils.getAllFilesFromDirectory(path).length == 0) {
			this.children = new ArrayList<>();
		}

		this.rootUIElement = rootUIElement;

		this.selectionScene = selectionScene;
		this.textScene = textScene;
	}

	public static DirectoryEntry createDirectoryEntry(DirectoryEntry parent, String path, String filename, UIElement rootUIElement, int selectionScene, int textScene) {
		File[] files = FileUtils.getAllFilesFromDirectory(path);
		if (files == null) {
			//this file isn't a folder
			return null;
		}

		return new DirectoryEntry(parent, path, filename, rootUIElement, selectionScene, textScene);
	}

	public void kill() {
		if (this.entryBaseUIElement != null) {
			this.entryBaseUIElement.kill();
			this.entryText.kill();
		}

		if (this.children != null) {
			for (DirectoryEntry e : this.children) {
				e.kill();
			}
		}
	}

	public void align(int offset) {
		if (this.parent != null) {
			System.err.println("TRIED TO ALIGN DIRECTORY ENTRY THAT IS NOT ROOT");
			return;
		}

		this._align(-FileExplorerWindow.entryHeight + offset);
	}

	//aligns this subtree of directory entries
	//returns the height of this subtree
	private int _align(int yOffset) {
		if (!this.isDisplayed) {
			return 0;
		}
		int totalHeight = FileExplorerWindow.entryHeight;

		this.yOffset = yOffset;

		//check if this thing is visible
		this.isVisible = false;
		if (-FileExplorerWindow.entryHeight <= this.yOffset && this.yOffset <= this.rootUIElement.getHeight()) {
			this.isVisible = true;
		}

		if (this.isVisible) {
			if (this.entryBaseUIElement == null) {
				this.entryBaseUIElement = new UIFilledRectangle(0, this.yOffset, 0, 0, FileExplorerWindow.entryHeight, this.selectionScene);
				this.entryBaseUIElement.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
				this.entryBaseUIElement.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
				this.entryBaseUIElement.setFillWidth(true);
				this.entryBaseUIElement.setFillWidthMargin(0);
				this.entryBaseUIElement.setMaterial(defaultDirectoryEntryMaterial);
				this.entryBaseUIElement.bind(this.rootUIElement);

				this.entryText = new Text(0, 0, this.filename + "         ", 12, Color.WHITE, this.textScene);
				this.entryText.setDoAntialiasing(false);
				this.entryText.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_TOP);
				this.entryText.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
				this.entryText.bind(this.entryBaseUIElement);
			}
			this.entryBaseUIElement.setYOffset(this.yOffset);
			this.entryText.setXOffset(this.xOffset);
		}
		else {
			if (this.entryBaseUIElement != null) {
				this.entryText.kill();
				this.entryBaseUIElement.kill();
				this.entryText = null;
				this.entryBaseUIElement = null;
			}
		}

		if (this.isExpanded) {
			for (DirectoryEntry e : this.children) {
				totalHeight += e._align(yOffset + totalHeight);
			}
		}

		return totalHeight;
	}

	public int getXOffset() {
		return this.xOffset;
	}

	public int getYOffset() {
		return this.yOffset;
	}

	public String getPath() {
		return this.path;
	}

	public String getFilename() {
		return this.filename;
	}

	public boolean isExpanded() {
		return this.isExpanded;
	}

	public DirectoryEntry getParent() {
		return this.parent;
	}

	//recursively display this subtree. 
	//only display children if the current entry is expanded
	public void display() {
		if (this.isDisplayed) {
			return;
		}
		this.isDisplayed = true;

		if (this.isExpanded) {
			for (DirectoryEntry e : this.children) {
				e.display();
			}
		}
	}

	//hide this subtree. 
	public void hide() {
		if (!this.isDisplayed) {
			return;
		}

		this.isDisplayed = false;
		this.isVisible = false;

		if (this.entryBaseUIElement != null) {
			this.entryText.kill();
			this.entryText = null;

			this.entryBaseUIElement.kill();
			this.entryBaseUIElement = null;
		}

		if (this.children != null) {
			for (DirectoryEntry e : this.children) {
				e.hide();
			}
		}
	}

	public void generateChildren() {
		if (this.childrenGenerated) {
			return;
		}
		this.childrenGenerated = true;

		//is root folder
		if (this.path == "") {
			//load all root drives
			File[] paths;
			FileSystemView fsv = FileSystemView.getFileSystemView();

			// returns pathnames for files and directory
			paths = File.listRoots();

			// for each pathname in pathname array
			for (File p : paths) {
				System.out.println("Drive Name: " + p.toString());
				System.out.println("Description: " + fsv.getSystemTypeDescription(p));

				String path = p.toString();
				String name = path.toString().substring(0, path.toString().length() - 1);

				DirectoryEntry e = DirectoryEntry.createDirectoryEntry(this, path, name, rootUIElement, selectionScene, textScene);
				if (e == null) {
					continue;
				}
				this.children.add(e);
			}
		}
		else {
			String[] filenames = FileUtils.getAllFilenamesFromDirectory(this.path);
			Arrays.sort(filenames);
			for (int i = 0; i < filenames.length; i++) {
				DirectoryEntry e = DirectoryEntry.createDirectoryEntry(this, this.path + filenames[i] + "\\", filenames[i], this.rootUIElement, this.selectionScene, this.textScene);
				if (e == null) {
					continue;
				}
				this.children.add(e);
			}
		}
	}

	public boolean childrenGenerated() {
		return this.childrenGenerated;
	}

	public void expand() {
		if (this.isExpanded) {
			return;
		}
		if (!this.isDisplayed) {
			System.err.println("PRESSED ON ENTRY THAT ISN'T DISPLAYED : " + this.filename);
			//we can't expand an entry if we can't see it. 
			return;
		}

		if (!this.childrenGenerated) {
			this.generateChildren();
		}

		this.isExpanded = true;

		for (DirectoryEntry e : this.children) {
			e.display();
		}
	}

	public void collapse() {
		if (this.parent == null) {
			//this is the root entry, don't collapse this. 
			return;
		}
		if (!this.isExpanded) {
			return;
		}
		if (!this.isDisplayed) {
			return;
		}

		this.isExpanded = false;

		for (DirectoryEntry e : this.children) {
			e.hide();
		}
	}

	public void update() {

		//set background material
		if (this.isVisible) {
			if (!this.entryBaseUIElement.isAlive()) {
				System.err.println("UIELEMENT ID : " + this.entryBaseUIElement.getID() + " IS NOT ALIVE!!!");
			}
			if (this.isSelected) {
				this.entryBaseUIElement.setMaterial(selectedDirectoryEntryMaterial);
			}
			else if (this.isHovered) {
				this.entryBaseUIElement.setMaterial(hoveredDirectoryEntryMaterial);
			}
			else {
				this.entryBaseUIElement.setMaterial(defaultDirectoryEntryMaterial);
			}
		}

		if (this.children != null) {
			for (DirectoryEntry e : this.children) {
				e.update();
			}
		}
	}

	public void hovered(long entityID) {
		if (!this.isDisplayed) {
			return;
		}

		if (this.isVisible) {
			if (entityID == this.entryBaseUIElement.getID()) {
				this.isHovered = true;
			}
			else {
				this.isHovered = false;
			}
		}
		else {
			this.isHovered = false;
		}

		if (this.children != null) {
			for (DirectoryEntry e : this.children) {
				e.hovered(entityID);
			}
		}

	}

	public void selected(long entityID) {
		if (!this.isDisplayed) {
			return;
		}

		if (this.isVisible) {
			if (entityID == this.entryBaseUIElement.getID()) {
				this.isSelected = true;
			}
			else {
				this.isSelected = false;
			}
		}
		else {
			this.isSelected = false;
		}

		if (this.children != null) {
			for (DirectoryEntry e : this.children) {
				e.selected(entityID);
			}
		}
	}

	//idk, might be kinda slow
	public void selected(String path) {
		this.isSelected = this.path.equals(path);
		if (this.children != null) {
			for (DirectoryEntry e : this.children) {
				e.selected(path);
			}
		}
	}

	public DirectoryEntry getSelected() {
		if (this.isSelected) {
			return this;
		}

		if (this.children != null) {
			for (DirectoryEntry e : this.children) {
				DirectoryEntry ans = e.getSelected();
				if (ans != null) {
					return ans;
				}
			}
		}
		return null;
	}

	public int count() {
		int ans = 1;
		if (this.children != null) {
			for (DirectoryEntry e : this.children) {
				ans += e.count();
			}
		}
		return ans;
	}

	public int countDisplayed() {
		if (!this.isDisplayed) {
			return 0;
		}

		int ans = 0;
		if (this.parent != null) {
			//don't count the root entry
			ans++;
		}
		if (this.children != null) {
			for (DirectoryEntry e : this.children) {
				ans += e.countDisplayed();
			}
		}
		return ans;
	}

	//labels all the displayed entries on this subtree, starting with the root down. 
	//essentially, preorder traversal?
	public int computeOrder(int ptr) {
		if (!this.isDisplayed) {
			return ptr;
		}
		this.order = ptr++;
		if (this.children != null) {
			for (DirectoryEntry e : this.children) {
				ptr = e.computeOrder(ptr);
			}
		}
		return ptr;
	}

	public int getOrder() {
		return this.order;
	}

	public boolean isSelected() {
		return this.isSelected;
	}

}
