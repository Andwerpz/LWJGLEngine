package lwjglengine.v10.window;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.filechooser.FileSystemView;

import org.lwjgl.glfw.GLFW;

import lwjglengine.v10.entity.Entity;
import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.graphics.Material;
import lwjglengine.v10.model.Model;
import lwjglengine.v10.scene.Scene;
import lwjglengine.v10.screen.UIScreen;
import lwjglengine.v10.ui.Text;
import lwjglengine.v10.ui.UIElement;
import lwjglengine.v10.ui.UIFilledRectangle;
import myutils.v11.file.FileUtils;
import myutils.v10.math.Vec3;
import myutils.v10.math.Vec4;

public class FileExplorerWindow extends AdjustableWindow {

	//TODO
	// - Only render directory and folder entries that are visible. 

	private final int DIRECTORY_BACKGROUND_SCENE = Scene.generateScene();
	private final int DIRECTORY_SELECTION_RECT_SCENE = Scene.generateScene();
	private final int DIRECTORY_TEXT_SCENE = Scene.generateScene();
	private final int FOLDER_SCENE = Scene.generateScene();
	private final int TOP_BAR_SCENE = Scene.generateScene();
	private final int BOTTOM_BAR_SCENE = Scene.generateScene();

	private UIScreen uiScreen;

	public static int topBarHeight = 24;
	private UIFilledRectangle topBarRect;
	public static Material topBarMaterial = new Material(new Vec3((float) (20 / 255.0)));

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

	private DirectoryEntry rootDirectoryEntry;

	private long hoveredDirectoryEntryID = -1;
	private long hoveredSectionID = -1;

