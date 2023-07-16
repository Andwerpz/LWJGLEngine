package lwjglengine.graphics;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;

import lwjglengine.util.BufferUtils;
import lwjglengine.util.TextureUtils;
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
		cubemapID = load(sides, GL_RGBA, GL_RGBA, GL_UNSIGNED_BYTE);
	}

	public Cubemap(BufferedImage[] sides) {
		cubemapID = load(sides, GL_RGBA, GL_RGBA, GL_UNSIGNED_BYTE);
	}

	public Cubemap(int internalFormat, int dataFormat, int dataType) {
		BufferedImage[] sides = new BufferedImage[6];

		for (int i = 0; i < 6; i++) {
			sides[i] = JarUtils.loadImage("/cubemap_default.png");
		}

		cubemapID = load(sides, internalFormat, dataFormat, dataType);
	}

	public int load(BufferedImage[] sides, int internalFormat, int dataFormat, int dataType) {
		int id = glGenTextures();
		glBindTexture(GL_TEXTURE_CUBE_MAP, id);

		for (int i = 0; i < 6; i++) {
			int[] outWH = new int[2];
			int[] data = TextureUtils.getDataFromImage(sides[i], 0, outWH);
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
