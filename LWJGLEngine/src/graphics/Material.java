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

public class Material {
	
	public static final Texture DIFFUSE_DEFAULT = new Texture("/tex_diffuse_default.png", false, false);
	public static final Texture SPECULAR_DEFAULT = new Texture("/tex_specular_default.png", false, false);
	public static final Texture NORMAL_DEFAULT = new Texture("/tex_normal_default.png", false, false);
	public static final Texture DISPLACEMENT_DEFAULT = new Texture("/tex_displacement_default.png", true, false);

	private int width, height;
	private Texture diffuse, specular, normal, displacement;
	
	public Material(String diffusePath, String specularPath, String normalPath, String displacementPath) {
		if(diffusePath == null) {
			diffuse = DIFFUSE_DEFAULT;
		}
		else {
			diffuse = new Texture(diffusePath, false, false);
		}
		
		if(specularPath == null) {
			specular = SPECULAR_DEFAULT;
		}
		else {
			specular = new Texture(specularPath, false, false);
		}
		
		if(normalPath == null) {
			normal = NORMAL_DEFAULT;
		}
		else {
			normal = new Texture(normalPath, false, false);
		}
		
		if(displacementPath == null) {
			displacement = DISPLACEMENT_DEFAULT;
		}
		else {
			displacement = new Texture(displacementPath, true, false);
		}
	}
	
	public void bind() {
		diffuse.bind(GL_TEXTURE0);
		specular.bind(GL_TEXTURE1);
		normal.bind(GL_TEXTURE2);
		displacement.bind(GL_TEXTURE3);
	}
	
	public void unbind() {
		diffuse.unbind(GL_TEXTURE0);
		specular.unbind(GL_TEXTURE1);
		normal.unbind(GL_TEXTURE2);
		displacement.unbind(GL_TEXTURE3);
	}
}
