package model;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;

import graphics.TextureMaterial;
import graphics.VertexArray;

public class Decal extends Model {

	public Decal() {
		super();
	}

	@Override
	public void create() {
		float[] vertices = new float[] { -0.5f, -0.5f, 0, 0.5f, -0.5f, 0, 0.5f, 0.5f, 0, -0.5f, 0.5f, 0, };

		float[] uvs = new float[] { 0, 0, 1, 0, 1, 1, 0, 1, };

		int[] indices = new int[] { 0, 1, 2, 0, 2, 3, };

		this.meshes.add(new VertexArray(vertices, uvs, indices, GL_TRIANGLES));
		this.defaultMaterials.add(DEFAULT_MATERIAL);
		this.textureMaterials.add(TextureMaterial.defaultMaterial());
	}

}
