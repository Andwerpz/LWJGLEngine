package lwjglengine.v10.window;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.graphics.Material;
import lwjglengine.v10.scene.Scene;
import lwjglengine.v10.screen.UIScreen;
import lwjglengine.v10.ui.Text;
import lwjglengine.v10.ui.UIElement;
import lwjglengine.v10.ui.UIFilledRectangle;
import myutils.v10.graphics.GraphicsTools;
import myutils.v10.math.MathUtils;
import myutils.v10.math.Vec3;

public class ListViewerWindow extends Window {
	//click on the currently selected entry to submit. 
	
	//TODO
	// - be able to do horizontal or vertical lists
	// - make top bar optional
	
	private final int TOP_BAR_BACKGROUND_SCENE = Scene.generateScene();
	private final int TOP_BAR_SELECTION_SCENE = Scene.generateScene();
	private final int TOP_BAR_TEXT_SCENE = Scene.generateScene();
	
	private final int CONTENT_BACKGROUND_SCENE = Scene.generateScene();
	private final int CONTENT_SELECTION_SCENE = Scene.generateScene();
	private final int CONTENT_TEXT_SCENE = Scene.generateScene();
	
	public Material topBarDefaultMaterial = new Material(new Vec3((float) (20 / 255.0)));
	public Material topBarHoveredMaterial = new Material(new Vec3((float) (30 / 255.0)));
	public Material topBarSelectedMaterial = new Material(new Vec3((float) (40 / 255.0)));
	
	public Material contentDefaultMaterial = new Material(new Vec3((float) (40 / 255.0)));
	public Material contentHoveredMaterial = new Material(new Vec3((float) (50 / 255.0)));
	public Material contentSelectedMaterial = new Material(new Vec3((float) (60 / 255.0)));
	
	public static Font entryFont = new Font("Dialogue", Font.PLAIN, 12);
	public static int entryFontSize = 12;
	
	public static int entryHeightPx = 20;
	public static int entryHorizontalMarginPx = 5;
	
	private int topBarHeightPx = 20;
	
	private Window callbackWindow;
	
	private UIScreen uiScreen;
	
	private UIFilledRectangle topBarBackgroundRect, contentBackgroundRect;
	
	private ArrayList<ListEntry> entryList;
	
	private ListEntry selectedListEntry = null;
	private ListEntry submittedListEntry = null;
	
	private boolean closeOnSubmit = true;
	
	private long hoveredSectionID;
	private long hoveredTopBarID, hoveredContentID;
	
	private int contentBaseYOffset = 0;
	private int contentBaseXOffset = 0;
	
	private boolean isHorizontal = false;
	private boolean renderTopBar = true;
	
	private int horizontalAlignWidthSum = 0;

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
		
		this.callbackWindow = callbackWindow;
		
		this.topBarBackgroundRect = new UIFilledRectangle(0, 0, 0, this.getWidth(), topBarHeightPx, TOP_BAR_BACKGROUND_SCENE);
		this.topBarBackgroundRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.topBarBackgroundRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		this.topBarBackgroundRect.setFillWidth(true);
		this.topBarBackgroundRect.setFillWidthMargin(0);
		this.topBarBackgroundRect.setMaterial(topBarDefaultMaterial);
		this.topBarBackgroundRect.bind(this.rootUIElement);
		
