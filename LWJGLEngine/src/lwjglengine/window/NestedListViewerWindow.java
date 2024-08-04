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

import java.awt.Button;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Stack;

import lwjglengine.graphics.Framebuffer;
import lwjglengine.graphics.Material;
import lwjglengine.graphics.Texture;
import lwjglengine.graphics.TextureMaterial;
import lwjglengine.input.Input;
import lwjglengine.model.FilledRectangle;
import lwjglengine.ui.Text;
import lwjglengine.ui.UIElement;
import lwjglengine.ui.UIFilledRectangle;
import lwjglengine.ui.UISection;
import lwjglengine.ui.UISectionListener;
import myutils.file.JarUtils;
import myutils.file.SystemUtils;
import myutils.file.xml.XMLNode;

public class NestedListViewerWindow extends Window implements UISectionListener {
	//entries can nest under one another, much like going through a file explorer tree. 
	//useful for searching through nested structures such as XML or smth. 

	//how to index the list entries to dynamically add and remove elements from a pre-existing list?
	//for now, maybe just make lists static. 

	private UISection bottomBarSection, topBarSection, contentSection;
	private Button bottomBarSubmitBtn;
	private Text topBarEntryPathText;

	private ListEntry rootEntry;

	private boolean shouldRealignEntries = false;
	private boolean shouldUpdateVisibility = false;
	private ListEntry topDisplayedEntry = null, bottomDisplayedEntry = null;

	private Text noListEntriesText;

	private boolean renderTopBar = true;
	private boolean renderBottomBar = true;

	private static int topBarHeightPx = 20;
	private static int bottomBarHeightPx = 20;

	private long hoveredSectionID, hoveredContentID;

	private TextureMaterial dropdownArrowTexture;
	private FilledRectangle dropdownArrowTextureRect;

	private boolean submitOnDoubleClick = false;
	private boolean closeOnSubmit = true;

	private boolean onlySubmitLeafEntries = true;

	private NestedListViewerCallback callback;

	public NestedListViewerWindow(int xOffset, int yOffset, int width, int height, NestedListViewerCallback callback, Window parent_window) {
		super(xOffset, yOffset, width, height, parent_window);
		this.init(callback);
	}

	public NestedListViewerWindow(NestedListViewerCallback callback, Window parent_window) {
		super(parent_window);
		this.init(callback);
	}

