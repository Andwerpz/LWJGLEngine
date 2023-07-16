package lwjglengine.graphics;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.HashSet;

import javax.imageio.ImageIO;

import lwjglengine.util.BufferUtils;
import lwjglengine.util.TextureUtils;
import myutils.v11.file.FileUtils;
import myutils.v10.graphics.GraphicsTools;
import myutils.v10.math.MathUtils;
import myutils.v10.math.Vec3;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.GL46.*;

public class Texture {

	public static final int INVERT_COLORS_BIT = TextureUtils.INVERT_COLORS_BIT;
	public static final int HORIZONTAL_FLIP_BIT = TextureUtils.HORIZONTAL_FLIP_BIT;
	public static final int VERTICAL_FLIP_BIT = TextureUtils.VERTICAL_FLIP_BIT;

	// if this is false, bind() and unbind() will not work.
	// used when you don't want models autobinding their own textures. 
	public static boolean bindingEnabled = true;

	private int textureID;

	// -- IMAGE TEXTURE CONSTRUCTORS --
	//uses relative path
	public Texture(String path) {
		this.textureID = TextureUtils.createTexture(FileUtils.loadImageRelative(path), 0, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR, 5);
	}

	public Texture(BufferedImage img) {
		this.textureID = TextureUtils.createTexture(img, 0, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR, 5);
	}

	public Texture(String path, int loadOptions) {
		this.textureID = TextureUtils.createTexture(FileUtils.loadImageRelative(path), loadOptions, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR, 5);
	}

	public Texture(BufferedImage img, int loadOptions) {
		this.textureID = TextureUtils.createTexture(img, loadOptions, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR, 5);
	}

	public Texture(BufferedImage img, int loadOptions, int minSampleType, int magSampleType) {
		this.textureID = TextureUtils.createTexture(img, loadOptions, minSampleType, magSampleType, 5);
	}

	//initializes a texture with a solid color
	public Texture(int r, int g, int b, float a) {
		BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		int alpha = (int) MathUtils.clamp(0, 255, (int) (a * 255f));
		int rgb = alpha;
		rgb <<= 8;
		rgb |= (int) MathUtils.clamp(0, 255, r);
		rgb <<= 8;
		rgb |= (int) MathUtils.clamp(0, 255, g);
		rgb <<= 8;
		rgb |= (int) MathUtils.clamp(0, 255, b);
		img.setRGB(0, 0, rgb);
		this.textureID = TextureUtils.createTexture(img, 0, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR, 1);
	}

	public Texture(BufferedImage img, int loadOptions, int minSampleType, int magSampleType, int numMipmapLevels) {
		this.textureID = TextureUtils.createTexture(img, loadOptions, minSampleType, magSampleType, numMipmapLevels);
	}

	public Texture(String path, int loadOptions, int minSampleType, int magSampleType, int numMipmapLevels) {
		this.textureID = TextureUtils.createTexture(FileUtils.loadImageRelative(path), loadOptions, minSampleType, magSampleType, numMipmapLevels);
	}

	// -- BUFFER TEXTURE CONSTRUCTORS -- 
	public Texture(int internalFormat, int width, int height, int dataFormat, int dataType) {
		this.textureID = TextureUtils.createTexture(internalFormat, width, height, dataFormat, dataType, GL_LINEAR, GL_LINEAR);
	}

	public Texture(int internalFormat, int width, int height, int dataFormat, int dataType, int sampleType) {
		this.textureID = TextureUtils.createTexture(internalFormat, width, height, dataFormat, dataType, sampleType, sampleType);
	}

	public int getID() {
		return this.textureID;
	}

	public void bind(int glTextureLocation) {
		if (!bindingEnabled)
			return;
		glActiveTexture(glTextureLocation);
		glBindTexture(GL_TEXTURE_2D, this.textureID);
	}

	public void unbind(int glTextureLocation) {
		if (!bindingEnabled)
			return;
		glActiveTexture(glTextureLocation);
		glBindTexture(GL_TEXTURE_2D, 0);
	}

	public void kill() {
		glDeleteTextures(BufferUtils.createIntBuffer(new int[] { this.textureID }));
	}
}