		this.contentBackgroundRect = new UIFilledRectangle(0, 0, 0, this.getWidth(), this.getHeight() - topBarHeightPx, CONTENT_BACKGROUND_SCENE);
		this.contentBackgroundRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_BOTTOM);
		this.contentBackgroundRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_BOTTOM);
		this.contentBackgroundRect.setFillWidth(true);
		this.contentBackgroundRect.setFillWidthMargin(0);
		this.contentBackgroundRect.setMaterial(contentDefaultMaterial);
		this.contentBackgroundRect.bind(this.rootUIElement);
		
		this._resize();
	}
	
	private void clearList() {
		for(ListEntry i : this.entryList) {
			i.kill();
		}
		this.entryList.clear();
	}
	
	/**
	 * Creates a string representation for each element of the list. 
	 * @param <T>
	 * @param list
	 */
	public <T> void setList(ArrayList<T> list) {
		this.clearList();
		for(T i : list) {
			if(i == null) {
				continue;
			}
			
			String text = i.toString();
			ListEntry e = new ListEntry(this.contentDefaultMaterial, this.contentHoveredMaterial, this.contentSelectedMaterial, text, this.contentBackgroundRect, CONTENT_SELECTION_SCENE, CONTENT_TEXT_SCENE);
			this.entryList.add(e);
			this.horizontalAlignWidthSum += e.getHorizontalAlignWidth();
		}
		this.sortList();
	}
	
	public <T> void addToList(T elem) {
		if(elem == null) {
			return;
		}
		
		String text = elem.toString();
		ListEntry e = new ListEntry(this.contentDefaultMaterial, this.contentHoveredMaterial, this.contentSelectedMaterial, text, this.contentBackgroundRect, CONTENT_SELECTION_SCENE, CONTENT_TEXT_SCENE);
		this.entryList.add(e);
		this.horizontalAlignWidthSum += e.getHorizontalAlignWidth();
		
		this.sortList();
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
	
	private void alignEntries() {
		//make sure offset is within bounds. 
		if(this.isHorizontal) {
			this.contentBaseYOffset = 0;
			this.contentBaseXOffset = (int) MathUtils.clamp(this.contentBackgroundRect.getHeight() - this.horizontalAlignWidthSum, 0, this.contentBaseXOffset);
		}
		else {
			this.contentBaseXOffset = 0;
			int minBaseYOffset = Math.min(0, (int) this.contentBackgroundRect.getHeight() - this.entryList.size() * entryHeightPx);
			this.contentBaseYOffset = (int) MathUtils.clamp(minBaseYOffset, 0, this.contentBaseYOffset);
		}
		
		int xOffset = this.contentBaseXOffset;
		int yOffset = this.contentBaseYOffset;
		for(ListEntry i : this.entryList) {
			i.align(xOffset, yOffset);
			if(this.isHorizontal) {
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
		
		this.alignEntries();
	}
	
	public void setCloseOnSubmit(boolean b) {
		this.closeOnSubmit = b;
	}
	
	public void setIsHorizontal(boolean b) {
		if(this.isHorizontal == b) {
			return;
		}
		this.isHorizontal = b;
		
		this.alignEntries();
	}
	
	public void setRenderTopBar(boolean b) {
		if(this.renderTopBar == b) {
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
		
		if(this.renderTopBar) {
			this.contentBackgroundRect.setHeight(this.getHeight() - this.topBarHeightPx);
		}
		else {
			this.contentBackgroundRect.setHeight(this.getHeight());
		}
	}

	@Override
	protected void _update() {
		if(this.submittedListEntry != null) {
			this.callbackWindow.handleString(this.submittedListEntry.getText());
			if(this.closeOnSubmit) {
				this.kill();
				return;
			}
		}
		
		for(ListEntry i : this.entryList) {
			i.hovered(this.hoveredContentID);
			i.update();
		}
	}

	@Override
	protected void renderContent(Framebuffer outputBuffer) {
		int mouseX = (int) this.getWindowMousePos().x;
		int mouseY = (int) this.getWindowMousePos().y;
		
		if(this.renderTopBar) {
			this.uiScreen.setUIScene(TOP_BAR_BACKGROUND_SCENE);
			this.uiScreen.render(outputBuffer);		
			if(this.uiScreen.getEntityIDAtCoord(mouseX, mouseY) == this.topBarBackgroundRect.getID()) {
				this.hoveredSectionID = this.topBarBackgroundRect.getID();
			}
			this.uiScreen.setUIScene(TOP_BAR_SELECTION_SCENE);
			this.uiScreen.render(outputBuffer);
			this.hoveredTopBarID = this.uiScreen.getEntityIDAtCoord(mouseX, mouseY);
			this.uiScreen.setUIScene(TOP_BAR_TEXT_SCENE);
			this.uiScreen.render(outputBuffer);
		}
		
		this.uiScreen.setUIScene(CONTENT_BACKGROUND_SCENE);
		this.uiScreen.render(outputBuffer);		
		if(this.uiScreen.getEntityIDAtCoord(mouseX, mouseY) == this.contentBackgroundRect.getID()) {
			this.hoveredSectionID = this.contentBackgroundRect.getID();
		}
		this.uiScreen.setUIScene(CONTENT_SELECTION_SCENE);
		this.uiScreen.render(outputBuffer);
		this.hoveredTopBarID = this.uiScreen.getEntityIDAtCoord(mouseX, mouseY);
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
		if(this.hoveredSectionID == this.topBarBackgroundRect.getID()) {
			
		}
		else if(this.hoveredSectionID == this.contentBackgroundRect.getID()) {
			if(this.hoveredContentID == this.contentBackgroundRect.getID()) {
				for(ListEntry i : this.entryList) {
					i.selected(this.hoveredContentID);
					if(i.isSelected()) {
						if(i == this.selectedListEntry) {
							this.submittedListEntry = this.selectedListEntry;
						}
						this.selectedListEntry = i;
					}
				}
			}
		}
	}

	@Override
	protected void _mouseReleased(int button) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void _mouseScrolled(float wheelOffset, float smoothOffset) {
		if(this.hoveredSectionID == this.contentBackgroundRect.getID()) {
			if(this.isHorizontal) {
				this.setContentBaseXOffset(this.contentBaseXOffset - (int) ((smoothOffset) * entryHeightPx));
			}
			else {
				this.setContentBaseYOffset(this.contentBaseYOffset - (int) ((smoothOffset) * entryHeightPx));
			}
		}
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

class ListEntry {
	
	private int selectionScene, textScene;
	
	private String text;
	
	private UIFilledRectangle entryRect;
	private Text entryText;
	
	private Material defaultMaterial;
	private Material selectedMaterial;
	private Material hoveredMaterial;
	
	private boolean isHovered = false;
	private boolean isSelected = false;
	private boolean isVisible = false;
	
	private boolean doFillWidth = true;
	
	private UIElement baseUIElement;
	
	private int horizontalAlignWidth;
	
	public ListEntry(Material defaultMaterial, Material hoveredMaterial, Material selectedMaterial, String text, UIElement baseUIElement, int selectionScene, int textScene) {
		this.selectionScene = selectionScene;
		this.text = text;
		this.baseUIElement = baseUIElement;
		
		this.defaultMaterial = defaultMaterial;
		this.hoveredMaterial = hoveredMaterial;
		this.selectedMaterial = selectedMaterial;
		
		this.horizontalAlignWidth = GraphicsTools.calculateTextWidth(this.text, ListViewerWindow.entryFont) + ListViewerWindow.entryHorizontalMarginPx * 2;
	}
	
	public Text getTextUI() {
		return this.entryText;
	}
	
	public int getHorizontalAlignWidth() {
		return this.horizontalAlignWidth;
	}
	
	public void doFillWidth(boolean b) {
		this.doFillWidth = b;
		
		if(!this.isVisible) {
			return;
		}
		
		if(this.doFillWidth) {
			this.entryRect.setFillWidth(true);
			this.entryRect.setFillWidthMargin(0);
		}
		else {
			this.entryRect.setFillWidth(false);
			this.entryRect.setWidth(this.horizontalAlignWidth);
		}
	}
	
	public void align(int xOffset, int yOffset) {
		this.isVisible = -ListViewerWindow.entryHeightPx >= yOffset && yOffset <= this.baseUIElement.getHeight();
		this.isVisible &= -this.entryRect.getWidth() >= xOffset && xOffset <= this.baseUIElement.getWidth();
		if(this.isVisible) {
			if(this.entryRect == null) {
				this.entryRect = new UIFilledRectangle(0, 0, 0, this.baseUIElement.getWidth(), ListViewerWindow.entryHeightPx, this.selectionScene);
				this.entryRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
				this.entryRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_BOTTOM);
				this.entryRect.setYOffset(yOffset);
				this.entryRect.setMaterial(this.defaultMaterial);
				this.entryRect.bind(this.baseUIElement);
				
				this.entryText = new Text(ListViewerWindow.entryHorizontalMarginPx, 0, this.text, 12, Color.WHITE, this.textScene);
				this.entryText.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_TOP);
				this.entryText.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
				this.entryText.setDoAntialiasing(false);
				this.entryText.bind(this.baseUIElement);
				
				if(this.doFillWidth) {
					this.entryRect.setFillWidth(true);
					this.entryRect.setFillWidthMargin(0);
				}
			}
		}
		else {
			if(this.entryRect != null) {
				this.entryRect.kill();
				this.entryText.kill();
				this.entryRect = null;
				this.entryText = null;
			}
		}
	}
	
	public void update() {
		if(this.isVisible) {
			if(this.isSelected) {
				this.entryRect.setMaterial(this.selectedMaterial);
			}
			else if(this.isHovered) {
				this.entryRect.setMaterial(this.hoveredMaterial);
			}
			else {
				this.entryRect.setMaterial(this.defaultMaterial);
			}
		}
	}
	
	public void selected(long entityID) {
		this.isSelected = this.entryRect.getID() == entityID;
	}
	
	public void hovered(long entityID) {
		this.isHovered = this.entryRect.getID() == entityID;
	}
	
	public void kill() {
		this.entryRect.kill();
		this.entryText.kill();
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
