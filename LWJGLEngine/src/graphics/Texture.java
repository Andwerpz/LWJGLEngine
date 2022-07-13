package graphics;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import util.BufferUtils;
import util.GraphicsTools;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;

public class Texture {

	private int width, height;
	private int diffuse, specular, normal, displacement;
	
	public Texture(String diffusePath, String specularPath, String normalPath, String displacementPath) {
		if(diffusePath == null) {
			diffuse = load("/tex_diffuse_default.png", false);
		}
		else {
			diffuse = load(diffusePath, false);
		}
		
		if(specularPath == null) {
			specular = load("/tex_specular_default.png", false);
		}
		else {
			specular = load(specularPath, false);
		}
		
		if(normalPath == null) {
			normal = load("/tex_normal_default.png", false);
		}
		else {
			normal = load(normalPath, false);
		}
		
		if(displacementPath == null) {
			displacement = load("/tex_displacement_default.png", true);
		}
		else {
			displacement = load(displacementPath, true);
		}
	}
	
	public int load(String path, boolean invert) {
		int[] pixels = null;
		BufferedImage image = GraphicsTools.loadImage(path);
		image = GraphicsTools.verticalFlip(image);
		width = image.getWidth();
		height = image.getHeight();
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
		
		int result = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, result);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);	//magnification filter
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, BufferUtils.createIntBuffer(data));
		glBindTexture(GL_TEXTURE_2D, 0);
		return result;
	}
	
	public void bind() {
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, diffuse);
		glActiveTexture(GL_TEXTURE1);
		glBindTexture(GL_TEXTURE_2D, specular);
		glActiveTexture(GL_TEXTURE2);
		glBindTexture(GL_TEXTURE_2D, normal);
		glActiveTexture(GL_TEXTURE3);
		glBindTexture(GL_TEXTURE_2D, displacement);
	}
	
	public void unbind() {
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, 0);
		glActiveTexture(GL_TEXTURE1);
		glBindTexture(GL_TEXTURE_2D, 0);
		glActiveTexture(GL_TEXTURE2);
		glBindTexture(GL_TEXTURE_2D, 0);
		glActiveTexture(GL_TEXTURE3);
		glBindTexture(GL_TEXTURE_2D, 0);
	}
}
