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

public class Text extends Entity {
    // utilizes java.fx to draw text onto a texture, which it then draws onto the
    // screen using a filled rectangle

    // width of text is fixed, text will be cut off if it grows too wide.

    private long rectangleID;

    private int x, y, z, width, height;
    private int textWidth, textMaxHeight, textMaxDescent, textMaxAscent, textSampleAscent, textSampleDescent;
    private int scene;

    private Material material;

    private Model textRectangle;
    private TextureMaterial textTextureMaterial;
    private String text;
    private Font font;
    private int fontSize;

    public Text(int x, int y, String text, int fontSize, Material material, int scene) {
	super();
	Font derivedFont = new Font("Dialogue", Font.PLAIN, fontSize);
	this.init(x, y, 0, GraphicsTools.calculateTextWidth(text, derivedFont), text, derivedFont, material, scene);
    }

    public Text(int x, int y, String text, Font font, int fontSize, Material material, int scene) {
	super();
	Font derivedFont = FontUtils.deriveSize(fontSize, font);
	this.init(x, y, 0, GraphicsTools.calculateTextWidth(text, derivedFont), text, derivedFont, material, scene);
    }

    public Text(int x, int y, String text, Font font, Color color, int scene) {
	super();
	this.init(x, y, 0, GraphicsTools.calculateTextWidth(text, font), text, font, new Material(color), scene);
    }

    public Text(int x, int y, String text, Font font, int fontSize, Color color, int scene) {
	super();
	Font derivedFont = FontUtils.deriveSize(fontSize, font);
	this.init(x, y, 0, GraphicsTools.calculateTextWidth(text, derivedFont), text, derivedFont, new Material(color),
		scene);
    }

    public Text(int x, int y, int z, String text, Font font, Material material, int scene) {
	super();
	this.init(x, y, z, GraphicsTools.calculateTextWidth(text, font), text, font, material, scene);
    }

    public Text(int x, int y, int z, int width, String text, Font font, Material material, int scene) {
	super();
	this.init(x, y, z, width, text, font, material, scene);
    }

    public Text(int x, int y, String text, Font font, Material material, int scene) {
	super();
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

	this.textSampleAscent = GraphicsTools.getFontSampleAscent(font);
	this.textSampleDescent = GraphicsTools.getFontSampleDescent(font);

	this.textMaxAscent = Math.max(GraphicsTools.getFontMaxAscent(font), this.textSampleAscent);
	this.textMaxDescent = Math.max(GraphicsTools.getFontMaxDescent(font), this.textSampleDescent);
	this.textMaxHeight = textMaxAscent + textMaxDescent;

	this.height = textMaxHeight;

	BufferedImage img = GraphicsTools.generateTextImage(text, font, Color.WHITE, this.width);
	Texture texture = new Texture(img, false, false, true);
	this.textTextureMaterial = new TextureMaterial(texture);

	Mat4 modelMat4 = Mat4.scale(this.width, textMaxHeight, 1)
		.mul(Mat4.translate(new Vec3(x, y - this.textMaxDescent, z)));

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

    // center the text around a point
    public void center(int x, int y) {
	int newX = x - this.textWidth / 2;
	int newY = y - this.textMaxDescent - this.textSampleAscent / 2;
	this.x = newX;
	this.y = newY;

	Mat4 modelMat4 = Mat4.scale(this.width, textMaxHeight, 1).mul(Mat4.translate(new Vec3(this.x, this.y, this.z)));
	this.updateModelInstance(this.rectangleID, modelMat4);
    }

    public void verticalCenter(int x, int y) {
	int newX = x;
	int newY = y - this.textMaxDescent - this.textSampleAscent / 2;
	this.x = newX;
	this.y = newY;

	Mat4 modelMat4 = Mat4.scale(this.width, textMaxHeight, 1).mul(Mat4.translate(new Vec3(this.x, this.y, this.z)));
	this.updateModelInstance(this.rectangleID, modelMat4);
    }

    public void setPos(int x, int y) {
	int newX = x;
	int newY = y - this.textMaxDescent;
	this.x = newX;
	this.y = newY;

	Mat4 modelMat4 = Mat4.scale(this.width, textMaxHeight, 1).mul(Mat4.translate(new Vec3(this.x, this.y, this.z)));
	this.updateModelInstance(this.rectangleID, modelMat4);
    }

    public void setText(String text) {
	if (this.text.equals(text)) {
	    return;
	}
	this.text = text;
	this.textWidth = GraphicsTools.calculateTextWidth(this.text, font);

	BufferedImage img = GraphicsTools.generateTextImage(text, font, Color.WHITE, this.width);
	Texture texture = new Texture(img, false, false, true);
	this.textTextureMaterial.setTexture(texture, TextureMaterial.DIFFUSE);
	this.textRectangle.setTextureMaterial(this.textTextureMaterial);
    }

    public void setMaterial(Material material) {
	if (material == this.material) {
	    return;
	}
	this.material = material;
	this.updateModelInstance(this.rectangleID, this.material);
    }

    @Override
    protected void _kill() {
	this.textRectangle.kill();
    }

    @Override
    public void update() {

    }

}
