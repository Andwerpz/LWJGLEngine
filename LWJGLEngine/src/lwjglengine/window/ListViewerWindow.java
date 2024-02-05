package lwjglengine.window;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import lwjglengine.graphics.Framebuffer;
import lwjglengine.graphics.Material;
import lwjglengine.input.Button;
import lwjglengine.input.Input;
import lwjglengine.input.KeyboardInput;
import lwjglengine.input.TextField;
import lwjglengine.scene.Scene;
import lwjglengine.screen.UIScreen;
import lwjglengine.ui.Text;
import lwjglengine.ui.UIElement;
import lwjglengine.ui.UIFilledRectangle;
import lwjglengine.ui.UISection;
import myutils.graphics.GraphicsTools;
import myutils.math.MathUtils;
import myutils.math.Vec3;
import myutils.math.Vec4;

public class ListViewerWindow extends Window {

	protected UISection topBarSection, bottomBarSection, contentSection;

	private Button bottomBarSubmitBtn;

	public static Font entryFont = new Font("Dialogue", Font.PLAIN, 12);
	public static int entryFontSize = 12;

	private int entryHeightPx = 16;
	public static int entryHorizontalMarginPx = 5;

	public static int topBarSearchTfWidthPx = 150;

	private int topBarHeightPx = 20;

	private int bottomBarHeightPx = 20;

	private Window callbackWindow;

	private TextField topBarSearchTf;
	private Text topBarSelectedEntryText;

	private ArrayList<ListEntry> entryList;
	private ArrayList<ListEntry> filteredEntryList;
	private HashSet<ListEntry> selectedListEntries;

	private boolean closeOnSubmit = true;

	private long hoveredSectionID;
	private long hoveredTopBarID, hoveredContentID;

	private boolean renderTopBar = true;

	private String filterString = "";
	private boolean shouldRefilterList = false;
	private boolean filterCaseSensitive = false;

	private boolean sortEntries = true;
	private boolean shouldResortList = false;

	//if there are no displayable list entries, then we'll display a message saying so. 
	private boolean noListEntries = false;

	//if true, displays the currently selected entry on the right side of the top bar. 
	//if false, then it just makes the text transparent. 
	private boolean displaySelectedEntryOnTopBar = true;

	private Text noListEntriesText;

	//if true, then you can only have 1 entry selected at a time. 
	private boolean singleEntrySelection = false;

	private int selectedPivotEntryIndex = 0;

	//if there is only 1 selected list entry, and the user clicks on it, then it will submit if this is true. 
	private boolean submitOnClickingSelectedListEntry = false;

	private boolean renderBottomBar = true;

	public ListViewerWindow(int xOffset, int yOffset, int width, int height, Window callbackWindow, Window parentWindow) {
		super(xOffset, yOffset, width, height, parentWindow);
		this.init(callbackWindow);
	}

	public ListViewerWindow(Window callbackWindow, Window parentWindow) {
		super(0, 0, 300, 300, parentWindow);
		this.init(callbackWindow);
	}

	private void init(Window callbackWindow) {
		this.topBarSection = new UISection();
		this.contentSection = new UISection();
		this.bottomBarSection = new UISection();

		this.contentSection.setIsScrollable(true);
		this.contentSection.setRenderScrollBar(true);

		this.entryList = new ArrayList<>();
		this.filteredEntryList = new ArrayList<>();
		this.selectedListEntries = new HashSet<>();

		this.callbackWindow = callbackWindow;

		UIFilledRectangle topBarBackgroundRect = this.topBarSection.getBackgroundRect();
		topBarBackgroundRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		topBarBackgroundRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		topBarBackgroundRect.setFillWidth(true);
		topBarBackgroundRect.setFillWidthMargin(0);
		topBarBackgroundRect.setMaterial(this.topBarDefaultMaterial);
		topBarBackgroundRect.bind(this.rootUIElement);

		this.topBarSearchTf = new TextField(3, 0, topBarSearchTfWidthPx, 16, "tf_filter", "Filter Entries", 12, this.topBarSection.getSelectionScene(), this.topBarSection.getTextScene());
		this.topBarSearchTf.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_CENTER_TOP);
		this.topBarSearchTf.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
		this.topBarSearchTf.getTextUIElement().setDoAntialiasing(false);
		this.topBarSearchTf.bind(topBarBackgroundRect);

