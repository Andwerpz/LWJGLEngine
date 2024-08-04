package lwjglengine.ui;

import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_LINEAR_MIPMAP_LINEAR;
import static org.lwjgl.opengl.GL11.GL_NEAREST;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import lwjglengine.entity.Entity;
import lwjglengine.graphics.Material;
import lwjglengine.graphics.Texture;
import lwjglengine.graphics.TextureMaterial;
import lwjglengine.main.Main;
import lwjglengine.model.FilledRectangle;
import lwjglengine.model.Model;
import myutils.graphics.FontUtils;
import myutils.graphics.GraphicsTools;
import myutils.math.Mat4;
import myutils.math.Vec3;
import myutils.math.Vec4;

public class Text extends UIElement {
	// utilizes java.fx to generate the text texture, which it then draws onto the screen using a filled rectangle
	// width of text is fixed, text will be cut off if it grows too wide.

	//note that text only really functions if you're fine with the 'world space' size of the text to be equal to 
	//the texture size. otherwise, it completely breaks. 

	//TODO 
	// - rework this entire thing D: Ideally, should render using a texture atlas, entirely cutting off java fx. 
	//   - should be able to independently set the texture and uielement size. 
	//   - when setting text, there should be an option to not change the dimensions of the uielement. 
	// - replace all occurrences of '12' related to font size with 'DEFAULT_FONT_SIZE'

	// FIXED
	// - with string "C:" with color set to white, font Dialogue, plain, size 12, the texture fails to generate. 
	//   - to fix, you just add a bunch of spaces to the string, so "C:        " works. 
	//   - the problem was the texture was too small to generate the required mipmaps. I just made it so that
	//		when generating a text texture, we don't generate any mipmaps. 
	// - figure out how to fix transparency issues when text size becomes small
	//   - seems like small text using java.fx is just kinda bad. It works well if the text and the background are around the same color. 
	//   - one fix could be to directly load from a texture atlas, but that's kinda annoying. 
	//   - upside is that we'll be able to render all our text in one render call, as we're just rendering from an atlas, 
	//     and we won't have annoying issues with text scaling. We can just set the font size, and it'll autoscale. 
	//   - the problem was that pixels on the edges of letters were being interpolated with transparent pixels, which
	//     had a default RGB value of (0, 0, 0). This caused them to be darker than they were supposed to be. This 
	//     is not an isolated problem with text, and I fixed it by making any texture loaded via buffered image be
	//     premultiplied with alpha. 

	public static final int DEFAULT_FONT_SIZE = 12;
	public static final Font DEFAULT_FONT = new Font("Consolas", Font.PLAIN, DEFAULT_FONT_SIZE);

	private int textWidth, textMaxHeight;
	private int textSampleAscent, textSampleDescent;

	private String text;
	private Font font;
	private int fontSize;

	private boolean textWrapping = false;
	private int lineSpacing = 3; //in pixels

	//if draw background is true, then the texture background will not be transparent, instead
	//replaced with a solid rectangle of the specified color. 
	//might be useful when trying to fix antialiasing issues with thin texts behind solid background colors. 
	private Color backgroundColor = Color.WHITE;
	private boolean drawBackground = false;

	private boolean doAntialiasing = true;

	private boolean changedText = false;

	public Text(String text, int scene) {
		this(0, 0, text, scene);
	}

	public Text(float x, float y, String text, int scene) {
		super(x, y, 0, 0, 0, new FilledRectangle(), scene);
		Font font = DEFAULT_FONT;
		this.init(GraphicsTools.calculateTextWidth(text, font), text, font, new Material(Color.WHITE));
	}

	public Text(float x, float y, String text, int fontSize, Material material, int scene) {
		super(x, y, 0, 0, 0, new FilledRectangle(), scene);
		Font derivedFont = FontUtils.deriveSize(fontSize, DEFAULT_FONT);
		this.init(GraphicsTools.calculateTextWidth(text, derivedFont), text, derivedFont, material);
	}

	public Text(float x, float y, String text, int fontSize, Color color, int scene) {
		super(x, y, 0, 0, 0, new FilledRectangle(), scene);
		Font derivedFont = FontUtils.deriveSize(fontSize, DEFAULT_FONT);
		this.init(GraphicsTools.calculateTextWidth(text, derivedFont), text, derivedFont, new Material(color));
	}

