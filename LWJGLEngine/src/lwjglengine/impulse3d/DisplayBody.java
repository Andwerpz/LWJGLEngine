package lwjglengine.impulse3d;

import static org.lwjgl.opengl.GL11.GL_LINES;

import java.util.ArrayList;

import lwjglengine.impulse3d.shape.Capsule;
import lwjglengine.impulse3d.shape.KDOP;
import lwjglengine.model.Model;
import lwjglengine.model.ModelInstance;
import lwjglengine.model.ModelTransform;
import lwjglengine.model.VertexArray;
import myutils.math.Mat4;
import myutils.math.MathUtils;
import myutils.math.Vec2;
import myutils.math.Vec3;

public class DisplayBody {
	Model wireframe = null;
	ModelInstance wmi = null; //wireframe model instance

	Body body;
	ModelInstance[] mi;
	Mat4[] baseTransform;

	public DisplayBody(Body _body, boolean generateWireframes, int WIREFRAME_SCENE, ModelInstance... _mi) {
		this.body = _body;
		this.mi = _mi;
		this.baseTransform = new Mat4[_mi.length];
		for (int i = 0; i < this.mi.length; i++) {
			this.baseTransform[i] = new Mat4(this.mi[i].getModelTransform().getModelMatrix());
		}

		if (generateWireframes) {
			switch (this.body.shape.type) {
			case KDOP:
				this.wireframe = generateKDOPWireframe((KDOP) this.body.shape);
				this.wmi = new ModelInstance(this.wireframe, WIREFRAME_SCENE);
				break;

			case CAPSULE:
				this.wireframe = generateCapsuleWireframe((Capsule) this.body.shape);
				this.wmi = new ModelInstance(this.wireframe, WIREFRAME_SCENE);
				break;
			}
		}
	}

	public void updateModelInstance() {
		for (int i = 0; i < this.mi.length; i++) {
			Mat4 transform = new Mat4(this.baseTransform[i]);
			transform.muli(MathUtils.quaternionToRotationMat4(this.body.orient));
			transform.muli(Mat4.translate(this.body.pos));
			this.mi[i].setModelTransform(new ModelTransform(transform));
		}

		if (this.wireframe != null) {
			Mat4 transform = MathUtils.quaternionToRotationMat4(this.body.orient);
			transform.muli(Mat4.translate(this.body.pos));
			this.wmi.setModelTransform(new ModelTransform(transform));
		}
	}

	public void kill() {
		for (ModelInstance m : mi) {
			m.kill();
		}

		if (this.wireframe != null) {
			this.wmi.kill();
			this.wireframe.kill();
		}
	}

	private static Model generateCapsuleWireframe(Capsule capsule) {
		int endcap_resolution = 20;
		Vec3[] cap2d = new Vec3[(endcap_resolution + 1) * 2];
		for (int i = 0; i <= endcap_resolution; i++) {
			float ang = (float) (Math.PI / 2.0 - Math.PI * ((float) i / (float) endcap_resolution));
			Vec2 dir = new Vec2(1, 0);
			dir.rotate(ang);
			cap2d[i] = new Vec3(0, dir.y * capsule.radius, dir.x * capsule.radius + capsule.length / 2.0f);
		}
		for (int i = 0; i <= endcap_resolution; i++) {
			float ang = (float) (-Math.PI / 2.0 - Math.PI * ((float) i / (float) endcap_resolution));
			Vec2 dir = new Vec2(1, 0);
			dir.rotate(ang);
			cap2d[i + (endcap_resolution + 1)] = new Vec3(0, dir.y * capsule.radius, dir.x * capsule.radius - capsule.length / 2.0f);
		}

		int nr_cap2d = 6;
		ArrayList<Vec3> vertex_list = new ArrayList<>();
		ArrayList<Integer> index_list = new ArrayList<>();
		for (int i = 0; i < nr_cap2d; i++) {
			float ang = (float) Math.PI * ((float) i / (float) nr_cap2d);
			for (int j = 0; j < cap2d.length; j++) {
				Vec3 v = new Vec3(cap2d[j]);
				v.rotateZ(ang);
				vertex_list.add(v);

				index_list.add(j + cap2d.length * i);
				index_list.add((j + 1) % cap2d.length + cap2d.length * i);
			}
		}

		float[] vertices = new float[vertex_list.size() * 3];
		int[] indices = new int[index_list.size()];
		for (int i = 0; i < vertex_list.size(); i++) {
			vertices[i * 3 + 0] = vertex_list.get(i).x;
			vertices[i * 3 + 1] = vertex_list.get(i).y;
			vertices[i * 3 + 2] = vertex_list.get(i).z;
		}
		for (int i = 0; i < index_list.size(); i++) {
			indices[i] = index_list.get(i);
		}

		VertexArray wire_va = new VertexArray(vertices, indices, GL_LINES);
		return new Model(wire_va);
	}

	private Model generateKDOPWireframe(KDOP kdop) {
		ArrayList<Vec3> vertex_list = new ArrayList<>();
		ArrayList<Integer> index_list = new ArrayList<>();
		Vec3[][] faces = kdop.getFaces();
		for (Vec3[] f : faces) {
			int face_start = vertex_list.size();
			for (Vec3 v : f) {
				vertex_list.add(v);
			}
			for (int i = 0; i < f.length; i++) {
				index_list.add(i + face_start);
				index_list.add((i + 1) % f.length + face_start);
			}
		}

		float[] vertices = new float[vertex_list.size() * 3];
		int[] indices = new int[index_list.size()];
		for (int i = 0; i < vertex_list.size(); i++) {
			vertices[i * 3 + 0] = vertex_list.get(i).x;
			vertices[i * 3 + 1] = vertex_list.get(i).y;
			vertices[i * 3 + 2] = vertex_list.get(i).z;
		}
		for (int i = 0; i < index_list.size(); i++) {
			indices[i] = index_list.get(i);
		}

		VertexArray wire_va = new VertexArray(vertices, indices, GL_LINES);
		return new Model(wire_va);
	}
}
