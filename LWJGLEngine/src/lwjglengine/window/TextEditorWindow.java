package lwjglengine.window;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_V;
import static org.lwjgl.glfw.GLFW.glfwGetKeyName;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_LINEAR_MIPMAP_LINEAR;

import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.lwjgl.glfw.GLFW;

import lwjglengine.graphics.Framebuffer;
import lwjglengine.graphics.Material;
import lwjglengine.graphics.Texture;
import lwjglengine.graphics.TextureMaterial;
import lwjglengine.input.KeyboardInput;
import lwjglengine.model.FilledRectangle;
import lwjglengine.model.Model;
import lwjglengine.screen.UIScreen;
import lwjglengine.ui.Text;
import lwjglengine.ui.UIElement;
import lwjglengine.ui.UIFilledRectangle;
import lwjglengine.ui.UISection;
import myutils.file.JarUtils;
import myutils.graphics.FontUtils;
import myutils.graphics.GraphicsTools;
import myutils.math.MathUtils;
import myutils.math.Vec2;
import myutils.math.Vec4;
import myutils.misc.Pair;

public class TextEditorWindow extends Window {
	//should be able to open .txt files to edit them like text files. 
	//later, we can add the ability to open any file. 

	//also, maybe a hex or binary editor?

	//is it faster to draw all lines independently, or to draw characters independently, but use instanced rendering
	//it should be fine to just draw all lines independently for now. 
	//but it's easier to figure out cursor alignment and stuff like that with individual character rendering. 
	//and, line wrapping as well. 

	//TODO
	// OPTIMIZATIONS
	// - optimize rendering
	//   - make it so that all characters can be rendered using 1 texture, so that we can just use one render call. 
	//   - in order to do this, we need to be able to have instanced uvs. 
	//   - If we want to do this, we're going to have to overhaul text rendering. 
	// - optimize writing
	//   - when we press enter, we have to realign all the lines, and in turn, all the individual characters, which causes lag spike
	//   - perhaps, if we are only rendering stuff on screen, we should only realign the stuff that is visible. 
	//   - we will call the realign function on all the lines below the cursor, and if a line has visible stuff, it will realign it. 
	// FEATURES
	// - scroll bar on the right side to let user know how much they can scroll. 
	// - highlight the line that the cursor is currently on. 
	//   - can't really do that in the background, since the characters are rendered on top of a solid backing plate. 
	// - hold backspace to continuously delete
	//   - hold any character to continuously press it. 
	//   - this should also include binds, like ctrl + v.
	// - interactions with highlighted chars
	//   - press and drag highlighted chars to move them around. 
	// - load / save using text files. 
	// - ctrl + z, ctrl + y
	// - ctrl + a

	private static final boolean DO_FONT_ANTIALIASING = true;

