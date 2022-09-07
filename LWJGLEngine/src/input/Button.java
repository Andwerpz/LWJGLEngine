package input;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL33.*;

import entity.Entity;
import graphics.Material;
import graphics.Texture;
import graphics.VertexArray;
import model.Model;
import util.Mat4;
import util.Vec3;

public class Button extends Entity {
	
	//for now, each button has it's own button model. Later, i would like it so that all buttons
	//of the same shape and size can share one model, or something like that. 
	
	//the button isn't responsible for checking if it is pressed, another class, probably ButtonManager
	//or InputManager should do that, and swap textures. 
	
	private Model model;
	
	private float x, y, width, height;	//x, y, specifies bottom left corner of button
	private Material pressedMaterial, releasedMaterial, hoveredMaterial;
	
	private boolean pressed, hovered, clicked;
	
	private long modelInstanceID;
	
	public Button(float x, float y, float width, float height, Texture releasedTexture, Texture pressedTexture, int scene) {
		super();
		
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.pressed = false;
		this.hovered = false;
		this.clicked = false;
		
		//create button model
		float[] vertices = new float[] {
			0, 0, 0,
			width, 0, 0,
			width, height, 0,
			0, height, 0,
		};
		
		float[] uvs = new float[] {
			0, 0,
			1, 0,
			1, 1,
			0, 1,
		};
		
		int[] indices = new int[] {
			0, 1, 2,
			0, 2, 3,
		};
		
		VertexArray vao = new VertexArray(vertices, uvs, indices, GL_TRIANGLES);
		
		//create button materials
		this.releasedMaterial = new Material(releasedTexture, null, null, null);
		this.pressedMaterial = new Material(pressedTexture, null, null, null);
		
		this.model = new Model(vao, releasedMaterial);
		Mat4 modelMat = Mat4.translate(new Vec3(x, y, 0));
		this.modelInstanceID = this.addModelInstance(model, modelMat, scene);
	}
	
	@Override
	protected void _kill() {
		this.removeModelInstance(this.modelInstanceID);
		this.model.kill();
	}

	@Override
	public void update() {
		if(this.clicked) {	//check for clicks happens when mouse is released. 
			this.clicked = false;
		}
		
		if(this.pressed) {
			
		}
		else if(this.hovered) {
			
		}
		else {
			
		}
	}
	
	public void pressed() {
		this.pressed = true;
	}
	
	public void released() {
		if(this.pressed) {
			this.clicked = true;
		}
		this.pressed = false;
	}
	
	public boolean isClicked() {
		return this.clicked;
	}
}
