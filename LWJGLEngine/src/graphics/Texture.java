package graphics;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.FloatBuffer;

import javax.imageio.ImageIO;

import util.BufferUtils;
import util.FileUtils;
import util.GraphicsTools;
import util.MathUtils;
import util.Vec3;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.GL46.*;

public class Texture {

	public static final int INVERT_COLORS_BIT = (1 << 0);
	public static final int HORIZONTAL_FLIP_BIT = (1 << 1);
	public static final int VERTICAL_FLIP_BIT = (1 << 2);

	// if this is false, bind() and unbind() will not work.
	// used when you don't want models autobinding their own textures. 
	public static boolean bindingEnabled = true;

	private int width, height;
	private int textureID;

	public Texture(String path) {
		this.textureID = this.load(path, 0, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR, 5);
	}

	public Texture(BufferedImage img) {
		this.textureID = this.load(img, 0, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR, 5);
	}

	public Texture(String path, int loadOptions) {
		this.textureID = this.load(path, loadOptions, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR, 5);
	}

	public Texture(BufferedImage img, int loadOptions) {
		this.textureID = this.load(img, loadOptions, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR, 5);
	}

	public Texture(BufferedImage img, int loadOptions, int minSampleType, int magSampleType) {
		this.textureID = this.load(img, loadOptions, minSampleType, magSampleType, 5);
	}

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
		this.textureID = this.load(img, 0, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR, 1);
	}

	public Texture(BufferedImage img, int loadOptions, int minSampleType, int magSampleType, int numMipmapLevels) {
		this.textureID = this.load(img, loadOptions, minSampleType, magSampleType, numMipmapLevels);
	}

	public Texture(String path, int loadOptions, int minSampleType, int magSampleType, int numMipmapLevels) {
		this.textureID = this.load(path, loadOptions, minSampleType, magSampleType, numMipmapLevels);
	}

	public Texture(int internalFormat, int width, int height, int dataFormat, int dataType) {
		this.textureID = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, textureID);
		glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, dataFormat, dataType, (FloatBuffer) null);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		if (internalFormat == GL_DEPTH_COMPONENT) {
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
			float borderColor[] = { 1.0f, 1.0f, 1.0f, 1.0f };
			glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, borderColor);
		}
		glBindTexture(GL_TEXTURE_2D, 0);
	}

	public Texture(int internalFormat, int width, int height, int dataFormat, int dataType, int sampleType) {
		this.textureID = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, textureID);
		glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, dataFormat, dataType, (FloatBuffer) null);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, sampleType);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, sampleType);
		if (internalFormat == GL_DEPTH_COMPONENT) {
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
			float borderColor[] = { 1.0f, 1.0f, 1.0f, 1.0f };
			glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, borderColor);
		}
		glBindTexture(GL_TEXTURE_2D, 0);
	}

	private int load(String path, int loadOptions, int minSampleType, int magSampleType, int numMipmapLevels) {
		BufferedImage img = FileUtils.loadImage(path);
		return this.load(img, loadOptions, minSampleType, magSampleType, numMipmapLevels);
	}

	private int load(BufferedImage img, int loadOptions, int minSampleType, int magSampleType, int numMipmapLevels) {
		int[] outWH = new int[2];
		int[] data = getDataFromImage(img, loadOptions, outWH);
		this.width = outWH[0];
		this.height = outWH[1];

		int result = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, result);
		glTexStorage2D(GL_TEXTURE_2D, numMipmapLevels, GL_RGBA8, width, height);
		glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, this.width, this.height, GL_RGBA, GL_UNSIGNED_BYTE, BufferUtils.createIntBuffer(data));
		glGenerateMipmap(GL_TEXTURE_2D); //Generate num_mipmaps number of mipmaps here.
		glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY, 16);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, minSampleType);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, magSampleType); // magnification filter
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		glBindTexture(GL_TEXTURE_2D, 0);
		return result;
	}

	public static int[] getDataFromImage(String path, int loadOptions, int[] outWH) {
		BufferedImage img = FileUtils.loadImage(path);
		return Texture.getDataFromImage(img, loadOptions, outWH);
	}

	public static int[] getDataFromImage(BufferedImage img, int loadOptions, int[] outWH) {
		int[] pixels = null;
		BufferedImage image = GraphicsTools.copyImage(img);
		if ((loadOptions & HORIZONTAL_FLIP_BIT) != 0)
			image = GraphicsTools.horizontalFlip(image);
		if ((loadOptions & VERTICAL_FLIP_BIT) != 0)
			image = GraphicsTools.verticalFlip(image);
		int width = image.getWidth();
		int height = image.getHeight();
		outWH[0] = image.getWidth();
		outWH[1] = image.getHeight();
		pixels = new int[width * height];
		image.getRGB(0, 0, width, height, pixels, 0, width);

		int[] data = new int[width * height];
		for (int i = 0; i < width * height; i++) {
			int a = (pixels[i] & 0xff000000) >> 24;
			int r = (pixels[i] & 0xff0000) >> 16;
			int g = (pixels[i] & 0xff00) >> 8;
			int b = (pixels[i] & 0xff);

			if ((loadOptions & INVERT_COLORS_BIT) != 0) {
				a = 255 - a;
				r = 255 - r;
				g = 255 - g;
				b = 255 - b;
			}

			data[i] = a << 24 | b << 16 | g << 8 | r;
		}

		return data;
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
