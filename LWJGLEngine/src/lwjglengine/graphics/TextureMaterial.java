package lwjglengine.graphics;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import lwjglengine.util.BufferUtils;
import myutils.file.FileUtils;
import myutils.file.JarUtils;
import myutils.file.SystemUtils;
import myutils.graphics.GraphicsTools;
import myutils.math.Vec3;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;

public class TextureMaterial {
	// in the shaders, the texture material is different from the rgb-specular material.
	// rgb-specular material is instanced along with the model mat4, while the texture materials 
	// must be bound seperately for each render call.
	// this means that one model object can only have one texture material for all of it's instances. 

	// by default, a rgb-specular material is included with every model instance, set to:
	// diffuse : (1, 1, 1)
	// specular : (1, 1, 1)
	// shininess : (32.0)

	// these values are then multiplied with the values supplied in the texture2D sampler to get the final value.
	// note that if you don't change the default rgb-specular material, the texture material should work as expected.

	// if a .mat file includes rgb-specular values, then those are used as default instead.

	public static final int DIFFUSE = 0;
	public static final int SPECULAR = 1;
	public static final int SHININESS = 2;
	public static final int NORMAL = 3;
	public static final int DISPLACEMENT = 4;

	public static final Texture DIFFUSE_DEFAULT = new Texture(255, 255, 255, 1);
	public static final Texture SPECULAR_DEFAULT = new Texture(255, 255, 255, 1);
	public static final Texture SHININESS_DEFAULT = new Texture(255, 255, 255, 1);
	public static final Texture NORMAL_DEFAULT = new Texture(JarUtils.loadImage("/tex_normal_default.png"));
	public static final Texture DISPLACEMENT_DEFAULT = new Texture(JarUtils.loadImage("/tex_displacement_default.png"), Texture.INVERT_COLORS_BIT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR, 1);

	private Texture diffuse, specular, shininess, normal, displacement;

	public static TextureMaterial defaultTextureMaterial() {
		return new TextureMaterial(DIFFUSE_DEFAULT, SPECULAR_DEFAULT, SHININESS_DEFAULT, NORMAL_DEFAULT, DISPLACEMENT_DEFAULT);
	}

	public TextureMaterial() {
		this.init();
	}

	public TextureMaterial(int r, int g, int b, float a, float specular) {
		this.init();
		this.setTexture(new Texture(r, g, b, a), DIFFUSE);
		this.setTexture(new Texture((int) (specular * 255), (int) (specular * 255), (int) (specular * 255), 1f), SPECULAR);
	}

	public TextureMaterial(BufferedImage diffuse) {
		this.init();
		this.setTexture(new Texture(diffuse), DIFFUSE);
	}

	public TextureMaterial(Texture diffuse) {
		this.init();
		this.setTexture(diffuse, DIFFUSE);
	}

	public TextureMaterial(Texture diffuse, Texture specular, Texture shininess, Texture normal, Texture displacement) {
		this.init();
		this.setTexture(diffuse, DIFFUSE);
		this.setTexture(specular, SPECULAR);
		this.setTexture(shininess, SHININESS);
		this.setTexture(normal, NORMAL);
		this.setTexture(displacement, DISPLACEMENT);
	}

	private void init() {
		this.diffuse = DIFFUSE_DEFAULT;
		this.specular = SPECULAR_DEFAULT;
		this.shininess = SHININESS_DEFAULT;
		this.normal = NORMAL_DEFAULT;
		this.displacement = DISPLACEMENT_DEFAULT;
	}

	public void setTexture(String path, int which) {
		this.setTexture(new Texture(path), which);
	}

	public void setTexture(BufferedImage img, int which) {
		this.setTexture(new Texture(img), which);
	}

	public void setTexture(Texture tex, int which) {
		if (tex == null) {
			System.err.println("TextureMaterial : Tried to set texture to null of type " + which);
			return;
		}

		switch (which) {
		case DIFFUSE:
			diffuse = tex;
			break;

		case SPECULAR:
			specular = tex;
			break;

		case SHININESS:
			shininess = tex;
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
		diffuse.bind(GL_TEXTURE0 + TextureMaterial.DIFFUSE);
		specular.bind(GL_TEXTURE0 + TextureMaterial.SPECULAR);
		shininess.bind(GL_TEXTURE0 + TextureMaterial.SHININESS);
		normal.bind(GL_TEXTURE0 + TextureMaterial.NORMAL);
		displacement.bind(GL_TEXTURE0 + TextureMaterial.DISPLACEMENT);
	}

	public void unbind() {
		diffuse.unbind(GL_TEXTURE0 + TextureMaterial.DIFFUSE);
		specular.unbind(GL_TEXTURE0 + TextureMaterial.SPECULAR);
		shininess.unbind(GL_TEXTURE0 + TextureMaterial.SHININESS);
		normal.unbind(GL_TEXTURE0 + TextureMaterial.NORMAL);
		displacement.unbind(GL_TEXTURE0 + TextureMaterial.DISPLACEMENT);
	}

	public void kill() {
		if (this.diffuse != DIFFUSE_DEFAULT) {
			this.diffuse.kill();
		}
		if (this.specular != SPECULAR_DEFAULT) {
			this.specular.kill();
		}
		if (this.shininess != SHININESS_DEFAULT) {
			this.shininess.kill();
		}
		if (this.normal != NORMAL_DEFAULT) {
			this.normal.kill();
		}
		if (this.displacement != DISPLACEMENT_DEFAULT) {
			this.displacement.kill();
		}
	}
}
