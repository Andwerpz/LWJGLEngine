package lwjglengine.v10.window;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.graphics.Material;
import lwjglengine.v10.input.Input;
import lwjglengine.v10.input.TextField;
import lwjglengine.v10.scene.Scene;
import lwjglengine.v10.screen.UIScreen;
import lwjglengine.v10.ui.Text;
import lwjglengine.v10.ui.UIElement;
import lwjglengine.v10.ui.UIFilledRectangle;
import myutils.v10.graphics.GraphicsTools;
import myutils.v10.math.MathUtils;
import myutils.v10.math.Vec3;
import myutils.v10.math.Vec4;

public class ListViewerWindow extends Window {
	//click on the currently selected entry to submit. 

	//TODO
	// - have name of selected entry show up on top bar. 

	protected final int TOP_BAR_BACKGROUND_SCENE = Scene.generateScene();
	protected final int TOP_BAR_SELECTION_SCENE = Scene.generateScene();
	protected final int TOP_BAR_TEXT_SCENE = Scene.generateScene();

	protected final int CONTENT_BACKGROUND_SCENE = Scene.generateScene();
	protected final int CONTENT_SELECTION_SCENE = Scene.generateScene();
	protected final int CONTENT_TEXT_SCENE = Scene.generateScene();

	public static Font entryFont = new Font("Dialogue", Font.PLAIN, 12);
	public static int entryFontSize = 12;

	private int entryHeightPx = 16;
	public static int entryHorizontalMarginPx = 5;

	public static int topBarSearchTfWidthPx = 150;

	private int topBarHeightPx = 20;

	private Window callbackWindow;

	private UIScreen uiScreen;

	protected UIFilledRectangle topBarBackgroundRect, contentBackgroundRect;

	private TextField topBarSearchTf;
	private Text topBarSelectedEntryText;

	private ArrayList<ListEntry> entryList;

	private ListEntry selectedListEntry = null;

	private boolean closeOnSubmit = true;

	private long hoveredSectionID;
	private long hoveredTopBarID, hoveredContentID;

	private int contentBaseYOffset = 0;
	private int contentBaseXOffset = 0;

	private boolean isHorizontal = false;
	private boolean renderTopBar = true;

	private String filterString = "";

	private boolean sortEntries = true;

	//if there are no displayable list entries, then we'll display a message saying so. 
	private boolean noListEntries = false;

	//if true, displays the currently selected entry on the right side of the top bar. 
	//if false, then it just makes the text transparent. 
	private boolean displaySelectedEntryOnTopBar = true;

	private Text noListEntriesText;

	public ListViewerWindow(int xOffset, int yOffset, int width, int height, Window callbackWindow, Window parentWindow) {
		super(xOffset, yOffset, width, height, parentWindow);
		this.init(callbackWindow);
	}

	public ListViewerWindow(Window callbackWindow, Window parentWindow) {
		super(0, 0, 300, 300, parentWindow);
		this.init(callbackWindow);
	}

	private void init(Window callbackWindow) {
		this.uiScreen = new UIScreen();

		this.entryList = new ArrayList<>();

		this.callbackWindow = callbackWindow;

		this.topBarBackgroundRect = new UIFilledRectangle(0, 0, 0, this.getWidth(), topBarHeightPx, TOP_BAR_BACKGROUND_SCENE);
		this.topBarBackgroundRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.topBarBackgroundRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		this.topBarBackgroundRect.setFillWidth(true);
		this.topBarBackgroundRect.setFillWidthMargin(0);
		this.topBarBackgroundRect.setMaterial(topBarDefaultMaterial);
		this.topBarBackgroundRect.bind(this.rootUIElement);

		this.topBarSearchTf = new TextField(3, 0, topBarSearchTfWidthPx, 16, "tf_filter", "Filter Entries", 12, TOP_BAR_SELECTION_SCENE, TOP_BAR_TEXT_SCENE);
		this.topBarSearchTf.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_CENTER_TOP);
		this.topBarSearchTf.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
		this.topBarSearchTf.getTextUIElement().setDoAntialiasing(false);
		this.topBarSearchTf.bind(this.topBarBackgroundRect);

