package lwjglengine.screen;

import static org.lwjgl.opengl.GL11.*;

import java.util.ArrayList;
import java.util.HashMap;

import lwjglengine.graphics.Material;
import lwjglengine.main.Main;
import lwjglengine.model.ModelTransform;
import lwjglengine.model.VertexArray;
import lwjglengine.scene.Scene;
import myutils.math.Mat4;
import myutils.math.Vec2;
import myutils.math.Vec3;

public class ScreenQuad {
	// a quad that covers the entire screen in normalized device coords (NDC)
	// used for rendering buffers to the screen

	public static ScreenQuad screenQuad = new ScreenQuad();

	private VertexArray mesh;

	public ScreenQuad() {
		float[] vertices = new float[] { -1f, -1f, -0f, -1f, 1f, -0f, 1f, 1f, -0f, 1f, -1f, -0f, };

		float[] uvs = new float[] { 0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, };

		int[] indices = new int[] { 0, 3, 2, 0, 2, 1, };

		this.mesh = new VertexArray(vertices, uvs, indices, GL_TRIANGLES);

		ArrayList<Long> ids = new ArrayList<>();
		ids.add(0L);
		ArrayList<ModelTransform> transforms = new ArrayList<>();
		transforms.add(new ModelTransform());
		ArrayList<Material> materials = new ArrayList<>();
		materials.add(Material.defaultMaterial());

		this.mesh.updateInstances(ids, transforms, materials, Scene.FRAMEBUFFER_SCENE);
	}

	//if you want to manipulate the screen quad to render to different portions of the screen; dont do it this way

	//use glViewport(xOffset, yOffset, width, height) instead. 
	//much easier

	public void render() {
		this.mesh.render(Scene.FRAMEBUFFER_SCENE);
	}

}
