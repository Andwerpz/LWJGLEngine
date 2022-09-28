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

public class Texture {

	// if this is false, bind() and unbind() will not work.
	// used for rendering depth maps with the same draw method as the geometry map
	public static boolean bindingEnabled = true;

	private int width, height;
	private int textureID;

	public Texture(String path) {
		this.textureID = this.load(path, false, false);
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
		this.textureID = this.load(img, false, false);
	}

	public Texture(BufferedImage img) {
		this.textureID = this.load(img, false, false);
	}

	public Texture(String path, boolean invertColors, boolean horizontalFlip) {
		this.textureID = this.load(path, invertColors, horizontalFlip);
	}

	public Texture(BufferedImage img, boolean invertColors, boolean horizontalFlip) {
		this.textureID = this.load(img, invertColors, horizontalFlip);
	}

	public Texture(String path, boolean invertColors, boolean horizontalFlip, boolean verticalFlip) {
		this.textureID = this.load(path, invertColors, horizontalFlip, verticalFlip);
	}

	public Texture(BufferedImage img, boolean invertColors, boolean horizontalFlip, boolean verticalFlip) {
		this.textureID = this.load(img, invertColors, horizontalFlip, verticalFlip);
	}

	public Texture(BufferedImage img, boolean invertColors, boolean horizontalFlip, boolean verticalFlip, int sampleType) {
		this.textureID = this.load(img, invertColors, horizontalFlip, verticalFlip, sampleType);
	}

	public Texture(String path, boolean invertColors, boolean horizontalFlip, boolean verticalFlip, int sampleType) {
		this.textureID = this.load(path, invertColors, horizontalFlip, verticalFlip, sampleType);
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

	public int load(BufferedImage img, boolean invertColors, boolean horizontalFlip, boolean verticalFlip, int sampleType) {
		int[] outWH = new int[2];
		int[] data = getDataFromImage(img, invertColors, horizontalFlip, verticalFlip, outWH);
		this.width = outWH[0];
		this.height = outWH[1];

		int result = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, result);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, sampleType);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, sampleType); // magnification filter
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, BufferUtils.createIntBuffer(data));
		glBindTexture(GL_TEXTURE_2D, 0);
		return result;
	}

	public int load(String path, boolean invertColors, boolean horizontalFlip, boolean verticalFlip, int sampleType) {
		return this.load(FileUtils.loadImage(path), invertColors, horizontalFlip, verticalFlip, sampleType);
	}

	public int load(String path, boolean invertColors, boolean horizontalFlip, boolean verticalFlip) {
		return this.load(FileUtils.loadImage(path), invertColors, horizontalFlip, verticalFlip, GL_LINEAR);
	}

	public int load(String path, boolean invertColors, boolean horizontalFlip) {
		return this.load(FileUtils.loadImage(path), invertColors, horizontalFlip, false, GL_LINEAR);
	}

	public int load(BufferedImage img, boolean invertColors, boolean horizontalFlip, boolean verticalFlip) {
		return this.load(img, invertColors, horizontalFlip, verticalFlip, GL_LINEAR);
	}

	public int load(BufferedImage img, boolean invertColors, boolean horizontalFlip) {
		return this.load(img, invertColors, horizontalFlip, false, GL_LINEAR);
	}

	public static int[] getDataFromImage(BufferedImage img, boolean invertColors, boolean horizontalFlip, int[] outWH) {
		return Texture.getDataFromImage(img, invertColors, horizontalFlip, false, outWH);
	}

	public static int[] getDataFromImage(String path, boolean invertColors, boolean horizontalFlip, int[] outWH) {
		return Texture.getDataFromImage(FileUtils.loadImage(path), invertColors, horizontalFlip, false, outWH);
	}

	public static int[] getDataFromImage(String path, boolean invertColors, boolean horizontalFlip, boolean verticalFlip, int[] outWH) {
		return Texture.getDataFromImage(FileUtils.loadImage(path), invertColors, horizontalFlip, verticalFlip, outWH);
	}

	public static int[] getDataFromImage(BufferedImage img, boolean invertColors, boolean horizontalFlip, boolean verticalFlip, int[] outWH) {
		int[] pixels = null;
		BufferedImage image = GraphicsTools.copyImage(img);
		if (horizontalFlip)
			image = GraphicsTools.horizontalFlip(image);
		if (verticalFlip)
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

			if (invertColors) {
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
}
