package graphics;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import util.BufferUtils;
import util.MathTools;
import util.Vec3;

public class VertexArray {
	
	private int renderType;
	private int vao, vbo, tbo, nbo, ibo;
	private int count;
	
	public VertexArray(float[] vertices, byte[] indices, float[] texCoordinates, int renderType) {
		float[] normals = computeNormals(vertices, indices);
		this.renderType = renderType;
		count = indices.length;
		
		vao = glGenVertexArrays();
		glBindVertexArray(vao);
		
		vbo = glGenBuffers();	//vertices
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		glBufferData(GL_ARRAY_BUFFER, BufferUtils.createFloatBuffer(vertices), GL_STATIC_DRAW);
		glVertexAttribPointer(Shader.VERTEX_ATTRIB, 3, GL_FLOAT, false, 0, 0);
		glEnableVertexAttribArray(Shader.VERTEX_ATTRIB);
		
		tbo = glGenBuffers();	//textures
		glBindBuffer(GL_ARRAY_BUFFER, tbo);
		glBufferData(GL_ARRAY_BUFFER, BufferUtils.createFloatBuffer(texCoordinates), GL_STATIC_DRAW);
		glVertexAttribPointer(Shader.TCOORD_ATTRIB, 2, GL_FLOAT, false, 0, 0);
		glEnableVertexAttribArray(Shader.TCOORD_ATTRIB);
		
		nbo = glGenBuffers();	//normals
		glBindBuffer(GL_ARRAY_BUFFER, nbo);
		glBufferData(GL_ARRAY_BUFFER, BufferUtils.createFloatBuffer(normals), GL_STATIC_DRAW);
		glVertexAttribPointer(Shader.NORMAL_ATTRIB, 3, GL_FLOAT, false, 0, 0);
		glEnableVertexAttribArray(Shader.NORMAL_ATTRIB);
		
		ibo = glGenBuffers();
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, BufferUtils.createByteBuffer(indices), GL_STATIC_DRAW);
		
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindVertexArray(0);
	}
	
	//for each vertex, it's normal is the weighted average of all the normals of the planes it touches. 
	//weights are based on the angle 
	public static float[] computeNormals(float[] vertices, byte[] indices) {
		int n = vertices.length / 3;
		Vec3[] normals = new Vec3[n];
		
		for(int i = 0; i < n; i++) {
			normals[i] = new Vec3(0);
		}
		
		for(int i = 0; i < indices.length; i += 3) {
			int a = indices[i];
			int b = indices[i + 1];
			int c = indices[i + 2];
			
			Vec3 va = new Vec3(vertices[a * 3], vertices[a * 3 + 1], vertices[a * 3 + 2]);
			Vec3 vb = new Vec3(vertices[b * 3], vertices[b * 3 + 1], vertices[b * 3 + 2]);
			Vec3 vc = new Vec3(vertices[c * 3], vertices[c * 3 + 1], vertices[c * 3 + 2]);
			
			Vec3 ab = new Vec3(va, vb);	ab.normalize();
			Vec3 ac = new Vec3(va, vc);	ac.normalize();
			
			Vec3 ba = new Vec3(vb, va);	ba.normalize();
			Vec3 bc = new Vec3(vb, vc);	bc.normalize();
			
			Vec3 ca = new Vec3(vc, va);	ca.normalize();
			Vec3 cb = new Vec3(vc, vb);	cb.normalize();
			
			Vec3 cross = MathTools.crossProduct(ab, ac);
			
			float angA = (float) Math.acos(MathTools.dotProduct(ab, ac));
			float angB = (float) Math.acos(MathTools.dotProduct(ba, bc));
			float angC = (float) Math.acos(MathTools.dotProduct(ca, cb));
			
			normals[a].addi(cross.mul(angA));
			normals[b].addi(cross.mul(angB));
			normals[c].addi(cross.mul(angC));
		}
		
		float[] ans = new float[n * 3];
		for(int i = 0; i < n; i++) {
			Vec3 next = normals[i];
			next.normalize();
			ans[i * 3] = next.x;
			ans[i * 3 + 1] = next.y;
			ans[i * 3 + 2] = next.z;
		}
		return ans;
	}
	
	public void bind() {
		glBindVertexArray(vao);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
	}
	
	public void unbind() {
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
		glBindVertexArray(0);
	}
	
	public void draw() {
		glDrawElements(renderType, count, GL_UNSIGNED_BYTE, 0);
	}
	
	public void render() {
		bind();
		draw();
		unbind();
	}
	
}
