package lwjglengine.graphics;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import lwjglengine.util.BufferUtils;
import myutils.v10.graphics.GraphicsTools;
import myutils.v10.math.Vec3;
import myutils.v11.file.FileUtils;
import myutils.v11.file.JarUtils;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;

public class TextureMaterial {
	// in the shaders, the texture material is different from the rgb-specular material.
	// rgb-specular material is instanced along with the model mat4, while the texture materials 
	// must be bound seperately for each render call.
	// this means that one model object can only have one texture material for all of it's instances. 

	// by default, a rgb-specular material is included with every model
	// instance, set to:
	// diffuse : (1, 1, 1)
	// specular : (1, 1, 1)
	// shininess : (32.0)

	// these values are then multiplied with the values supplied in the texture2D sampler to get the final value.
	// note that if you don't change the default rgb-specular material, the texture material should work as expected.

	// if a .mat file includes rgb-specular values, then those are used as default instead.

	public static final int DIFFUSE = 0;
	public static final int SPECULAR = 1;
	public static final int NORMAL = 3;
	public static final int DISPLACEMENT = 4;

	public static final Texture DIFFUSE_DEFAULT = new Texture(255, 255, 255, 1);
	public static final Texture SPECULAR_DEFAULT = new Texture(255, 255, 255, 1);
	public static final Texture NORMAL_DEFAULT = new Texture(JarUtils.loadImage("/tex_normal_default.png"));
	public static final Texture DISPLACEMENT_DEFAULT = new Texture(JarUtils.loadImage("/tex_displacement_default.png"), Texture.INVERT_COLORS_BIT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR, 1);

	private int width, height;
	private Texture diffuse, specular, normal, displacement;

	public static TextureMaterial defaultTextureMaterial() {
		return new TextureMaterial(DIFFUSE_DEFAULT, SPECULAR_DEFAULT, NORMAL_DEFAULT, DISPLACEMENT_DEFAULT);
	}

	public TextureMaterial() {
		this.diffuse = DIFFUSE_DEFAULT;
		this.specular = SPECULAR_DEFAULT;
		this.normal = NORMAL_DEFAULT;
		this.displacement = DISPLACEMENT_DEFAULT;
	}

	public TextureMaterial(int r, int g, int b, float a, float specular) {
		this.diffuse = new Texture(r, g, b, a);
		this.specular = new Texture((int) (specular * 255), (int) (specular * 255), (int) (specular * 255), 1f);
		normal = NORMAL_DEFAULT;
		displacement = DISPLACEMENT_DEFAULT;
	}

	public TextureMaterial(BufferedImage diffuse) {
		this.diffuse = new Texture(diffuse);
		this.specular = SPECULAR_DEFAULT;
		this.normal = NORMAL_DEFAULT;
		this.displacement = DISPLACEMENT_DEFAULT;
	}

	public TextureMaterial(Texture diffuse) {
		this.diffuse = diffuse;
		this.specular = SPECULAR_DEFAULT;
		this.normal = NORMAL_DEFAULT;
		this.displacement = DISPLACEMENT_DEFAULT;
	}

	public TextureMaterial(String diffusePath, String specularPath, String normalPath, String displacementPath) {
		if (diffusePath == null) {
			diffuse = DIFFUSE_DEFAULT;
		}
		else {
			diffuse = new Texture(diffusePath);
		}

		if (specularPath == null) {
			specular = SPECULAR_DEFAULT;
		}
		else {
			specular = new Texture(specularPath);
		}

		if (normalPath == null) {
			normal = NORMAL_DEFAULT;
		}
		else {
			normal = new Texture(normalPath);
		}

		if (displacementPath == null) {
			displacement = DISPLACEMENT_DEFAULT;
		}
		else {
			displacement = new Texture(displacementPath, Texture.INVERT_COLORS_BIT);
		}
	}

	public TextureMaterial(Texture diffuse, Texture specular, Texture normal, Texture displacement) {
		if (diffuse == null) {
			this.diffuse = DIFFUSE_DEFAULT;
		}
		else {
			this.diffuse = diffuse;
		}

		if (specular == null) {
			this.specular = SPECULAR_DEFAULT;
		}
		else {
			this.specular = specular;
		}

		if (normal == null) {
			this.normal = NORMAL_DEFAULT;
		}
		else {
			this.normal = normal;
		}

		if (displacement == null) {
			this.displacement = DISPLACEMENT_DEFAULT;
		}
		else {
			this.displacement = displacement;
		}
	}

	public void setTexture(String path, int which) {
		this.setTexture(new Texture(path), which);
	}

	public void setTexture(BufferedImage img, int which) {
		this.setTexture(new Texture(img), which);
	}

	public void setTexture(Texture tex, int which) {
		switch (which) {
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

	public void kill() {
		if (this.diffuse != DIFFUSE_DEFAULT) {
			this.diffuse.kill();
		}
		if (this.specular != SPECULAR_DEFAULT) {
			this.specular.kill();
		}
		if (this.normal != NORMAL_DEFAULT) {
			this.normal.kill();
		}
		if (this.displacement != DISPLACEMENT_DEFAULT) {
			this.displacement.kill();
		}
	}
}
