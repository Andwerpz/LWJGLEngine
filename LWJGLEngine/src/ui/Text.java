package ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import entity.Entity;
import graphics.Material;
import graphics.Texture;
import graphics.TextureMaterial;
import model.FilledRectangle;
import model.Model;
import util.FontUtils;
import util.GraphicsTools;
import util.Mat4;
import util.Vec3;
import util.Vec4;

public class Text extends UIElement {
	// utilizes java.fx to generate the text texture, which it then draws onto the screen using a filled rectangle

	// width of text is fixed, text will be cut off if it grows too wide.

	private int textWidth, textMaxHeight;
	private int textSampleAscent, textSampleDescent;

	private TextureMaterial textTextureMaterial;
	private String text;
	private Font font;
	private int fontSize;

	private boolean textWrapping = false;
	private int lineSpacing = 3; //in pixels

	public Text(float x, float y, String text, int fontSize, Material material, int scene) {
		super(x, y, 0, 0, 0, new FilledRectangle(), scene);
		Font derivedFont = new Font("Dialogue", Font.PLAIN, fontSize);
		this.init(GraphicsTools.calculateTextWidth(text, derivedFont), text, derivedFont, material);
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

	public Text(float x, float y, String text, Font font, Material material, int scene) {
		super(x, y, 0, 0, 0, new FilledRectangle(), scene);
		this.init(GraphicsTools.calculateTextWidth(text, font), text, font, material);
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

		this.textWidth = GraphicsTools.calculateTextWidth(text, font);

		this.textSampleAscent = GraphicsTools.getFontSampleAscent(font);
		this.textSampleDescent = GraphicsTools.getFontSampleDescent(font);

		this.textMaxHeight = textSampleAscent + textSampleDescent;

		this.width = width;
		this.height = this.textMaxHeight;

		BufferedImage img = GraphicsTools.generateTextImage(text, font, Color.WHITE, (int) this.width);
		Texture texture = new Texture(img, Texture.VERTICAL_FLIP_BIT);
		this.textTextureMaterial = new TextureMaterial(texture);

		this.setTextureMaterial(this.textTextureMaterial);
		this.setMaterial(textMaterial);

		this.setContentAlignmentStyle(ALIGN_LEFT, ALIGN_BOTTOM);
	}

	@Override
	protected void _alignContents() {
		this.setTextureMaterial(new TextureMaterial(this.generateAlignedTexture()));
	}

	private Texture generateAlignedTexture() {
		BufferedImage img = new BufferedImage((int) this.width, (int) this.height, BufferedImage.TYPE_INT_ARGB);
		Graphics g = img.getGraphics();
		GraphicsTools.enableAntialiasing(g);
		g.setFont(font);
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

			g.drawString(text, alignedX, this.textSampleAscent);

			return new Texture(img, Texture.VERTICAL_FLIP_BIT);
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
		return new Texture(img, Texture.VERTICAL_FLIP_BIT);
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

		this.align();
	}

	public void setTextWrapping(boolean b) {
		this.textWrapping = b;
		this.align();
	}

	@Override
	protected void __kill() {

	}

	@Override
	public void update() {

	}

}
