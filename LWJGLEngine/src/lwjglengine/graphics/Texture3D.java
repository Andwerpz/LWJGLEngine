package lwjglengine.graphics;

import static org.lwjgl.opengl.GL12.GL_TEXTURE_3D;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_WRAP_R;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.opengl.GL42.glTexStorage2D;
import static org.lwjgl.opengl.GL45.glGetTextureLevelParameteriv;
import static org.lwjgl.opengl.GL12.glTexImage3D;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.GL42.glTexStorage2D;
import static org.lwjgl.opengl.GL42.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import lwjglengine.util.BufferUtils;

public class Texture3D {

	private int textureID;

	public Texture3D(int width, int height, int depth) {
		this.textureID = Texture3D.createTexture(GL_RGBA8, width, height, depth, GL_RGBA, GL_UNSIGNED_BYTE, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR);
	}

	public Texture3D(int width, int height, int depth, int[] data) {
		this.textureID = Texture3D.createTexture(data, GL_RGBA8, width, height, depth, GL_RGBA, GL_UNSIGNED_BYTE, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR);
	}

	public int getID() {
		return this.textureID;
	}

	public void bind(int glTextureLocation) {
		glActiveTexture(glTextureLocation);
		glBindTexture(GL_TEXTURE_3D, this.textureID);
	}

	public void unbind(int glTextureLocation) {
		glActiveTexture(glTextureLocation);
		glBindTexture(GL_TEXTURE_3D, 0);
	}

	public int getWidth() {
		int[] ret = new int[1];
		glGetTextureLevelParameteriv(this.getID(), 0, GL_TEXTURE_WIDTH, ret);
		return ret[0];
	}

	public int getHeight() {
		int[] ret = new int[1];
		glGetTextureLevelParameteriv(this.getID(), 0, GL_TEXTURE_HEIGHT, ret);
		return ret[0];
	}

	public int getDepth() {
		int[] ret = new int[1];
		glGetTextureLevelParameteriv(this.getID(), 0, GL_TEXTURE_DEPTH, ret);
		return ret[0];
	}

	public void kill() {
		glDeleteTextures(BufferUtils.createIntBuffer(new int[] { this.textureID }));
	}

	/**
	 * Creates an empty texture and returns the handle
	 * @param internalFormat
	 * @param width
	 * @param height
	 * @param depth
	 * @param dataFormat
	 * @param dataType
	 * @param minSampleType
	 * @param magSampleType
	 * @return
	 */
	public static int createTexture(int internalFormat, int width, int height, int depth, int dataFormat, int dataType, int minSampleType, int magSampleType) {
		int textureID = glGenTextures();
		glBindTexture(GL_TEXTURE_3D, textureID);
		glTexStorage3D(GL_TEXTURE_3D, 0, internalFormat, width, height, depth);
		glTexSubImage3D(GL_TEXTURE_3D, 0, 0, 0, 0, width, height, depth, dataFormat, dataType, (ByteBuffer) null);
		glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, minSampleType);
		glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, magSampleType);
		glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_REPEAT);
		glBindTexture(GL_TEXTURE_3D, 0);
		return textureID;
	}

	/**
	 * Creates an empty texture, initializes it with the given data, and returns the handle. 
	 * 
	 * Note that data.length needs to be equal to width * height * depth
	 * @param data
	 * @param internalFormat
	 * @param width
	 * @param height
	 * @param depth
	 * @param dataFormat
	 * @param dataType
	 * @param minSampleType
	 * @param magSampleType
	 * @return
	 */
	public static int createTexture(int[] data, int internalFormat, int width, int height, int depth, int dataFormat, int dataType, int minSampleType, int magSampleType) {
		System.out.println("Texture3D: CREATING TEXTURE3D : " + width + " " + height + " " + depth + " " + data.length);
		int textureID = glGenTextures();
		glBindTexture(GL_TEXTURE_3D, textureID);
		glTexStorage3D(GL_TEXTURE_3D, 1, internalFormat, width, height, depth);
		glTexSubImage3D(GL_TEXTURE_3D, 0, 0, 0, 0, width, height, depth, dataFormat, dataType, BufferUtils.createIntBuffer(data));
		glGenerateMipmap(GL_TEXTURE_3D);
		glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, minSampleType);
		glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, magSampleType);
		glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_REPEAT);
		glBindTexture(GL_TEXTURE_3D, 0);
		System.out.println("Texture3D: Finished generating texture with error code : " + glGetError());
		return textureID;
	}

}
