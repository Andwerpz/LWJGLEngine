package lwjglengine.v10.screen;

import static org.lwjgl.opengl.GL11.*;

import java.util.ArrayList;
import java.util.HashMap;

import lwjglengine.v10.graphics.Material;
import lwjglengine.v10.graphics.VertexArray;
import lwjglengine.v10.main.Main;
import lwjglengine.v10.scene.Scene;
import myutils.v10.math.Mat4;
import myutils.v10.math.Vec3;

public class ScreenQuad {

	// a quad that covers the entire screen in normalized device coords (NDC)
	// used for post-processing

	private VertexArray mesh;

	public ScreenQuad() {
		float[] vertices = new float[] { -1f, -1f, -0f, -1f, 1f, -0f, 1f, 1f, -0f, 1f, -1f, -0f, };

		float[] uvs = new float[] { 0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, };

		int[] indices = new int[] { 0, 3, 2, 0, 2, 1, };

		this.mesh = new VertexArray(vertices, uvs, indices, GL_TRIANGLES);
		HashMap<Long, Mat4> mat4Map = new HashMap<>();
		mat4Map.put((long) 0, Mat4.identity());
		HashMap<Long, Material> materialMap = new HashMap<>();
		materialMap.put((long) 0, Material.defaultMaterial());
		this.mesh.updateInstances(mat4Map, materialMap, Scene.FRAMEBUFFER_SCENE);
	}

	public void render(int xOffset, int yOffset, int width, int height) {
		Mat4 modelMat4 = Mat4.scale((float) width / Main.windowWidth, (float) height / Main.windowHeight, 1);
		modelMat4.muli(Mat4.translate(new Vec3((float) (xOffset - Main.windowWidth + width) / Main.windowWidth, (float) (yOffset - Main.windowHeight + height) / Main.windowHeight, 0)));

		HashMap<Long, Mat4> mat4Map = new HashMap<>();
		mat4Map.put((long) 0, modelMat4);
		HashMap<Long, Material> materialMap = new HashMap<>();
		materialMap.put((long) 0, Material.defaultMaterial());

		this.mesh.updateInstances(mat4Map, materialMap, Scene.FRAMEBUFFER_SCENE);

		this.mesh.render(Scene.FRAMEBUFFER_SCENE);
	}

	public void render() {
		this.render(0, 0, Main.windowWidth, Main.windowHeight);
	}

}