	public Text(float x, float y, String text, Font font, int fontSize, Material material, int scene) {
		super(x, y, 0, 0, 0, new FilledRectangle(), scene);
		Font derivedFont = FontUtils.deriveSize(fontSize, font);
		this.init(GraphicsTools.calculateTextWidth(text, derivedFont), text, derivedFont, material);
	}

	public Text(float x, float y, String text, Font font, Color color, int scene) {
		super(x, y, 0, 0, 0, new FilledRectangle(), scene);
		this.init(GraphicsTools.calculateTextWidth(text, font), text, font, new Material(color));
	}

	public Text(float x, float y, String text, Font font, Material material, int scene) {
		super(x, y, 0, 0, 0, new FilledRectangle(), scene);
		this.init(GraphicsTools.calculateTextWidth(text, font), text, font, material);
	}

	public Text(float x, float y, String text, Font font, int fontSize, Color color, int scene) {
		super(x, y, 0, 0, 0, new FilledRectangle(), scene);
		Font derivedFont = FontUtils.deriveSize(fontSize, font);
		this.init(GraphicsTools.calculateTextWidth(text, derivedFont), text, derivedFont, new Material(color));
	}

	public Text(float x, float y, float z, String text, Font font, Material material, int scene) {
		super(x, y, z, 0, 0, new FilledRectangle(), scene);
		this.init(GraphicsTools.calculateTextWidth(text, font), text, font, material);
	}

	public Text(float x, float y, float z, float width, String text, Font font, Material material, int scene) {
		super(x, y, z, 0, 0, new FilledRectangle(), scene);
		this.init(width, text, font, material);
	}

	public Text(float x, float y, float z, float width, String text, Font font, int fontSize, Color color, int scene) {
		super(x, y, z, 0, 0, new FilledRectangle(), scene);
		Font derivedFont = FontUtils.deriveSize(fontSize, font);
		this.init(width, text, derivedFont, new Material(color));
	}

	private void init(float width, String text, Font font, Material material) {
		this.init(width, GraphicsTools.getFontSampleAscent(font), text, font, material);
	}

	private void init(float width, float height, String text, Font font, Material textMaterial) {
		this.text = text;
		if (this.text.length() == 0) {
			this.text = " ";
		}

		this.font = font;
		this.fontSize = font.getSize();
		System.out.println("FONT SIZE : " + this.fontSize);

		this.textWidth = GraphicsTools.calculateTextWidth(text, font);

		this.textSampleAscent = GraphicsTools.getFontSampleAscent(font);
		this.textSampleDescent = GraphicsTools.getFontSampleDescent(font);

		this.textMaxHeight = textSampleAscent + textSampleDescent;

		this.width = width;
		this.height = this.textMaxHeight;

		this.alignContentInsteadOfFrame = true;
		this.contentWidth = this.width;
		this.contentHeight = this.textSampleAscent;
		this.contentYOffset = this.textSampleDescent;

		this.setTextureMaterial(new TextureMaterial(this.generateAlignedTexture()));
		this.setMaterial(textMaterial);

		this.setContentAlignmentStyle(ALIGN_LEFT, ALIGN_BOTTOM);
	}

	@Override
	protected void _alignContents() {
		if (this.changedContentAlignmentStyle || this.changedDimensions || this.changedText) {
			if (!(this.width <= 0)) {
				this.setTextureMaterial(new TextureMaterial(this.generateAlignedTexture()));
			}
			this.changedText = false;
		}
	}

	public void setBackgroundColor(Color c) {
		this.backgroundColor = c;
		this.setTextureMaterial(new TextureMaterial(this.generateAlignedTexture()));
	}

	public void setDrawBackground(boolean b) {
		this.drawBackground = b;
		this.setTextureMaterial(new TextureMaterial(this.generateAlignedTexture()));
	}

	public void setDoAntialiasing(boolean b) {
		this.doAntialiasing = b;
		this.setTextureMaterial(new TextureMaterial(this.generateAlignedTexture()));
	}

