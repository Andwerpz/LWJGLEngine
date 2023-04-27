package lwjglengine.v10.screen;

import static org.lwjgl.opengl.GL11.*;

import java.util.ArrayList;
import java.util.HashMap;

import lwjglengine.v10.graphics.Material;
import lwjglengine.v10.graphics.VertexArray;
import lwjglengine.v10.main.Main;
import lwjglengine.v10.scene.Scene;
import myutils.v10.math.Mat4;
import myutils.v10.math.Vec2;
import myutils.v10.math.Vec3;

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
		HashMap<Long, Mat4> mat4Map = new HashMap<>();
		mat4Map.put((long) 0, Mat4.identity());
		HashMap<Long, Material> materialMap = new HashMap<>();
		materialMap.put((long) 0, Material.defaultMaterial());
		this.mesh.updateInstances(mat4Map, materialMap, Scene.FRAMEBUFFER_SCENE);
	}

	//if you want to manipulate the screen quad to render to different portions of the screen; dont do it this way

	//use glViewport(xOffset, yOffset, width, height) instead. 
	//much easier

	public void render() {
		this.mesh.render(Scene.FRAMEBUFFER_SCENE);
	}

}
