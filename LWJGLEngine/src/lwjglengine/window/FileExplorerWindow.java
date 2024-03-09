package lwjglengine.window;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.filechooser.FileSystemView;

import org.lwjgl.glfw.GLFW;

import lwjglengine.entity.Entity;
import lwjglengine.graphics.Framebuffer;
import lwjglengine.graphics.Material;
import lwjglengine.input.Input;
import lwjglengine.input.Button;
import lwjglengine.input.TextField;
import lwjglengine.model.Model;
import lwjglengine.scene.Scene;
import lwjglengine.screen.UIScreen;
import lwjglengine.ui.Text;
import lwjglengine.ui.UIElement;
import lwjglengine.ui.UIFilledRectangle;
import lwjglengine.ui.UISection;
import lwjglengine.ui.UISectionListener;
import myutils.file.FileUtils;
import myutils.math.MathUtils;
import myutils.math.Vec3;
import myutils.math.Vec4;

public class FileExplorerWindow extends Window implements UISectionListener {
	//this class handles the interactions with navigating directories, it delegates displaying the contents of a directory
	//and selecting to the list viewer class. 

	//TODO 
	// - figure out how other things are going to call this one, and retrieve the submitted file
	// - qol
	//   - icons? maybe just icons in the folder rect to tell the user which ones are folders
	//   - text indication when you didn't select a directory in the text rect?
	//   - text indication when the filter filters out all files in folder
	// - ability to create folder inside selected directory
	// - when selecting a folder or directory entry, just make sure that it exists first.
	// - set root directory when creating an instance of file explorer

	private UISection directorySection;
	private UISection topBarSection;

	public static int entryHeight = 16;
	public static int entryXOffsetInterval = 10;
	public static int entryXOffsetBase = 5;

	public static int topBarHeight = 24;
	private UIFilledRectangle topBarRect;
	public static Material topBarMaterial = new Material(new Vec3((float) (20 / 255.0)));

	private Button topBarBackButton;
	private Text topBarPathText;
	private TextField topBarFilterTextField;

	private static int topBarFilterTextFieldWidth = 200;

	private static int directoryMinWidth = 75;
	private int directoryWidth = 200;
	private UIFilledRectangle directoryBackgroundRect, directoryRect;
	public static Material directoryMaterial = new Material(new Vec3((float) (20 / 255.0)));

	public static int directoryGrabTolerancePx = 5;
	private boolean directoryGrabbed = false;

	private DirectoryEntry rootDirectoryEntry;

	private DirectoryEntry selectedDirectoryEntry = null;

	private long hoveredSectionID = -1;

	private long hoveredDirectoryEntryID = -1;
	private long hoveredTopBarID = -1;

	private ListViewerWindow folderWindow;

	public FileExplorerWindow(Window parentWindow) {
		super(0, 0, 400, 300, parentWindow);
		this.init();
	}

	private void init() {
		this.directorySection = new UISection();
		this.directorySection.setIsScrollable(true);
		this.directorySection.setIsHorizontalScroll(false);
		this.directorySection.addListener(this);

		this.topBarSection = new UISection();

		this.directoryBackgroundRect = this.directorySection.getBackgroundRect();
		this.directoryBackgroundRect.setFrameAlignmentOffset(0, topBarHeight);
		this.directoryBackgroundRect.setDimensions(this.directoryWidth, this.getHeight() - topBarHeight);
		this.directoryBackgroundRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.directoryBackgroundRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		this.directoryBackgroundRect.setMaterial(directoryMaterial);
		this.directoryBackgroundRect.bind(this.rootUIElement);

		this.directoryRect = this.directorySection.getScrollBackgroundRect();

		this.topBarRect = this.topBarSection.getBackgroundRect();
		this.topBarRect.setFrameAlignmentOffset(0, 0);
		this.topBarRect.setDimensions(this.getWidth(), topBarHeight);
		this.topBarRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.topBarRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		this.topBarRect.setFillWidth(true);
		this.topBarRect.setFillWidthMargin(0);
		this.topBarRect.setMaterial(topBarMaterial);
		this.topBarRect.bind(this.rootUIElement);

		this.rootDirectoryEntry = new DirectoryEntry(null, "", "", this.directoryRect, this.directorySection.getSelectionScene(), this.directorySection.getTextScene());
		this.rootDirectoryEntry.display();
		this.rootDirectoryEntry.expand();
		this.alignDirectoryEntries();

		this.topBarBackButton = new Button(3, 0, 20, 20, "btn_directory_back", "          ", new Font("Dialog", Font.PLAIN, 12), 12, this.topBarSection.getSelectionScene(), this.topBarSection.getTextScene());
		this.topBarBackButton.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_TOP);
		this.topBarBackButton.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
		this.topBarBackButton.setReleasedMaterial(new Material(new Vec3(100 / 255.0f)));
		this.topBarBackButton.setHoveredMaterial(new Material(new Vec3(150 / 255.0f)));
		this.topBarBackButton.setPressedMaterial(new Material(new Vec3(200 / 255.0f)));
		this.topBarBackButton.bind(this.topBarRect);

