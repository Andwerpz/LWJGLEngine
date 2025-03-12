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

public class SkyboxCube extends Model {
	public static SkyboxCube skyboxCube = new SkyboxCube();

	public SkyboxCube() {
		super(createMesh());
		ModelInstance inst = new ModelInstance(this, Scene.FRAMEBUFFER_SCENE);
	}
	
	public static VertexArray createMesh() {
		//@formatter:off
		float[] vertices = new float[] { 
				-1f, 1f, -1f, 
				-1f, 1f, 1f, 
				1f, 1f, 1f, 
				1f, 1f, -1f, 
				-1f, -1f, 1f, 
				-1f, -1f, -1f, 
				1f, -1f, -1f, 
				1f, -1f, 1f, 
				-1f, -1f, -1f, 
				-1f, 1f, -1f, 
				1f, 1f, -1f, 
				1f, -1f, -1f, 
				1f, -1f, 1f, 
				1f, 1f, 1f, 
				-1f, 1f, 1f, 
				-1f, -1f, 1f, 
				-1f, -1f, 1f, 
				-1f, 1f, 1f, 
				-1f, 1f, -1f,
				-1f, -1f, -1f, 
				1f, -1f, -1f, 
				1f, 1f, -1f, 
				1f, 1f, 1f, 
				1f, -1f, 1f, 
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
				0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 
				0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 
				0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 
				0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 
				0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 
				0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 
		};
		//@formatter:on
		return new VertexArray(vertices, tex, indices, GL_TRIANGLES);
	}

	public void render() {
		this.meshes.get(0).render(Scene.FRAMEBUFFER_SCENE);
	}
}
