package lwjglengine.graphics;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

import lwjglengine.util.BufferUtils;

public class Texture1D {

	public final int textureID;

	public Texture1D(int internalFormat, int width, int dataFormat, int dataType) {
		this.textureID = Texture1D.createTexture(internalFormat, width, dataFormat, dataType, GL_NEAREST, GL_NEAREST, null);
	}

	public Texture1D(int internalFormat, int width, int dataFormat, int dataType, int sampleType) {
		this.textureID = Texture1D.createTexture(internalFormat, width, dataFormat, dataType, sampleType, sampleType, null);
	}

	public Texture1D(int internalFormat, int width, int dataFormat, int dataType, float[] pixels) {
		this.textureID = Texture1D.createTexture(internalFormat, width, dataFormat, dataType, GL_NEAREST, GL_NEAREST, pixels);
	}

	public int getID() {
		return this.textureID;
	}

	private static int createTexture(int internalFormat, int width, int dataFormat, int dataType, int minSampleType, int magSampleType, float[] data) {
		int textureID = glGenTextures();
		glBindTexture(GL_TEXTURE_1D, textureID);
		glTexImage1D(GL_TEXTURE_1D, 0, internalFormat, width, 0, dataFormat, dataType, data);
		glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MIN_FILTER, minSampleType);
		glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAG_FILTER, magSampleType);
		glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		glBindTexture(GL_TEXTURE_1D, 0);
		return textureID;
	}

	public void kill() {
		glDeleteTextures(BufferUtils.createIntBuffer(new int[] { this.textureID }));
	}

}
