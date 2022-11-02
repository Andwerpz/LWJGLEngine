package graphics;

import static org.lwjgl.opengl.GL20.*;

import java.util.HashMap;
import java.util.Map;

import util.Mat4;
import util.ShaderUtils;
import util.Vec3;

public class Shader {

	public static Shader GEOMETRY, SKYBOX, LIGHTING, DEPTH, CUBE_DEPTH, GEOM_POST_PROCESS;
	public static Shader IMG_POST_PROCESS, SPLASH, OVERWRITE_ALPHA, DECAL, RENDER_BUFFER;
	public static Shader PARTICLE;

	private boolean enabled = false;

	private final int ID;
	private Map<String, Integer> locationCache = new HashMap<>();

	public Shader(String vertex, String fragment) {
		ID = ShaderUtils.load(vertex, fragment);
	}

	public static void init() {
		GEOMETRY = new Shader("/geometry.vert", "/geometry.frag");
		SKYBOX = new Shader("/skybox.vert", "/skybox.frag");
		LIGHTING = new Shader("/lighting.vert", "/lighting.frag");
		DEPTH = new Shader("/simpleDepthShader.vert", "/simpleDepthShader.frag");
		CUBE_DEPTH = new Shader("/cubemapDepthShader.vert", "/cubemapDepthShader.frag");
		GEOM_POST_PROCESS = new Shader("/geom_postprocessing.vert", "/geom_postprocessing.frag"); // post processing with geometry information
		IMG_POST_PROCESS = new Shader("/img_postprocessing.vert", "/img_postprocessing.frag"); // post processing with only final color information
		SPLASH = new Shader("/splash.vert", "/splash.frag"); // takes in a texture and alpha value.
		OVERWRITE_ALPHA = new Shader("/splash.vert", "/overwrite_alpha.frag"); // uses the first textures color, and the second textures alpha.
		DECAL = new Shader("/decal.vert", "/decal.frag");
		PARTICLE = new Shader("/particle.vert", "/particle.frag");

		Shader.GEOMETRY.setUniform1i("tex_diffuse", 0);
		Shader.GEOMETRY.setUniform1i("tex_specular", 1);
		Shader.GEOMETRY.setUniform1i("tex_normal", 2);
		Shader.GEOMETRY.setUniform1i("tex_displacement", 3);
		Shader.GEOMETRY.setUniform1i("enableParallaxMapping", 0);
		Shader.GEOMETRY.setUniform1i("enableTexScaling", 1);

		Shader.SKYBOX.setUniform1i("skybox", 0);

		Shader.LIGHTING.setUniform1i("tex_position", 0);
		Shader.LIGHTING.setUniform1i("tex_normal", 1);
		Shader.LIGHTING.setUniform1i("tex_diffuse", 2);
		Shader.LIGHTING.setUniform1i("tex_specular", 3);
		Shader.LIGHTING.setUniform1i("shadowMap", 4);
		Shader.LIGHTING.setUniform1i("shadowBackfaceMap", 5);
		Shader.LIGHTING.setUniform1i("shadowCubemap", 6);

		Shader.GEOM_POST_PROCESS.setUniform1i("tex_color", 0);
		Shader.GEOM_POST_PROCESS.setUniform1i("tex_position", 1);
		Shader.GEOM_POST_PROCESS.setUniform1i("skybox", 2);

		Shader.IMG_POST_PROCESS.setUniform1i("tex_color", 0);

		Shader.SPLASH.setUniform1i("tex_color", 0);

		Shader.OVERWRITE_ALPHA.setUniform1i("tex_color", 0);
		Shader.OVERWRITE_ALPHA.setUniform1i("tex_alpha", 1);
		Shader.OVERWRITE_ALPHA.setUniform1f("alpha", 1f);

		Shader.DECAL.setUniform1i("tex_diffuse", 0);
		Shader.DECAL.setUniform1i("tex_specular", 1);
		Shader.DECAL.setUniform1i("tex_normal", 2);
		Shader.DECAL.setUniform1i("tex_displacement", 3);
		Shader.DECAL.setUniform1i("tex_position", 4);

		Shader.PARTICLE.setUniform1i("tex_diffuse", 0);
		Shader.PARTICLE.setUniform1i("tex_specular", 1);
		Shader.PARTICLE.setUniform1i("tex_normal", 2);
		Shader.PARTICLE.setUniform1i("tex_displacement", 3);
		Shader.PARTICLE.setUniform1i("tex_pos", 4);
		Shader.PARTICLE.setUniform1i("enableParallaxMapping", 0);
		Shader.PARTICLE.setUniform1i("enableTexScaling", 1);
	}

	public int getUniform(String name) {
		if (locationCache.containsKey(name)) {
			return locationCache.get(name);
		}

		int result = glGetUniformLocation(ID, name);
		if (result == -1) {
			System.err.println("Could not find uniform variable " + name);
		}
		locationCache.put(name, result);
		return result;
	}

	public void setUniform1i(String name, int value) {
		if (!enabled)
			enable();
		glUniform1i(getUniform(name), value);
	}

	public void setUniform1f(String name, float value) {
		if (!enabled)
			enable();
		glUniform1f(getUniform(name), value);
	}

	public void setUniform2f(String name, float x, float y) {
		if (!enabled)
			enable();
		glUniform2f(getUniform(name), x, y);
	}

	public void setUniform3f(String name, Vec3 v) {
		if (!enabled)
			enable();
		glUniform3f(getUniform(name), v.x, v.y, v.z);
	}

	public void setUniformMat4(String name, Mat4 mat) {
		if (!enabled)
			enable();
		glUniformMatrix4fv(getUniform(name), false, mat.toFloatBuffer());
	}

	public void enable() {
		glUseProgram(ID);
		enabled = true;
	}

	public void disable() {
		glUseProgram(0);
		enabled = false;
	}

}