	public FileExplorerWindow(int xOffset, int yOffset, int contentWidth, int contentHeight, Window parentWindow) {
		super(xOffset, yOffset, contentWidth, contentHeight, "File Explorer", parentWindow);

		this.setMinHeight(topBarHeight + bottomBarHeight + 20);

		this.uiScreen = new UIScreen();

		this.directoryRect = new UIFilledRectangle(0, topBarHeight, 0, this.directoryWidth, this.getContentHeight() - topBarHeight - bottomBarHeight, DIRECTORY_BACKGROUND_SCENE);
		this.directoryRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.directoryRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		this.directoryRect.setFillHeight(true);
		this.directoryRect.setFillHeightMargin(0);
		this.directoryRect.setMaterial(directoryMaterial);
		this.directoryRect.bind(this.contentRootUIElement);

		this.folderRect = new UIFilledRectangle(0, topBarHeight, 0, this.getWidth() - this.directoryWidth, this.getContentHeight() - topBarHeight - bottomBarHeight, FOLDER_SCENE);
		this.folderRect.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_TOP);
		this.folderRect.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_TOP);
		this.folderRect.setFillHeight(true);
		this.folderRect.setFillHeightMargin(0);
		this.folderRect.setMaterial(folderMaterial);
		this.folderRect.bind(this.contentRootUIElement);

		this.topBarRect = new UIFilledRectangle(0, 0, 0, this.getWidth(), topBarHeight, TOP_BAR_SCENE);
		this.topBarRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.topBarRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		this.topBarRect.setFillWidth(true);
		this.topBarRect.setFillWidthMargin(0);
		this.topBarRect.setMaterial(topBarMaterial);
		this.topBarRect.bind(this.contentRootUIElement);

		this.bottomBarRect = new UIFilledRectangle(0, 0, 0, this.getWidth(), topBarHeight, BOTTOM_BAR_SCENE);
		this.bottomBarRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_BOTTOM);
		this.bottomBarRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_BOTTOM);
		this.bottomBarRect.setFillWidth(true);
		this.bottomBarRect.setFillWidthMargin(0);
		this.bottomBarRect.setMaterial(bottomBarMaterial);
		this.bottomBarRect.bind(this.contentRootUIElement);

		this.rootDirectoryEntry = new DirectoryEntry(null, "", "", this.directoryRect, DIRECTORY_SELECTION_RECT_SCENE, DIRECTORY_TEXT_SCENE);
		this.rootDirectoryEntry.display();
		this.rootDirectoryEntry.expand();
		this.rootDirectoryEntry.align();

		this.__resize();
	}

	@Override
	protected void __kill() {
		this.uiScreen.kill();
		Scene.removeScene(FOLDER_SCENE);
		Scene.removeScene(DIRECTORY_BACKGROUND_SCENE);
		Scene.removeScene(DIRECTORY_SELECTION_RECT_SCENE);
		Scene.removeScene(DIRECTORY_TEXT_SCENE);
		Scene.removeScene(TOP_BAR_SCENE);
		Scene.removeScene(BOTTOM_BAR_SCENE);
	}

	@Override
	protected void __resize() {
		if (this.uiScreen != null) {
			this.uiScreen.setScreenDimensions(this.getContentWidth(), this.getContentHeight());
		}

		if (this.folderRect != null) {
			this.folderRect.setWidth(this.getWidth() - this.directoryWidth);
		}
	}

	@Override
	protected void __update() {
		this.rootDirectoryEntry.update();

		//update directory width
		if (this.directoryGrabbed) {
			int newDirectoryWidth = (int) this.getRelativeMousePos().x;
			newDirectoryWidth = Math.max(newDirectoryWidth, directoryMinWidth);
			this.directoryWidth = newDirectoryWidth;
			this.directoryRect.setWidth(this.directoryWidth);
			this.folderRect.setWidth(this.getWidth() - this.directoryWidth);
		}
	}

	@Override
	protected void _renderContent(Framebuffer outputBuffer) {
		int mouseX = (int) this.getRelativeMousePos().x;
		int mouseY = (int) this.getRelativeMousePos().y;

		this.uiScreen.setUIScene(DIRECTORY_BACKGROUND_SCENE);
		this.uiScreen.render(outputBuffer);
		if (this.uiScreen.getEntityIDAtCoord(mouseX, this.getHeight() - mouseY) == this.directoryRect.getID()) {
			this.hoveredSectionID = this.directoryRect.getID();
		}

		this.uiScreen.setUIScene(DIRECTORY_SELECTION_RECT_SCENE);
		this.uiScreen.render(outputBuffer);

		this.hoveredDirectoryEntryID = this.uiScreen.getEntityIDAtCoord(mouseX, this.getHeight() - mouseY);
		this.rootDirectoryEntry.hovered(this.hoveredDirectoryEntryID);

		this.uiScreen.setUIScene(DIRECTORY_TEXT_SCENE);
		this.uiScreen.render(outputBuffer);

		this.uiScreen.setUIScene(FOLDER_SCENE);
		this.uiScreen.render(outputBuffer);
		if (this.uiScreen.getEntityIDAtCoord(mouseX, this.getHeight() - mouseY) == this.folderRect.getID()) {
			this.hoveredSectionID = this.folderRect.getID();
		}

		this.uiScreen.setUIScene(TOP_BAR_SCENE);
		this.uiScreen.render(outputBuffer);
		if (this.uiScreen.getEntityIDAtCoord(mouseX, this.getHeight() - mouseY) == this.topBarRect.getID()) {
			this.hoveredSectionID = this.topBarRect.getID();
		}

		this.uiScreen.setUIScene(BOTTOM_BAR_SCENE);
		this.uiScreen.render(outputBuffer);
		if (this.uiScreen.getEntityIDAtCoord(mouseX, this.getHeight() - mouseY) == this.bottomBarRect.getID()) {
			this.hoveredSectionID = this.bottomBarRect.getID();
		}
	}

	@Override
	protected void contentSelected() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void contentDeselected() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void __mousePressed(int button) {
		int mouseX = (int) this.getRelativeMousePosClampedToWindow().x;
		int mouseY = (int) this.getRelativeMousePosClampedToWindow().y;

		//see if user is trying to drag the directory window
		if (Math.abs(this.directoryWidth - mouseX) <= directoryGrabTolerancePx) {
			this.directoryGrabbed = true;
			return;
		}

		if (this.hoveredSectionID == this.directoryRect.getID()) {
			//select the directory entry
			this.rootDirectoryEntry.selected(this.hoveredDirectoryEntryID);

			DirectoryEntry e = this.rootDirectoryEntry.getSelected();
			if (e != null) {
				if (e.isExpanded()) {
					e.collapse();
				}
				else {
					e.expand();
				}
				this.rootDirectoryEntry.align();
			}
		}

	}

	@Override
	protected void __mouseReleased(int button) {
		this.directoryGrabbed = false;
	}

	@Override
	protected void __mouseScrolled(float wheelOffset, float smoothOffset) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void __keyPressed(int key) {
		switch (key) {
		case GLFW.GLFW_KEY_E:
			System.out.println("NUM ENTITIES : " + Entity.getNumEntities());
			break;

		case GLFW.GLFW_KEY_M:
			System.out.println("NUM MODEL INSTANCES : " + Model.getNumInstances());
			System.out.println("NUM MODELS : " + Model.getNumModels());
			break;

		case GLFW.GLFW_KEY_C:
			System.out.println("NUM ENTRIES : " + this.rootDirectoryEntry.count());
			break;
		}
	}

	@Override
	protected void __keyReleased(int key) {
		// TODO Auto-generated method stub

	}

}

