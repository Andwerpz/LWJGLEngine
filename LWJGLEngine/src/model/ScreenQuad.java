package model;

import static org.lwjgl.opengl.GL11.*;

import java.util.ArrayList;

import graphics.VertexArray;

public class ScreenQuad extends Model{
	
	//a quad that covers the entire screen in normalized device coords (NDC)
	//used for post-processing

	public ScreenQuad() {
		super();
	}
	
	@Override
	public void create() {
		
		float[] vertices = new float[] {
			-1f, -1f, -0f,
			-1f, 1f, -0f,
			1f, 1f, -0f,
			1f, -1f, -0f,
		};
		
		float[] uvs = new float[] {
			0f, 0f,
			0f, 1f,
			1f, 1f,
			1f, 0f,
		};
		
		int[] indices = new int[] {
			0, 3, 2,
			0, 2, 1,
		};
		
		this.meshes.add(new VertexArray(vertices, uvs, indices, GL_TRIANGLES));
		this.materials.add(null);
	}

}
