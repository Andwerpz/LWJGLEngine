package graphics;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;

import util.BufferUtils;

public class Cubemap {

	private int cubemapID;
	private int size;

	// images have to have the same dimensions
	public Cubemap(String right, String left, String up, String down, String front, String back) {
		String[] sides = new String[] { right, left, up, down, back, front }; // front and back are in wrong order
		cubemapID = load(sides);
	}

	public Cubemap(String[] sides) {
		cubemapID = load(sides);
	}

	public Cubemap(int internalFormat, int dataFormat, int dataType) {
		String[] sides = new String[] { "/cubemap_default.png", "/cubemap_default.png", "/cubemap_default.png", "/cubemap_default.png", "/cubemap_default.png", "/cubemap_default.png" };
		cubemapID = load(sides, internalFormat, dataFormat, dataType);
	}

	public int load(String[] sides, int internalFormat, int dataFormat, int dataType) {
		int id = glGenTextures();
		glBindTexture(GL_TEXTURE_CUBE_MAP, id);

		for (int i = 0; i < 6; i++) {
			int[] outWH = new int[2];
			int[] data = Texture.getDataFromImage(sides[i], 0, outWH);
			glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, internalFormat, outWH[0], outWH[1], 0, dataFormat, dataType, data);
			this.size = outWH[0];
		}

		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
		glBindTexture(GL_TEXTURE_CUBE_MAP, 0);

		return id;
	}

	public int load(String[] sides) {
		int id = glGenTextures();
		glBindTexture(GL_TEXTURE_CUBE_MAP, id);

		for (int i = 0; i < 6; i++) {
			int[] outWH = new int[2];
			int[] data = Texture.getDataFromImage(sides[i], 0, outWH);
			glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL_RGBA, outWH[0], outWH[1], 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
			this.size = outWH[0];
		}

		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
		glBindTexture(GL_TEXTURE_CUBE_MAP, 0);

		return id;
	}

	public int getSize() {
		return this.size;
	}

	public int getID() {
		return this.cubemapID;
	}

	public void bind(int textureID) {
		glActiveTexture(textureID);
		glBindTexture(GL_TEXTURE_CUBE_MAP, cubemapID);
	}

	public void unbind(int textureID) {
		glActiveTexture(textureID);
		glBindTexture(GL_TEXTURE_CUBE_MAP, 0);
	}

}
