package lwjglengine.model;

import static org.lwjgl.opengl.GL11.*;

import lwjglengine.graphics.Material;
import lwjglengine.graphics.TextureMaterial;
import myutils.math.Mat4;
import myutils.math.Vec3;

public class Rectangle extends Model {
	// this rectangle is not filled, it's just a wireframe.
	// you can render any rectangle with the transformation of the rectangle (0, 0) to (1, 1).

	private static Rectangle rectangle = new Rectangle();

	public Rectangle() {
		super(createMesh());
	}
	
	private static VertexArray createMesh() {
		float[] vertices = new float[] { 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, };
		float[] uvs = new float[] { 0, 0, 1, 0, 1, 1, 0, 1, };
		int[] indices = new int[] { 1, 0, 1, 2, 1, 2, 3, 2, 3, 0, 3, 0, };
		return new VertexArray(vertices, uvs, indices, GL_LINES);
	}

	public static ModelInstance addRectangle(float x, float y, float width, float height, int scene) {
		ModelTransform transform = new ModelTransform();
		transform.setScale(new Vec3(width, height, 1));
		transform.setTranslation(new Vec3(x, y, 0));
		return new ModelInstance(Rectangle.rectangle, transform, scene);
	}

}
