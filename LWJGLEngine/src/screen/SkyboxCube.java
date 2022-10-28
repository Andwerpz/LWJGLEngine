package screen;

import static org.lwjgl.opengl.GL11.*;

import java.util.ArrayList;
import java.util.HashMap;

import graphics.Material;
import graphics.VertexArray;
import main.Main;
import scene.Scene;
import util.Mat4;

public class SkyboxCube {

	private VertexArray mesh;

	public SkyboxCube() {
		float[] vertices = new float[] { -1f, 1f, -1f, -1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, -1f, -1f, -1f, 1f, -1f, -1f, -1f, 1f, -1f, -1f, 1f, -1f, 1f, -1f, -1f, -1f, -1f, 1f, -1f, 1f, 1f, -1f, 1f, -1f,
			-1f, 1f, -1f, 1f, 1f, 1f, 1f, -1f, 1f, 1f, -1f, -1f, 1f, -1f, -1f, 1f, -1f, 1f, 1f, -1f, 1f, -1f, -1f, -1f, -1f, 1f, -1f, -1f, 1f, 1f, -1f, 1f, 1f, 1f, 1f, -1f, 1f, };

		int[] indices = new int[] { 0, 1, 2, 0, 2, 3, 4, 5, 6, 4, 6, 7, 8, 9, 10, 8, 10, 11, 12, 13, 14, 12, 14, 15, 16, 17, 18, 16, 18, 19, 20, 21, 22, 20, 22, 23, };

		float[] tex = new float[] { 0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 0f,
			0f, 0f, 1f, 1f, 1f, 1f, 0f, };

		this.mesh = new VertexArray(vertices, tex, indices, GL_TRIANGLES);
		HashMap<Long, Mat4> mat4Map = new HashMap<>();
		mat4Map.put((long) 0, Mat4.identity());
		HashMap<Long, Material> materialMap = new HashMap<>();
		materialMap.put((long) 0, Material.defaultMaterial());
		this.mesh.updateInstances(mat4Map, materialMap, Scene.FRAMEBUFFER_SCENE);
	}

	public void render() {
		this.mesh.render(Scene.FRAMEBUFFER_SCENE);
	}
}