		this.topBarFilterTextField = new TextField(3, 0, topBarFilterTextFieldWidth, 20, "tf_filter", "Search Folder", new Font("Dialog", Font.PLAIN, 12), 12, this.topBarSection.getSelectionScene(), this.topBarSection.getTextScene());
		this.topBarFilterTextField.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_CENTER_TOP);
		this.topBarFilterTextField.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
		this.topBarFilterTextField.getTextUIElement().setDoAntialiasing(false);
		this.topBarFilterTextField.bind(this.topBarRect);

		this.topBarPathText = new Text(this.topBarBackButton.getRightBorder() + 5, 0, "          ", 12, Color.WHITE, this.topBarSection.getTextScene());
		this.topBarPathText.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_TOP);
		this.topBarPathText.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
		this.topBarPathText.setDoAntialiasing(false);
		this.topBarPathText.bind(this.topBarRect);

		this.folderWindow = new ListViewerWindow(this, this);
		this.folderWindow.setRenderBottomBar(false);
		this.folderWindow.setDisplaySelectedEntryOnTopBar(true);
		this.folderWindow.setSubmitOnClickingSelectedListEntry(true);
		this.folderWindow.setSortEntries(true);
		this.folderWindow.setAllowInputWhenNotSelected(false);
		this.folderWindow.setRenderTopBar(false);
		this.folderWindow.setCloseOnSubmit(false);
		this.folderWindow.setInheritParentCursor(true);

		this._resize();
	}

	@Override
	protected int _getCursorShape() {
		return this.canGrabDirectory() || this.directoryGrabbed ? GLFW.GLFW_HRESIZE_CURSOR : GLFW.GLFW_ARROW_CURSOR;
	}

	//this is how we switch directories by double clicking a folder entry. 
	@Override
	public void handleObjects(Object[] objects) {
		if (objects.length != 1) {
			return;
		}

		File f = (File) objects[0];

		if (f.isDirectory()) {
			this.rootDirectoryEntry.selected(f.getPath() + "\\");
			DirectoryEntry e = this.rootDirectoryEntry.getSelected();

			this.setSelectedDirectoryEntry(e);
		}
	}

	@Override
	public String getDefaultTitle() {
		return "File Explorer";
	}

	public File[] getSelectedFiles() {
		Object[] objects = this.folderWindow.getSelectedListEntryObjects();
		File[] files = new File[objects.length];
		for (int i = 0; i < files.length; i++) {
			files[i] = (File) objects[i];
		}
		return files;
	}

	@Override
	protected void _kill() {
		this.directorySection.kill();
	}

	@Override
	protected void _resize() {
		this.topBarSection.setScreenDimensions(this.getWidth(), this.getHeight());
		this.directorySection.setScreenDimensions(this.getWidth(), this.getHeight());

		if (this.folderWindow != null) {
			this.folderWindow.setWidth(this.getWidth() - this.directoryWidth);
			this.folderWindow.setHeight(this.getHeight() - topBarHeight);

			this.folderWindow.setBottomLeftCoords(this.directoryWidth, 0);
		}

		if (this.directoryBackgroundRect != null) {
			this.directoryBackgroundRect.setWidth(this.directoryWidth);
			this.directoryBackgroundRect.setHeight(this.getHeight() - topBarHeight);

			//realign entries
			this.alignDirectoryEntries();
		}

		if (this.topBarRect != null) {
			this.topBarPathText.setWidth(this.topBarRect.getWidth() - topBarFilterTextFieldWidth - this.topBarBackButton.getWidth() - 10);
		}
	}

	@Override
	protected void _update() {
		this.topBarSection.update();
		this.directorySection.update();

		this.rootDirectoryEntry.hovered(this.hoveredDirectoryEntryID);
		this.rootDirectoryEntry.update();

		//update directory width
		if (this.directoryGrabbed) {
			int newDirectoryWidth = (int) this.getWindowMousePos().x;
			newDirectoryWidth = Math.max(newDirectoryWidth, directoryMinWidth);
			this.directoryWidth = newDirectoryWidth;
			this._resize();
		}
	}

	@Override
	protected void renderContent(Framebuffer outputBuffer) {
		this.directorySection.render(outputBuffer, this.getWindowMousePos());
		this.topBarSection.render(outputBuffer, this.getWindowMousePos());

		this.hoveredDirectoryEntryID = this.directorySection.getHoveredEntityID();
		this.hoveredTopBarID = this.topBarSection.getHoveredEntityID();

		this.hoveredSectionID = -1;
		if (this.directorySection.isSectionHovered()) {
			this.hoveredSectionID = this.directoryBackgroundRect.getID();
		}
		else if (this.topBarSection.isSectionHovered()) {
			this.hoveredSectionID = this.topBarRect.getID();
		}
	}

	public void setSingleEntrySelection(boolean b) {
		this.folderWindow.setSingleEntrySelection(b);
	}

	@Override
	protected void renderOverlay(Framebuffer outputBuffer) {

	}

	private void alignDirectoryEntries() {
		this.directorySection.setScrollRectHeight(this.rootDirectoryEntry.countDisplayed() * FileExplorerWindow.entryHeight);
		this.rootDirectoryEntry.align();
	}

	public void setCurrentDirectory(String path) {
		this.rootDirectoryEntry.selected(path);
		DirectoryEntry e = this.rootDirectoryEntry.getSelected();
		this.setSelectedDirectoryEntry(e);
	}

	public String getCurrentDirectory() {
		return this.selectedDirectoryEntry.getPath();
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
			this.alignDirectoryEntries();
			return;
		}

		this.selectedDirectoryEntry = e;

		//reset the filter
		this.folderWindow.setFilter("");
		this.topBarFilterTextField.setText("");

		if (e == null) {
			this.topBarPathText.setText("          ");
			return;
		}

		if (!this.selectedDirectoryEntry.isSelected()) {
			this.rootDirectoryEntry.selected(this.selectedDirectoryEntry.getPath());
		}

		this.topBarPathText.setText(e.getPath());

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
		int minYOffset = maxYOffset - (int) this.directoryBackgroundRect.getHeight() + entryHeight;
		int newScrollOffset = MathUtils.clamp(minYOffset, maxYOffset, this.directorySection.getScrollOffset());
		this.directorySection.setScrollOffset(newScrollOffset);
		this.alignDirectoryEntries();

		//populate list window with files
		File[] files = FileUtils.getAllFilesFromDirectory(e.getPath());
		ArrayList<File> fileList = new ArrayList<>();
		ArrayList<String> strList = new ArrayList<>();
		for (int i = 0; i < files.length; i++) {
			fileList.add(files[i]);
			strList.add(files[i].getName());
		}
		this.folderWindow.setList(fileList, strList);
	}

	@Override
	protected void selected() {

	}

	@Override
	protected void deselected() {

	}

	@Override
	protected void subtreeSelected() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void subtreeDeselected() {
		// TODO Auto-generated method stub

	}

	private boolean canGrabDirectory() {
		int mouseX = (int) this.getWindowMousePos().x;
		return mouseX <= this.directoryWidth && Math.abs(this.directoryWidth - mouseX) <= directoryGrabTolerancePx;
	}

	@Override
	protected void _mousePressed(int button) {
		this.topBarSection.mousePressed(button);
		this.directorySection.mousePressed(button);

		//see if user is trying to drag the directory window
		if (this.canGrabDirectory()) {
			this.directoryGrabbed = true;
			return;
		}

		if (this.hoveredSectionID == this.directoryBackgroundRect.getID()) {
			if (this.hoveredDirectoryEntryID != this.directoryBackgroundRect.getID()) {
				//select the directory entry
				this.rootDirectoryEntry.selected(this.hoveredDirectoryEntryID);

				DirectoryEntry e = this.rootDirectoryEntry.getSelected();
				this.setSelectedDirectoryEntry(e);
			}
		}
	}

	@Override
	protected void _mouseReleased(int button) {
		this.directorySection.mouseReleased(button);
		this.topBarSection.mouseReleased(button);

		switch (Input.getClicked(this.topBarSection.getSelectionScene())) {
		case "btn_directory_back":
			if (this.selectedDirectoryEntry != null && this.selectedDirectoryEntry.getParent() != this.rootDirectoryEntry) {
				this.setSelectedDirectoryEntry(this.selectedDirectoryEntry.getParent());
			}
			break;
		}

		this.directoryGrabbed = false;
	}

	@Override
	protected void _mouseScrolled(float wheelOffset, float smoothOffset) {
		this.directorySection.mouseScrolled(wheelOffset, smoothOffset);
	}

	@Override
	protected void _keyPressed(int key) {
		this.topBarSection.keyPressed(key);

		if (this.topBarFilterTextField.isClicked()) {
			this.folderWindow.setFilter(this.topBarFilterTextField.getText());
		}
	}

	@Override
	protected void _keyReleased(int key) {
		this.topBarSection.keyReleased(key);
	}

	@Override
	public void uiSectionScrolled(UISection section) {
		if (section == this.directorySection) {
			//update visibility of entries
			this.alignDirectoryEntries();
		}
	}

	class DirectoryEntry {
		public Material defaultDirectoryEntryMaterial = new Material(new Vec3((float) (20 / 255.0)));
		public Material hoveredDirectoryEntryMaterial = new Material(new Vec3((float) (30 / 255.0)));
		public Material selectedDirectoryEntryMaterial = new Material(new Vec3((float) (40 / 255.0)));

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

		public DirectoryEntry createDirectoryEntry(DirectoryEntry parent, String path, String filename, UIElement rootUIElement, int selectionScene, int textScene) {
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

		public void align() {
			if (this.parent != null) {
				System.err.println("FileExplorerWindow : TRIED TO ALIGN DIRECTORY ENTRY THAT IS NOT ROOT");
				return;
			}

			//offset by one entry because the actual root element should be invisible. 
			this._align(-FileExplorerWindow.entryHeight);
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
			int scrollOffset = directorySection.getScrollOffset();
			this.isVisible = false;
			if (-FileExplorerWindow.entryHeight <= this.yOffset - scrollOffset && this.yOffset - scrollOffset <= directorySection.getBackgroundRect().getHeight()) {
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

					DirectoryEntry e = this.createDirectoryEntry(this, path, name, rootUIElement, selectionScene, textScene);
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
					DirectoryEntry e = this.createDirectoryEntry(this, this.path + filenames[i] + "\\", filenames[i], this.rootUIElement, this.selectionScene, this.textScene);
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
				System.err.println("FileExplorerWindow : PRESSED ON ENTRY THAT ISN'T DISPLAYED : " + this.filename);
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
					System.err.println("FileExplorerWindow : UIELEMENT ID : " + this.entryBaseUIElement.getID() + " IS NOT ALIVE!!!");
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

}