		this.topBarSelectedEntryText = new Text(3, 0, "       ", 12, Color.WHITE, TOP_BAR_TEXT_SCENE);
		this.topBarSelectedEntryText.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_TOP);
		this.topBarSelectedEntryText.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
		this.topBarSelectedEntryText.setDoAntialiasing(false);
		this.topBarSelectedEntryText.bind(this.topBarBackgroundRect);

		this.contentBackgroundRect = new UIFilledRectangle(0, 0, 0, this.getWidth(), this.getHeight() - topBarHeightPx, CONTENT_BACKGROUND_SCENE);
		this.contentBackgroundRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_BOTTOM);
		this.contentBackgroundRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_BOTTOM);
		this.contentBackgroundRect.setFillWidth(true);
		this.contentBackgroundRect.setFillWidthMargin(0);
		this.contentBackgroundRect.setMaterial(contentDefaultMaterial);
		this.contentBackgroundRect.bind(this.rootUIElement);

		this.noListEntriesText = new Text(0, 0, "No List Entries to Display", 12, Color.WHITE, CONTENT_TEXT_SCENE);
		this.noListEntriesText.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_TOP);
		this.noListEntriesText.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_CENTER);
		this.noListEntriesText.setDoAntialiasing(false);
		this.noListEntriesText.bind(this.contentBackgroundRect);

		this._resize();
	}

	private void clearList() {
		for (ListEntry i : this.entryList) {
			i.kill();
		}
		this.entryList.clear();

		if (this.selectedListEntry != null) {
			this.selectedListEntry = null;
		}

		this.topBarSelectedEntryText.setText("        ");
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
			ListEntry e = new ListEntry(list.get(i), this.contentDefaultMaterial, this.contentHoveredMaterial, this.contentSelectedMaterial, s.get(i), this.contentBackgroundRect, CONTENT_SELECTION_SCENE, CONTENT_TEXT_SCENE);
			this.entryList.add(e);
		}

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

		ListEntry e = new ListEntry(elem, this.contentDefaultMaterial, this.contentHoveredMaterial, this.contentSelectedMaterial, s, this.contentBackgroundRect, CONTENT_SELECTION_SCENE, CONTENT_TEXT_SCENE);
		this.entryList.add(e);

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

	private void setContentBaseXOffset(int offset) {
		this.contentBaseXOffset = offset;
		this.alignEntries();
	}

	private void setContentBaseYOffset(int offset) {
		this.contentBaseYOffset = offset;
		this.alignEntries();
	}

	public void setEntryHeightPx(int height) {
		this.entryHeightPx = height;
		this.alignEntries();
	}

	public String getSelectedEntryString() {
		if (this.selectedListEntry == null) {
			return "";
		}
		return this.selectedListEntry.getText();
	}

	private void alignEntries() {
		this.contentBackgroundRect.align();

		if (this.sortEntries) {
			this.sortList();
		}

		int horizontalAlignWidthSum = 0;
		ArrayList<ListEntry> filteredEntries = new ArrayList<>();
		for (ListEntry i : this.entryList) {
			if (i.getText().toLowerCase().contains(this.filterString)) {
				filteredEntries.add(i);
				horizontalAlignWidthSum += i.getHorizontalAlignWidth();
			}
			else {
				i.align(this.getWidth() * 2, this.getHeight() * 2);
			}
		}

		if (filteredEntries.size() == 0) {
			this.noListEntries = true;
			this.noListEntriesText.setFrameAlignmentOffset(0, 0);
		}
		else {
			this.noListEntriesText.setFrameAlignmentOffset(this.getWidth(), this.getHeight());
		}

		//make sure offset is within bounds. 
		if (this.isHorizontal) {
			this.contentBaseYOffset = 0;
			int minBaseXOffset = (int) Math.min(0, this.contentBackgroundRect.getWidth() - horizontalAlignWidthSum);
			this.contentBaseXOffset = (int) MathUtils.clamp(minBaseXOffset, 0, this.contentBaseXOffset);
		}
		else {
			this.contentBaseXOffset = 0;
			int minBaseYOffset = Math.min(0, (int) this.contentBackgroundRect.getHeight() - this.entryList.size() * entryHeightPx);
			this.contentBaseYOffset = (int) MathUtils.clamp(minBaseYOffset, 0, this.contentBaseYOffset);
		}

		int xOffset = this.contentBaseXOffset;
		int yOffset = this.contentBaseYOffset;
		for (ListEntry i : filteredEntries) {
			i.setEntryHeightPx(this.entryHeightPx);

			if (this.isHorizontal) {
				i.doFillWidth(false);
			}
			else {
				i.doFillWidth(true);
			}
			i.align(xOffset, yOffset);
			if (this.isHorizontal) {
				xOffset += i.getHorizontalAlignWidth();
			}
			else {
				yOffset += entryHeightPx;
			}
		}
	}

	private void sortList() {
		Collections.sort(this.entryList, (a, b) -> {
			return a.getText().compareTo(b.getText());
		});
	}

	public void setSortEntries(boolean b) {
		this.sortEntries = b;
	}

	public void setCloseOnSubmit(boolean b) {
		this.closeOnSubmit = b;
	}

	public void setIsHorizontal(boolean b) {
		if (this.isHorizontal == b) {
			return;
		}
		this.isHorizontal = b;

		this.alignEntries();
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
		this.uiScreen.kill();

		Scene.removeScene(TOP_BAR_BACKGROUND_SCENE);
		Scene.removeScene(TOP_BAR_SELECTION_SCENE);
		Scene.removeScene(TOP_BAR_TEXT_SCENE);

		Scene.removeScene(CONTENT_BACKGROUND_SCENE);
		Scene.removeScene(CONTENT_SELECTION_SCENE);
		Scene.removeScene(CONTENT_TEXT_SCENE);
	}

	@Override
	protected void _resize() {
		this.uiScreen.setScreenDimensions(this.getWidth(), this.getHeight());

		this.topBarBackgroundRect.setHeight(this.topBarHeightPx);

		if (this.renderTopBar) {
			this.contentBackgroundRect.setHeight(this.getHeight() - this.topBarHeightPx);
		}
		else {
			this.contentBackgroundRect.setHeight(this.getHeight());
		}

		this.topBarSelectedEntryText.setWidth(this.getWidth() - 6);

		this.alignEntries();
	}

	protected void submitEntry(Object o) {
		this.callbackWindow.handleObject(o);
		if (this.closeOnSubmit) {
			this.close();
			return;
		}
	}

	@Override
	protected void _update() {
		Input.inputsHovered(this.hoveredTopBarID, TOP_BAR_SELECTION_SCENE);

		for (ListEntry i : this.entryList) {
			i.hovered(this.hoveredContentID);
			i.update();
		}
	}

	@Override
	protected void renderContent(Framebuffer outputBuffer) {
		int mouseX = (int) this.getWindowMousePos().x;
		int mouseY = (int) this.getWindowMousePos().y;

		this.uiScreen.setUIScene(CONTENT_BACKGROUND_SCENE);
		this.uiScreen.render(outputBuffer);
		if (this.uiScreen.getEntityIDAtCoord(mouseX, mouseY) == this.contentBackgroundRect.getID()) {
			this.hoveredSectionID = this.contentBackgroundRect.getID();
		}
		this.uiScreen.setUIScene(CONTENT_SELECTION_SCENE);
		this.uiScreen.render(outputBuffer);
		this.hoveredContentID = this.uiScreen.getEntityIDAtCoord(mouseX, mouseY);
		this.uiScreen.setUIScene(CONTENT_TEXT_SCENE);
		this.uiScreen.render(outputBuffer);

		if (this.renderTopBar) {
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
		Input.inputsPressed(this.hoveredTopBarID, TOP_BAR_SELECTION_SCENE);

		if (this.hoveredSectionID == this.topBarBackgroundRect.getID()) {

		}
		else if (this.hoveredSectionID == this.contentBackgroundRect.getID()) {
			if (this.hoveredContentID != this.contentBackgroundRect.getID()) {
				for (ListEntry i : this.entryList) {
					i.selected(this.hoveredContentID);
					if (i.isSelected()) {
						if (i == this.selectedListEntry) {
							Object o = this.selectedListEntry.getObject();
							this.submitEntry(o);
						}
						this.selectedListEntry = i;
						this.topBarSelectedEntryText.setText(this.selectedListEntry.getText());
					}
				}
			}
		}
	}

	@Override
	protected void _mouseReleased(int button) {
		Input.inputsReleased(this.hoveredTopBarID, TOP_BAR_SELECTION_SCENE);
	}

	@Override
	protected void _mouseScrolled(float wheelOffset, float smoothOffset) {
		if (this.hoveredSectionID == this.contentBackgroundRect.getID()) {
			if (this.isHorizontal) {
				this.setContentBaseXOffset(this.contentBaseXOffset - (int) ((smoothOffset) * entryHeightPx));
			}
			else {
				this.setContentBaseYOffset(this.contentBaseYOffset - (int) ((smoothOffset) * entryHeightPx));
			}
		}
	}

	@Override
	protected void _keyPressed(int key) {
		Input.inputsKeyPressed(key, TOP_BAR_SELECTION_SCENE);

		if (this.topBarSearchTf.isClicked()) {
			this.filterString = this.topBarSearchTf.getText().toLowerCase();
		}

		this.alignEntries();
	}

	@Override
	protected void _keyReleased(int key) {
		Input.inputsKeyReleased(key, TOP_BAR_SELECTION_SCENE);
	}

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
		this.isVisible = -this.entryHeightPx <= yOffset && yOffset <= this.baseUIElement.getHeight();
		this.isVisible &= -this.horizontalAlignWidth <= xOffset && xOffset <= this.baseUIElement.getWidth();

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

}
