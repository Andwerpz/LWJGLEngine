package lwjglengine.screen;

import static org.lwjgl.opengl.GL11.*;

import java.util.ArrayList;
import java.util.HashMap;

import lwjglengine.graphics.Material;
import lwjglengine.main.Main;
import lwjglengine.model.ModelTransform;
import lwjglengine.model.VertexArray;
import lwjglengine.scene.Scene;
import myutils.v10.math.Mat4;

public class SkyboxCube {

	public static SkyboxCube skyboxCube = new SkyboxCube();

	private VertexArray mesh;

	public SkyboxCube() {
		float[] vertices = new float[] { -1f, 1f, -1f, -1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, -1f, -1f, -1f, 1f, -1f, -1f, -1f, 1f, -1f, -1f, 1f, -1f, 1f, -1f, -1f, -1f, -1f, 1f, -1f, 1f, 1f, -1f, 1f, -1f, -1f, 1f, -1f, 1f, 1f, 1f, 1f, -1f, 1f, 1f, -1f, -1f, 1f, -1f, -1f, 1f, -1f, 1f, 1f, -1f, 1f, -1f,
				-1f, -1f, -1f, 1f, -1f, -1f, 1f, 1f, -1f, 1f, 1f, 1f, 1f, -1f, 1f, };

		int[] indices = new int[] { 0, 1, 2, 0, 2, 3, 4, 5, 6, 4, 6, 7, 8, 9, 10, 8, 10, 11, 12, 13, 14, 12, 14, 15, 16, 17, 18, 16, 18, 19, 20, 21, 22, 20, 22, 23, };

		float[] tex = new float[] { 0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, };

		this.mesh = new VertexArray(vertices, tex, indices, GL_TRIANGLES);

		ArrayList<Long> ids = new ArrayList<>();
		ids.add(0L);
		ArrayList<ModelTransform> transforms = new ArrayList<>();
		transforms.add(new ModelTransform());
		ArrayList<Material> materials = new ArrayList<>();
		materials.add(Material.defaultMaterial());

		this.mesh.updateInstances(ids, transforms, materials, Scene.FRAMEBUFFER_SCENE);
	}

	public void render() {
		this.mesh.render(Scene.FRAMEBUFFER_SCENE);
	}
}
