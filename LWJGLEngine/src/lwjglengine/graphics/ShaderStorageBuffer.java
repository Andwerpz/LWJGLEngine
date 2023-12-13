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
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL33.*;

public class ShaderStorageBuffer {
	//utility class for opengl SSBOs 

	private int ssbo;

	public ShaderStorageBuffer() {
		this.ssbo = glGenBuffers();
	}

	public void bind() {
		glBindBuffer(GL_SHADER_STORAGE_BUFFER, this.ssbo);
	}

	public void unbind() {
		glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
	}

	public void setData(int[] data) {
		this.bind();
		glBufferData(GL_SHADER_STORAGE_BUFFER, data, GL_STATIC_READ);
	}

	public void bindToBase(int which) {
		glBindBufferBase(GL_SHADER_STORAGE_BUFFER, which, this.ssbo);
	}

	public void kill() {
		glDeleteBuffers(this.ssbo);
	}

}
