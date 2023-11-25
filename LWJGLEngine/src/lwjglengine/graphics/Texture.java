package lwjglengine.graphics;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.HashSet;

import javax.imageio.ImageIO;

import lwjglengine.util.BufferUtils;
import myutils.file.FileUtils;
import myutils.graphics.GraphicsTools;
import myutils.math.MathUtils;
import myutils.math.Vec3;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.GL42.glTexStorage2D;
import static org.lwjgl.opengl.GL46.*;

public class Texture {
	//internalFormat is like data format, but it can be more specific with how many bits each channel gets, eg GL_RGBA8
	//dataFormat specifies what is being stored, and the ordering, eg GL_RGB, GL_ARGB, GL_RED
	//dataType specifies how exactly the data is being stored, eg GL_BYTE, GL_FLOAT, GL_INT

	public static final boolean VERTICAL_FLIP_DEFAULT = true; //if textures are always flipped upside down, then turn this on. 
	public static final boolean HORIZONTAL_FLIP_DEFAULT = false;

	public static final int INVERT_COLORS_BIT = (1 << 0);
	public static final int HORIZONTAL_FLIP_BIT = (1 << 1);
	public static final int VERTICAL_FLIP_BIT = (1 << 2);

	// if this is false, bind() and unbind() will not work.
	// used when you don't want models autobinding their own textures. 
	// this is here because when we want to render the geometry, 3d models will automatically try to bind
	// their textures. This might be bad when we want to render the geometry, but have other textures bound, so
	// we override all binding of 2d textures with this
	// probably should move that functionality into the model object. 
	public static boolean bindingEnabled = true;

	private int textureID;

	// -- IMAGE TEXTURE CONSTRUCTORS --
	//uses relative path
	public Texture(String path) {
		this.textureID = Texture.createTexture(FileUtils.loadImageRelative(path), 0, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR, 5);
	}

	public Texture(BufferedImage img) {
		this.textureID = Texture.createTexture(img, 0, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR, 5);
	}

	public Texture(String path, int loadOptions) {
		this.textureID = Texture.createTexture(FileUtils.loadImageRelative(path), loadOptions, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR, 5);
	}

	public Texture(BufferedImage img, int loadOptions) {
		this.textureID = Texture.createTexture(img, loadOptions, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR, 5);
	}

	public Texture(BufferedImage img, int loadOptions, int minSampleType, int magSampleType) {
		this.textureID = Texture.createTexture(img, loadOptions, minSampleType, magSampleType, 5);
	}

	public Texture(int[] data, int width, int height, int minSampleType, int magSampleType) {
		this.textureID = Texture.createTexture(data, width, height, minSampleType, magSampleType, 5);
	}

	public Texture(int textureID) {
		this.textureID = textureID;
	}

	//initializes a texture with a solid color
	public Texture(int r, int g, int b, float a) {
		BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		int alpha = MathUtils.clamp(0, 255, (int) (a * 255f));
		int rgb = alpha;
		rgb <<= 8;
		rgb |= MathUtils.clamp(0, 255, r);
		rgb <<= 8;
		rgb |= MathUtils.clamp(0, 255, g);
		rgb <<= 8;
		rgb |= MathUtils.clamp(0, 255, b);
		img.setRGB(0, 0, rgb);
		this.textureID = Texture.createTexture(img, 0, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR, 1);
	}

	public Texture(BufferedImage img, int loadOptions, int minSampleType, int magSampleType, int numMipmapLevels) {
		this.textureID = Texture.createTexture(img, loadOptions, minSampleType, magSampleType, numMipmapLevels);
	}

	public Texture(String path, int loadOptions, int minSampleType, int magSampleType, int numMipmapLevels) {
		this.textureID = Texture.createTexture(FileUtils.loadImageRelative(path), loadOptions, minSampleType, magSampleType, numMipmapLevels);
	}

