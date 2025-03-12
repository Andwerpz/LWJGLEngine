package lwjglengine.model;

import static org.lwjgl.opengl.GL11.*;

import lwjglengine.graphics.TextureMaterial;
import myutils.math.IVec2;
import myutils.math.Mat4;
import myutils.math.Vec2;
import myutils.math.Vec3;

public class Line extends Model {
	// you can render any line with a linear scaling of the line (0, 0, 0) to (1, 1, 1).

	public static final Line DEFAULT_LINE = new Line();

	public Line() {
		super(createMesh());
	}
	
	private static VertexArray createMesh() {
		float[] vertices = new float[] { 0, 0, 0, 1, 1, 1, };
		float[] uvs = new float[] { 0, 0, 1, 1, };
		int[] indices = new int[] { 0, 1, 0, };// 3 points since everything has to be a triangle
		return new VertexArray(vertices, uvs, indices, GL_LINES);
	}

	public ModelInstance addLine(Vec3 a, Vec3 b, int scene) {
		ModelTransform transform = Line.generateLineModelTransform(a, b);
		return new ModelInstance(this, transform, scene);
	}

	public static ModelTransform generateLineModelTransform(float x1, float y1, float z1, float x2, float y2, float z2) {
		float dx = x2 - x1;
		float dy = y2 - y1;
		float dz = z2 - z1;

		ModelTransform transform = new ModelTransform();
		transform.setScale(new Vec3(dx, dy, dz));
		transform.setTranslation(new Vec3(x1, y1, z1));

		return transform;
	}

	public static ModelTransform generateLineModelTransform(Vec3 a, Vec3 b) {
		return Line.generateLineModelTransform(a.x, a.y, a.z, b.x, b.y, b.z);
	}

	public static ModelTransform generateLineModelTransform(float x1, float y1, float x2, float y2) {
		return Line.generateLineModelTransform(x1, y1, 0, x2, y2, 0);
	}

	public static ModelTransform generateLineModelTransform(Vec2 a, Vec2 b) {
		return Line.generateLineModelTransform(a.x, a.y, b.x, b.y);
	}

	public static ModelTransform generateLineModelTransform(Vec2 a, Vec2 b, float z) {
		return Line.generateLineModelTransform(a.x, a.y, z, b.x, b.y, z);
	}

	public static ModelInstance addDefaultLine(float x1, float y1, float z1, float x2, float y2, float z2, int scene) {
		return DEFAULT_LINE.addLine(new Vec3(x1, y1, z1), new Vec3(x2, y2, z2), scene);
	}

	public static ModelInstance addDefaultLine(int scene) {
		return Line.addDefaultLine(0, 0, 0, 1, 1, 1, scene);
	}

	public static ModelInstance addDefaultLine(Vec3 a, Vec3 b, int scene) {
		return Line.addDefaultLine(a.x, a.y, a.z, b.x, b.y, b.z, scene);
	}

	public static ModelInstance addDefaultLine(float x1, float y1, float x2, float y2, int scene) {
		return Line.addDefaultLine(x1, y1, 0, x2, y2, 0, scene);
	}

	public static ModelInstance addDefaultLine(Vec2 a, Vec2 b, int scene) {
		return Line.addDefaultLine(a.x, a.y, b.x, b.y, scene);
	}

	public static ModelInstance addDefaultLine(IVec2 a, IVec2 b, int scene) {
		return Line.addDefaultLine(new Vec2(a), new Vec2(b), scene);
	}

	public static void setLineModelTransform(float x1, float y1, float z1, float x2, float y2, float z2, ModelInstance instance) {
		ModelTransform transform = Line.generateLineModelTransform(x1, y1, z1, x2, y2, z2);
		instance.setModelTransform(transform);
	}

	public static void setLineModelTransform(Vec3 a, Vec3 b, ModelInstance instance) {
		Line.setLineModelTransform(a.x, a.y, a.z, b.x, b.y, b.z, instance);
	}

	public static void setLineModelTransform(float x1, float y1, float x2, float y2, ModelInstance instance) {
		Line.setLineModelTransform(x1, y1, 0, x2, y2, 0, instance);
	}

	public static void setLineModelTransform(Vec2 a, Vec2 b, ModelInstance instance) {
		Line.setLineModelTransform(a.x, a.y, b.x, b.y, instance);
	}

}
