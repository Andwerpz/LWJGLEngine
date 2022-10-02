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
import model.Model;
import util.FontUtils;
import util.GraphicsTools;
import util.Mat4;
import util.Vec3;

public class Text extends UIElement {
	// utilizes java.fx to draw text onto a texture, which it then draws onto the
	// screen using a filled rectangle

	// width of text is fixed, text will be cut off if it grows too wide.

	private long rectangleID;

	private int width, height;
	private int textWidth, textMaxHeight, textMaxDescent, textMaxAscent, textSampleAscent, textSampleDescent;
	private int scene;

	private Material material;

	private Model textRectangle;
	private TextureMaterial textTextureMaterial;
	private String text;
	private Font font;
	private int fontSize;

	public Text(int x, int y, String text, int fontSize, Material material, int scene) {
		super(x, y);
		Font derivedFont = new Font("Dialogue", Font.PLAIN, fontSize);
		this.init(x, y, 0, GraphicsTools.calculateTextWidth(text, derivedFont), text, derivedFont, material, scene);
	}

	public Text(int x, int y, String text, Font font, int fontSize, Material material, int scene) {
		super(x, y);
		Font derivedFont = FontUtils.deriveSize(fontSize, font);
		this.init(x, y, 0, GraphicsTools.calculateTextWidth(text, derivedFont), text, derivedFont, material, scene);
	}

	public Text(int x, int y, String text, Font font, Color color, int scene) {
		super(x, y);
		this.init(x, y, 0, GraphicsTools.calculateTextWidth(text, font), text, font, new Material(color), scene);
	}

	public Text(int x, int y, String text, Font font, int fontSize, Color color, int scene) {
		super(x, y);
		Font derivedFont = FontUtils.deriveSize(fontSize, font);
		this.init(x, y, 0, GraphicsTools.calculateTextWidth(text, derivedFont), text, derivedFont, new Material(color), scene);
	}

	public Text(int x, int y, int z, String text, Font font, Material material, int scene) {
		super(x, y);
		this.init(x, y, z, GraphicsTools.calculateTextWidth(text, font), text, font, material, scene);
	}

	public Text(int x, int y, int z, int width, String text, Font font, Material material, int scene) {
		super(x, y);
		this.init(x, y, z, width, text, font, material, scene);
	}

	public Text(int x, int y, String text, Font font, Material material, int scene) {
		super(x, y);
		this.init(x, y, 0, GraphicsTools.calculateTextWidth(text, font), text, font, material, scene);
	}

	private void init(int x, int y, int z, int width, String text, Font font, Material material, int scene) {
		this.text = text;
		this.font = font;
		this.fontSize = font.getSize();
		this.x = x;
		this.y = y;
		this.z = z;
		this.width = width; // text will get cut off after this
		this.material = material;
		this.textWidth = GraphicsTools.calculateTextWidth(text, font);

		this.setContentAlignmentStyle(ALIGN_LEFT, ALIGN_RIGHT);

		this.textSampleAscent = GraphicsTools.getFontSampleAscent(font);
		this.textSampleDescent = GraphicsTools.getFontSampleDescent(font);

		this.textMaxAscent = Math.max(GraphicsTools.getFontMaxAscent(font), this.textSampleAscent);
		this.textMaxDescent = Math.max(GraphicsTools.getFontMaxDescent(font), this.textSampleDescent);
		this.textMaxHeight = textMaxAscent + textMaxDescent;

		this.height = textMaxHeight;

		BufferedImage img = GraphicsTools.generateTextImage(text, font, Color.WHITE, this.width);
		Texture texture = new Texture(img, false, false, true);
		this.textTextureMaterial = new TextureMaterial(texture);

		Mat4 modelMat4 = Mat4.scale(this.width, textMaxHeight, 1).mul(Mat4.translate(new Vec3(x, y, this.z)));

		this.textRectangle = new FilledRectangle();
		this.textRectangle.setTextureMaterial(textTextureMaterial);
		this.rectangleID = this.addModelInstance(this.textRectangle, modelMat4, scene);
		this.updateModelInstance(this.rectangleID, this.material);
	}

	public int getHeight() {
		return this.height;
	}

	public int getWidth() {
		return this.width;
	}

	@Override
	protected void alignContents() {
		int alignedX = x;
		switch (this.horizontalAlignContent) {
		case ALIGN_CENTER:
			alignedX = x - this.width / 2;
			break;

		case ALIGN_RIGHT:
			alignedX = x - this.width;
			break;

		case ALIGN_LEFT:
			alignedX = x;
			break;
		}

		int alignedY = y;
		switch (this.verticalAlignContent) {
		case ALIGN_CENTER:
			alignedY = y - this.textMaxDescent - this.textSampleAscent / 2;
			break;

		case ALIGN_TOP:
			alignedY = y - this.textMaxDescent - this.textSampleAscent;
			break;

		case ALIGN_BOTTOM:
			alignedY = y - this.textMaxDescent;
			break;
		}

		Mat4 modelMat4 = Mat4.scale(this.width, textMaxHeight, 1).mul(Mat4.translate(new Vec3(alignedX, alignedY, this.z)));
		this.textTextureMaterial.setTexture(this.generateAlignedTexture(), TextureMaterial.DIFFUSE);
		this.updateModelInstance(this.rectangleID, modelMat4);
	}

	private Texture generateAlignedTexture() {
		BufferedImage img = new BufferedImage(this.width, this.textMaxDescent + this.textMaxAscent, BufferedImage.TYPE_INT_ARGB);
		Graphics g = img.getGraphics();
		GraphicsTools.enableAntialiasing(g);
		g.setFont(font);
		g.setColor(Color.WHITE);

		int alignedX = 0;

		if (this.horizontalAlignContent == ALIGN_CENTER) {
			alignedX = 0;
		}
		else if (this.horizontalAlignContent == ALIGN_LEFT) {
			alignedX = 0;
		}
		else if (this.horizontalAlignContent == ALIGN_RIGHT) {
			alignedX = this.width - this.textWidth;
		}

		g.drawString(text, alignedX, textMaxAscent);

		return new Texture(img, false, false, true);
	}

	public void setText(String text) {
		if (this.text.equals(text)) {
			return;
		}

		this.text = text;
		this.textWidth = GraphicsTools.calculateTextWidth(this.text, this.font);

		this.alignContents();
	}

	public void setMaterial(Material material) {
		if (material == this.material) {
			return;
		}
		this.material = material;
		this.updateModelInstance(this.rectangleID, this.material);
	}

	@Override
	protected void __kill() {
		this.textRectangle.kill();
	}

	@Override
	public void update() {

	}

}