	// -- BUFFER TEXTURE CONSTRUCTORS -- 
	public Texture(int internalFormat, int width, int height, int dataFormat, int dataType) {
		this.textureID = Texture.createTexture(internalFormat, width, height, dataFormat, dataType, GL_NEAREST, GL_NEAREST, null);
	}

	public Texture(int internalFormat, int width, int height, int dataFormat, int dataType, int sampleType) {
		this.textureID = Texture.createTexture(internalFormat, width, height, dataFormat, dataType, sampleType, sampleType, null);
	}

	public Texture(int internalFormat, int width, int height, int dataFormat, int dataType, float[] pixels) {
		this.textureID = Texture.createTexture(internalFormat, width, height, dataFormat, dataType, GL_NEAREST, GL_NEAREST, pixels);
	}

	public int getID() {
		return this.textureID;
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

	public void setWrapping(int wrap) {
		glBindTexture(GL_TEXTURE_2D, this.textureID);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, wrap);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, wrap);
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

	/**
	 * Converts the image into ARGB format, then extracts the ARGB information into an int buffer. 
	 * 
	 * outWH returns the width and height of the original image. 
	 * @param img
	 * @param loadOptions
	 * @param outWH
	 * @return
	 */
	public static int[] getDataFromImage(BufferedImage img, int loadOptions, int[] outWH) {
		int[] pixels = null;

		//convert image to proper format
		BufferedImage image = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
		image.getGraphics().drawImage(img, 0, 0, null);
		image.getGraphics().dispose();

		if (((loadOptions & HORIZONTAL_FLIP_BIT) != 0) ^ HORIZONTAL_FLIP_DEFAULT)
			image = GraphicsTools.horizontalFlip(image);
		if (((loadOptions & VERTICAL_FLIP_BIT) != 0) ^ VERTICAL_FLIP_DEFAULT)
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

	/**
	 * Creates an empty texture, and returns the handle. 
	 * @return
	 */
	public static int createTexture(int internalFormat, int width, int height, int dataFormat, int dataType, int minSampleType, int magSampleType, float[] data) {
		int textureID = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, textureID);
		glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, dataFormat, dataType, data);
		//glGenerateMipmap(GL_TEXTURE_2D);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, minSampleType);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, magSampleType);
		if (internalFormat == GL_DEPTH_COMPONENT) {
			//we make depth textures clamp to border because of how perspective screen does directional lighting. 
			//if it didn't clamp to border, any pixel outside of the shadow cascade will appear lit as default, or it will wrap. 
			//probably want to make this sort of thing something to choose. 
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
			float borderColor[] = { 1.0f, 1.0f, 1.0f, 1.0f };
			glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, borderColor);
		}
		else {
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		}
		glBindTexture(GL_TEXTURE_2D, 0);
		return textureID;
	}

	/**
	 * Creates a texture initialized with the given image, and returns the handle
	 * @return
	 */
	public static int createTexture(BufferedImage img, int loadOptions, int minSampleType, int magSampleType, int numMipmapLevels) {
		int[] outWH = new int[2];
		int[] data = Texture.getDataFromImage(img, loadOptions, outWH);
		int width = outWH[0];
		int height = outWH[1];

		return createTexture(data, width, height, minSampleType, magSampleType, numMipmapLevels);
	}

	/**
	 * Creates a texture initialized with the given data, and returns the handle
	 * @return
	 */
	public static int createTexture(int[] data, int width, int height, int minSampleType, int magSampleType, int numMipmapLevels) {
		int textureID = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, textureID);
		glTexStorage2D(GL_TEXTURE_2D, numMipmapLevels, GL_RGBA8, width, height);
		glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, BufferUtils.createIntBuffer(data));
		glGenerateMipmap(GL_TEXTURE_2D); //Generate num_mipmaps number of mipmaps here.
		glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY, 16); //enable anisotropic filtering
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, minSampleType);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, magSampleType); // magnification filter
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT); //enable texture wrapping
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		glBindTexture(GL_TEXTURE_2D, 0);
		return textureID;
	}
}
