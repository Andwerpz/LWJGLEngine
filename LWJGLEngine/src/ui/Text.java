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
	private int maxTextWidth;

	private Model textRectangle;
	private TextureMaterial textTextureMaterial;
	private String text;
	private Font font;
	private int fontSize;
	
	private int textHorizontalMargin, textVerticalMargin;

	public Text(int x, int y, String text, int fontSize, Material material, int scene) {
		super(x, y, 0, 0, 0, scene);
		Font derivedFont = new Font("Dialogue", Font.PLAIN, fontSize);
		this.init(GraphicsTools.calculateTextWidth(text, derivedFont), text, derivedFont, material);
	}

	public Text(int x, int y, String text, Font font, int fontSize, Material material, int scene) {
		super(x, y, 0, 0, 0, scene);
		Font derivedFont = FontUtils.deriveSize(fontSize, font);
		this.init(GraphicsTools.calculateTextWidth(text, derivedFont), text, derivedFont, material);
	}

	public Text(int x, int y, String text, Font font, Color color, int scene) {
		super(x, y, 0, 0, 0, scene);
		this.init(GraphicsTools.calculateTextWidth(text, font), text, font, new Material(color));
	}

	public Text(int x, int y, String text, Font font, int fontSize, Color color, int scene) {
		super(x, y, 0, 0, 0, scene);
		Font derivedFont = FontUtils.deriveSize(fontSize, font);
		this.init(GraphicsTools.calculateTextWidth(text, derivedFont), text, derivedFont, new Material(color));
	}

	public Text(int x, int y, int z, String text, Font font, Material material, int scene) {
		super(x, y, z, 0, 0, scene);
		this.init(GraphicsTools.calculateTextWidth(text, font), text, font, material);
	}

	public Text(int x, int y, int z, int width, String text, Font font, Material material, int scene) {
		super(x, y, z, 0, 0, scene);
		this.init(width, text, font, material);
	}

	public Text(int x, int y, String text, Font font, Material material, int scene) {
		super(x, y, 0, 0, 0, scene);
		this.init(GraphicsTools.calculateTextWidth(text, font), text, font, material);
	}
	
	private void init(int width, String text, Font font, Material material) {
		this.init(width, GraphicsTools.getFontSampleAscent(font), text, font, material);
	}

	private void init(int width, int height, String text, Font font, Material textMaterial) {
		this.text = text;
		this.font = font;
		this.fontSize = font.getSize();
		
		this.textHorizontalMargin = 0;
		this.textVerticalMargin = 0;
		this.maxTextWidth = width;	//text will get cut off after this
		
		//width and height of the background rectangle
		this.width = this.maxTextWidth + textHorizontalMargin * 2;
		this.height = height + this.textVerticalMargin * 2;
		
		this.textWidth = GraphicsTools.calculateTextWidth(text, font);

		this.setContentAlignmentStyle(ALIGN_LEFT, ALIGN_BOTTOM);

		this.textSampleAscent = GraphicsTools.getFontSampleAscent(font);
		this.textSampleDescent = GraphicsTools.getFontSampleDescent(font);

		this.textMaxHeight = textSampleAscent + textSampleDescent;

		BufferedImage img = GraphicsTools.generateTextImage(text, font, Color.WHITE, this.maxTextWidth);
		Texture texture = new Texture(img, false, false, true);
		this.textTextureMaterial = new TextureMaterial(texture);

		Mat4 modelMat4 = Mat4.scale(this.maxTextWidth, this.textMaxHeight, 1).mul(Mat4.translate(new Vec3(this.x, this.y, this.z + 1)));

		this.textRectangle = new FilledRectangle();
		this.textRectangle.setTextureMaterial(textTextureMaterial);
		this.textRectangleID = this.addModelInstance(this.textRectangle, modelMat4, scene);
		this.updateModelInstance(this.textRectangleID, textMaterial);
	}

	public int getHeight() {
		return this.height;
	}

	public int getWidth() {
		return this.width;
	}

	@Override
	protected void _alignContents() {
//		switch (this.horizontalAlignContent) {
//		case ALIGN_CENTER:
//			this.alignedX = this.x - this.width / 2;
//			break;
//
//		case ALIGN_RIGHT:
//			this.alignedX = this.x - this.width;
//			break;
//
//		case ALIGN_LEFT:
//			this.alignedX = this.x;
//			break;
//		}
//
//		switch (this.verticalAlignContent) {
//		case ALIGN_CENTER:
//			this.alignedY = this.y - this.textSampleDescent - this.textSampleAscent / 2;
//			break;
//
//		case ALIGN_TOP:
//			this.alignedY = this.y - this.textSampleDescent - this.textSampleAscent;
//			break;
//
//		case ALIGN_BOTTOM:
//			this.alignedY = this.y - this.textSampleDescent;
//			break;
//		}

		Mat4 modelMat4 = Mat4.scale(this.maxTextWidth, this.textMaxHeight, 1).mul(Mat4.translate(new Vec3(this.alignedX + this.textHorizontalMargin, alignedY + this.textVerticalMargin - this.textSampleDescent, this.z + 1)));
		this.textTextureMaterial.setTexture(this.generateAlignedTexture(), TextureMaterial.DIFFUSE);
		this.updateModelInstance(this.textRectangleID, modelMat4);
		
		if(this.backgroundRectangle != null) {
			this.backgroundRectangle.setFrameAlignmentOffset(this.alignedX, this.alignedY);
			this.backgroundRectangle.setWidth(this.width);
			this.backgroundRectangle.setHeight(this.height);
			
			this.backgroundRectangle.align();
		}
	}

	private Texture generateAlignedTexture() {
		BufferedImage img = new BufferedImage(this.maxTextWidth, this.textMaxHeight, BufferedImage.TYPE_INT_ARGB);
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
			alignedX = this.maxTextWidth - this.textWidth;
		}

		g.drawString(text, alignedX, this.textSampleAscent);

		return new Texture(img, false, false, true);
	}

	public void setText(String text) {
		if (this.text.equals(text)) {
			return;
		}

		this.text = text;
		this.textWidth = GraphicsTools.calculateTextWidth(this.text, this.font);

		this.align();
	}

	public void setTextMaterial(Material material) {
		this.updateModelInstance(this.textRectangleID, material);
	}
	
	public void setBackgroundMaterial(Material material) {
		if(this.backgroundRectangle != null) {
			this.backgroundRectangle.setMaterial(material);
		}
	}
	
	public void setDrawBackgroundRectangle(boolean b) {
		this.drawBackgroundRectangle = b;
		if(this.drawBackgroundRectangle) {
			this.backgroundRectangle = new UIFilledRectangle(this.x, this.y, this.z, this.width, this.height, this.scene);
			this.backgroundRectangle.setFrameAlignmentStyle(FROM_LEFT, FROM_BOTTOM);
			this.backgroundRectangle.setContentAlignmentStyle(ALIGN_LEFT, ALIGN_BOTTOM);
			
			this.backgroundRectangle.setMaterial(new Material(new Vec4(0)));
		}
		else {
			if(this.backgroundRectangle != null) {
				this.backgroundRectangle.kill();
				this.backgroundRectangle = null;
			}
		}
	}
	
	public void setMargin(int margin) {
		this.textHorizontalMargin = margin;
		this.textVerticalMargin = margin;
		
		this.width = this.textWidth + this.textHorizontalMargin * 2;
		this.height = this.textSampleAscent + this.textVerticalMargin * 2;
		
		this.align();
	}

	@Override
	protected void __kill() {
		this.textRectangle.kill();
		
		if(this.backgroundRectangle != null) {
			this.backgroundRectangle.kill();
		}
	}

	@Override
	public void update() {

	}

}
