package ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import entity.Entity;
import graphics.Texture;
import graphics.TextureMaterial;
import model.Model;
import util.GraphicsTools;
import util.Mat4;
import util.Vec3;

public class Text extends Entity {
	//utilizes java.fx to draw text onto a texture, which it then draws onto the screen using a filled rectangle
	
	private Model rectangle;
	private TextureMaterial textureMaterial;
	
	private int x, y, fontSize, textWidth;
	private int scene;
	
	private Color color;
	
	public Text(String text, int fontSize, Color color, int x, int y, int scene) {
		this.fontSize = fontSize;
		this.x = x;
		this.y = y;
		this.color = color;
		
		Font font = new Font("Dialogue", Font.PLAIN, fontSize);
		this.textWidth = GraphicsTools.calculateTextWidth(text, font);
		
		BufferedImage img = new BufferedImage(textWidth, fontSize, BufferedImage.TYPE_INT_ARGB);
		Graphics gImg = img.getGraphics();
		gImg.setFont(font);
		gImg.setColor(color);
		GraphicsTools.enableAntialiasing(gImg);
		gImg.drawString(text, 0, fontSize);
		
		Texture texture = new Texture(img, false, false, true);
		
		this.textureMaterial = new TextureMaterial(texture);
		
		Mat4 modelMat4 = Mat4.scale(textWidth, fontSize, 1).mul(Mat4.translate(new Vec3(x, y, 0)));
		
		this.rectangle = new FilledRectangle();
		this.rectangle.setTextureMaterial(textureMaterial);
		Model.addInstance(this.rectangle, modelMat4, scene);
	}

	@Override
	protected void _kill() {
		this.rectangle.kill();
	}

	@Override
	public void update() {
		
	}
	
}
