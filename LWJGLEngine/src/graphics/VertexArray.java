package graphics;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL33.*;

import java.util.ArrayList;

import util.BufferUtils;
import util.Mat4;
import util.MathTools;
import util.Vec2;
import util.Vec3;

public class VertexArray {

	protected int renderType;
	protected int vao, vbo, tbo, nbo, ntbo, nbtbo, ibo, mbo;
	protected int count;

	protected int numInstances = 0;
	
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
	
	private void init(float[] vertices, float[] normals, float[] tangents, float[] bitangents, float[] uvs, int[] indices, int renderType) {
		this.renderType = renderType;
		count = indices.length;

		vao = glGenVertexArrays();
		glBindVertexArray(vao);

		vbo = glGenBuffers(); // vertices
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		glBufferData(GL_ARRAY_BUFFER, BufferUtils.createFloatBuffer(vertices), GL_STATIC_DRAW);
		glVertexAttribPointer(Shader.VERTEX_ATTRIB, 3, GL_FLOAT, false, 0, 0);
		glEnableVertexAttribArray(Shader.VERTEX_ATTRIB);

		tbo = glGenBuffers(); // textures
		glBindBuffer(GL_ARRAY_BUFFER, tbo);
		glBufferData(GL_ARRAY_BUFFER, BufferUtils.createFloatBuffer(uvs), GL_STATIC_DRAW);
		glVertexAttribPointer(Shader.TCOORD_ATTRIB, 2, GL_FLOAT, false, 0, 0);
		glEnableVertexAttribArray(Shader.TCOORD_ATTRIB);

		nbo = glGenBuffers(); // normals
		glBindBuffer(GL_ARRAY_BUFFER, nbo);
		glBufferData(GL_ARRAY_BUFFER, BufferUtils.createFloatBuffer(normals), GL_STATIC_DRAW);
		glVertexAttribPointer(Shader.NORMAL_ATTRIB, 3, GL_FLOAT, false, 0, 0);
		glEnableVertexAttribArray(Shader.NORMAL_ATTRIB);
		
		ntbo = glGenBuffers(); 	//tangents
		glBindBuffer(GL_ARRAY_BUFFER, ntbo);
		glBufferData(GL_ARRAY_BUFFER, BufferUtils.createFloatBuffer(tangents), GL_STATIC_DRAW);
		glVertexAttribPointer(Shader.TANGENT_ATTRIB, 3, GL_FLOAT, false, 0, 0);
		glEnableVertexAttribArray(Shader.TANGENT_ATTRIB);
		
		nbtbo = glGenBuffers(); //bitangents
		glBindBuffer(GL_ARRAY_BUFFER, nbtbo);
		glBufferData(GL_ARRAY_BUFFER, BufferUtils.createFloatBuffer(bitangents), GL_STATIC_DRAW);
		glVertexAttribPointer(Shader.BITANGENT_ATTRIB, 3, GL_FLOAT, false, 0, 0);
		glEnableVertexAttribArray(Shader.BITANGENT_ATTRIB);
		
		mbo = glGenBuffers(); // per instance model matrix
		glBindBuffer(GL_ARRAY_BUFFER, mbo);
		glBufferData(GL_ARRAY_BUFFER, BufferUtils.createFloatBuffer(new float[0]), GL_STREAM_DRAW);
		for (int i = 0; i < 4; i++) {
			glVertexAttribPointer(Shader.INSTANCED_MODEL_ATTRIB + i, 4, GL_FLOAT, false, 16 * 4, 16 * i);
			glVertexAttribDivisor(Shader.INSTANCED_MODEL_ATTRIB + i, 1);
			glEnableVertexAttribArray(Shader.INSTANCED_MODEL_ATTRIB + i);
		}

		ibo = glGenBuffers();
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, BufferUtils.createIntBuffer(indices), GL_STATIC_DRAW);

		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindVertexArray(0);
	}

	public void updateModelMats(Mat4[] modelMats) {
		numInstances = modelMats.length;
		glBindVertexArray(vao);
		glBindBuffer(GL_ARRAY_BUFFER, mbo);
		glBufferData(GL_ARRAY_BUFFER, BufferUtils.createFloatBuffer(modelMats), GL_STREAM_DRAW);
		glBindVertexArray(0);
	}
	
	public void updateModelMats(ArrayList<Mat4> modelMats) {
		this.updateModelMats(modelMats.toArray(new Mat4[modelMats.size()]));
	}
	
	public static void computeTB(float[] vertices, float[] normals, float[] uvs, int[] indices, float[] outTangents, float[] outBitangents) {
		int n = vertices.length / 3;
		Vec3[] tangents = new Vec3[n];
		Vec3[] bitangents = new Vec3[n];
		float[] angWeights = new float[indices.length];	//each face has 3 angles
		
		for (int i = 0; i < n; i++) {
			tangents[i] = new Vec3(0);
			bitangents[i] = new Vec3(0);
		}
		
		//ANGLE WEIGHTS
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

			float angA = (float) Math.acos(MathTools.dotProduct(ab, ac));
			float angB = (float) Math.acos(MathTools.dotProduct(ba, bc));
			float angC = (float) Math.acos(MathTools.dotProduct(ca, cb));
			
			angWeights[i] = angA;
			angWeights[i + 1] = angB;
			angWeights[i + 2] = angC;
		}
		
		//TANGENTS & BITANGENTS
		for(int i = 0; i < indices.length; i += 3) {
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
		float[] angWeights = new float[indices.length];	//each face has 3 angles
		
		for (int i = 0; i < n; i++) {
			normals[i] = new Vec3(0);
			tangents[i] = new Vec3(0);
			bitangents[i] = new Vec3(0);
		}
		
		//NORMALS
		for (int i = 0; i < indices.length; i += 3) {
			int a = indices[i];
			int b = indices[i + 1];
			int c = indices[i + 2];
			
			//System.out.println(a + " " + b + " " + c);

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

			Vec3 cross = MathTools.crossProduct(ab, ac);

			float angA = (float) Math.acos(MathTools.dotProduct(ab, ac));
			float angB = (float) Math.acos(MathTools.dotProduct(ba, bc));
			float angC = (float) Math.acos(MathTools.dotProduct(ca, cb));
			
			angWeights[i] = angA;
			angWeights[i + 1] = angB;
			angWeights[i + 2] = angC;
			
			//System.out.println(angA + " " + angB + " " + angC);

			normals[a].addi(cross.mul(angA));
			normals[b].addi(cross.mul(angB));
			normals[c].addi(cross.mul(angC));
			
			//System.out.println(normals[a] + " " + normals[b] + " " + normals[c]);
		}
		
		for (int i = 0; i < n; i++) {
			Vec3 next = normals[i];
			next.normalize();
			outNormals[i * 3] = next.x;
			outNormals[i * 3 + 1] = next.y;
			outNormals[i * 3 + 2] = next.z;
		}
		
		//TANGENTS & BITANGENTS
		for(int i = 0; i < indices.length; i += 3) {
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

	public void bind() {
		glBindVertexArray(vao);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
	}

	public void unbind() {
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
		glBindVertexArray(0);
	}

	public void drawInstanced(int amt) {
		glDrawElementsInstanced(renderType, count, GL_UNSIGNED_INT, 0, amt);
	}

	public void render() {
		bind();
		drawInstanced(numInstances);
		unbind();
	}
}
