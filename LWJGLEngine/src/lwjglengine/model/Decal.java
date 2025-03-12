package lwjglengine.model;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;

import lwjglengine.graphics.TextureMaterial;

public class Decal extends Model {

	public Decal() {
		super(createMesh());
	}
	
	private static VertexArray createMesh() {
		//@formatter:off
		float[] vertices = new float[] { 
			0, 0, 0, 
			1, 0, 0, 
			1, 1, 0, 
			0, 1, 0,
			0, 0, 1, 
			1, 0, 1, 
			1, 1, 1, 
			0, 1, 1,
		};

		float[] uvs = new float[] { 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, };

		int[] indices = new int[] { 
			1, 0, 2, 
			2, 0, 3, 
			4, 5, 6,
			4, 6, 7,
			0, 4, 7,
			0, 7, 3,
			3, 7, 6, 
			3, 6, 2,
			2, 6, 5,
			2, 5, 1,
			1, 5, 4,
			1, 4, 0
		};
		//@formatter:on
		return new VertexArray(vertices, uvs, indices, GL_TRIANGLES);
	}

}
