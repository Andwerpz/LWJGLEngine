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
	
	public static final int DIFFUSE = 0;
	public static final int SPECULAR = 1;
	public static final int NORMAL = 3;
	public static final int DISPLACEMENT = 4;
	
	public static final Texture DIFFUSE_DEFAULT = new Texture("/tex_diffuse_default.png", false, false);
	public static final Texture SPECULAR_DEFAULT = new Texture("/tex_specular_default.png", false, false);
	public static final Texture NORMAL_DEFAULT = new Texture("/tex_normal_default.png", false, false);
	public static final Texture DISPLACEMENT_DEFAULT = new Texture("/tex_displacement_default.png", true, false);

	private int width, height;
	private Texture diffuse, specular, normal, displacement;
	
	public Material() {
		this.diffuse = DIFFUSE_DEFAULT;
		this.specular = SPECULAR_DEFAULT;
		this.normal = NORMAL_DEFAULT;
		this.displacement = DISPLACEMENT_DEFAULT;
	}
	
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
	
	public Material(Texture diffuse, Texture specular, Texture normal, Texture displacement) {
		if(diffuse == null) {
			this.diffuse = DIFFUSE_DEFAULT;
		}
		else {
			this.diffuse = diffuse;
		}
		
		if(specular == null) {
			this.specular = SPECULAR_DEFAULT;
		}
		else {
			this.specular = specular;
		}
		
		if(normal == null) {
			this.normal = NORMAL_DEFAULT;
		}
		else {
			this.normal = normal;
		}
		
		if(displacement == null) {
			this.displacement = DISPLACEMENT_DEFAULT;
		}
		else {
			this.displacement = displacement;
		}
	}
	
	public static Material defaultMaterial() {
		return new Material();
	}
	
	public void setTexture(String path, int which) {
		switch(which) {
		case DIFFUSE:
			diffuse = new Texture(path, false, false);
			break;
			
		case SPECULAR:
			specular = new Texture(path, false, false);
			break;
			
		case NORMAL:
			normal = new Texture(path, false, false);
			break;
			
		case DISPLACEMENT:
			displacement = new Texture(path, true, false);
			break;
		}
	}
	
	public void setTexture(BufferedImage img, int which) {
		switch(which) {
		case DIFFUSE:
			diffuse = new Texture(img, false, false);
			break;
			
		case SPECULAR:
			specular = new Texture(img, false, false);
			break;
			
		case NORMAL:
			normal = new Texture(img, false, false);
			break;
			
		case DISPLACEMENT:
			displacement = new Texture(img, true, false);
			break;
		}
	}
	
	public void setTexture(Texture tex, int which) {
		switch(which) {
		case DIFFUSE:
			diffuse = tex;
			break;
			
		case SPECULAR:
			specular = tex;
			break;
			
		case NORMAL:
			normal = tex;
			break;
			
		case DISPLACEMENT:
			displacement = tex;
			break;
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