class DirectoryEntry {
	private static int entryHeight = 16;
	private static int entryXOffsetInterval = 10;
	private static int entryXOffsetBase = 5;

	public static Material defaultEntryMaterial = new Material(new Vec3((float) (20 / 255.0)));
	public static Material hoveredEntryMaterial = new Material(new Vec3((float) (30 / 255.0)));
	public static Material selectedEntryMaterial = new Material(new Vec3((float) (40 / 255.0)));

	private boolean isHovered = false;
	private boolean isSelected = false;

	//if this is null, then it means that we still need to find this entry's children. 
	//one special case is where this entry has no children, then we just set it to an empty arraylist. 
	private ArrayList<DirectoryEntry> children = null;

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

	public DirectoryEntry(DirectoryEntry parent, String path, String filename, UIElement rootUIElement, int selectionScene, int textScene) {
		this.parent = parent;

		this.xOffset = 0;
		this.yOffset = 0;

		if (this.parent != null) {
			this.xOffset = this.parent.xOffset + entryXOffsetInterval;
		}
		else {
			this.xOffset = entryXOffsetBase - entryXOffsetInterval;
		}

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

	public void align() {
		if (this.parent != null) {
			System.err.println("TRIED TO ALIGN DIRECTORY ENTRY THAT IS NOT ROOT");
			return;
		}

		this.align(-entryHeight);
	}

	//aligns this subtree of directory entries
	//returns the height of this subtree
	private int align(int yOffset) {
		if (!this.isDisplayed) {
			return 0;
		}
		int totalHeight = entryHeight;

		this.yOffset = yOffset;

		//check if this thing is visible
		this.isVisible = false;
		if (-entryHeight <= this.yOffset && this.yOffset <= this.rootUIElement.getHeight()) {
			this.isVisible = true;
		}

		if (this.isVisible) {
			if (this.entryBaseUIElement == null) {
				this.entryBaseUIElement = new UIFilledRectangle(0, this.yOffset, 0, 0, entryHeight, this.selectionScene);
				this.entryBaseUIElement.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
				this.entryBaseUIElement.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
				this.entryBaseUIElement.setFillWidth(true);
				this.entryBaseUIElement.setFillWidthMargin(0);
				this.entryBaseUIElement.setMaterial(defaultEntryMaterial);
				this.entryBaseUIElement.bind(this.rootUIElement);

				this.entryText = new Text(0, 0, this.filename + "         ", 12, Color.WHITE, this.textScene);
				this.entryText.setDoAntialiasing(false);
				this.entryText.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_TOP);
				this.entryText.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
				this.entryText.setMaterial(new Material(Color.WHITE));
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
				totalHeight += e.align(yOffset + totalHeight);
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

	public void expand() {
		if (this.isExpanded) {
			return;
		}
		if (!this.isDisplayed) {
			System.err.println("PRESSED ON ENTRY THAT ISN'T DISPLAYED : " + this.filename);
			//we can't expand an entry if we can't see it. 
			return;
		}

		if (this.children == null) {
			this.children = new ArrayList<>();

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
				this.entryBaseUIElement.setMaterial(selectedEntryMaterial);
			}
			else if (this.isHovered) {
				this.entryBaseUIElement.setMaterial(hoveredEntryMaterial);
			}
			else {
				this.entryBaseUIElement.setMaterial(defaultEntryMaterial);
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

		if (this.children != null) {
			for (DirectoryEntry e : this.children) {
				e.selected(entityID);
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

}
