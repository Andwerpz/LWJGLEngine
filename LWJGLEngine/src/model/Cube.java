package model;

import static org.lwjgl.opengl.GL11.*;

import java.util.ArrayList;

import graphics.VertexArray;

public class Cube extends Model {
	
	public Cube() {
		super();
	}
	
	@Override
	public void create() {
		float[] vertices = new float[] {
			0f, 1f, 0f,
			0f, 1f, 1f,
			1f, 1f, 1f,
			1f, 1f, 0f,
			0f, 0f, 1f,
			0f, 0f, 0f,
			1f, 0f, 0f,
			1f, 0f, 1f,
			0f, 0f, 0f,
			0f, 1f, 0f,
			1f, 1f, 0f,
			1f, 0f, 0f,
			1f, 0f, 1f,
			1f, 1f, 1f,
			0f, 1f, 1f,
			0f, 0f, 1f,
			0f, 0f, 1f,
			0f, 1f, 1f,
			0f, 1f, 0f,
			0f, 0f, 0f,
			1f, 0f, 0f,
			1f, 1f, 0f,
			1f, 1f, 1f,
			1f, 0f, 1f,
		};
		
		int[] indices = new int[] {
			0, 1, 2,
			0, 2, 3,
			4, 5, 6,
			4, 6, 7,
			8, 9, 10,
			8, 10, 11,
			12, 13, 14,
			12, 14, 15,
			16, 17, 18,
			16, 18, 19,
			20, 21, 22,
			20, 22, 23,
		};
		
		float[] tex = new float[] {
			0f, 0f,
			0f, 1f,
			1f, 1f,
			1f, 0f,
			0f, 0f,
			0f, 1f,
			1f, 1f,
			1f, 0f,
			0f, 0f,
			0f, 1f,
			1f, 1f,
			1f, 0f,
			0f, 0f,
			0f, 1f,
			1f, 1f,
			1f, 0f,
			0f, 0f,
			0f, 1f,
			1f, 1f,
			1f, 0f,
			0f, 0f,
			0f, 1f,
			1f, 1f,
			1f, 0f,
		};
		
		this.meshes.add(new VertexArray(vertices, tex, indices, GL_TRIANGLES));
		this.materials.add(null);
	}
}