	public Texture generateAlignedTexture() {
		int font_sample_type = GL_LINEAR;
		System.out.println("CREATE TEXT TEXTURE : " + this.text);

		BufferedImage img = new BufferedImage((int) this.width, (int) this.height, BufferedImage.TYPE_INT_ARGB);
		Graphics g = img.getGraphics();

		if (this.doAntialiasing) {
			GraphicsTools.enableAntialiasing(g);
		}

		if (this.drawBackground) {
			g.setColor(this.backgroundColor);
			g.fillRect(0, 0, (int) this.width, (int) this.height);
		}

		g.setFont(this.font);
		g.setColor(Color.WHITE);

		String[] a = this.text.split(" ");

		if (!this.textWrapping || a.length == 0) {
			int alignedX = 0;
			if (this.horizontalAlignContent == ALIGN_CENTER) {
				alignedX = 0;
			}
			else if (this.horizontalAlignContent == ALIGN_LEFT) {
				alignedX = 0;
			}
			else if (this.horizontalAlignContent == ALIGN_RIGHT) {
				alignedX = (int) this.width - this.textWidth;
			}

			g.drawString(this.text, alignedX, this.textSampleAscent);

			return new Texture(img, 0, GL_LINEAR_MIPMAP_LINEAR, font_sample_type, 1);
		}

		String currentLine = a[0];
		int p = 1;
		int curY = 0;
		while (p < a.length) {
			int addedWidth = GraphicsTools.calculateTextWidth(currentLine + " " + a[p], this.font);
			if (addedWidth > (int) this.width && currentLine.length() != 0) {
				int lineWidth = GraphicsTools.calculateTextWidth(currentLine, font);
				int alignedX = 0;
				if (this.horizontalAlignContent == ALIGN_CENTER) {
					alignedX = 0;
				}
				else if (this.horizontalAlignContent == ALIGN_LEFT) {
					alignedX = 0;
				}
				else if (this.horizontalAlignContent == ALIGN_RIGHT) {
					alignedX = (int) this.height - lineWidth;
				}

				g.drawString(currentLine, alignedX, curY + this.textSampleAscent);

				curY += this.textMaxHeight + this.lineSpacing;
				currentLine = a[p];
				p++;
			}
			else {
				currentLine += " " + a[p];
				p++;
			}
		}
		int lineWidth = GraphicsTools.calculateTextWidth(currentLine, font);
		int alignedX = 0;
		if (this.horizontalAlignContent == ALIGN_CENTER) {
			alignedX = 0;
		}
		else if (this.horizontalAlignContent == ALIGN_LEFT) {
			alignedX = 0;
		}
		else if (this.horizontalAlignContent == ALIGN_RIGHT) {
			alignedX = (int) this.width - lineWidth;
		}
		g.drawString(currentLine, alignedX, curY + this.textSampleAscent);
		return new Texture(img, 0, GL_LINEAR_MIPMAP_LINEAR, font_sample_type, 1);
	}

	private int calculateHeight() {
		if (!this.textWrapping) {
			return this.textMaxHeight;
		}

		int ans = 0;
		String[] a = this.text.split(" ");
		if (a.length == 0) {
			return this.textMaxHeight;
		}
		String currentLine = a[0];
		int p = 1;
		while (p < a.length) {
			int addedWidth = GraphicsTools.calculateTextWidth(currentLine + " " + a[p], this.font);
			if (addedWidth > (int) this.width && currentLine.length() != 0) {
				ans += this.textMaxHeight + this.lineSpacing;
				currentLine = a[p];
				p++;
			}
			else {
				currentLine += " " + a[p];
				p++;
			}
		}
		ans += this.textMaxHeight;
		return ans;
	}

	public void setText(String text) {
		if (this.text.equals(text)) {
			return;
		}

		this.text = text;
		this.textWidth = GraphicsTools.calculateTextWidth(this.text, this.font);
		this.height = this.calculateHeight();

		this.setContentDimensions(this.textWidth, this.height);

		this.changedText = true;

		this.align();
	}

	public String getText() {
		return this.text;
	}

	public int getTextWidth() {
		return this.textWidth;
	}

	public void setTextWrapping(boolean b) {
		this.textWrapping = b;
		this.align();
	}

	public Font getFont() {
		return this.font;
	}

	@Override
	protected void __kill() {

	}

	@Override
	protected void _update() {

	}

}
