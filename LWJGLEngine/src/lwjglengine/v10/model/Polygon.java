package lwjglengine.v10.model;

import static org.lwjgl.opengl.GL11.*;

import java.util.ArrayList;

import lwjglengine.v10.graphics.Material;
import lwjglengine.v10.graphics.TextureMaterial;
import myutils.v10.math.Mat4;
import myutils.v10.math.MathUtils;
import myutils.v10.math.Vec2;
import myutils.v10.math.Vec3;

public class Polygon extends Model {
	// render simple polygons
	// simple polygons are polygons that don't have holes and don't intersect themselves. 

	public Polygon(ArrayList<Vec2> points) {
		super(createMesh(points, 1));
	}
	
	public Polygon(ArrayList<Vec2> points, float uvMult) {
		super(createMesh(points, uvMult));
	}

	private static VertexArray createMesh(ArrayList<Vec2> points, float uvMult) {
		if (!MathUtils.isSimplePolygon(points)) {
			System.err.println("CAN'T RENDER NON-SIMPLE POLYGON");
			return null;
		}

		ArrayList<int[]> tris = MathUtils.calculateTrianglePartition(points);

		float[] vertices = new float[points.size() * 3];
		float[] uvs = new float[points.size() * 2];
		int[] indices = new int[tris.size() * 3];

		for (int i = 0; i < points.size(); i++) {
			vertices[i * 3 + 0] = points.get(i).x;
			vertices[i * 3 + 1] = points.get(i).y;
			vertices[i * 3 + 2] = 0;

			uvs[i * 2 + 0] = points.get(i).x * uvMult;
			uvs[i * 2 + 1] = points.get(i).y * uvMult;
		}

		boolean clockwise = !MathUtils.isCounterClockwiseWinding(points);
		for (int i = 0; i < tris.size(); i++) {
			indices[i * 3 + 0] = tris.get(i)[0];
			indices[i * 3 + 1] = tris.get(i)[clockwise ? 2 : 1];
			indices[i * 3 + 2] = tris.get(i)[clockwise ? 1 : 2];
		}

		return new VertexArray(vertices, uvs, indices, GL_TRIANGLES);
	}

}
