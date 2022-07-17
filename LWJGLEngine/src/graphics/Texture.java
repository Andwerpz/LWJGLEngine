package graphics;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.FloatBuffer;

import javax.imageio.ImageIO;

import util.BufferUtils;
import util.GraphicsTools;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;

public class Texture {

	//if this is false, bind() and unbind() will not work. 
	//used for rendering depth maps with the same draw method as the geometry map
	public static boolean bindingEnabled = true;
	
	private int width, height;
	private int textureID;
	
	public Texture(String path, boolean invert, boolean rotate180) {
		this.textureID = this.load(path, false, false);
	}
	
	public Texture(int internalFormat, int width, int height, int dataFormat, int dataType) {
		this.textureID = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, textureID);
		glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, dataFormat, dataType, (FloatBuffer) null);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);  
		if(internalFormat == GL_DEPTH_COMPONENT) {
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER); 
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);  
			float borderColor[] = { 1.0f, 1.0f, 1.0f, 1.0f };
			glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, borderColor); 
		}
		glBindTexture(GL_TEXTURE_2D, 0);
	}
	
	public int load(String path, boolean invert, boolean rotate180) {
		int[] outWH = new int[2];
		int[] data = getDataFromImage(path, invert, rotate180, outWH);
		this.width = outWH[0];
		this.height = outWH[1];
		
		int result = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, result);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);	//magnification filter
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT); 
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);  
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, BufferUtils.createIntBuffer(data));
		glBindTexture(GL_TEXTURE_2D, 0);
		return result;
	}
	
	public static int[] getDataFromImage(String path, boolean invert, boolean rotate180, int[] outWH) {
		int[] pixels = null;
		BufferedImage image = GraphicsTools.loadImage(path);
		image = GraphicsTools.verticalFlip(image);
		if(rotate180) image = GraphicsTools.rotateImageByDegrees(image, 180);
		int width = image.getWidth();
		int height = image.getHeight();
		outWH[0] = image.getWidth();
		outWH[1] = image.getHeight();
		pixels = new int[width * height];
		image.getRGB(0, 0, width, height, pixels, 0, width);
		
		int[] data = new int[width * height];
		for(int i = 0; i < width * height; i++) {
			int a = (pixels[i] & 0xff000000) >> 24;
			int r = (pixels[i] & 0xff0000) >> 16;
			int g = (pixels[i] & 0xff00) >> 8;
			int b = (pixels[i] & 0xff);
			
			if(invert) {
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
		if(!bindingEnabled) return;
		glActiveTexture(glTextureLocation);
		glBindTexture(GL_TEXTURE_2D, this.textureID);
	}
	
	public void unbind(int glTextureLocation) {
		if(!bindingEnabled) return;
		glActiveTexture(glTextureLocation);
		glBindTexture(GL_TEXTURE_2D, 0);
	}
}
