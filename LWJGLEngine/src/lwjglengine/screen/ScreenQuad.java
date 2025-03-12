package lwjglengine.screen;

import static org.lwjgl.opengl.GL11.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import lwjglengine.graphics.Material;
import lwjglengine.main.Main;
import lwjglengine.model.Model;
import lwjglengine.model.ModelInstance;
import lwjglengine.model.ModelTransform;
import lwjglengine.model.VertexArray;
import lwjglengine.scene.Scene;
import myutils.math.Mat4;
import myutils.math.Vec2;
import myutils.math.Vec3;

public class ScreenQuad extends Model {
	// a quad that covers the entire screen in normalized device coords (NDC)
	// used for rendering buffers to the screen
	
	//if you want to manipulate the screen quad to render to different portions of the screen; dont do it this way
	//use glViewport(xOffset, yOffset, width, height) instead. 
	//much easier
	
	//this class is really jank lol, probably try to find a better solution?

	public static ScreenQuad screenQuad = new ScreenQuad();

	public ScreenQuad() {
		super(createMesh());
		ModelInstance inst = new ModelInstance(this, Scene.FRAMEBUFFER_SCENE);
	}
	
	public static VertexArray createMesh() {
		//@formatter:off
		float[] vertices = new float[] { -1f, -1f, -0f, -1f, 1f, -0f, 1f, 1f, -0f, 1f, -1f, -0f, };
		float[] uvs = new float[] { 0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, };
		int[] indices = new int[] { 0, 3, 2, 0, 2, 1, };
		//@formatter:on
		return new VertexArray(vertices, uvs, indices, GL_TRIANGLES);
	}
	
	public void render() {
		this.meshes.get(0).render(Scene.FRAMEBUFFER_SCENE);
	}
}