	private static char[] charList = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '`', '-', '=', '[', ']', '\\', ';', '\'', ',', '.', '/', '~', '_', '+', '{', '}', '|', ':', '"', '<', '>', '?', ' ' };

	private static Font charFont = FontUtils.deriveSize(13, JarUtils.loadFont("/font/lucida_console.ttf"));
	private static Color charColor = Color.WHITE;
	private static int charHorizontalPaddingPx = 0;
	private static int charAscent = GraphicsTools.getFontSampleAscent(charFont);
	private static int charDescent = GraphicsTools.getFontSampleDescent(charFont);
	private static int charMaxDescent = GraphicsTools.getFontMaxDescent(charFont);
	private static int charMaxAscent = GraphicsTools.getFontMaxAscent(charFont);
	private static int charHeight = charMaxAscent + charMaxDescent;

	private UIScreen uiScreen;
	private UISection cosmeticUnderlaySection; //this will just sit statically on the screen. 
	private UISection textEditorSection; //want this seperate to be able to scroll up and down using glViewport
	private UISection cursorSection; //reserved for the cursor, and highlighting. 

	private HashMap<Character, FilledRectangle> charModels;
	private HashMap<Character, Integer> charWidths;

	//TODO implement this
	//currently, we just replace all unknown chars with spaces. 
	private FilledRectangle unknownCharModel; //when we can't find a char, we'll use this model. 

	private static int lineNumberSidebarWidth = 30;
	private UIFilledRectangle lineNumberSidebar;
	private UIFilledRectangle lineContainer;

	private ArrayList<Line> lines;
	private Line selectedLine;

	//just like the name suggests, these should always point towards the first and last visible line. 
	//the selected line is not guaranteed to be visible. The user can always just scroll off the line. 
	private int firstVisibleLine, lastVisibleLine;

	private boolean shouldAlignLines = false;

	private static int cursorWidthPx = 2;
	private UIFilledRectangle cursorRect;

	//this should be done only once per update. 
	private boolean shouldUpdateCosmeticCursorPos = false;

	private int lineHeightSum = 0;
	private int minScrollOffset = -1;
	private int maxScrollOffset = 0;
	private int scrollOffset = -1;

	private boolean isHighlighting = false; //this is true when the user presses the mouse somewhere on a line. 

	//saves the cursor position where the user first pressed
	private int highlightFirstLineIndex = -1; //which line is it?
	private int highlightFirstCharIndex = -1; //index within the line

	//cursor position of the 'moving' highlight end. 
	private int highlightCurLineIndex = -1;
	private int highlightCurCharIndex = -1;

	private static Material highlightMaterial = new Material(new Vec4(48, 197, 255, 120).mul(1.0f / 255.0f));

	//when typing a bracket, will automatically put the appropriate closing bracket right after the cursor position. 
	//if typing a closing bracket, and the next character is a closing bracket, will just move the cursor after the closing bracket. 
	private boolean bracketFinishMode = false;

	//when pressing enter on a line, will copy over all of the prefix whitespace of the old line onto the new line. 
	private boolean prefixWhitespaceIndentingMode = false;

	//when pressing enter directly between two curly brackets, will create two new lines, one normally, and another one with an extra tab.
	private boolean curlyBracketIndentingMode = false;

	public TextEditorWindow(Window parentWindow) {
		super(parentWindow);
		this.init();
	}

	public TextEditorWindow(int x, int y, int width, int height, Window parentWindow) {
		super(x, y, width, height, parentWindow);
		this.init();
	}

	private void init() {
		this.uiScreen = new UIScreen();

		// -- COSMETIC UNDERLAY --
		this.cosmeticUnderlaySection = new UISection(0, 0, this.getWidth(), this.getHeight(), this.uiScreen);
		UIFilledRectangle underlayBackgroundRect = this.cosmeticUnderlaySection.getBackgroundRect();
		underlayBackgroundRect.setFillWidth(true);
		underlayBackgroundRect.setFillHeight(true);
		underlayBackgroundRect.bind(this.rootUIElement);

		UIFilledRectangle cosmeticLineNumberSidebar = new UIFilledRectangle(0, 0, 0, lineNumberSidebarWidth, this.getHeight(), this.cosmeticUnderlaySection.getBackgroundScene());
		cosmeticLineNumberSidebar.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		cosmeticLineNumberSidebar.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		cosmeticLineNumberSidebar.setFillHeight(true);
		cosmeticLineNumberSidebar.setMaterial(this.topBarDefaultMaterial);
		cosmeticLineNumberSidebar.bind(underlayBackgroundRect);

		Color lineBackgroundColor = new Color(40, 40, 40);
		UIFilledRectangle lineContainerBackground = new UIFilledRectangle(0, 0, 0, this.getWidth() - lineNumberSidebarWidth, this.getHeight(), this.cosmeticUnderlaySection.getBackgroundScene());
		lineContainerBackground.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_TOP);
		lineContainerBackground.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_TOP);
		lineContainerBackground.setFillHeight(true);
		lineContainerBackground.setFillWidth(true);
		lineContainerBackground.setFillWidthMargin(lineNumberSidebarWidth / 2);
		lineContainerBackground.setMaterial(new Material(lineBackgroundColor));
		lineContainerBackground.bind(underlayBackgroundRect);

		// -- TEXT EDITOR --
		this.textEditorSection = new UISection(0, 0, this.getWidth(), this.getHeight(), this.uiScreen);
		UIFilledRectangle textEditorBackgroundRect = this.textEditorSection.getBackgroundRect();
		textEditorBackgroundRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		textEditorBackgroundRect.setContentAlignmentStyle(UIElement.FROM_LEFT, UIElement.ALIGN_TOP);
		textEditorBackgroundRect.setFillWidth(true);
		textEditorBackgroundRect.bind(this.rootUIElement);

		this.lineContainer = new UIFilledRectangle(0, 0, 0, this.getWidth() - lineNumberSidebarWidth, this.getHeight(), this.textEditorSection.getBackgroundScene());
		this.lineContainer.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_TOP);
		this.lineContainer.setContentAlignmentStyle(UIElement.FROM_RIGHT, UIElement.ALIGN_TOP);
		this.lineContainer.setMaterial(Material.transparent());
		this.lineContainer.bind(textEditorBackgroundRect);

		this.lineNumberSidebar = new UIFilledRectangle(0, 0, 0, lineNumberSidebarWidth, this.getHeight(), this.textEditorSection.getBackgroundScene());
		this.lineNumberSidebar.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.lineNumberSidebar.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		this.lineNumberSidebar.setMaterial(Material.transparent());
		this.lineNumberSidebar.bind(textEditorBackgroundRect);

		// -- CURSOR --
		this.cursorSection = new UISection(0, 0, this.getWidth(), this.getHeight(), this.uiScreen);
		UIFilledRectangle cursorBackgroundRect = this.cursorSection.getBackgroundRect();
		cursorBackgroundRect.setFillWidth(true);
		cursorBackgroundRect.setFillHeight(true);
		cursorBackgroundRect.bind(this.lineContainer);

		this.cursorRect = new UIFilledRectangle(0, 0, 0, cursorWidthPx, charHeight, this.cursorSection.getTextScene());
		this.cursorRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.cursorRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		this.cursorRect.setMaterial(new Material(new Vec4(1, 1, 1, 1f)));
		this.cursorRect.setEasingStyle(UIElement.EASE_OUT_QUAD);
		this.cursorRect.setEasingDurationMillis(50);
		this.cursorRect.bind(cursorBackgroundRect);

		//create all the char models
		this.charModels = new HashMap<>();
		this.charWidths = new HashMap<>();
		for (int i = 0; i < charList.length; i++) {
			String c = charList[i] + "";
			int charWidth = GraphicsTools.calculateTextWidth(c, charFont);

			BufferedImage img = GraphicsTools.generateTextImage(c, charFont, charColor, charWidth, lineBackgroundColor, DO_FONT_ANTIALIASING);
			Texture charTexture = new Texture(img, 0, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR, 1);
			TextureMaterial charTextureMaterial = new TextureMaterial(charTexture);

			FilledRectangle charRect = new FilledRectangle();
			charRect.setTextureMaterial(charTextureMaterial);

			this.charModels.put(charList[i], charRect);
			this.charWidths.put(charList[i], charWidth);
		}

		//tab character
		{
			//4 spaces
			String tab = "    ";
			int width = GraphicsTools.calculateTextWidth(tab, charFont);

			BufferedImage img = GraphicsTools.generateTextImage(tab, charFont, charColor, width, lineBackgroundColor, DO_FONT_ANTIALIASING);
			Texture charTexture = new Texture(img, 0, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR, 1);
			TextureMaterial charTextureMaterial = new TextureMaterial(charTexture);

			FilledRectangle charRect = new FilledRectangle();
			charRect.setTextureMaterial(charTextureMaterial);

			this.charModels.put((char) 9, charRect);
			this.charWidths.put((char) 9, width);
		}

		this.lines = new ArrayList<>();
		this.addLine(0);
		this.selectLine(0);

		this.firstVisibleLine = 0;
		this.lastVisibleLine = 0;

		this._resize();
	}

	/**
	 * Returns whatever text is currently stored inside the editor. 
	 * @return
	 */
	public String getText() {
		StringBuilder res = new StringBuilder();
		for (int i = 0; i < this.lines.size(); i++) {
			res.append(this.lines.get(i).getText());
			if (i != this.lines.size() - 1) {
				res.append("\n");
			}
		}
		return res.toString();
	}

	public int getScrollOffset() {
		return this.scrollOffset;
	}

	public void setBracketFinishMode(boolean b) {
		this.bracketFinishMode = b;
	}

	public void setPrefixWhitespaceIndentingMode(boolean b) {
		this.prefixWhitespaceIndentingMode = b;
	}

	public void setCurlyBracketIndentingMode(boolean b) {
		this.curlyBracketIndentingMode = b;
	}

	@Override
	protected int _getCursorShape() {
		return GLFW.GLFW_IBEAM_CURSOR;
	}

	private void addLine(int index) {
		if (index < 0 || index > this.lines.size()) {
			System.err.println("TextEditorWindow: Tried to add element at index " + index + " when line amount is " + this.lines.size());
			return;
		}
		this.lines.add(index, new Line());
		this.alignLines(index);
	}

	private void removeLine(int index) {
		if (index < 0 || index >= this.lines.size()) {
			System.err.println("TextEditorWindow: Tried to remove element at index " + index + " when line amount is " + this.lines.size());
			return;
		}
		this.lines.get(index).kill();
		this.lines.remove(index);
		this.alignLines(index);
	}

	public void appendTextAtCursor(String text) {
		if (this.selectedLine == null) {
			return;
		}
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '\n') {
				this.selectedLine.pressEnterAtCursor();
			}
			else {
				this.selectedLine.addCharacterAtCursor(c);
			}
		}
	}

	private void updateLineVisibility() {
		//linear search to find the new visible ranges. 
		int n_firstVisible = -1;
		int n_lastVisible = -1;
		{
			int yOffset = 0;
			int screenTop = this.scrollOffset;
			int screenBottom = this.scrollOffset + this.getHeight();
			for (int i = 0; i < this.lines.size(); i++) {
				Line l = this.lines.get(i);
				if (yOffset <= screenBottom) {
					n_lastVisible = i;
				}
				if (n_firstVisible == -1 && yOffset + l.height >= screenTop) {
					n_firstVisible = i;
				}
				yOffset += l.height;
			}
		}

		//update the visibility of the lines within the old range
		for (int i = this.firstVisibleLine; i <= this.lastVisibleLine; i++) {
			if (i >= this.lines.size()) {
				continue;
			}
			if (n_firstVisible <= i && i <= n_lastVisible) {
				continue;
			}
			this.lines.get(i).setIsVisible(false);
		}

		//update visibility of lines in new range
		for (int i = n_firstVisible; i <= n_lastVisible; i++) {
			this.lines.get(i).setIsVisible(true);
		}

		this.firstVisibleLine = n_firstVisible;
		this.lastVisibleLine = n_lastVisible;
	}

	private void alignLines(int startInd) {
		int yOffset = 0;
		for (int i = 0; i < this.lines.size(); i++) {
			Line l = this.lines.get(i);
			if (i >= startInd) {
				l.alignLine(i, yOffset);
			}

			yOffset += l.height;
		}

		this.lineHeightSum = yOffset;
		this.maxScrollOffset = Math.max(this.lineHeightSum - this.lines.get(this.lines.size() - 1).height, 0);
		this.scrollOffset = MathUtils.clamp(this.minScrollOffset, this.maxScrollOffset, this.scrollOffset);

		if (this.selectedLine != null) {
			this.shouldUpdateCosmeticCursorPos = true;
		}

		//update the visibility of lines. 
		this.updateLineVisibility();
	}

	private void alignAllLines() {
		this.alignLines(0);
	}

	private void selectLine(int index) {
		if (index < 0 || index >= this.lines.size()) {
			System.err.println("TextEditorWindow: Tried to select line at out of bounds index");
			return;
		}

		if (this.selectedLine != null) {
			this.deselectLine();
		}

		this.selectedLine = this.lines.get(index);
		this.selectedLine.select();
	}

	private void deselectLine() {
		if (this.selectedLine == null) {
			System.err.println("TextEditorWindow: Tried to deselect line when no line is selected");
			return;
		}

		this.selectedLine.deselect();
		this.selectedLine = null;
	}

	private UIElement createCharUIElement(char c) {
		if (this.charModels.get(c) == null) {
			System.err.println("TextEditorWindow: No model for character : " + c + " defaulting to space");
			c = ' ';
		}

		UIFilledRectangle charRect = new UIFilledRectangle(0, 0, 0, charWidths.get(c), charHeight, charModels.get(c), textEditorSection.getTextScene());
		charRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		charRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		charRect.setKillCustomBoundingRect(false);
		return charRect;
	}

	private int getCharWidth(char c) {
		if (this.charWidths.get(c) == null) {
			return this.charWidths.get(' ');
		}
		return this.charWidths.get(c);
	}

	@Override
	protected void _kill() {
		this.uiScreen.kill();
		this.cosmeticUnderlaySection.kill();
		this.textEditorSection.kill();
		this.cursorSection.kill();
	}

	@Override
	protected void _resize() {
		this.uiScreen.setScreenDimensions(this.getWidth(), this.getHeight());

		this.lineContainer.setWidth(this.getWidth() - lineNumberSidebarWidth);

		this.alignAllLines();
	}

	@Override
	public String getDefaultTitle() {
		return "Text Editor";
	}

	@Override
	protected void _update() {
		this.cosmeticUnderlaySection.update();
		this.textEditorSection.update();
		this.cursorSection.update();

		if (this.shouldAlignLines) {
			this.alignAllLines();
			this.shouldAlignLines = false;
		}

		if (this.isHighlighting) {
			//find the line that is the closest to current mouse position. 
			Vec2 mouseOffset = this.getWindowMousePos();
			mouseOffset.y -= this.scrollOffset;

			Line closestLine = null;
			for (int i = 0; i < this.lines.size(); i++) {
				Line l = this.lines.get(i);
				float minY = l.textBackgroundRect.getGlobalAlignedY();
				float maxY = minY + l.textBackgroundRect.getHeight();

				if (minY <= mouseOffset.y && mouseOffset.y <= maxY) {
					closestLine = l;
					break;
				}
				if (i == 0 && mouseOffset.y > maxY) {
					closestLine = l;
					break;
				}
				if (i == this.lines.size() - 1 && mouseOffset.y < minY) {
					closestLine = l;
					break;
				}
			}

			//clamp mouse position to inside of the line container
			//only need to clamp along y axis
			float minY = closestLine.textBackgroundRect.getGlobalAlignedY();
			float maxY = minY + closestLine.textBackgroundRect.getHeight();
			mouseOffset.y = MathUtils.clamp(minY, maxY, mouseOffset.y);

			//find best cursor pos
			int charIndex = closestLine.findBestCursorPos(mouseOffset);
			int lineIndex = closestLine.lineIndex;

			//update highlight status of characters. 
			//migrate the current char and line index to the new indexes. 
			if (lineIndex > this.highlightCurLineIndex || (lineIndex == this.highlightCurLineIndex && charIndex > this.highlightCurCharIndex)) {
				//increasing
				while (lineIndex != this.highlightCurLineIndex || charIndex != this.highlightCurCharIndex) {
					Line l = this.lines.get(this.highlightCurLineIndex);
					if (l.chars.size() != this.highlightCurCharIndex) {
						if (l.isCharHighlighted(this.highlightCurCharIndex)) {
							l.unhighlightChar(this.highlightCurCharIndex);
						}
						else {
							l.highlightChar(this.highlightCurCharIndex);
						}
					}
					this.highlightCurCharIndex++;
					if (this.highlightCurCharIndex == l.chars.size() + 1) {
						this.highlightCurCharIndex = 0;
						this.highlightCurLineIndex++;
					}
				}
			}
			else {
				//decreasing
				while (lineIndex != this.highlightCurLineIndex || charIndex != this.highlightCurCharIndex) {
					Line l = this.lines.get(this.highlightCurLineIndex);
					this.highlightCurCharIndex--;
					if (this.highlightCurCharIndex == -1) {
						l = this.lines.get(this.highlightCurLineIndex - 1);
						this.highlightCurCharIndex = l.chars.size();
						this.highlightCurLineIndex--;
					}

					if (this.highlightCurCharIndex != l.chars.size()) {
						if (l.isCharHighlighted(this.highlightCurCharIndex)) {
							l.unhighlightChar(this.highlightCurCharIndex);
						}
						else {
							l.highlightChar(this.highlightCurCharIndex);
						}
					}
				}
			}

			//set current cursor pos
			this.selectLine(this.highlightCurLineIndex);
			this.selectedLine.setCursorPos(this.highlightCurCharIndex);
		}

		if (this.shouldUpdateCosmeticCursorPos) {
			this.selectedLine.updateCosmeticCursorPos();
			this.shouldUpdateCosmeticCursorPos = false;
		}
	}

	@Override
	protected void renderContent(Framebuffer outputBuffer) {
		this.cosmeticUnderlaySection.render(outputBuffer, this.getWindowMousePos());

		this.uiScreen.setViewportOffset(new Vec2(0, -this.scrollOffset));
		this.textEditorSection.render(outputBuffer, this.getWindowMousePos());
		this.cursorSection.render(outputBuffer, this.getWindowMousePos());
		this.uiScreen.setViewportOffset(new Vec2(0, 0));
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
		//clear all highlighting.
		this.resetHighlighting();

		//find which line was pressed
		long lineID = this.textEditorSection.getHoveredEntityID();
		int pressedLine = -1;
		for (int i = 0; i < this.lines.size(); i++) {
			if (this.lines.get(i).textBackgroundRect.getID() == lineID) {
				pressedLine = i;
				break;
			}
		}
		if (pressedLine != -1) {
			this.selectLine(pressedLine);

			Vec2 mouseOffset = this.getWindowMousePos();
			mouseOffset.y -= this.scrollOffset;
			this.selectedLine.setCursorPos(mouseOffset);

			this.isHighlighting = true;
			for (int i = 0; i < this.lines.size(); i++) {
				if (this.selectedLine == this.lines.get(i)) {
					this.highlightFirstLineIndex = i;
					break;
				}
			}
			this.highlightFirstCharIndex = this.selectedLine.cursorPos;

			this.highlightCurLineIndex = this.highlightFirstLineIndex;
			this.highlightCurCharIndex = this.highlightFirstCharIndex;
		}
	}

	private void resetHighlighting() {
		if (!this.areCharactersHighlighted()) {
			return;
		}

		//remove all highlighting
		int f_line = Math.min(this.highlightFirstLineIndex, this.highlightCurLineIndex);
		int l_line = Math.max(this.highlightFirstLineIndex, this.highlightCurLineIndex);
		for (int i = f_line; i <= l_line; i++) {
			this.lines.get(i).unhighlightAllChars();
		}

		//reset highlighting variables. 
		this.highlightFirstCharIndex = -1;
		this.highlightFirstLineIndex = -1;
		this.highlightCurCharIndex = -1;
		this.highlightCurLineIndex = -1;

		this.isHighlighting = false;
	}

	private boolean areCharactersHighlighted() {
		if (this.highlightFirstCharIndex == this.highlightCurCharIndex && this.highlightFirstLineIndex == this.highlightCurLineIndex) {
			return false;
		}
		return true;
	}

	private void removeHighlightedCharacters() {
		if (!this.areCharactersHighlighted()) {
			return;
		}

		//record where to remove. 
		int firstChar = this.highlightFirstCharIndex;
		int firstLine = this.highlightFirstLineIndex;
		int lastChar = this.highlightCurCharIndex;
		int lastLine = this.highlightCurLineIndex;

		//make sure they are in correct order
		if (firstLine > lastLine || (firstLine == lastLine && firstChar > lastChar)) {
			{
				int tmp = firstLine;
				firstLine = lastLine;
				lastLine = tmp;
			}
			{
				int tmp = firstChar;
				firstChar = lastChar;
				lastChar = tmp;
			}
		}

		//erase all the highlighting. 
		this.resetHighlighting();

		//remove characters from the end lines. 
		if (firstLine == lastLine) {
			this.lines.get(firstLine).removeCharactersBetweenIndex(firstChar, lastChar);
		}
		else {
			this.lines.get(firstLine).removeCharactersAfterIndex(firstChar);
			this.lines.get(lastLine).removeCharactersBeforeIndex(lastChar);
		}

		//if we deleted from multiple lines, append the last line onto the first one, and delete the last one. 
		if (firstLine != lastLine) {
			this.lines.get(firstLine).appendCharacters(this.lines.get(lastLine).chars);
			this.removeLine(lastLine);
		}

		//remove all lines we should remove
		for (int i = lastLine - 1; i > firstLine; i--) {
			this.removeLine(i);
		}

		//set the first line as selected
		this.selectLine(firstLine);
		this.lines.get(firstLine).setCursorPos(firstChar);
	}

	@Override
	protected void _mouseReleased(int button) {
		this.isHighlighting = false;
	}

	private void setScrollOffset(int offset) {
		this.scrollOffset = MathUtils.clamp(this.minScrollOffset, this.maxScrollOffset, offset);

		//update the visibility of lines
		this.updateLineVisibility();
	}

	@Override
	protected void _mouseScrolled(float wheelOffset, float smoothOffset) {
		int scrollAmt = (int) (smoothOffset * charHeight);
		this.setScrollOffset(this.scrollOffset - scrollAmt);
	}

	@Override
	protected void _keyPressed(int key) {
		//this shouldn't ever happen hmm
		if (this.selectedLine == null) {
			System.err.println("TextEditorWindow : selectedLine shouldn't equal null");
			return;
		}

		if (key == GLFW.GLFW_KEY_LEFT_CONTROL || key == GLFW.GLFW_KEY_RIGHT_CONTROL) {
			return;
		}
		if (key == GLFW.GLFW_KEY_LEFT_SHIFT || key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
			return;
		}

		// ctrl + c or ctrl + x
		if ((KeyboardInput.isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL) || KeyboardInput.isKeyPressed(GLFW.GLFW_KEY_RIGHT_CONTROL)) && (key == GLFW.GLFW_KEY_C || key == GLFW.GLFW_KEY_X)) {
			if (this.areCharactersHighlighted()) {
				ArrayList<Character> chars = new ArrayList<>();
				int lineIndex = this.highlightFirstLineIndex;
				int charIndex = this.highlightFirstCharIndex;

				int lastLineIndex = this.highlightCurLineIndex;
				int lastCharIndex = this.highlightCurCharIndex;
				if (this.highlightCurLineIndex < lineIndex || (this.highlightCurLineIndex == lineIndex && this.highlightCurCharIndex < charIndex)) {
					lineIndex = this.highlightCurLineIndex;
					charIndex = this.highlightCurCharIndex;

					lastLineIndex = this.highlightFirstLineIndex;
					lastCharIndex = this.highlightFirstCharIndex;
				}

				while (lineIndex != lastLineIndex || charIndex != lastCharIndex) {
					Line l = this.lines.get(lineIndex);
					if (charIndex == l.chars.size()) {
						charIndex = 0;
						lineIndex++;
						chars.add('\n');
						continue;
					}

					chars.add(l.chars.get(charIndex));
					charIndex++;
				}

				char[] charArr = new char[chars.size()];
				for (int i = 0; i < chars.size(); i++) {
					charArr[i] = chars.get(i);
				}
				String result = new String(charArr);

				StringSelection stringSelection = new StringSelection(result);
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(stringSelection, null);
			}

			if (key == GLFW.GLFW_KEY_X) {
				this.removeHighlightedCharacters();
			}
			return;
		}

		if (this.areCharactersHighlighted()) {
			//remove the highlighted characters
			this.removeHighlightedCharacters();

			if (key == GLFW.GLFW_KEY_BACKSPACE) {
				return;
			}
		}

		// ctrl + v
		if ((KeyboardInput.isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL) || KeyboardInput.isKeyPressed(GLFW.GLFW_KEY_RIGHT_CONTROL)) && key == GLFW.GLFW_KEY_V) {
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			String result = "";
			try {
				result = (String) clipboard.getData(DataFlavor.stringFlavor);
			}
			catch (UnsupportedFlavorException e) {
				e.printStackTrace();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			this.appendTextAtCursor(result);
			return;
		}
		else {
			this.selectedLine.keyPressed(key);
		}
	}

	@Override
	protected void _keyReleased(int key) {

	}

	class Line {

		private ArrayList<Character> chars;
		private ArrayList<Pair<Integer, Integer>> charOffsets;
		private ArrayList<Integer> charSubline; //if the window is narrow enough, each line can be wrapped into sublines. 
		private int maxSubline = 0;
		private ArrayList<Boolean> charHighlighted;

		private ArrayList<UIFilledRectangle> charRects;
		private ArrayList<UIFilledRectangle> highlightRects;

		private int height;

		private int lineIndex = -1;

		private Text lineIndexText;

		private UIFilledRectangle textBackgroundRect;

		private boolean isSelected = false;
		private int cursorPos = -1;

		private boolean isVisible = false;

		public Line() {
			this.chars = new ArrayList<>();
			this.charOffsets = new ArrayList<>();
			this.charSubline = new ArrayList<>();
			this.charHighlighted = new ArrayList<>();

			this.charRects = null;
			this.highlightRects = null;

			this.height = charHeight;

			this.textBackgroundRect = new UIFilledRectangle(2, 0, 0, lineContainer.getWidth(), this.height, textEditorSection.getSelectionScene());
			this.textBackgroundRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
			this.textBackgroundRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
			this.textBackgroundRect.setMaterial(Material.transparent());
			this.textBackgroundRect.setFillWidth(true);
			this.textBackgroundRect.setFillWidthMargin(2);
			this.textBackgroundRect.bind(lineContainer);
		}

		public String getText() {
			char[] arr = new char[this.chars.size()];
			for (int i = 0; i < this.chars.size(); i++) {
				arr[i] = this.chars.get(i);
			}
			return new String(arr);
		}

		public void kill() {
			this.textBackgroundRect.kill();
			this.lineIndexText.kill();
		}

		public void appendString(String s) {
			for (int i = 0; i < s.length(); i++) {
				char c = s.charAt(i);
				this.addCharacter(this.chars.size(), c);
			}
			this.alignCharacters();
		}

		public void appendCharacters(ArrayList<Character> clist) {
			for (char c : clist) {
				this.addCharacter(this.chars.size(), c);
			}
			this.alignCharacters();
		}

		public void setIsVisible(boolean b) {
			if (this.isVisible == b) {
				return;
			}

			this.isVisible = b;

			if (this.isVisible) {
				//create all the char rects
				this.charRects = new ArrayList<>();
				this.highlightRects = new ArrayList<>();

				for (int i = 0; i < this.chars.size(); i++) {
					UIFilledRectangle charRect = (UIFilledRectangle) createCharUIElement(this.chars.get(i));
					charRect.setFrameAlignmentOffset(this.charOffsets.get(i).first, this.charOffsets.get(i).second);
					charRect.bind(this.textBackgroundRect);
					this.charRects.add(charRect);

					if (this.isCharHighlighted(i)) {
						UIFilledRectangle highlightRect = new UIFilledRectangle(0, 0, 0, charRect.getWidth(), charRect.getHeight(), cursorSection.getBackgroundScene());
						highlightRect.setFillWidth(true);
						highlightRect.setFillHeight(true);
						highlightRect.setMaterial(highlightMaterial);
						highlightRect.bind(charRect);
						this.highlightRects.add(highlightRect);
					}
					else {
						this.highlightRects.add(null);
					}
				}
			}
			else {
				//delete all the char rects. 
				for (int i = 0; i < this.chars.size(); i++) {
					this.charRects.get(i).kill();
				}
				this.charRects.clear();

				this.charRects = null;
				this.highlightRects = null;
			}
		}

		public boolean isCharHighlighted(int index) {
			return this.charHighlighted.get(index);
		}

		public void highlightChar(int index) {
			if (this.charHighlighted.get(index) == true) {
				return;
			}
			this.charHighlighted.set(index, true);

			//check if line is visible
			if (this.isVisible) {
				//add highlighting rect
				UIFilledRectangle charRect = this.charRects.get(index);
				UIFilledRectangle highlightRect = new UIFilledRectangle(0, 0, 0, charRect.getWidth(), charRect.getHeight(), cursorSection.getBackgroundScene());
				highlightRect.setFillWidth(true);
				highlightRect.setFillHeight(true);
				highlightRect.setMaterial(highlightMaterial);
				highlightRect.bind(charRect);

				this.highlightRects.set(index, highlightRect);
			}
		}

		public void unhighlightChar(int index) {
			if (this.charHighlighted.get(index) == false) {
				return;
			}
			this.charHighlighted.set(index, false);

			//check if line is visible. 
			if (this.isVisible) {
				//remove highlighting rect
				this.highlightRects.get(index).kill();
				this.highlightRects.set(index, null);
			}
		}

		public void unhighlightAllChars() {
			for (int i = 0; i < this.chars.size(); i++) {
				unhighlightChar(i);
			}
		}

		private void _addCharacter(int index, char c) {
			this.chars.add(index, c);
			this.charOffsets.add(new Pair<>(0, 0));
			this.charSubline.add(index, 0);
			this.charHighlighted.add(false);

			//check if line is visible
			if (this.isVisible) {
				UIFilledRectangle charRect = (UIFilledRectangle) createCharUIElement(c);
				charRect.bind(this.textBackgroundRect);
				this.charRects.add(index, charRect);
				this.highlightRects.add(index, null);
			}
		}

		private void _removeCharacter(int index) {
			this.chars.remove(index);
			this.charOffsets.remove(index);
			this.charSubline.remove(index);
			this.charHighlighted.remove(index);

			//check if line is visible
			if (this.isVisible) {
				this.charRects.get(index).kill();
				this.charRects.remove(index);
				this.highlightRects.remove(index);
			}
		}

		private void addCharacter(int index, char c) {
			if (index < 0 || index > chars.size()) {
				System.err.println("TextEditorWindow{Line} : Tried to add character at index out of bounds");
				return;
			}

			this._addCharacter(index, c);
			this.alignCharacters();

			if (this.isSelected) {
				//update cursor pos
				if (index >= this.cursorPos) {
					this.setCursorPos(this.cursorPos + 1);
				}
			}
		}

		private void removeCharacter(int index) {
			if (index < 0 || index >= chars.size()) {
				System.err.println("TextEditorWindow{Line} : Tried to remove character at index out of bounds");
				return;
			}

			this._removeCharacter(index);
			this.alignCharacters();

			if (this.isSelected) {
				//update cursor pos
				if (index < this.cursorPos) {
					this.setCursorPos(this.cursorPos - 1);
				}
			}
		}

		//if the cursor was positioned at the given index, all characters before it would be removed. 
		public void removeCharactersBeforeIndex(int index) {
			for (int i = index - 1; i >= 0; i--) {
				this.removeCharacter(i);
			}
			this.alignCharacters();

			if (this.isSelected) {
				//update cursor pos
				if (index < this.cursorPos) {
					this.setCursorPos(this.cursorPos - index);
				}
				else {
					this.setCursorPos(0);
				}
			}
		}

		//if the cursor was positioned at the given index, all characters after it would be removed. 
		public void removeCharactersAfterIndex(int index) {
			while (this.chars.size() > index) {
				int n_ind = this.chars.size() - 1;
				this.removeCharacter(n_ind);
			}
			this.alignCharacters();

			if (this.isSelected) {
				//update cursor pos
				if (index > this.cursorPos) {
					this.setCursorPos(index);
				}
			}
		}

		public void removeCharactersBetweenIndex(int f_index, int l_index) {
			for (int i = l_index - 1; i >= f_index; i--) {
				this.removeCharacter(i);
			}
			this.alignCharacters();

			if (this.isSelected) {
				//update cursor pos
				if (l_index < this.cursorPos) {
					this.setCursorPos(this.cursorPos - (l_index - f_index + 1));
				}
				else if (f_index < this.cursorPos && this.cursorPos < l_index) {
					this.setCursorPos(f_index);
				}
			}
		}

		public void removeAllCharacters() {
			this.removeCharactersAfterIndex(0);
		}

		//adds the character at the cursor location, and then moves the cursor up 1 character. 
		public void addCharacterAtCursor(char c) {
			this.addCharacter(this.cursorPos, c);
		}

		//makes a new line directly after this one, and puts everything to the right of the cursor on that new line. 
		public void pressEnterAtCursor() {
			//create a new line, and put everything that is to the right of the cursor on the new line
			ArrayList<Character> cursorRight = new ArrayList<>();
			for (int i = this.cursorPos; i < this.chars.size(); i++) {
				cursorRight.add(this.chars.get(i));
			}

			while (this.chars.size() > this.cursorPos) {
				this.removeCharacter(this.chars.size() - 1);
			}

			addLine(this.lineIndex + 1);
			Line newLine = lines.get(this.lineIndex + 1);
			newLine.appendCharacters(cursorRight);

			//make the selected line the next line
			selectLine(this.lineIndex + 1);
			selectedLine.setCursorPos(0);

			if (prefixWhitespaceIndentingMode) {
				for (int i = 0; i < this.chars.size(); i++) {
					if (!Character.isWhitespace(this.chars.get(i))) {
						break;
					}
					selectedLine.addCharacterAtCursor(this.chars.get(i));
				}
			}
		}

		//removes the character before the cursor location, if there is one. 
		//otherwise, tries to merge this line with the previous one. 
		public void pressBackspaceAtCursor() {
			if (this.cursorPos != 0) {
				//normal backspace action
				this.removeCharacter(this.cursorPos - 1);
			}
			else if (this.lineIndex != 0) {
				//make the selected line the previous line
				selectLine(this.lineIndex - 1);

				//delete this line, and we'll need to put the contents of this line onto the previous line
				Line prevLine = lines.get(this.lineIndex - 1);
				int newCursorPos = prevLine.chars.size();
				prevLine.appendCharacters(this.chars);

				removeLine(this.lineIndex);
				selectedLine.setCursorPos(newCursorPos);
			}
		}

		//find an alignment for all the characters, and update the height if needed. 
		private void alignCharacters() {
			int xOffset = 0;
			int yOffset = 0;
			this.maxSubline = 0;

			for (int i = 0; i < this.chars.size(); i++) {
				int curCharWidth = getCharWidth(this.chars.get(i));

				if (curCharWidth + xOffset > this.textBackgroundRect.getWidth()) {
					yOffset += charHeight;
					xOffset = 0;
					this.maxSubline++;
				}

				this.charOffsets.get(i).first = xOffset;
				this.charOffsets.get(i).second = yOffset;
				this.charSubline.set(i, this.maxSubline);

				xOffset += curCharWidth;
			}

			//update char rect alignments
			if (this.isVisible) {
				for (int i = 0; i < this.chars.size(); i++) {
					int x = this.charOffsets.get(i).first;
					int y = this.charOffsets.get(i).second;
					this.charRects.get(i).setFrameAlignmentOffset(x, y);
				}
			}

			if (this.height != yOffset + charHeight) {
				shouldAlignLines = true;
			}
			this.height = yOffset + charHeight;
		}

		public void alignLine(int lineIndex, int yOffset) {
			if (this.lineIndexText == null) {
				this.lineIndexText = new Text(2, 0, " ", 12, Color.WHITE, textEditorSection.getTextScene());
				this.lineIndexText.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_TOP);
				this.lineIndexText.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_CENTER);
				this.lineIndexText.setDoAntialiasing(false);
				this.lineIndexText.bind(lineNumberSidebar);
			}

			this.alignCharacters();

			this.lineIndex = lineIndex;
			this.lineIndexText.setText((lineIndex + 1) + "");
			this.lineIndexText.setWidth(this.lineIndexText.getTextWidth());

			this.textBackgroundRect.setHeight(this.height);

			this.textBackgroundRect.setYOffset(yOffset);
			this.lineIndexText.setYOffset(yOffset + charHeight / 2 - 2);
		}

		public void updateCosmeticCursorPos() {
			if (!this.isSelected) {
				System.err.println("TextEditorWindow{Line} : Tried to set cosmetic cursor pos of unselected line");
				return;
			}

			if (this.cursorPos == -1) {
				this.cursorPos = 0;
			}

			//find position of cosmetic cursor
			int xOffset = (int) this.textBackgroundRect.getXOffset();
			int yOffset = (int) this.textBackgroundRect.getYOffset();

			if (this.cursorPos != 0) {
				int charX = this.charOffsets.get(this.cursorPos - 1).first;
				int charY = this.charOffsets.get(this.cursorPos - 1).second;
				int charWidth = getCharWidth(this.chars.get(this.cursorPos - 1));
				xOffset += charX + charWidth;
				yOffset += charY;
			}

			cursorRect.easeFrameAlignmentOffset(xOffset, yOffset);

			//if cursor is not visible, then set the scroll offset so that it is
			int n_scrollOffset = scrollOffset;

			int screenTopY = scrollOffset;
			int screenBottomY = scrollOffset + getHeight();
			int cursorTopY = yOffset;
			int cursorBottomY = yOffset + charHeight;

			if (cursorTopY < screenTopY) {
				n_scrollOffset += cursorTopY - screenTopY;
			}
			else if (cursorBottomY > screenBottomY) {
				n_scrollOffset += cursorBottomY - screenBottomY;
			}

			setScrollOffset(n_scrollOffset);
		}

		//positive for right, negative for left. Clamped to the ends of the line
		public void shiftCursor(int amount) {
			int n_cursorPos = MathUtils.clamp(0, this.chars.size(), this.cursorPos + amount);
			this.setCursorPos(n_cursorPos);
		}

		public void setCursorPos(int index) {
			if (!this.isSelected) {
				System.err.println("TextEditorWindow{Line}: Can't set cursor pos when not selected");
				return;
			}
			if (index < 0 || index > this.chars.size()) {
				System.err.println("TextEditorWindow{Line}: Tried to set cursor pos out of bounds");
				return;
			}

			this.cursorPos = index;

			shouldUpdateCosmeticCursorPos = true;
		}

		//find the closest cursor location to the given offset point. 
		//input mouse offset has to be relative to textBackgroundRect
		public void setCursorPos(Vec2 mouseOffset) {
			this.setCursorPos(this.findBestCursorPos(mouseOffset));
		}

		public int findBestCursorPos(Vec2 relLineContainerOffset) {
			int bestCursorPos = 0;
			float minDist = Vec2.distanceSq(relLineContainerOffset, new Vec2(this.textBackgroundRect.getGlobalAlignedX(), this.textBackgroundRect.getGlobalAlignedY() + this.textBackgroundRect.getHeight() - charHeight / 2));
			for (int i = 0; i < this.chars.size(); i++) {
				float globalAlignedX = this.textBackgroundRect.getGlobalAlignedX();
				float globalAlignedY = this.textBackgroundRect.getGlobalAlignedY();

				//char offsets is the offset from the top left, so convert to bottom left. 
				globalAlignedX += this.charOffsets.get(i).first + getCharWidth(this.chars.get(i));
				globalAlignedY += this.textBackgroundRect.getHeight() - (this.charOffsets.get(i).second + charHeight) + charHeight / 2;

				Vec2 nextCharPos = new Vec2(globalAlignedX, globalAlignedY);
				if (Math.abs(nextCharPos.y - relLineContainerOffset.y) > charHeight / 2) {
					continue;
				}
				float nextDist = Vec2.distanceSq(relLineContainerOffset, nextCharPos);
				if (nextDist < minDist) {
					bestCursorPos = i + 1;
					minDist = nextDist;
				}
			}
			return bestCursorPos;
		}

		public Vec2 getGlobalCursorOffset() {
			Vec2 ret = new Vec2(0);
			if (this.cursorPos == 0) {
				ret.x = this.textBackgroundRect.getGlobalAlignedX();
				ret.y = this.textBackgroundRect.getGlobalAlignedY() + this.textBackgroundRect.getHeight() - charHeight / 2;
			}
			else {
				float globalAlignedX = this.textBackgroundRect.getGlobalAlignedX();
				float globalAlignedY = this.textBackgroundRect.getGlobalAlignedY();

				//char offsets is the offset from the top left, so convert to bottom left. 
				globalAlignedX += this.charOffsets.get(this.cursorPos - 1).first + getCharWidth(this.chars.get(this.cursorPos - 1));
				globalAlignedY += this.textBackgroundRect.getHeight() - (this.charOffsets.get(this.cursorPos - 1).second + charHeight) + charHeight / 2;

				ret.x = globalAlignedX;
				ret.y = globalAlignedY;
			}
			return ret;
		}

		public void select() {
			this.isSelected = true;
			this.setCursorPos(0);
		}

		public void deselect() {
			this.isSelected = false;
			this.cursorPos = -1;
		}

		public void mousePressed() {

		}

		public void keyPressed(int key) {
			switch (key) {
			case GLFW.GLFW_KEY_LEFT: {
				this.setCursorPos(Math.max(0, this.cursorPos - 1));
				break;
			}

			case GLFW.GLFW_KEY_RIGHT: {
				this.setCursorPos(Math.min(this.chars.size(), this.cursorPos + 1));
				break;
			}

			case GLFW.GLFW_KEY_UP: {
				//check if we should just do nothing
				if (this.lineIndex == 0 && (this.cursorPos == 0 || this.charSubline.get(this.cursorPos - 1) == 0)) {
					break;
				}

				Vec2 cursorOffset = this.getGlobalCursorOffset();
				cursorOffset.y += charHeight;
				if (this.lineIndex != 0 && (this.cursorPos == 0 || this.charSubline.get(this.cursorPos - 1) == 0)) {
					//select the line above me
					selectLine(this.lineIndex - 1);
				}
				selectedLine.setCursorPos(cursorOffset);
				break;
			}

			case GLFW.GLFW_KEY_DOWN: {
				//check if we should just do nothing
				if (this.lineIndex == lines.size() - 1 && (this.maxSubline == 0 || (this.cursorPos != 0 && this.charSubline.get(this.cursorPos - 1) == maxSubline))) {
					break;
				}

				Vec2 cursorOffset = this.getGlobalCursorOffset();
				cursorOffset.y -= charHeight;
				if (this.lineIndex != lines.size() - 1 && ((this.cursorPos != 0 && this.charSubline.get(this.cursorPos - 1) == this.maxSubline) || this.maxSubline == 0)) {
					//select the line below me
					selectLine(this.lineIndex + 1);
				}
				selectedLine.setCursorPos(cursorOffset);
				break;
			}

			case GLFW.GLFW_KEY_BACKSPACE: {
				this.pressBackspaceAtCursor();
				break;
			}

			case GLFW.GLFW_KEY_TAB: {
				this.addCharacterAtCursor((char) 9);
				break;
			}

			case GLFW.GLFW_KEY_ENTER: {
				if (curlyBracketIndentingMode) {
					if (this.cursorPos != this.chars.size() && this.cursorPos != 0 && this.chars.get(cursorPos - 1) == '{' && this.chars.get(cursorPos) == '}') {
						this.pressEnterAtCursor();
						selectLine(this.lineIndex);
						this.cursorPos = this.chars.size();
						this.pressEnterAtCursor();
						selectedLine.addCharacterAtCursor((char) 9);
						break;
					}
				}

				this.pressEnterAtCursor();
				break;
			}

			case GLFW.GLFW_KEY_SPACE: {
				//when i try to extract the key name from space it returns null, so here is a special case
				this.addCharacterAtCursor(' ');
				break;
			}

			default: {
				String keyName = glfwGetKeyName(key, 0);
				if (keyName == null || keyName.length() != 1) {
					return;
				}
				char k = keyName.charAt(0);
				if (KeyboardInput.isKeyPressed(GLFW.GLFW_KEY_LEFT_SHIFT) || KeyboardInput.isKeyPressed(GLFW.GLFW_KEY_RIGHT_SHIFT)) {
					k = KeyboardInput.shiftMap.get(k);
				}

				if (bracketFinishMode) {
					if (k == '(' || k == '{' || k == '[') {
						//auto finish this bracket. 
						this.addCharacterAtCursor(k);
						switch (k) {
						case '(':
							this.addCharacterAtCursor(')');
							break;

						case '{':
							this.addCharacterAtCursor('}');
							break;

						case '[':
							this.addCharacterAtCursor(']');
							break;
						}
						this.shiftCursor(-1);
						break;
					}
					else if (k == ')' || k == '}' || k == ']') {
						//check if end bracket already is there at cursor pos. 
						if (this.cursorPos != this.chars.size() && this.chars.get(this.cursorPos) == k) {
							this.shiftCursor(1);
						}
						else {
							this.addCharacterAtCursor(k);
						}
						break;
					}
				}

				this.addCharacterAtCursor(k);
				break;
			}
			}
		}

	}

}
