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
	//utilizes java.fx to draw text onto a texture, which it then draws onto the screen using a filled rectangle
	
	private Model rectangle;
	private long rectangleID;
	private TextureMaterial textureMaterial;
	
	private int x, y, z, textHeight, textWidth;
	private int textMaxHeight, textAscent, textMaxDescent;
	private int scene;
	
	private Material material;
	private Font font;
	private int fontSize;
	
	public Text(int x, int y, String text, int fontSize, Material material, int scene) {
		super();
		this.init(text, new Font("Dialogue", Font.PLAIN, fontSize), material, x, y, 0, scene);
	}
	
	public Text(int x, int y, String text, Font font, int fontSize, Material material, int scene) {
		super();
		this.init(text, FontUtils.deriveSize(fontSize, font), material, x, y, 0, scene);
	}
	
	public Text(int x, int y, String text, Font font, Color color, int scene) {
		super();
		this.init(text, font, new Material(color), x, y, 0, scene);
	}
	
	public Text(int x, int y, String text, Font font, int fontSize, Color color, int scene) {
		super();
		this.init(text, FontUtils.deriveSize(fontSize, font), new Material(color), x, y, 0, scene);
	}
	
	public Text(int x, int y, int z, String text, Font font, Material material, int scene) {
		super();
		this.init(text, font, material, x, y, z, scene);
	}
	
	public Text(int x, int y, String text, Font font, Material material, int scene) {
		super();
		this.init(text, font, material, x, y, 0, scene);
	}
	
	private void init(String text, Font font, Material material, int x, int y, int z, int scene) {
		this.font = font;
		this.fontSize = font.getSize();
		this.x = x;
		this.y = y;
		this.z = z;
		this.material = material;
		this.textWidth = GraphicsTools.calculateTextWidth(text, font);
		this.textHeight = GraphicsTools.getFontHeight(font);
		
		int textMaxAscent = GraphicsTools.getFontMaxAscent(font);
		this.textMaxDescent = GraphicsTools.getFontMaxDescent(font);
		this.textMaxHeight = textMaxAscent + textMaxDescent;
		
		this.textAscent = GraphicsTools.getFontAscent(font);
		
		BufferedImage img = new BufferedImage(textWidth, textMaxHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics gImg = img.getGraphics();
		gImg.setFont(font);
		gImg.setColor(Color.WHITE);	//adjusting the material can get you any color
		GraphicsTools.enableAntialiasing(gImg);
		gImg.drawString(text, 0, textMaxHeight);
		
		Texture texture = new Texture(img, false, false, true);
		
		this.textureMaterial = new TextureMaterial(texture);
		
		Mat4 modelMat4 = Mat4.scale(textWidth, textMaxHeight, 1).mul(Mat4.translate(new Vec3(x, y, z)));
		
		this.rectangle = new FilledRectangle();
		this.rectangle.setTextureMaterial(textureMaterial);
		this.rectangleID = this.addModelInstance(this.rectangle, modelMat4, scene);
		this.updateModelInstance(this.rectangleID, material);
	}
	
	//center the text around a point
	public void center(int x, int y) {
		int newX = x - textWidth / 2;
		int newY = y - this.textMaxDescent - this.textAscent / 2;
		this.x = newX;
		this.y = newY;
		
		Mat4 modelMat4 = Mat4.scale(textWidth, textMaxHeight, 1).mul(Mat4.translate(new Vec3(this.x, this.y, this.z)));
		
		this.updateModelInstance(this.rectangleID, modelMat4);
	}
	
	public void setMaterial(Material material) {
		this.material = material;
		this.updateModelInstance(this.rectangleID, this.material);
	}

	@Override
	protected void _kill() {
		this.rectangle.kill();
	}

	@Override
	public void update() {
		
	}
	
}
