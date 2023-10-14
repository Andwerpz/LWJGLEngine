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
	// - optimize rendering
	//   - only render whatever can be seen on the screen.
	// - optimize writing
	//   - when we press enter, we have to realign all the lines, and in turn, all the individual characters, which causes lag spike
	//   - perhaps, if we are only rendering stuff on screen, we should only realign the stuff that is visible. 
	// - scroll bar on the right side to let user know how much they can scroll. 
	// - highlight the line that the cursor is currently on. 
	//   - can't really do that in the background, since the characters are rendered on top of a solid backing plate. 
	// - hold backspace to continuously delete
	//   - hold any character to continuously press it. 
	//   - this should also include binds, like ctrl + v.
	// - interactions with highlighted chars
	//   - ctrl + x to cut highlighted chars
	//   - press backspace to delete all highlighted chars
	//   - press and drag highlighted chars to move them around. 
	// - load / save using text files. 

	private static char[] charList = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '`', '-', '=', '[', ']', '\\', ';', '\'', ',', '.', '/', '~', '_', '+', '{', '}', '|', ':', '"', '<', '>', '?', ' ' };

	private static Font charFont = new Font("Consolas", Font.PLAIN, 14);
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

	private boolean shouldAlignLines = false;

	private static int cursorWidthPx = 2;
	private UIFilledRectangle cursorRect;

	private int lineHeightSum = 0;
	private int maxScrollOffset = 0;
	private int scrollOffset = 0;

	private boolean isHighlighting = false; //this is true when the user presses the mouse somewhere on a line. 
	//saves the cursor position where the user first pressed
	private int highlightFirstLineIndex = -1; //which line is it?
	private int highlightFirstCharIndex = -1; //index within the line

	private int highlightCurLineIndex = -1;
	private int highlightCurCharIndex = -1;

	private static Material highlightMaterial = new Material(new Vec4(48, 197, 255, 120).mul(1.0f / 255.0f));
	private HashMap<Pair<Integer, Integer>, UIFilledRectangle> highlightRects;

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

		this.highlightRects = new HashMap<>();

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

			BufferedImage img = GraphicsTools.generateTextImage(c, charFont, charColor, charWidth, lineBackgroundColor, true);
			Texture charTexture = new Texture(img, 0, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR, 1);
			TextureMaterial charTextureMaterial = new TextureMaterial(charTexture);

			FilledRectangle charRect = new FilledRectangle();
			charRect.setTextureMaterial(charTextureMaterial);

			this.charModels.put(charList[i], charRect);
			this.charWidths.put(charList[i], charWidth);
		}

		//tab character
		{
			String tab = "    ";
			int width = GraphicsTools.calculateTextWidth(tab, charFont);

			BufferedImage img = GraphicsTools.generateTextImage(tab, charFont, charColor, width, lineBackgroundColor, true);
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

	private void addLine(int index) {
		if (index < 0 || index > this.lines.size()) {
			System.err.println("TextEditorWindow: Tried to add element at index " + index + " when line amount is " + this.lines.size());
			return;
		}
		this.lines.add(index, new Line());
		this.alignLines();
	}

	private void removeLine(int index) {
		if (index < 0 || index >= this.lines.size()) {
			System.err.println("TextEditorWindow: Tried to remove element at index " + index + " when line amount is " + this.lines.size());
			return;
		}
		this.lines.get(index).kill();
		this.lines.remove(index);
		this.alignLines();
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

	private void alignLines() {
		int yOffset = 0;
		for (int i = 0; i < this.lines.size(); i++) {
			Line l = this.lines.get(i);
			l.alignLine(i, yOffset);

			yOffset += l.height;
		}

		this.lineHeightSum = yOffset;
		this.maxScrollOffset = Math.max(this.lineHeightSum - this.getHeight(), 0);
		this.scrollOffset = Math.min(this.scrollOffset, this.maxScrollOffset);

		if (this.selectedLine != null) {
			this.selectedLine.updateCosmeticCursorPos();
		}
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

		this.alignLines();
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
			this.alignLines();
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
						Pair<Integer, Integer> p = new Pair<>(this.highlightCurLineIndex, this.highlightCurCharIndex);
						if (this.highlightRects.containsKey(p)) {
							this.highlightRects.get(p).kill();
							this.highlightRects.remove(p);
						}
						else {
							UIFilledRectangle charRect = l.charRects.get(this.highlightCurCharIndex);
							UIFilledRectangle highlightRect = new UIFilledRectangle(0, 0, 0, charRect.getWidth(), charRect.getHeight(), cursorSection.getBackgroundScene());
							highlightRect.setFillWidth(true);
							highlightRect.setFillHeight(true);
							highlightRect.setMaterial(highlightMaterial);
							highlightRect.bind(charRect);

							this.highlightRects.put(p, highlightRect);
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
						Pair<Integer, Integer> p = new Pair<>(this.highlightCurLineIndex, this.highlightCurCharIndex);
						if (this.highlightRects.containsKey(p)) {
							this.highlightRects.get(p).kill();
							this.highlightRects.remove(p);
						}
						else {
							UIFilledRectangle charRect = l.charRects.get(this.highlightCurCharIndex);
							UIFilledRectangle highlightRect = new UIFilledRectangle(0, 0, 0, charRect.getWidth(), charRect.getHeight(), cursorSection.getBackgroundScene());
							highlightRect.setFillWidth(true);
							highlightRect.setFillHeight(true);
							highlightRect.setMaterial(highlightMaterial);
							highlightRect.bind(charRect);

							this.highlightRects.put(p, highlightRect);
						}
					}
				}
			}

			//set current cursor pos
			this.selectLine(this.highlightCurLineIndex);
			this.selectedLine.setCursorPos(this.highlightCurCharIndex);
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
		this.highlightFirstCharIndex = -1;
		this.highlightFirstLineIndex = -1;
		this.highlightCurCharIndex = -1;
		this.highlightCurLineIndex = -1;
		for (UIFilledRectangle rect : this.highlightRects.values()) {
			rect.kill();
		}
		this.highlightRects.clear();

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

	@Override
	protected void _mouseReleased(int button) {
		this.isHighlighting = false;
	}

	@Override
	protected void _mouseScrolled(float wheelOffset, float smoothOffset) {
		int scrollAmt = (int) (smoothOffset * charHeight);
		this.scrollOffset = MathUtils.clamp(0, this.maxScrollOffset, this.scrollOffset - scrollAmt);
	}

	@Override
	protected void _keyPressed(int key) {
		if (this.selectedLine != null) {
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
			// ctrl + c
			else if ((KeyboardInput.isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL) || KeyboardInput.isKeyPressed(GLFW.GLFW_KEY_RIGHT_CONTROL)) && key == GLFW.GLFW_KEY_C) {
				if (this.highlightRects.size() != 0) {
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
				return;
			}
			else {
				this.selectedLine.keyPressed(key);
			}
		}
	}

	@Override
	protected void _keyReleased(int key) {

	}

	class Line {

		private ArrayList<Character> chars;
		private ArrayList<UIFilledRectangle> charRects;
		private ArrayList<Integer> charSubline; //if the window is narrow enough, each line can be wrapped into sublines. 
		private int maxSubline = 0;

		private int height;

		private int lineIndex = -1;

		private Text lineIndexText;

		private UIFilledRectangle textBackgroundRect;

		private boolean isSelected = false;
		private int cursorPos = -1;

		public Line() {
			this.chars = new ArrayList<>();
			this.charRects = new ArrayList<>();
			this.charSubline = new ArrayList<>();

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
		}

		public void appendCharacters(ArrayList<Character> clist) {
			for (char c : clist) {
				this.addCharacter(this.chars.size(), c);
			}
		}

		private void addCharacter(int index, char c) {
			if (index < 0 || index > chars.size()) {
				System.err.println("TextEditorWindow{Line} : Tried to add character at index out of bounds");
				return;
			}

			UIFilledRectangle charRect = (UIFilledRectangle) createCharUIElement(c);
			charRect.bind(this.textBackgroundRect);

			this.chars.add(index, c);
			this.charRects.add(index, charRect);
			this.charSubline.add(index, 0);

			this.alignCharacters();
		}

		private void removeCharacter(int index) {
			if (index < 0 || index >= chars.size()) {
				System.err.println("TextEditorWindow{Line} : Tried to remove character at index out of bounds");
				return;
			}

			this.charRects.get(index).kill();

			this.chars.remove(index);
			this.charRects.remove(index);
			this.charSubline.remove(index);

			this.alignCharacters();
		}

		//adds the character at the cursor location, and then moves the cursor up 1 character. 
		public void addCharacterAtCursor(char c) {
			this.addCharacter(this.cursorPos, c);
			this.setCursorPos(this.cursorPos + 1);
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
		}

		//removes the character before the cursor location, if there is one. 
		//otherwise, tries to merge this line with the previous one. 
		public void pressBackspaceAtCursor() {
			if (this.cursorPos != 0) {
				//normal backspace action
				this.removeCharacter(this.cursorPos - 1);
				this.setCursorPos(this.cursorPos - 1);
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
				UIFilledRectangle rect = this.charRects.get(i);
				if (rect.getWidth() + xOffset > this.textBackgroundRect.getWidth()) {
					yOffset += charHeight;
					xOffset = 0;
					this.maxSubline++;
				}

				rect.setFrameAlignmentOffset(xOffset, yOffset);
				this.charSubline.set(i, this.maxSubline);

				xOffset += rect.getWidth();
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

		private void updateCosmeticCursorPos() {
			if (this.cursorPos == -1) {
				this.cursorPos = 0;
			}

			//find position of cosmetic cursor
			int xOffset = (int) this.textBackgroundRect.getXOffset();
			int yOffset = (int) this.textBackgroundRect.getYOffset();

			if (cursorPos != 0) {
				UIFilledRectangle charRect = this.charRects.get(this.cursorPos - 1);
				xOffset += charRect.getXOffset() + charRect.getWidth();
				yOffset += charRect.getYOffset();
			}

			cursorRect.easeFrameAlignmentOffset(xOffset, yOffset);
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

			this.updateCosmeticCursorPos();
		}

		//find the closest cursor location to the given offset point. 
		//input mouse offset has to be relative to textBackgroundRect
		public void setCursorPos(Vec2 mouseOffset) {
			this.cursorPos = this.findBestCursorPos(mouseOffset);

			this.updateCosmeticCursorPos();
		}

		public int findBestCursorPos(Vec2 relLineContainerOffset) {
			int bestCursorPos = 0;
			float minDist = Vec2.distanceSq(relLineContainerOffset, new Vec2(this.textBackgroundRect.getGlobalAlignedX(), this.textBackgroundRect.getGlobalAlignedY() + this.textBackgroundRect.getHeight() - charHeight / 2));
			for (int i = 0; i < this.chars.size(); i++) {
				UIFilledRectangle charRect = this.charRects.get(i);
				Vec2 nextCharPos = new Vec2(charRect.getGlobalAlignedX(), charRect.getGlobalAlignedY());
				nextCharPos.x += charRect.getWidth();
				nextCharPos.y += charHeight / 2;
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
				UIFilledRectangle charRect = this.charRects.get(this.cursorPos - 1);
				ret.x = charRect.getGlobalAlignedX() + charRect.getWidth();
				ret.y = charRect.getGlobalAlignedY() + charHeight / 2;
			}
			return ret;
		}

		public void select() {
			this.isSelected = true;
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

				this.addCharacterAtCursor(k);
				break;
			}
			}
		}

	}

}
