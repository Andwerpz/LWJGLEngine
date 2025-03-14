package lwjglengine.model;

import static org.lwjgl.opengl.GL11.*;

import lwjglengine.graphics.TextureMaterial;
import myutils.math.Mat4;
import myutils.math.Vec2;
import myutils.math.Vec3;

public class FilledRectangle extends Model {
	// as the name suggests, this rectangle is indeed filled
	// you can render any rectangle with the transformation of the rectangle (0, 0, 0) to (1, 1, 0).

	public static final FilledRectangle DEFAULT_RECTANGLE = new FilledRectangle();

	public FilledRectangle() {
		super(createMesh());
	}

	private static VertexArray createMesh() {
		//@formatter:off
		float[] vertices = new float[] { 
				0, 0, 0, 
				1, 0, 0, 
				1, 1, 0, 
				0, 1, 0, 
		};
		float[] uvs = new float[] { 
				0, 0, 
				1, 0, 
				1, 1, 
				0, 1, 
		};
		int[] indices = new int[] { 
				0, 1, 2,
				0, 2, 3,
		};
		//@formatter:on
		return new VertexArray(vertices, uvs, indices, GL_TRIANGLES);
	}

	public ModelInstance addRectangle(float x, float y, float z, float width, float height, int scene) {
		ModelTransform transform = FilledRectangle.generateRectangleModelTransform(x, y, z, width, height);
		return new ModelInstance(this, transform, scene);
	}

	public ModelInstance addRectangle(float x, float y, float width, float height, int scene) {
		return this.addRectangle(x, y, 0, width, height, scene);
	}

	public ModelInstance addRectangle(Vec2 bl, Vec2 tr, float z, int scene) {
		float width = tr.x - bl.x;
		float height = tr.y - bl.y;
		return this.addRectangle(bl.x, bl.y, z, width, height, scene);
	}

	public static ModelTransform generateRectangleModelTransform(float x, float y, float z, float width, float height) {
		ModelTransform transform = new ModelTransform();
		transform.setScale(new Vec3(width, height, 1));
		transform.setTranslation(new Vec3(x, y, z));
		return transform;
	}

	public static ModelTransform generateRectangleModelTransform(Vec2 bl, Vec2 tr, float z) {
		float width = tr.x - bl.x;
		float height = tr.y - bl.y;
		return FilledRectangle.generateRectangleModelTransform(bl.x, bl.y, z, width, height);
	}

	public static ModelInstance addDefaultRectangle(float x, float y, float z, float width, float height, int scene) {
		return DEFAULT_RECTANGLE.addRectangle(x, y, z, width, height, scene);
	}

	public static ModelInstance addDefaultRectangle(float x, float y, float width, float height, int scene) {
		return DEFAULT_RECTANGLE.addRectangle(x, y, 0, width, height, scene);
	}

	public static ModelInstance addDefaultRectangle(Vec2 bl, Vec2 tr, float z, int scene) {
		return DEFAULT_RECTANGLE.addRectangle(bl, tr, z, scene);
	}

	public static ModelInstance addDefaultRectangle(int scene) {
		return DEFAULT_RECTANGLE.addRectangle(0, 0, 0, 1, 1, scene);
	}

	public TextureMaterial getTextureMaterial() {
		return this.textureMaterials.get(0);
	}

}