		this.topBarSelectedEntryText = new Text(5, 0, "       ", 12, Color.WHITE, this.topBarSection.getTextScene());
		this.topBarSelectedEntryText.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_TOP);
		this.topBarSelectedEntryText.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
		this.topBarSelectedEntryText.setDoAntialiasing(false);
		this.topBarSelectedEntryText.bind(topBarBackgroundRect);

		UIFilledRectangle contentBackgroundRect = this.contentSection.getBackgroundRect();
		contentBackgroundRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_BOTTOM);
		contentBackgroundRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_BOTTOM);
		contentBackgroundRect.setFillWidth(true);
		contentBackgroundRect.setFillWidthMargin(0);
		contentBackgroundRect.setMaterial(contentDefaultMaterial);
		contentBackgroundRect.bind(this.rootUIElement);

		this.noListEntriesText = new Text(0, 0, "No List Entries to Display", 12, Color.WHITE, this.contentSection.getTextScene());
		this.noListEntriesText.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_TOP);
		this.noListEntriesText.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_CENTER);
		this.noListEntriesText.setDoAntialiasing(false);
		this.noListEntriesText.bind(contentBackgroundRect);

		UIFilledRectangle bottomBarRect = this.bottomBarSection.getBackgroundRect();
		bottomBarRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_BOTTOM);
		bottomBarRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_BOTTOM);
		bottomBarRect.setFillWidth(true);
		bottomBarRect.setMaterial(this.topBarDefaultMaterial);
		bottomBarRect.bind(this.rootUIElement);

		this.bottomBarSubmitBtn = new Button(3, 0, 100, 16, "btn_submit", "Submit", 12, this.bottomBarSection.getSelectionScene(), this.bottomBarSection.getTextScene());
		this.bottomBarSubmitBtn.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_CENTER_TOP);
		this.bottomBarSubmitBtn.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
		this.bottomBarSubmitBtn.getButtonText().setDoAntialiasing(false);
		this.bottomBarSubmitBtn.bind(bottomBarRect);

		this._resize();
	}

	@Override
	public String getDefaultTitle() {
		return "List Viewer";
	}

	public void clearList() {
		for (ListEntry i : this.entryList) {
			i.kill();
		}
		this.entryList.clear();
		this.selectedListEntries.clear();

		this.selectedPivotEntryIndex = 0;
		this.topBarSelectedEntryText.setText("        ");

		this.shouldRefilterList = true;
		this.alignEntries();
	}

	/**
	 * Uses the given string list as the string representation of list. 
	 * 
	 * Both lists must be the same size. 
	 * @param <T>
	 * @param list
	 * @param s
	 */
	public <T> void setList(ArrayList<T> list, ArrayList<String> s) {
		if (list == null) {
			System.err.println("List cannot be null");
			return;
		}
		if (s == null) {
			System.err.println("String list cannot be null");
			return;
		}
		if (list.size() != s.size()) {
			System.err.println("String list must be the same size as elem list");
			return;
		}

		this.clearList();

		for (int i = 0; i < list.size(); i++) {
			ListEntry e = new ListEntry(list.get(i), this.contentDefaultMaterial, this.contentHoveredMaterial, this.contentSelectedMaterial, s.get(i), this.contentSection.getScrollBackgroundRect(), this.contentSection.getSelectionScene(), this.contentSection.getTextScene());
			this.entryList.add(e);
		}

		this.shouldRefilterList = true;
		this.shouldResortList = true;
		this.alignEntries();
	}

	/**
	 * Uses the string as the string representation of elem in the list. 
	 * @param <T>
	 * @param elem
	 * @param s
	 */
	public <T> void addToList(T elem, String s) {
		if (elem == null) {
			return;
		}
		if (s == null) {
			return;
		}

		ListEntry e = new ListEntry(elem, this.contentDefaultMaterial, this.contentHoveredMaterial, this.contentSelectedMaterial, s, this.contentSection.getScrollBackgroundRect(), this.contentSection.getSelectionScene(), this.contentSection.getTextScene());
		this.entryList.add(e);

		this.shouldRefilterList = true;
		this.shouldResortList = true;
		this.alignEntries();
	}

	/**
	 * Creates a string representation for each element of the list using toString.
	 * @param <T>
	 * @param list
	 */
	public <T> void setList(ArrayList<T> list) {
		this.clearList();

		ArrayList<String> s = new ArrayList<>();
		for (T i : list) {
			if (i == null) {
				continue;
			}

			s.add(i.toString());
		}

		this.setList(list, s);
	}

	/**
	 * Creates string representation using toString
	 * @param <T>
	 * @param elem
	 */
	public <T> void addToList(T elem) {
		if (elem == null) {
			return;
		}

		this.addToList(elem, elem.toString());
	}

	public void setRenderBottomBar(boolean b) {
		this.renderBottomBar = b;
		this._resize();
	}

	public void setFilter(String filter) {
		this.filterString = filter;
		this.shouldRefilterList = true;
		this.alignEntries();
	}

	public void setFilterCaseSensitive(boolean b) {
		this.filterCaseSensitive = b;
		this.shouldRefilterList = true;
		this.alignEntries();
	}

	public void setSubmitOnClickingSelectedListEntry(boolean b) {
		this.submitOnClickingSelectedListEntry = b;
	}

	public void setSingleEntrySelection(boolean b) {
		this.singleEntrySelection = b;
	}

	public void setDisplaySelectedEntryOnTopBar(boolean b) {
		this.displaySelectedEntryOnTopBar = b;
		if (this.displaySelectedEntryOnTopBar) {
			this.topBarSelectedEntryText.setMaterial(new Material(Color.WHITE));
		}
		else {
			this.topBarSelectedEntryText.setMaterial(new Material(new Vec4(0)));
		}
	}

	public void setTopBarHeight(int h) {
		this.topBarHeightPx = h;
		this._resize();
	}

	public void setEntryHeightPx(int height) {
		this.entryHeightPx = height;
		this.alignEntries();
	}

	//returns the string of the current pivot entry. 
	public String getSelectedEntryString() {
		if (this.selectedPivotEntryIndex >= this.entryList.size()) {
			return "";
		}
		return this.entryList.get(this.selectedPivotEntryIndex).getText();
	}

	public Object[] getSelectedListEntryObjects() {
		Object[] ret = new Object[this.selectedListEntries.size()];

		int i = 0;
		for (ListEntry e : this.selectedListEntries) {
			ret[i] = e.getObject();
			i++;
		}

		return ret;
	}

	//TODO fix this
	private void alignEntries() {
		if (this.shouldResortList) {
			this.sortList();
		}

		if (this.shouldRefilterList) {
			this.shouldRefilterList = false;
			this.filteredEntryList.clear();
			for (ListEntry i : this.entryList) {
				boolean passFilter = this.filterCaseSensitive && i.getText().contains(this.filterString);
				passFilter |= !this.filterCaseSensitive && i.getText().toLowerCase().contains(this.filterString.toLowerCase());
				if (passFilter) {
					this.filteredEntryList.add(i);
				}
				else {
					//just make it not visible
					i.align(this.getWidth() * 2, this.getHeight() * 2);
				}
			}
		}

		if (this.filteredEntryList.size() == 0) {
			this.noListEntries = true;
			this.noListEntriesText.setFrameAlignmentOffset(0, 0);
		}
		else {
			this.noListEntries = false;
			this.noListEntriesText.setFrameAlignmentOffset(this.getWidth(), this.getHeight());
		}

		this.contentSection.setScrollRectHeight(this.filteredEntryList.size() * this.entryHeightPx);

		int xOffset = 0;
		int yOffset = 0;
		for (ListEntry i : this.filteredEntryList) {
			i.setEntryHeightPx(this.entryHeightPx);
			i.doFillWidth(true);
			i.align(xOffset, yOffset);

			yOffset += entryHeightPx;
		}
	}

	private void sortList() {
		if (!this.sortEntries) {
			return;
		}
		Collections.sort(this.entryList, (a, b) -> {
			return a.getText().compareTo(b.getText());
		});
	}

	public void setSortEntries(boolean b) {
		this.sortEntries = b;

		this.shouldResortList = true;
		this.alignEntries();
	}

	public void setCloseOnSubmit(boolean b) {
		this.closeOnSubmit = b;
	}

	public void setRenderTopBar(boolean b) {
		if (this.renderTopBar == b) {
			return;
		}
		this.renderTopBar = b;

		this._resize();
	}

	@Override
	protected void _kill() {
		this.topBarSection.kill();
		this.contentSection.kill();
		this.bottomBarSection.kill();
	}

	@Override
	protected void _resize() {
		this.topBarSection.setScreenDimensions(this.getWidth(), this.getHeight());
		this.bottomBarSection.setScreenDimensions(this.getWidth(), this.getHeight());
		this.contentSection.setScreenDimensions(this.getWidth(), this.getHeight());

		this.topBarSection.getBackgroundRect().setHeight(this.topBarHeightPx);
		this.bottomBarSection.getBackgroundRect().setHeight(this.bottomBarHeightPx);

		int contentHeight = this.getHeight();

		if (this.renderTopBar) {
			contentHeight -= this.topBarHeightPx;
		}

		if (this.renderBottomBar) {
			contentHeight -= this.bottomBarHeightPx;
			this.contentSection.getBackgroundRect().setFrameAlignmentOffset(0, this.bottomBarHeightPx);
		}
		else {
			this.contentSection.getBackgroundRect().setFrameAlignmentOffset(0, 0);
		}

		this.contentSection.getBackgroundRect().setHeight(contentHeight);

		this.topBarSelectedEntryText.setWidth(this.getWidth() - 6);

		this.alignEntries();
	}

	protected void submitEntries(Object[] o) {
		this.callbackWindow.handleObjects(o);
		if (this.closeOnSubmit) {
			this.close();
			return;
		}
	}

	@Override
	protected void _update() {
		this.topBarSection.update();
		this.contentSection.update();
		this.bottomBarSection.update();

		for (ListEntry i : this.entryList) {
			i.hovered(this.hoveredContentID);
			i.update();
		}
	}

	@Override
	protected void renderContent(Framebuffer outputBuffer) {
		int mouseX = (int) this.getWindowMousePos().x;
		int mouseY = (int) this.getWindowMousePos().y;

		this.contentSection.render(outputBuffer, getWindowMousePos());
		if (this.contentSection.isSectionHovered()) {
			this.hoveredSectionID = this.contentSection.getBackgroundRect().getID();
		}
		this.hoveredContentID = this.contentSection.getHoveredEntityID();

		if (this.renderTopBar) {
			this.topBarSection.render(outputBuffer, getWindowMousePos());
			if (this.topBarSection.isSectionHovered()) {
				this.hoveredSectionID = this.topBarSection.getBackgroundRect().getID();
			}
			this.hoveredTopBarID = this.topBarSection.getHoveredEntityID();
		}

		if (this.renderBottomBar) {
			this.bottomBarSection.render(outputBuffer, this.getWindowMousePos());
			if (this.bottomBarSection.isSectionHovered()) {
				this.hoveredSectionID = this.bottomBarSection.getBackgroundRect().getID();
			}
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

	private void listEntryClicked(ListEntry clickedEntry) {
		int clickedIndex = -1;
		for (int i = 0; i < this.entryList.size(); i++) {
			if (this.entryList.get(i) == clickedEntry) {
				clickedIndex = i;
				break;
			}
		}

		if (this.singleEntrySelection) {
			if (this.selectedListEntries.size() == 1 && this.selectedListEntries.contains(clickedEntry) && this.submitOnClickingSelectedListEntry) {
				this.submitEntries(new Object[] { clickedEntry.getObject() });
				return;
			}

			this.selectedListEntries.clear();
			this.selectedListEntries.add(clickedEntry);

			this.selectedPivotEntryIndex = clickedIndex;
		}
		else {
			boolean ctrlPressed = KeyboardInput.isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL) || KeyboardInput.isKeyPressed(GLFW.GLFW_KEY_RIGHT_CONTROL);
			boolean shiftPressed = KeyboardInput.isKeyPressed(GLFW.GLFW_KEY_LEFT_SHIFT) || KeyboardInput.isKeyPressed(GLFW.GLFW_KEY_RIGHT_SHIFT);

			if (!ctrlPressed && !shiftPressed && this.selectedListEntries.size() == 1 && this.selectedListEntries.contains(clickedEntry) && this.submitOnClickingSelectedListEntry) {
				this.submitEntries(new Object[] { clickedEntry.getObject() });
				return;
			}

			if (!shiftPressed) {
				//set the pivot
				this.selectedPivotEntryIndex = clickedIndex;
			}

			if (!ctrlPressed) {
				//clear current list of selected entries
				this.selectedListEntries.clear();
			}

			//add pivot to list of selected entries
			this.selectedListEntries.add(clickedEntry);

			if (shiftPressed) {
				int l = Math.min(clickedIndex, this.selectedPivotEntryIndex);
				int r = Math.max(clickedIndex, this.selectedPivotEntryIndex);
				for (int i = l; i <= r; i++) {
					this.selectedListEntries.add(this.entryList.get(i));
				}
			}
		}

		//go through all folder entries and set the selected status of each
		for (ListEntry e : this.entryList) {
			if (this.selectedListEntries.contains(e)) {
				e.setSelected(true);
			}
			else {
				e.setSelected(false);
			}
		}

		//set the top bar text
		if (this.selectedListEntries.size() == 1) {
			this.topBarSelectedEntryText.setText(clickedEntry.getText());
		}
		else {
			this.topBarSelectedEntryText.setText(this.selectedListEntries.size() + " entries selected");
		}
	}

	@Override
	protected void _mousePressed(int button) {
		this.topBarSection.mousePressed(button);
		this.bottomBarSection.mousePressed(button);
		this.contentSection.mousePressed(button);

		if (this.hoveredSectionID == this.topBarSection.getBackgroundRect().getID()) {

		}
		else if (this.hoveredSectionID == this.contentSection.getBackgroundRect().getID()) {
			if (this.hoveredContentID != this.contentSection.getBackgroundRect().getID()) {
				for (ListEntry i : this.entryList) {
					i.selected(this.hoveredContentID);
					if (i.isSelected()) {
						this.listEntryClicked(i);
						break;
					}
				}
			}
		}
	}

	@Override
	protected void _mouseReleased(int button) {
		this.topBarSection.mouseReleased(button);
		this.bottomBarSection.mouseReleased(button);

		if (this.hoveredSectionID == this.bottomBarSection.getBackgroundRect().getID()) {
			switch (Input.getClicked(this.bottomBarSection.getSelectionScene())) {
			case "btn_submit": {
				if (this.selectedListEntries.size() != 0) {
					Object[] objects = new Object[this.selectedListEntries.size()];
					int i = 0;
					for (ListEntry e : this.selectedListEntries) {
						objects[i] = e.getObject();
						i++;
					}

					this.submitEntries(objects);
				}
				break;
			}
			}
		}
	}

	@Override
	protected void _mouseScrolled(float wheelOffset, float smoothOffset) {
		if (this.hoveredSectionID == this.contentSection.getBackgroundRect().getID()) {
			this.contentSection.mouseScrolled(wheelOffset, smoothOffset);
			this.alignEntries();
		}
	}

	@Override
	protected void _keyPressed(int key) {
		this.topBarSection.keyPressed(key);

		if (this.topBarSearchTf.isClicked()) {
			this.setFilter(this.topBarSearchTf.getText());
		}
	}

	@Override
	protected void _keyReleased(int key) {
		this.topBarSection.keyReleased(key);
	}

	class ListEntry {

		private Object o;

		private int selectionScene = -1;
		private int textScene;

		private String text;

		private UIFilledRectangle entryRect = null;
		private Text entryText = null;

		private Material defaultMaterial;
		private Material selectedMaterial;
		private Material hoveredMaterial;

		private boolean isHovered = false;
		private boolean isSelected = false;
		private boolean isVisible = false;

		private boolean doFillWidth = true;

		private UIElement baseUIElement;

		private int horizontalAlignWidth;

		private int entryHeightPx;

		public ListEntry(Object o, Material defaultMaterial, Material hoveredMaterial, Material selectedMaterial, String text, UIElement baseUIElement, int selectionScene, int textScene) {
			this.o = o;

			this.textScene = textScene;
			this.selectionScene = selectionScene;
			this.text = text;
			this.baseUIElement = baseUIElement;

			this.defaultMaterial = defaultMaterial;
			this.hoveredMaterial = hoveredMaterial;
			this.selectedMaterial = selectedMaterial;

			this.horizontalAlignWidth = GraphicsTools.calculateTextWidth(this.text, ListViewerWindow.entryFont) + ListViewerWindow.entryHorizontalMarginPx * 2;
		}

		public Object getObject() {
			return this.o;
		}

		public Text getTextUI() {
			return this.entryText;
		}

		public int getHorizontalAlignWidth() {
			return this.horizontalAlignWidth;
		}

		public void doFillWidth(boolean b) {
			this.doFillWidth = b;

			if (!this.isVisible) {
				return;
			}

			if (this.doFillWidth) {
				this.entryRect.setFillWidth(true);
				this.entryRect.setFillWidthMargin(0);

				this.entryText.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_TOP);
				this.entryText.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
				this.entryText.setXOffset(ListViewerWindow.entryHorizontalMarginPx);
			}
			else {
				this.entryRect.setFillWidth(false);
				this.entryRect.setWidth(this.horizontalAlignWidth);

				this.entryText.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_TOP);
				this.entryText.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_CENTER);
				this.entryText.setXOffset(0);
			}
		}

		public void setEntryHeightPx(int height) {
			this.entryHeightPx = height;
		}

		public void align(int xOffset, int yOffset) {
			int g_xOffset = xOffset;
			int g_yOffset = yOffset - contentSection.getScrollOffset();

			this.isVisible = -this.entryHeightPx <= g_yOffset && g_yOffset <= contentSection.getBackgroundRect().getHeight();
			this.isVisible &= -this.horizontalAlignWidth <= g_xOffset && g_xOffset <= this.baseUIElement.getWidth();

			if (this.isVisible) {
				if (this.entryRect == null) {
					this.entryRect = new UIFilledRectangle(0, 0, 0, this.horizontalAlignWidth, this.entryHeightPx, this.selectionScene);
					this.entryRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
					this.entryRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
					this.entryRect.setMaterial(new Material(new Vec4(1)));
					this.entryRect.bind(this.baseUIElement);

					this.entryText = new Text(0, 0, this.text, 12, Color.WHITE, this.textScene);
					this.entryText.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_TOP);
					this.entryText.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_CENTER);
					this.entryText.setDoAntialiasing(false);
					this.entryText.bind(this.entryRect);

					this.doFillWidth(this.doFillWidth);
				}
				this.entryRect.setFrameAlignmentOffset(xOffset, yOffset);
			}
			else {
				if (this.entryRect != null) {
					this.entryRect.kill();
					this.entryText.kill();
					this.entryRect = null;
					this.entryText = null;
				}
			}
		}

		public void update() {
			if (this.isVisible) {
				if (this.isSelected) {
					this.entryRect.setMaterial(this.selectedMaterial);
				}
				else if (this.isHovered) {
					this.entryRect.setMaterial(this.hoveredMaterial);
				}
				else {
					this.entryRect.setMaterial(this.defaultMaterial);
				}
			}
		}

		public void selected(long entityID) {
			if (this.entryRect == null) {
				this.isSelected = false;
				return;
			}
			this.isSelected = this.entryRect.getID() == entityID;
		}

		public void hovered(long entityID) {
			if (this.entryRect == null) {
				this.isHovered = false;
				return;
			}
			this.isHovered = this.entryRect.getID() == entityID;
		}

		public void kill() {
			if (this.entryRect != null) {
				this.entryRect.kill();
				this.entryText.kill();
			}
		}

		public String getText() {
			return this.text;
		}

		public boolean isHovered() {
			return this.isHovered;
		}

		public boolean isSelected() {
			return this.isSelected;
		}

		public void setSelected(boolean b) {
			this.isSelected = b;
		}

	}

}
