package model;

import static org.lwjgl.opengl.GL11.*;

import java.util.ArrayList;
import java.util.HashMap;

import graphics.VertexArray;
import util.Mat4;

public class ScreenQuad {
	
	//a quad that covers the entire screen in normalized device coords (NDC)
	//used for post-processing
	
	private VertexArray mesh;

	public ScreenQuad() {
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
		
		this.mesh = new VertexArray(vertices, uvs, indices, GL_TRIANGLES);
		HashMap<Long, Mat4> map = new HashMap<>();
		map.put((long) 0, Mat4.identity());
		this.mesh.updateInstances(map);
	}
	
	//used in main to render framebuffers
	public void render() {
		this.mesh.render();
	}

}