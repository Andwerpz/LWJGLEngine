package input;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL33.*;

import java.awt.Color;
import java.awt.Font;

import entity.Entity;
import graphics.TextureMaterial;
import graphics.Material;
import graphics.Texture;
import graphics.VertexArray;
import model.Model;
import scene.Scene;
import ui.FilledRectangle;
import ui.Text;
import util.FontUtils;
import util.GraphicsTools;
import util.Mat4;
import util.Vec3;
import util.Vec4;

public class Button extends Input {
	
	//for now, each button has it's own button model. Later, i would like it so that all buttons
	//of the same shape and size can share one model, or something like that. 
	
	//the button isn't responsible for checking if it is pressed, another class, probably ButtonManager
	//or InputManager should do that, and swap textures. 
	
	private long buttonInnerID;
	private Text buttonText;
	
	private int x, y, z, width, height;	//x, y, specifies bottom left corner of button
	private int scene;
	
	private Material pressedMaterial, releasedMaterial, hoveredMaterial;
	private Material pressedTextMaterial, releasedTextMaterial, hoveredTextMaterial;
	private Material currentMaterial;
	
	private long modelInstanceID;
	
	public Button(int x, int y, int width, int height, String text, Font font, int fontSize, int scene) {
		super();
		this.init(x, y, 0, width, height, text, FontUtils.deriveSize(fontSize, font), scene);
	}
	
	//text size should already be included in the font
	private void init(int x, int y, int z, int width, int height, String text, Font font, int scene) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.width = width;
		this.height = height;
		this.scene = scene;
		
		this.pressedTextMaterial = new Material(Color.YELLOW);
		this.releasedTextMaterial = new Material(Color.WHITE);
		
		//calculate where to put the text in order to center it in the button
		int centerX = this.x + this.width / 2;
		int centerY = this.y + this.height / 2;
		this.buttonText = new Text(0, 0, this.z + 1, text, font, this.releasedTextMaterial, scene);
		this.buttonText.center(centerX, centerY);
		
		this.pressedMaterial = new Material(new Vec4(0, 0, 0, 0.6f));
		this.hoveredMaterial = new Material(new Vec4(0, 0, 0, 0.3f));
		this.releasedMaterial = new Material(new Vec4(0, 0, 0, 0.0f));
		
		this.buttonInnerID = FilledRectangle.addRectangle(this.x, this.y, this.z, this.width, this.height, this.scene);
		this.registerModelInstance(this.buttonInnerID);
		this.updateModelInstance(this.buttonInnerID, releasedMaterial);
		
		this.currentMaterial = this.releasedMaterial;
	}

	@Override
	public void update() {
		Material nextMaterial = null;
		if(this.clicked) {	//check for clicks happens when mouse is released. 
			this.clicked = false;
			nextMaterial = this.pressedMaterial;
		}
		else if(this.pressed) {
			nextMaterial = this.pressedMaterial;
		}
		else if(this.hovered) {
			nextMaterial = this.hoveredMaterial;
		}
		else {
			nextMaterial = this.releasedMaterial;
		}
		if(this.currentMaterial != nextMaterial) {
			this.currentMaterial = nextMaterial;
			this.updateModelInstance(this.buttonInnerID, this.currentMaterial);
		}
	}
	
	@Override
	protected void _kill() {
		this.buttonText.kill();
	}
}
