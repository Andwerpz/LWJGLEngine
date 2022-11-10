package graphics;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL33.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import model.Model;
import scene.Scene;
import util.BufferUtils;
import util.Mat4;
import util.Pair;
import util.Vec2;
import util.Vec3;

public class VertexArray {

	// vertex array buffers and buffer objects aren't created under each other,
	// but a buffer object can be bound to a vertex array object.

	// glBindVertexArray(vao);
	// glBindBuffer(GL_ARRAY_BUFFER, vbo); //binds vbo to GL_ARRAY_BUFFER
	// glEnableVertexAttribArray(VERTEX_ATTRIB); //uses whatever is currently inside GL_ARRAY_BUFFER

	// each scene will have different model mat4s for each vertex array.

	// vertex array information location when passed into shaders
	public static final int VERTEX_ATTRIB = 0;
	public static final int TCOORD_ATTRIB = 1;
	public static final int NORMAL_ATTRIB = 2;
	public static final int TANGENT_ATTRIB = 3;
	public static final int BITANGENT_ATTRIB = 4;
	public static final int INSTANCED_MODEL_ATTRIB = 5; // takes up 4 slots
	public static final int INSTANCED_COLOR_ATTRIB = 9; // used for quick model selection
	public static final int INSTANCED_MATERIAL_ATTRIB = 10; // takes up 3 slots

	private int renderType;
	private int vao, vbo, tbo, nbo, ntbo, nbtbo, ibo;
	private HashMap<Integer, int[]> scenes; // numInstances, mat4, colorID, material
	private int triCount; // number of triangles in the mesh

	// don't edit these, these are just for future reference
	private float[] vertices, normals, tangents, bitangents, uvs;
	private int[] indices;

	public VertexArray(float[] vertices, float[] normals, float[] uvs, int[] indices, int renderType) {
		int n = vertices.length;
		float[] tangents = new float[n], bitangents = new float[n];
		computeTB(vertices, normals, uvs, indices, tangents, bitangents);
		this.init(vertices, normals, tangents, bitangents, uvs, indices, renderType);
	}

	public VertexArray(float[] vertices, float[] uvs, int[] indices, int renderType) {
		int n = vertices.length;
		float[] normals = new float[n], tangents = new float[n], bitangents = new float[n];
		computeTBN(vertices, uvs, indices, normals, tangents, bitangents);
		this.init(vertices, normals, tangents, bitangents, uvs, indices, renderType);
	}