	private void init(NestedListViewerCallback callback) {
		this.callback = callback;

		this.rootEntry = new ListEntry(null, null, " ");

		this.contentSection = new UISection();
		this.contentSection.setIsScrollable(true);
		this.contentSection.addListener(this);
		this.contentSection.getBackgroundRect().setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.contentSection.getBackgroundRect().setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		this.contentSection.getBackgroundRect().setFrameAlignmentOffset(0, 0);
		this.contentSection.getBackgroundRect().setFillWidth(true);
		this.contentSection.getBackgroundRect().setMaterial(Material.CONTENT_DEFAULT_MATERIAL);
		this.contentSection.getBackgroundRect().bind(this.rootUIElement);

		this.noListEntriesText = new Text(0, 0, "No List Entries to Display", 12, Color.WHITE, this.contentSection.getTextScene());
		this.noListEntriesText.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_TOP);
		this.noListEntriesText.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_CENTER);
		this.noListEntriesText.bind(this.contentSection.getBackgroundRect());

		this.topBarSection = new UISection();
		this.topBarSection.getBackgroundRect().setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.topBarSection.getBackgroundRect().setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		this.topBarSection.getBackgroundRect().setFrameAlignmentOffset(0, 0);
		this.topBarSection.getBackgroundRect().setFillWidth(true);
		this.topBarSection.getBackgroundRect().setHeight(topBarHeightPx);
		this.topBarSection.getBackgroundRect().setMaterial(Material.TOP_BAR_DEFAULT_MATERIAL);
		this.topBarSection.getBackgroundRect().bind(this.rootUIElement);

		this.topBarEntryPathText = new Text(5, 0, "   ", 12, Color.WHITE, this.topBarSection.getTextScene());
		this.topBarEntryPathText.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_BOTTOM);
		this.topBarEntryPathText.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
		this.topBarEntryPathText.bind(this.topBarSection.getBackgroundRect());

		this.bottomBarSection = new UISection();
		this.bottomBarSection.getBackgroundRect().setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_BOTTOM);
		this.bottomBarSection.getBackgroundRect().setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_BOTTOM);
		this.bottomBarSection.getBackgroundRect().setFrameAlignmentOffset(0, 0);
		this.bottomBarSection.getBackgroundRect().setFillWidth(true);
		this.bottomBarSection.getBackgroundRect().setHeight(bottomBarHeightPx);
		this.bottomBarSection.getBackgroundRect().setMaterial(Material.TOP_BAR_DEFAULT_MATERIAL);
		this.bottomBarSection.getBackgroundRect().bind(this.rootUIElement);

		{
			Texture tex = new Texture(JarUtils.loadImage("/dropdown_arrow.png"));
			tex.setMinSampleType(GL_LINEAR_MIPMAP_LINEAR);

			this.dropdownArrowTexture = new TextureMaterial(tex);
			this.dropdownArrowTextureRect = new FilledRectangle();
			this.dropdownArrowTextureRect.setTextureMaterial(this.dropdownArrowTexture);
		}

		this._resize();
	}

	public void setRenderTopBar(boolean b) {
		this.renderTopBar = b;
		this._resize();
	}

	public void setRenderBottomBar(boolean b) {
		this.renderBottomBar = b;
		this._resize();
	}

	public void setSubmitOnDoubleClick(boolean b) {
		this.submitOnDoubleClick = b;
	}

	public void setCloseOnSubmit(boolean b) {
		this.closeOnSubmit = b;
	}

	private ArrayList<ListEntry> listVisibleEntries() {
		ArrayList<ListEntry> ret = new ArrayList<>();
		if (this.topDisplayedEntry == null) {
			return ret;
		}
		ListEntry ptr = this.topDisplayedEntry;
		while (ptr != null && ptr != this.bottomDisplayedEntry) {
			ret.add(ptr);
			ptr = this.nextDisplayedEntry(ptr);
		}
		ret.add(this.bottomDisplayedEntry);
		return ret;
	}

	private ArrayList<ListEntry> listAllEntries() {
		ArrayList<ListEntry> ret = new ArrayList<>();
		Stack<ListEntry> s = new Stack<>();
		s.push(this.rootEntry);
		while (s.size() != 0) {
			ListEntry e = s.pop();
			if (e != this.rootEntry) {
				ret.add(e);
			}
			for (ListEntry c : e.children) {
				s.push(c);
			}
		}
		return ret;
	}

	private void triggerRealignEntries() {
		this.shouldRealignEntries = true;
	}

	private void triggerUpdateVisibility() {
		this.shouldUpdateVisibility = true;
	}

	public void clearList() {
		for (int i = this.rootEntry.children.size() - 1; i >= 0; i--) {
			this.rootEntry.children.get(i).kill();
		}
	}

	public void setList(XMLNode root) {
		this.clearList();
		for (XMLNode c : root.getChildren()) {
			this._setList(c, this.rootEntry);
		}
	}

	private void _setList(XMLNode root, ListEntry parent) {
		ListEntry entry = new ListEntry(parent, root, root.getName());
		for (XMLNode c : root.getChildren()) {
			this._setList(c, entry);
		}
	}

	@Override
	protected void _kill() {
		this.contentSection.kill();
		this.bottomBarSection.kill();
		this.topBarSection.kill();

		this.dropdownArrowTextureRect.kill();
		this.dropdownArrowTexture.kill();
	}

	@Override
	protected void _resize() {
		this.contentSection.setScreenDimensions(this.getWidth(), this.getHeight());
		this.bottomBarSection.setScreenDimensions(this.getWidth(), this.getHeight());
		this.topBarSection.setScreenDimensions(this.getWidth(), this.getHeight());

		int content_height = this.getHeight();
		int content_yoffset = 0;
		if (this.renderTopBar) {
			content_height -= topBarHeightPx;
			content_yoffset += topBarHeightPx;
		}
		if (this.renderBottomBar) {
			content_height -= bottomBarHeightPx;
		}
		this.contentSection.getBackgroundRect().setYOffset(content_yoffset);
		this.contentSection.getBackgroundRect().setHeight(content_height);

		this.topBarEntryPathText.setWidth(this.getWidth() - 6);

		this.triggerUpdateVisibility();
	}

	@Override
	public String getDefaultTitle() {
		return "Nested List Viewer";
	}

	//returns null if no next entry exists
	private ListEntry nextDisplayedEntry(ListEntry e) {
		int ind = 0;
		while (true) {
			while (ind != e.children.size() && !e.children.get(ind).isDisplayed) {
				ind++;
			}
			if (ind != e.children.size()) {
				break;
			}
			if (e.parent == null) {
				return null;
			}
			ind = e.parent.children.indexOf(e) + 1;
			e = e.parent;
		}
		return e.children.get(ind);
	}

	//returns null if no previous entry exists
	private ListEntry previousDisplayedEntry(ListEntry e) {
		if (e == this.rootEntry) {
			return null;
		}
		int ind = e.parent.children.indexOf(e) - 1;
		while (ind != -1 && !e.parent.children.get(ind).isDisplayed) {
			ind--;
		}
		if (ind >= 0) {
			//we've found the parent of the previous entry.
			//still need to traverse all the way down. 
			ListEntry ret = e.parent.children.get(ind);
			while (true) {
				ListEntry next = null;
				for (int i = ret.children.size() - 1; i >= 0; i--) {
					if (ret.children.get(i).isDisplayed) {
						next = ret.children.get(i);
						break;
					}
				}
				if (next == null) {
					break;
				}
				ret = next;
			}
			return ret;
		}
		return e.parent;
	}

	@Override
	protected void _update() {
		if (this.shouldRealignEntries) {
			this.shouldRealignEntries = false;
			this.rootEntry.root_align();

			this.topDisplayedEntry = null;
			this.bottomDisplayedEntry = null;

			//update size of scroll window
			int newScrollHeight = this.rootEntry.subtreeHeightPx - entryHeightPx; //since root entry shouldn't be displayed. 
			this.contentSection.setScrollRectHeight(newScrollHeight);
			this.triggerUpdateVisibility();
		}

		update_vis_block:
		if (this.shouldUpdateVisibility) {
			//not ready to do anything yet
			if (this.rootEntry == null) {
				break update_vis_block;
			}

			this.shouldUpdateVisibility = false;

			if (this.rootEntry.children.size() == 0) {
				this.noListEntriesText.setFrameAlignmentOffset(0, 0);
			}
			else {
				this.noListEntriesText.setFrameAlignmentOffset(this.getWidth(), this.getHeight());
			}

			//nothing to update. 
			if (this.rootEntry.children.size() == 0) {
				this.topDisplayedEntry = null;
				this.bottomDisplayedEntry = null;
				break update_vis_block;
			}

			int top_offset = this.contentSection.getScrollOffset();
			int bottom_offset = this.contentSection.getScrollOffset() + (int) this.contentSection.getBackgroundRect().getHeight();

			//look for the top visible entry. 
			if (this.topDisplayedEntry == null) {
				assert this.topDisplayedEntry == null && this.bottomDisplayedEntry == null;
				this.topDisplayedEntry = this.rootEntry;
				while (this.topDisplayedEntry.yOffset + entryHeightPx < top_offset) {
					ListEntry next_entry = this.nextDisplayedEntry(this.topDisplayedEntry);
					if (next_entry != null) {
						this.topDisplayedEntry = next_entry;
					}
					else {
						break;
					}
				}
				this.bottomDisplayedEntry = this.topDisplayedEntry;

				this.topDisplayedEntry.setVisible(true);
			}

			//update the top and bottom pointers. 
			//first expand
			while (true) {
				ListEntry prev_entry = this.previousDisplayedEntry(this.topDisplayedEntry);
				if (prev_entry != null && prev_entry.yOffset + entryHeightPx >= top_offset) {
					prev_entry.setVisible(true);
					this.topDisplayedEntry = prev_entry;
				}
				else {
					break;
				}
			}
			while (true) {
				ListEntry next_entry = this.nextDisplayedEntry(this.bottomDisplayedEntry);
				if (next_entry != null && next_entry.yOffset <= bottom_offset) {
					next_entry.setVisible(true);
					this.bottomDisplayedEntry = next_entry;
				}
				else {
					break;
				}
			}

			//then shrink
			while (true) {
				ListEntry next_entry = this.nextDisplayedEntry(this.topDisplayedEntry);
				if (next_entry != null && next_entry.yOffset + entryHeightPx < top_offset) {
					this.topDisplayedEntry.setVisible(false);
					this.topDisplayedEntry = next_entry;
				}
				else {
					break;
				}
			}
			while (true) {
				ListEntry prev_entry = this.previousDisplayedEntry(this.bottomDisplayedEntry);
				if (prev_entry != null && prev_entry.yOffset > bottom_offset) {
					this.bottomDisplayedEntry.setVisible(false);
					this.bottomDisplayedEntry = prev_entry;
				}
				else {
					break;
				}
			}
		}

		if (this.hoveredSectionID == this.contentSection.getBackgroundRect().getID()) {
			for (ListEntry e : this.listVisibleEntries()) {
				e.hovered(this.hoveredContentID);
			}
		}

		for (ListEntry e : this.listVisibleEntries()) {
			e.updateMaterial();
		}

		this.contentSection.update();
		this.bottomBarSection.update();
		this.topBarSection.update();
	}

	@Override
	protected void renderContent(Framebuffer outputBuffer) {
		this.contentSection.render(outputBuffer, this.getWindowMousePos());
		if (this.renderTopBar) {
			this.topBarSection.render(outputBuffer, this.getWindowMousePos());
		}
		if (this.renderBottomBar) {
			this.bottomBarSection.render(outputBuffer, this.getWindowMousePos());
		}

		this.hoveredSectionID = this.contentSection.getBackgroundRect().getID();
		this.hoveredContentID = this.contentSection.getHoveredSelectionID();
		if (this.topBarSection.isSectionHovered()) {
			this.hoveredSectionID = this.topBarSection.getBackgroundRect().getID();
		}
		if (this.bottomBarSection.isSectionHovered()) {
			this.hoveredSectionID = this.bottomBarSection.getBackgroundRect().getID();
		}
	}

	@Override
	protected void renderOverlay(Framebuffer outputBuffer) {

	}

	@Override
	protected void selected() {

	}

	@Override
	protected void deselected() {

	}

	@Override
	protected void subtreeSelected() {

	}

	@Override
	protected void subtreeDeselected() {

	}

	public void submit(ListEntry e) {
		if (this.onlySubmitLeafEntries && e.children.size() != 0) {
			return;
		}

		this.callback.handleCallback(e.getPath(), e.contents);
		if (this.closeOnSubmit) {
			this.close();
		}
	}

	@Override
	protected void _mousePressed(int button) {
		this.contentSection.mousePressed(button);
		this.bottomBarSection.mousePressed(button);
		this.topBarSection.mousePressed(button);

		if (this.hoveredSectionID == this.contentSection.getBackgroundRect().getID()) {
			for (ListEntry e : this.listVisibleEntries()) {
				boolean already_selected = e.isSelected;
				if (e.clicked(this.hoveredContentID)) {
					if (this.submitOnDoubleClick && already_selected) {
						this.submit(e);
					}
					this.topBarEntryPathText.setText(e.getPath());
				}
			}
		}

	}

	@Override
	protected void _mouseReleased(int button) {
		this.contentSection.mouseReleased(button);
		this.bottomBarSection.mouseReleased(button);
		this.topBarSection.mouseReleased(button);
	}

	@Override
	protected void _mouseScrolled(float wheelOffset, float smoothOffset) {
		this.contentSection.mouseScrolled(wheelOffset, smoothOffset);
	}

	@Override
	protected void _keyPressed(int key) {

	}

	@Override
	protected void _keyReleased(int key) {

	}

	@Override
	public void uiSectionScrolled(UISection section) {
		if (section == this.contentSection) {
			this.triggerUpdateVisibility();
		}
	}

	public static int entryHeightPx = 16;
	public static int entryLeftMarginBasePx = 16;
	public static int entryLeftMarginIntervalPx = 12;

	public static Font entryFont = Text.DEFAULT_FONT;
	public static int entryFontSize = 12;

	class ListEntry {
		public ListEntry parent;
		public ArrayList<ListEntry> children;

		public Object contents;
		public String text;
		public UIFilledRectangle entryRect;
		public Text entryText;

		public UIFilledRectangle dropdownArrowRect;

		public int yOffset;

		//root should always be expanded and displayed. 
		public boolean isExpanded = false; //this is pretty obvious
		public boolean isDisplayed = false; //if true, should be visible on screen when scrolled down enough.
		public boolean isVisible = false; //is this actually visible on screen?

		public boolean isSelected = false;
		public boolean isHovered = false;

		public int depth;
		public int subtreeHeightPx = 0;

		public ListEntry(ListEntry parent, Object contents, String text) {
			this.parent = parent;
			this.children = new ArrayList<>();
			if (this.parent != null) {
				this.parent.addChild(this);
				this.depth = this.parent.depth + 1;
			}
			else {
				//this is the root
				this.depth = -1;
				this.isDisplayed = true;
				this.isExpanded = true;
			}
			this.subtreeHeightPx = entryHeightPx;

			this.contents = contents;
			this.text = text;
			this.entryRect = null;
			this.entryText = null;

			triggerRealignEntries();
		}

		private void addChild(ListEntry child) {
			this.children.add(child);
		}

		private void removeChild(ListEntry child) {
			this.children.remove(child);
		}

		public void updateMaterial() {
			if (this.isVisible) {
				//update background rect material based off of current state. 
				if (this.isSelected) {
					this.entryRect.setMaterial(Material.CONTENT_SELECTED_MATERIAL);
				}
				else if (this.isHovered) {
					this.entryRect.setMaterial(Material.CONTENT_HOVERED_MATERIAL);
				}
				else {
					this.entryRect.setMaterial(Material.CONTENT_DEFAULT_MATERIAL);
				}
			}
		}

		public void setVisible(boolean b) {
			if (this.isVisible && !b) {
				this.entryRect.kill();
				this.entryRect = null;
				this.entryText.kill();
				this.entryText = null;

				if (this.dropdownArrowRect != null) {
					this.dropdownArrowRect.kill();
					this.dropdownArrowRect = null;
				}
			}
			else if (!this.isVisible && b) {
				UIFilledRectangle backgroundRect = contentSection.getScrollBackgroundRect();

				this.entryRect = new UIFilledRectangle(0, 0, 0, backgroundRect.getWidth(), entryHeightPx, contentSection.getSelectionScene());
				this.entryRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
				this.entryRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
				this.entryRect.setFillWidth(true);
				this.entryRect.setYOffset(this.yOffset);
				this.entryRect.bind(backgroundRect);

				int x_offset = entryLeftMarginBasePx + this.depth * entryLeftMarginIntervalPx;

				this.entryText = new Text(x_offset, 0, this.text, entryFont, entryFontSize, Color.WHITE, contentSection.getTextScene());
				this.entryText.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_TOP);
				this.entryText.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
				this.entryText.bind(this.entryRect);

				if (this.children.size() != 0) {
					int arrow_size = 10;
					this.dropdownArrowRect = new UIFilledRectangle(x_offset - (entryHeightPx - arrow_size) / 2, 0, 0, arrow_size, arrow_size, dropdownArrowTextureRect, contentSection.getTextScene());
					this.dropdownArrowRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_TOP);
					this.dropdownArrowRect.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
					this.dropdownArrowRect.setRotationRads((float) (Math.PI / 2.0 + (this.isExpanded ? Math.PI / 2.0 : 0)));
					this.dropdownArrowRect.setMaterial(new Material(Color.WHITE));
					this.dropdownArrowRect.setKillCustomBoundingRect(false);
					this.dropdownArrowRect.bind(this.entryRect);
				}
			}
			this.isVisible = b;
		}

		//after calling, need to realign everything. 
		public void kill() {
			triggerRealignEntries();
			if (this.parent != null) {
				this.parent.removeChild(this);
			}
			this.setVisible(false);

			for (int i = this.children.size() - 1; i >= 0; i--) {
				this.children.get(i).kill();
			}
		}

		//calling this should make all entries not visible again. 
		public void root_align() {
			assert this.parent == null;
			this.align(-entryHeightPx);
		}

		//recursively aligns all children. 
		private void align(int y_offset) {
			if (this.parent != null) {
				this.isDisplayed = this.parent.isDisplayed && this.parent.isExpanded;
			}

			this.subtreeHeightPx = this.isDisplayed ? entryHeightPx : 0;
			this.yOffset = y_offset;

			//visibility should be set externally, so after we realign, just set to not visible. 
			this.setVisible(false);

			int next_offset = y_offset + (this.isDisplayed ? entryHeightPx : 0);
			for (ListEntry e : this.children) {
				e.align(next_offset);
				this.subtreeHeightPx += e.subtreeHeightPx;
				next_offset += e.subtreeHeightPx;
			}
		}

		public void setIsExpanded(boolean b) {
			if (this.children.size() == 0) {
				return;
			}
			if (b != this.isExpanded) {
				triggerRealignEntries();
			}
			this.isExpanded = b;
		}

		public boolean clicked(long entityID) {
			this.isSelected = this.isVisible && entityID == this.entryRect.getID();
			if (this.isSelected) {
				this.setIsExpanded(!this.isExpanded);
			}
			return this.isSelected;
		}

		public void hovered(long entityID) {
			this.isHovered = this.isVisible && entityID == this.entryRect.getID();
		}

		public String getPath() {
			//is my parent the root entry?
			if (this.parent.parent == null) {
				return this.text;
			}
			return this.parent.getPath() + "/" + this.text;
		}

	}

	public interface NestedListViewerCallback {
		void handleCallback(String path, Object contents);
	}
}
