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
	// utilizes java.fx to draw text onto a texture, which it then draws onto the
	// screen using a filled rectangle

	// width of text is fixed, text will be cut off if it grows too wide.

	private long textRectangleID;

	private boolean drawBackgroundRectangle = false;
	private UIFilledRectangle backgroundRectangle;

	private int textWidth, textMaxHeight, textSampleAscent, textSampleDescent;
	private int maxTextWidth, maxTextHeight; //cutoff boundaries for text

	private Model textRectangle;
	private TextureMaterial textTextureMaterial;
	private String text;
	private Font font;
	private int fontSize;

	private int textHorizontalMargin, textVerticalMargin;

	private boolean textWrapping = false;
	private int lineSpacing = 3; //in pixels

	public Text(float x, float y, String text, int fontSize, Material material, int scene) {
		super(x, y, 0, 0, 0, scene);
		Font derivedFont = new Font("Dialogue", Font.PLAIN, fontSize);
		this.init(GraphicsTools.calculateTextWidth(text, derivedFont), text, derivedFont, material);
	}

	public Text(float x, float y, String text, Font font, int fontSize, Material material, int scene) {
		super(x, y, 0, 0, 0, scene);
		Font derivedFont = FontUtils.deriveSize(fontSize, font);
		this.init(GraphicsTools.calculateTextWidth(text, derivedFont), text, derivedFont, material);
	}

	public Text(float x, float y, String text, Font font, Color color, int scene) {
		super(x, y, 0, 0, 0, scene);
		this.init(GraphicsTools.calculateTextWidth(text, font), text, font, new Material(color));
	}

	public Text(float x, float y, String text, Font font, int fontSize, Color color, int scene) {
		super(x, y, 0, 0, 0, scene);
		Font derivedFont = FontUtils.deriveSize(fontSize, font);
		this.init(GraphicsTools.calculateTextWidth(text, derivedFont), text, derivedFont, new Material(color));
	}

	public Text(float x, float y, float z, String text, Font font, Material material, int scene) {
		super(x, y, z, 0, 0, scene);
		this.init(GraphicsTools.calculateTextWidth(text, font), text, font, material);
	}

	public Text(float x, float y, float z, float width, String text, Font font, Material material, int scene) {
		super(x, y, z, 0, 0, scene);
		this.init(width, text, font, material);
	}

	public Text(float x, float y, float z, float width, String text, Font font, int fontSize, Color color, int scene) {
		super(x, y, z, 0, 0, scene);
		Font derivedFont = FontUtils.deriveSize(fontSize, font);
		this.init(width, text, derivedFont, new Material(color));
	}

	public Text(float x, float y, String text, Font font, Material material, int scene) {
		super(x, y, 0, 0, 0, scene);
		this.init(GraphicsTools.calculateTextWidth(text, font), text, font, material);
	}

	private void init(float width, String text, Font font, Material material) {
		this.init(width, GraphicsTools.getFontSampleAscent(font), text, font, material);
	}

	private void init(float width, float height, String text, Font font, Material textMaterial) {
		this.text = text;
		this.font = font;
		this.fontSize = font.getSize();

		this.textHorizontalMargin = 0;
		this.textVerticalMargin = 0;
		this.maxTextWidth = (int) width; //text will get cut off after this

		//width and height of the background rectangle
		this.width = this.maxTextWidth + textHorizontalMargin * 2;
		this.height = height + this.textVerticalMargin * 2;

		this.textWidth = GraphicsTools.calculateTextWidth(text, font);

		this.textSampleAscent = GraphicsTools.getFontSampleAscent(font);
		this.textSampleDescent = GraphicsTools.getFontSampleDescent(font);

		this.textMaxHeight = textSampleAscent + textSampleDescent;
		this.maxTextHeight = this.textMaxHeight;

		BufferedImage img = GraphicsTools.generateTextImage(text, font, Color.WHITE, this.maxTextWidth);
		Texture texture = new Texture(img, Texture.VERTICAL_FLIP_BIT);
		this.textTextureMaterial = new TextureMaterial(texture);

		Mat4 modelMat4 = Mat4.scale(this.maxTextWidth, this.textMaxHeight, 1).mul(Mat4.translate(new Vec3(this.x, this.y, this.z + 1)));

		this.textRectangle = new FilledRectangle();
		this.textRectangle.setTextureMaterial(textTextureMaterial);
		this.textRectangleID = this.addModelInstance(this.textRectangle, modelMat4, scene);
		this.updateModelInstance(this.textRectangleID, textMaterial);

		this.setContentAlignmentStyle(ALIGN_LEFT, ALIGN_BOTTOM);
	}

	@Override
	protected void _alignContents() {
		Mat4 modelMat4 = Mat4.scale(this.maxTextWidth, this.maxTextHeight, 1).mul(Mat4.translate(new Vec3(this.alignedX + this.textHorizontalMargin, alignedY + this.textVerticalMargin - this.textSampleDescent, this.z + 1)));
		this.textTextureMaterial.setTexture(this.generateAlignedTexture(), TextureMaterial.DIFFUSE);
		this.updateModelInstance(this.textRectangleID, modelMat4);

		if (this.backgroundRectangle != null) {
			this.backgroundRectangle.setFrameAlignmentOffset(this.alignedX, this.alignedY);
			this.backgroundRectangle.setWidth(this.width);
			this.backgroundRectangle.setHeight(this.height);

			this.backgroundRectangle.align();
		}
	}

	private Texture generateAlignedTexture() {
		BufferedImage img = new BufferedImage(this.maxTextWidth, this.maxTextHeight, BufferedImage.TYPE_INT_ARGB);
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
				alignedX = this.maxTextWidth - this.textWidth;
			}

			g.drawString(text, alignedX, this.textSampleAscent);

			return new Texture(img, Texture.VERTICAL_FLIP_BIT);
		}

		String currentLine = a[0];
		int p = 1;
		int curY = 0;
		while (p < a.length) {
			int addedWidth = GraphicsTools.calculateTextWidth(currentLine + " " + a[p], this.font);
			if (addedWidth > this.maxTextWidth && currentLine.length() != 0) {
				int lineWidth = GraphicsTools.calculateTextWidth(currentLine, font);
				int alignedX = 0;
				if (this.horizontalAlignContent == ALIGN_CENTER) {
					alignedX = 0;
				}
				else if (this.horizontalAlignContent == ALIGN_LEFT) {
					alignedX = 0;
				}
				else if (this.horizontalAlignContent == ALIGN_RIGHT) {
					alignedX = this.maxTextWidth - lineWidth;
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
			alignedX = this.maxTextWidth - lineWidth;
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
			if (addedWidth > this.maxTextWidth && currentLine.length() != 0) {
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
		this.maxTextHeight = this.calculateHeight();
		this.setMargin(this.textHorizontalMargin);
	}

	public void setTextWrapping(boolean b) {
		this.textWrapping = b;

		this.maxTextHeight = this.calculateHeight();
		this.setMargin(this.textHorizontalMargin);

		this.align();
	}

	public void setTextMaterial(Material material) {
		this.updateModelInstance(this.textRectangleID, material);
	}

	public void setBackgroundMaterial(Material material) {
		if (this.backgroundRectangle != null) {
			this.backgroundRectangle.setMaterial(material);
		}
	}

	public void setDrawBackgroundRectangle(boolean b) {
		this.drawBackgroundRectangle = b;
		if (this.drawBackgroundRectangle) {
			this.backgroundRectangle = new UIFilledRectangle(this.x, this.y, this.z, this.width, this.height, this.scene);
			this.backgroundRectangle.setFrameAlignmentStyle(FROM_LEFT, FROM_BOTTOM);
			this.backgroundRectangle.setContentAlignmentStyle(ALIGN_LEFT, ALIGN_BOTTOM);

			this.backgroundRectangle.setMaterial(new Material(new Vec4(0)));
		}
		else {
			if (this.backgroundRectangle != null) {
				this.backgroundRectangle.kill();
				this.backgroundRectangle = null;
			}
		}
	}

	public void setMargin(int margin) {
		this.textHorizontalMargin = margin;
		this.textVerticalMargin = margin;

		this.width = this.maxTextWidth + this.textHorizontalMargin * 2;
		this.height = this.maxTextHeight + this.textVerticalMargin * 2;

		this.align();
	}

	@Override
	protected void __kill() {
		this.textRectangle.kill();

		if (this.backgroundRectangle != null) {
			this.backgroundRectangle.kill();
		}
	}

	@Override
	public void update() {

	}

}