	//vertices, indices, and render type must not be null. 
	private void init(float[] vertices, float[] normals, float[] tangents, float[] bitangents, float[] uvs, int[] indices, int renderType) {
		this.vertices = vertices;
		this.normals = normals;
		this.tangents = tangents;
		this.bitangents = bitangents;
		this.uvs = uvs;
		this.indices = indices;

		this.renderType = renderType;
		this.triCount = indices.length;
		this.scenes = new HashMap<Integer, int[]>();

		vao = glGenVertexArrays();
		glBindVertexArray(vao);

		vbo = glGenBuffers(); // vertices
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		glBufferData(GL_ARRAY_BUFFER, BufferUtils.createFloatBuffer(vertices), GL_STATIC_DRAW);
		glVertexAttribPointer(VERTEX_ATTRIB, 3, GL_FLOAT, false, 0, 0);
		glEnableVertexAttribArray(VERTEX_ATTRIB);

		tbo = glGenBuffers(); // uvs
		glBindBuffer(GL_ARRAY_BUFFER, tbo);
		glBufferData(GL_ARRAY_BUFFER, BufferUtils.createFloatBuffer(uvs), GL_STATIC_DRAW);
		glVertexAttribPointer(TCOORD_ATTRIB, 2, GL_FLOAT, false, 0, 0);
		glEnableVertexAttribArray(TCOORD_ATTRIB);

		nbo = glGenBuffers(); // normals
		glBindBuffer(GL_ARRAY_BUFFER, nbo);
		glBufferData(GL_ARRAY_BUFFER, BufferUtils.createFloatBuffer(normals), GL_STATIC_DRAW);
		glVertexAttribPointer(NORMAL_ATTRIB, 3, GL_FLOAT, false, 0, 0);
		glEnableVertexAttribArray(NORMAL_ATTRIB);

		ntbo = glGenBuffers(); // tangents
		glBindBuffer(GL_ARRAY_BUFFER, ntbo);
		glBufferData(GL_ARRAY_BUFFER, BufferUtils.createFloatBuffer(tangents), GL_STATIC_DRAW);
		glVertexAttribPointer(TANGENT_ATTRIB, 3, GL_FLOAT, false, 0, 0);
		glEnableVertexAttribArray(TANGENT_ATTRIB);

		nbtbo = glGenBuffers(); // bitangents
		glBindBuffer(GL_ARRAY_BUFFER, nbtbo);
		glBufferData(GL_ARRAY_BUFFER, BufferUtils.createFloatBuffer(bitangents), GL_STATIC_DRAW);
		glVertexAttribPointer(BITANGENT_ATTRIB, 3, GL_FLOAT, false, 0, 0);
		glEnableVertexAttribArray(BITANGENT_ATTRIB);

		ibo = glGenBuffers();
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, BufferUtils.createIntBuffer(indices), GL_STATIC_DRAW);

		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindVertexArray(0);
	}

	//copies over the information in the given vertex array to this one
	public void set(VertexArray v) {
		this.killVertexBuffers();
		this.init(v.getVertices(), v.getNormals(), v.getTangents(), v.getBitangents(), v.getUVs(), v.getIndices(), v.getRenderType());
	}

	// seperate methods for updating materials and updating model mats??
	// but updating instances is the way that we can get rid of them...
	// there has to be one method for updating both, no other way around it.

	// orr, we can have a seperate method for removing a model instance.
	// but then, we'd have to modify the entire buffer data anyways.
	public void updateInstances(HashMap<Long, Mat4> mat4Map, HashMap<Long, Material> materialMap, int whichScene) {
		int numInstances = mat4Map.size();
		Mat4[] modelMats = new Mat4[numInstances];
		Vec3[] colorIDs = new Vec3[numInstances];
		Material[] materials = new Material[numInstances];
		int i = 0;
		for (long ID : mat4Map.keySet()) {
			colorIDs[i] = Model.convertIDToRGB(ID);
			modelMats[i] = mat4Map.get(ID);
			materials[i] = materialMap.get(ID);
			i++;
		}

		if (scenes.get(whichScene) == null) {
			// instanced model buffer doesn't exist yet
			scenes.put(whichScene, new int[] { numInstances, glGenBuffers(), glGenBuffers(), glGenBuffers() });
		}
		int[] scene = scenes.get(whichScene);
		scene[0] = numInstances;
		int modelMatBuffer = scene[1];
		int colorIDBuffer = scene[2];
		int materialBuffer = scene[3];

		glBindBuffer(GL_ARRAY_BUFFER, modelMatBuffer);
		glBufferData(GL_ARRAY_BUFFER, BufferUtils.createFloatBuffer(modelMats), GL_DYNAMIC_DRAW); // TODO switch to glBufferSubData
		glBindBuffer(GL_ARRAY_BUFFER, colorIDBuffer);
		glBufferData(GL_ARRAY_BUFFER, BufferUtils.createFloatBuffer(colorIDs), GL_DYNAMIC_DRAW); // TODO switch to glBufferSubData
		glBindBuffer(GL_ARRAY_BUFFER, materialBuffer);
		glBufferData(GL_ARRAY_BUFFER, BufferUtils.createFloatBuffer(materials), GL_DYNAMIC_DRAW); // TODO switch to glBufferSubData
	}

	public void bindScene(int whichScene) {
		if (scenes.get(whichScene) == null) {
			System.err.println("SCREEN " + whichScene + " MODEL NOT INSTANTIATED");
			return;
		}
		int[] scene = scenes.get(whichScene);
		int modelMatBuffer = scene[1];
		int colorIDBuffer = scene[2];
		int materialBuffer = scene[3];

		// TODO figure out whether or not we need to configure the pointer gaps and
		// offsets every time we want to bind a new buffer
		glBindVertexArray(vao);
		glBindBuffer(GL_ARRAY_BUFFER, modelMatBuffer);
		for (int i = 0; i < 4; i++) {
			glVertexAttribPointer(INSTANCED_MODEL_ATTRIB + i, 4, GL_FLOAT, false, 16 * 4, 16 * i);
			glVertexAttribDivisor(INSTANCED_MODEL_ATTRIB + i, 1);
			glEnableVertexAttribArray(INSTANCED_MODEL_ATTRIB + i);
		}

		glBindBuffer(GL_ARRAY_BUFFER, colorIDBuffer);
		glVertexAttribPointer(INSTANCED_COLOR_ATTRIB, 3, GL_FLOAT, false, 0, 0);
		glVertexAttribDivisor(INSTANCED_COLOR_ATTRIB, 1);
		glEnableVertexAttribArray(INSTANCED_COLOR_ATTRIB);

		glBindBuffer(GL_ARRAY_BUFFER, materialBuffer);
		glVertexAttribPointer(INSTANCED_MATERIAL_ATTRIB + 0, 4, GL_FLOAT, false, 9 * 4, 0); // diffuse : 4 floats
		glVertexAttribPointer(INSTANCED_MATERIAL_ATTRIB + 1, 4, GL_FLOAT, false, 9 * 4, 16); // specular : 4 floats
		glVertexAttribPointer(INSTANCED_MATERIAL_ATTRIB + 2, 1, GL_FLOAT, false, 9 * 4, 32); // shininess : 1 float
		glVertexAttribDivisor(INSTANCED_MATERIAL_ATTRIB + 0, 1);
		glVertexAttribDivisor(INSTANCED_MATERIAL_ATTRIB + 1, 1);
		glVertexAttribDivisor(INSTANCED_MATERIAL_ATTRIB + 2, 1);
		glEnableVertexAttribArray(INSTANCED_MATERIAL_ATTRIB + 0);
		glEnableVertexAttribArray(INSTANCED_MATERIAL_ATTRIB + 1);
		glEnableVertexAttribArray(INSTANCED_MATERIAL_ATTRIB + 2);

		glBindVertexArray(0);
	}

	public static void computeTB(float[] vertices, float[] normals, float[] uvs, int[] indices, float[] outTangents, float[] outBitangents) {
		int n = vertices.length / 3;
		Vec3[] tangents = new Vec3[n];
		Vec3[] bitangents = new Vec3[n];
		float[] angWeights = new float[indices.length]; // each face has 3 angles

		for (int i = 0; i < n; i++) {
			tangents[i] = new Vec3(0);
			bitangents[i] = new Vec3(0);
		}

		// ANGLE WEIGHTS
		for (int i = 0; i < indices.length; i += 3) {
			int a = indices[i];
			int b = indices[i + 1];
			int c = indices[i + 2];

			Vec3 va = new Vec3(vertices[a * 3], vertices[a * 3 + 1], vertices[a * 3 + 2]);
			Vec3 vb = new Vec3(vertices[b * 3], vertices[b * 3 + 1], vertices[b * 3 + 2]);
			Vec3 vc = new Vec3(vertices[c * 3], vertices[c * 3 + 1], vertices[c * 3 + 2]);

			Vec3 ab = new Vec3(va, vb);
			ab.normalize();
			Vec3 ac = new Vec3(va, vc);
			ac.normalize();

			Vec3 ba = new Vec3(vb, va);
			ba.normalize();
			Vec3 bc = new Vec3(vb, vc);
			bc.normalize();

			Vec3 ca = new Vec3(vc, va);
			ca.normalize();
			Vec3 cb = new Vec3(vc, vb);
			cb.normalize();

			float angA = (float) Math.acos(ab.dot(ac));
			float angB = (float) Math.acos(ba.dot(bc));
			float angC = (float) Math.acos(ca.dot(cb));

			angWeights[i] = angA;
			angWeights[i + 1] = angB;
			angWeights[i + 2] = angC;
		}

		// TANGENTS & BITANGENTS
		for (int i = 0; i < indices.length; i += 3) {
			int a = indices[i + 0];
			int b = indices[i + 1];
			int c = indices[i + 2];

			Vec3 va = new Vec3(vertices[a * 3], vertices[a * 3 + 1], vertices[a * 3 + 2]);
			Vec3 vb = new Vec3(vertices[b * 3], vertices[b * 3 + 1], vertices[b * 3 + 2]);
			Vec3 vc = new Vec3(vertices[c * 3], vertices[c * 3 + 1], vertices[c * 3 + 2]);

			Vec2 uva = new Vec2(uvs[a * 2], uvs[a * 2 + 1]);
			Vec2 uvb = new Vec2(uvs[b * 2], uvs[b * 2 + 1]);
			Vec2 uvc = new Vec2(uvs[c * 2], uvs[c * 2 + 1]);

			Vec3 edge1 = new Vec3(va, vb);
			Vec3 edge2 = new Vec3(va, vc);
			Vec2 deltaUV1 = new Vec2(uva, uvb);
			Vec2 deltaUV2 = new Vec2(uva, uvc);

			Vec3 tangent = new Vec3(0);
			Vec3 bitangent = new Vec3(0);

			float f = 1.0f / (deltaUV1.x * deltaUV2.y - deltaUV2.x * deltaUV1.y);

			tangent.x = f * (deltaUV2.y * edge1.x - deltaUV1.y * edge2.x);
			tangent.y = f * (deltaUV2.y * edge1.y - deltaUV1.y * edge2.y);
			tangent.z = f * (deltaUV2.y * edge1.z - deltaUV1.y * edge2.z);

			bitangent.x = f * (-deltaUV2.x * edge1.x + deltaUV1.x * edge2.x);
			bitangent.y = f * (-deltaUV2.x * edge1.y + deltaUV1.x * edge2.y);
			bitangent.z = f * (-deltaUV2.x * edge1.z + deltaUV1.x * edge2.z);

			tangents[a].addi(tangent.mul(angWeights[i + 0]));
			tangents[b].addi(tangent.mul(angWeights[i + 1]));
			tangents[c].addi(tangent.mul(angWeights[i + 2]));

			bitangents[a].addi(bitangent.mul(angWeights[i + 0]));
			bitangents[b].addi(bitangent.mul(angWeights[i + 1]));
			bitangents[c].addi(bitangent.mul(angWeights[i + 2]));
		}

		for (int i = 0; i < n; i++) {
			Vec3 tangent = tangents[i];
			tangent.normalize();
			outTangents[i * 3] = tangent.x;
			outTangents[i * 3 + 1] = tangent.y;
			outTangents[i * 3 + 2] = tangent.z;

			Vec3 bitangent = bitangents[i];
			bitangent.normalize();
			outBitangents[i * 3] = bitangent.x;
			outBitangents[i * 3 + 1] = bitangent.y;
			outBitangents[i * 3 + 2] = bitangent.z;
		}
	}

	// for each vertex, it's normal is the weighted average of all the normals of
	// the planes it touches.
	// weights are based on the angle
	public static void computeTBN(float[] vertices, float[] uvs, int[] indices, float[] outNormals, float[] outTangents, float[] outBitangents) {
		int n = vertices.length / 3;
		Vec3[] normals = new Vec3[n];
		Vec3[] tangents = new Vec3[n];
		Vec3[] bitangents = new Vec3[n];
		float[] angWeights = new float[indices.length]; // each face has 3 angles

		for (int i = 0; i < n; i++) {
			normals[i] = new Vec3(0);
			tangents[i] = new Vec3(0);
			bitangents[i] = new Vec3(0);
		}

		// NORMALS
		for (int i = 0; i < indices.length; i += 3) {
			int a = indices[i];
			int b = indices[i + 1];
			int c = indices[i + 2];

			// System.out.println(a + " " + b + " " + c);

			Vec3 va = new Vec3(vertices[a * 3], vertices[a * 3 + 1], vertices[a * 3 + 2]);
			Vec3 vb = new Vec3(vertices[b * 3], vertices[b * 3 + 1], vertices[b * 3 + 2]);
			Vec3 vc = new Vec3(vertices[c * 3], vertices[c * 3 + 1], vertices[c * 3 + 2]);

			Vec3 ab = new Vec3(va, vb);
			ab.normalize();
			Vec3 ac = new Vec3(va, vc);
			ac.normalize();

			Vec3 ba = new Vec3(vb, va);
			ba.normalize();
			Vec3 bc = new Vec3(vb, vc);
			bc.normalize();

			Vec3 ca = new Vec3(vc, va);
			ca.normalize();
			Vec3 cb = new Vec3(vc, vb);
			cb.normalize();

			Vec3 cross = ab.cross(ac);

			float angA = (float) Math.acos(ab.dot(ac));
			float angB = (float) Math.acos(ba.dot(bc));
			float angC = (float) Math.acos(ca.dot(cb));

			angWeights[i] = angA;
			angWeights[i + 1] = angB;
			angWeights[i + 2] = angC;

			// System.out.println(angA + " " + angB + " " + angC);

			normals[a].addi(cross.mul(angA));
			normals[b].addi(cross.mul(angB));
			normals[c].addi(cross.mul(angC));

			// System.out.println(normals[a] + " " + normals[b] + " " + normals[c]);
		}

		for (int i = 0; i < n; i++) {
			Vec3 next = normals[i];
			next.normalize();
			outNormals[i * 3] = next.x;
			outNormals[i * 3 + 1] = next.y;
			outNormals[i * 3 + 2] = next.z;
		}

		// TANGENTS & BITANGENTS
		for (int i = 0; i < indices.length; i += 3) {
			int a = indices[i + 0];
			int b = indices[i + 1];
			int c = indices[i + 2];

			Vec3 va = new Vec3(vertices[a * 3], vertices[a * 3 + 1], vertices[a * 3 + 2]);
			Vec3 vb = new Vec3(vertices[b * 3], vertices[b * 3 + 1], vertices[b * 3 + 2]);
			Vec3 vc = new Vec3(vertices[c * 3], vertices[c * 3 + 1], vertices[c * 3 + 2]);

			Vec2 uva = new Vec2(uvs[a * 2], uvs[a * 2 + 1]);
			Vec2 uvb = new Vec2(uvs[b * 2], uvs[b * 2 + 1]);
			Vec2 uvc = new Vec2(uvs[c * 2], uvs[c * 2 + 1]);

			Vec3 edge1 = new Vec3(va, vb);
			Vec3 edge2 = new Vec3(va, vc);
			Vec2 deltaUV1 = new Vec2(uva, uvb);
			Vec2 deltaUV2 = new Vec2(uva, uvc);

			Vec3 tangent = new Vec3(0);
			Vec3 bitangent = new Vec3(0);

			float f = 1.0f / (deltaUV1.x * deltaUV2.y - deltaUV2.x * deltaUV1.y);

			tangent.x = f * (deltaUV2.y * edge1.x - deltaUV1.y * edge2.x);
			tangent.y = f * (deltaUV2.y * edge1.y - deltaUV1.y * edge2.y);
			tangent.z = f * (deltaUV2.y * edge1.z - deltaUV1.y * edge2.z);

			bitangent.x = f * (-deltaUV2.x * edge1.x + deltaUV1.x * edge2.x);
			bitangent.y = f * (-deltaUV2.x * edge1.y + deltaUV1.x * edge2.y);
			bitangent.z = f * (-deltaUV2.x * edge1.z + deltaUV1.x * edge2.z);

			tangents[a].addi(tangent.mul(angWeights[i + 0]));
			tangents[b].addi(tangent.mul(angWeights[i + 1]));
			tangents[c].addi(tangent.mul(angWeights[i + 2]));

			bitangents[a].addi(bitangent.mul(angWeights[i + 0]));
			bitangents[b].addi(bitangent.mul(angWeights[i + 1]));
			bitangents[c].addi(bitangent.mul(angWeights[i + 2]));
		}

		for (int i = 0; i < n; i++) {
			Vec3 tangent = tangents[i];
			tangent.normalize();
			outTangents[i * 3] = tangent.x;
			outTangents[i * 3 + 1] = tangent.y;
			outTangents[i * 3 + 2] = tangent.z;

			Vec3 bitangent = bitangents[i];
			bitangent.normalize();
			outBitangents[i * 3] = bitangent.x;
			outBitangents[i * 3 + 1] = bitangent.y;
			outBitangents[i * 3 + 2] = bitangent.z;
		}

	}

	public float[] getVertices() {
		return this.vertices;
	}

	public int[] getIndices() {
		return this.indices;
	}

	public float[] getUVs() {
		return this.uvs;
	}

	public float[] getBitangents() {
		return this.bitangents;
	}

	public float[] getTangents() {
		return this.tangents;
	}

	public float[] getNormals() {
		return this.normals;
	}

	public int getRenderType() {
		return this.renderType;
	}

	public void bind() {
		glBindVertexArray(vao);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
	}

	public void unbind() {
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
		glBindVertexArray(0);
	}

	public void drawInstanced(int amt) {
		glDrawElementsInstanced(this.renderType, this.triCount, GL_UNSIGNED_INT, 0, amt);
	}

	public void render(int whichScene) {
		if (this.scenes.get(whichScene) == null) { // TODO fix this
			return;
		}
		int numInstances = this.scenes.get(whichScene)[0];
		bindScene(whichScene);
		bind();
		drawInstanced(numInstances);
		unbind();
	}

	//doesn't take care of model instance ids, that must be done on the model side. 
	public void kill() {
		this.killVertexBuffers();
		this.killInstanceBuffers();
	}

	private void killVertexBuffers() {
		glDeleteVertexArrays(new int[] { this.vao });
		glDeleteBuffers(new int[] { this.vbo, this.nbo, this.tbo, this.ntbo, this.nbtbo, this.ibo });
	}

	private void killInstanceBuffers() {
		for (int[] i : this.scenes.values()) {
			glDeleteBuffers(new int[] { i[1], i[2], i[3] });
		}
	}
}
