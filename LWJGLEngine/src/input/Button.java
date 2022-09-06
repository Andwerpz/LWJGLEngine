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
	private Material pressedMaterial, releasedMaterial;
	
	public Button(float x, float y, float width, float height, Texture releasedTexture, Texture pressedTexture, int scene) {
		super();
		
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		
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
		this.addModelInstance(model, modelMat, scene);
	}
	
	@Override
	protected void _kill() {
		this.model.kill();
	}

	@Override
	public void update() {
		// TODO Auto-generated method stub
		
	}

}
