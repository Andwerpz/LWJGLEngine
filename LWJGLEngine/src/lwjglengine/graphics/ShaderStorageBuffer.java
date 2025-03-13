package lwjglengine.graphics;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL21.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.GL43.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL33.*;

public class ShaderStorageBuffer {
	//utility class for opengl SSBOs 

	//make sure to set the usage before you set the data. 

	private int ssbo;

	private int usage = GL_STATIC_READ;

	public ShaderStorageBuffer() {
		this.ssbo = glGenBuffers();
	}

	public ShaderStorageBuffer(long size) {
		this.ssbo = glGenBuffers();
		this.setSize(size);
	}

	public void bind() {
		glBindBuffer(GL_SHADER_STORAGE_BUFFER, this.ssbo);
	}

	public void unbind() {
		glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
	}

	public int getHandle() {
		return this.ssbo;
	}

	public void setUsage(int usage) {
		this.usage = usage;
	}

	public void setSize(long nrBytes) {
		if (nrBytes < 0) {
			System.err.println("ShaderStorageBuffer : Size cannot be negative");
			return;
		}
		this.bind();
		glBufferData(GL_SHADER_STORAGE_BUFFER, nrBytes, this.usage);
	}

	public long getSize() {
		this.bind();
		IntBuffer ret = BufferUtils.createIntBuffer(1);
		glGetBufferParameteriv(GL_SHADER_STORAGE_BUFFER, GL_BUFFER_SIZE, ret);
		return ret.get();
	}

	public void setData(int[] data) {
		this.bind();
		glBufferData(GL_SHADER_STORAGE_BUFFER, data, this.usage);
	}

	public void setData(float[] data) {
		this.bind();
		glBufferData(GL_SHADER_STORAGE_BUFFER, data, this.usage);
	}

	public void setSubData(int[] data, long byteOffset) {
		this.bind();
		glBufferSubData(GL_SHADER_STORAGE_BUFFER, byteOffset, data);
	}

	public void setSubData(float[] data, long byteOffset) {
		this.bind();
		glBufferSubData(GL_SHADER_STORAGE_BUFFER, byteOffset, data);
	}

	public void getSubData(int[] arr, long byteOffset) {
		this.bind();
		glGetBufferSubData(GL_SHADER_STORAGE_BUFFER, byteOffset, arr);
	}

	public void getSubData(float[] arr, long byteOffset) {
		this.bind();
		glGetBufferSubData(GL_SHADER_STORAGE_BUFFER, byteOffset, arr);
	}

	public void bindToBase(int which) {
		glBindBufferBase(GL_SHADER_STORAGE_BUFFER, which, this.ssbo);
	}

	public void kill() {
		glDeleteBuffers(this.ssbo);
	}

	public void setData(FloatBuffer buf, int byteOffset) {
		this.bind();
		glBufferData(GL_SHADER_STORAGE_BUFFER, buf, this.usage);
	}
	
	public void setData(IntBuffer buf, int byteOffset) {
		this.bind();
		glBufferData(GL_SHADER_STORAGE_BUFFER, buf, this.usage);
	}
	
	public void setSubData(FloatBuffer buf, int byteOffset) {
		this.bind();
		glBufferSubData(GL_SHADER_STORAGE_BUFFER, byteOffset, buf);
	}
	
	public void setSubData(IntBuffer buf, int byteOffset) {
		this.bind();
		glBufferSubData(GL_SHADER_STORAGE_BUFFER, byteOffset, buf);
	}

	

}
