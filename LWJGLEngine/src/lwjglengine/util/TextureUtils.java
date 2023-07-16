package lwjglengine.util;

import static org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_BORDER_COLOR;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameterf;
import static org.lwjgl.opengl.GL11.glTexParameterfv;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glTexSubImage2D;
import static org.lwjgl.opengl.GL13.GL_CLAMP_TO_BORDER;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.opengl.GL42.glTexStorage2D;
import static org.lwjgl.opengl.GL46.GL_TEXTURE_MAX_ANISOTROPY;

import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;

import myutils.v10.graphics.GraphicsTools;

public class TextureUtils {
	public static final boolean VERTICAL_FLIP_DEFAULT = false; //if textures are always flipped upside down, then turn this on. 
	public static final boolean HORIZONTAL_FLIP_DEFAULT = false;

	public static final int INVERT_COLORS_BIT = (1 << 0);
	public static final int HORIZONTAL_FLIP_BIT = (1 << 1);
	public static final int VERTICAL_FLIP_BIT = (1 << 2);

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

	/**
	 * Creates an empty texture, and returns the handle. 
	 * @return
	 */
	public static int createTexture(int internalFormat, int width, int height, int dataFormat, int dataType, int minSampleType, int magSampleType) {
		int textureID = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, textureID);
		glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, dataFormat, dataType, (FloatBuffer) null);
		glGenerateMipmap(GL_TEXTURE_2D);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, minSampleType);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, magSampleType);
		if (internalFormat == GL_DEPTH_COMPONENT) {
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
			float borderColor[] = { 1.0f, 1.0f, 1.0f, 1.0f };
			glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, borderColor);
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
		int[] data = TextureUtils.getDataFromImage(img, loadOptions, outWH);
		int width = outWH[0];
		int height = outWH[1];

		int result = TextureUtils.createTexture(GL_RGBA8, width, height, GL_RGBA, GL_RGBA8, minSampleType, magSampleType);
		glBindTexture(GL_TEXTURE_2D, result);
		glTexStorage2D(GL_TEXTURE_2D, numMipmapLevels, GL_RGBA8, width, height);
		glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, BufferUtils.createIntBuffer(data));
		glGenerateMipmap(GL_TEXTURE_2D); //Generate num_mipmaps number of mipmaps here.
		glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY, 16); //enable anisotropic filtering
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT); //enable texture wrapping
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		glBindTexture(GL_TEXTURE_2D, 0);
		return result;
	}

}
