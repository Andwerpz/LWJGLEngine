package lwjglengine.graphics;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL21.*;
import static org.lwjgl.opengl.GL33.*;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import lwjglengine.util.BufferUtils;
import myutils.v10.file.SystemUtils;
import myutils.v11.file.JarUtils;

public class Cubemap {

	public static final String[] CUBEMAP_SIDE_NAMES = { "right", "left", "top", "bottom", "front", "back" };

	private int cubemapID;
	private int size;

	// images have to have the same dimensions
	public Cubemap(BufferedImage right, BufferedImage left, BufferedImage top, BufferedImage bottom, BufferedImage front, BufferedImage back) {
		// front and back are in wrong order?
		BufferedImage[] sides = new BufferedImage[] { right, left, top, bottom, back, front };
		this.cubemapID = load(sides, GL_RGBA, GL_RGBA, GL_UNSIGNED_BYTE);
	}

	public Cubemap(BufferedImage[] sides) {
		this.cubemapID = load(sides, GL_RGBA, GL_RGBA, GL_UNSIGNED_BYTE);
	}

	/**
	 * Textures passed in must be of type GL_RGBA
	 * @param sides
	 */
	public Cubemap(Texture[] sides) {
		this.load(sides);
	}

	public Cubemap(int internalFormat, int dataFormat, int dataType) {
		BufferedImage[] sides = new BufferedImage[6];

		for (int i = 0; i < 6; i++) {
			sides[i] = JarUtils.loadImage("/cubemap_default.png");
		}

		this.cubemapID = load(sides, internalFormat, dataFormat, dataType);
	}

	public Cubemap(int internalFormat, int dataFormat, int dataType, int resolution) {
		this.cubemapID = load(internalFormat, dataFormat, dataType, resolution);
	}

	public Cubemap(int textureID) {
		this.cubemapID = textureID;
	}

	public int load(Texture[] sides) {
		if (sides.length != 6) {
			System.err.println("Cubemap: Number of provided textures must be 6");
			return -1;
		}

		int[] w = { 0 };
		int[] h = { 0 };
		int[] internalFormat = { 0 };
		glGetTexLevelParameteriv(sides[0].getID(), 0, GL_TEXTURE_WIDTH, w);
		glGetTexLevelParameteriv(sides[0].getID(), 0, GL_TEXTURE_HEIGHT, h);
		glGetTexLevelParameteriv(sides[0].getID(), 0, GL_TEXTURE_INTERNAL_FORMAT, w);
		int bufSize = w[0] * h[0];

		int id = glGenTextures();
		glBindTexture(GL_TEXTURE_CUBE_MAP, id);
		for (int i = 0; i < 6; i++) {
			int[] data = new int[bufSize];
			glGetTexImage(sides[i].getID(), 0, internalFormat[0], GL_INT, data);
			glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, internalFormat[0], w[0], h[1], 0, internalFormat[0], GL_UNSIGNED_BYTE, data);
		}

		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
		glBindTexture(GL_TEXTURE_CUBE_MAP, 0);

		return id;
	}

	public int load(BufferedImage[] sides, int internalFormat, int dataFormat, int dataType) {
		int id = glGenTextures();
		glBindTexture(GL_TEXTURE_CUBE_MAP, id);

		for (int i = 0; i < 6; i++) {
			int[] outWH = new int[2];
			int[] data = Texture.getDataFromImage(sides[i], Texture.VERTICAL_FLIP_BIT, outWH);
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

	public int load(int internalFormat, int dataFormat, int dataType, int resolution) {
		int id = glGenTextures();
		glBindTexture(GL_TEXTURE_CUBE_MAP, id);
		for (int i = 0; i < 6; i++) {
			// note that we store each face with 16 bit floating point values
			glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, internalFormat, resolution, resolution, 0, dataFormat, dataType, (ByteBuffer) null);
		}
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
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

	public void kill() {
		glDeleteTextures(BufferUtils.createIntBuffer(new int[] { this.cubemapID }));
	}

}
