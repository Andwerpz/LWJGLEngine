package lwjglengine.graphics;

import static org.lwjgl.opengl.GL20.*;

import java.util.HashMap;
import java.util.Map;

import lwjglengine.util.BufferUtils;
import lwjglengine.util.ShaderUtils;
import myutils.math.Mat4;
import myutils.math.Vec2;
import myutils.math.Vec3;

public class Shader {

	public static Shader GEOMETRY, SKYBOX, LIGHTING, DEPTH, CUBE_DEPTH, GEOM_POST_PROCESS;
	public static Shader IMG_POST_PROCESS, SPLASH, OVERWRITE_ALPHA, RENDER_BUFFER;
	public static Shader RAYTRACING, RAYTRACING_HDR, RAYTRACING_EXTRACT_BLOOM;
	public static Shader GAUSSIAN_BLUR, RENDER_ALPHA, TEXTURE3D_DISPLAY, TEXTURE_DISPLAY;
	public static Shader PARTICLE, DECAL;

	private boolean enabled = false;

	private final int ID;
	private Map<String, Integer> locationCache = new HashMap<>();

	public Shader(int programID) {
		ID = programID;
	}

	public static void init() {
		GEOMETRY = ShaderUtils.createShader("/geometry.vert", "/geometry.frag");
		SKYBOX = ShaderUtils.createShader("/skybox.vert", "/skybox.frag");
		LIGHTING = ShaderUtils.createShader("/lighting.vert", "/lighting.frag");
		DEPTH = ShaderUtils.createShader("/simpleDepthShader.vert", "/simpleDepthShader.frag");
		CUBE_DEPTH = ShaderUtils.createShader("/cubemapDepthShader.vert", "/cubemapDepthShader.frag");
		GEOM_POST_PROCESS = ShaderUtils.createShader("/geom_postprocessing.vert", "/geom_postprocessing.frag"); // post processing with geometry information
		IMG_POST_PROCESS = ShaderUtils.createShader("/img_postprocessing.vert", "/img_postprocessing.frag"); // post processing with only final color information
		SPLASH = ShaderUtils.createShader("/splash.vert", "/splash.frag"); // takes in a texture and alpha value.
		OVERWRITE_ALPHA = ShaderUtils.createShader("/splash.vert", "/overwrite_alpha.frag"); // uses the first textures color, and the second textures alpha.
		DECAL = ShaderUtils.createShader("/decal.vert", "/decal.frag");
		PARTICLE = ShaderUtils.createShader("/particle.vert", "/particle.frag");
		RAYTRACING = ShaderUtils.createShader("/raytracing.vert", "/raytracing.frag");
		RAYTRACING_HDR = ShaderUtils.createShader("/raytracing_hdr.vert", "/raytracing_hdr.frag");
		RAYTRACING_EXTRACT_BLOOM = ShaderUtils.createShader("/raytracing_extract_bloom.vert", "/raytracing_extract_bloom.frag");
		GAUSSIAN_BLUR = ShaderUtils.createShader("/gaussian_blur.vert", "/gaussian_blur.frag");
		RENDER_ALPHA = ShaderUtils.createShader("/render_alpha.vert", "/render_alpha.frag");
		TEXTURE3D_DISPLAY = ShaderUtils.createShader("/texture3d_display.vert", "/texture3d_display.frag");
		TEXTURE_DISPLAY = ShaderUtils.createShader("/texture_display.vert", "/texture_display.frag");

		Shader.GEOMETRY.setUniform1i("tex_diffuse", 0);
		Shader.GEOMETRY.setUniform1i("tex_specular", 1);
		Shader.GEOMETRY.setUniform1i("tex_shininess", 2);
		Shader.GEOMETRY.setUniform1i("tex_normal", 3);
		Shader.GEOMETRY.setUniform1i("tex_displacement", 4);
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
		Shader.DECAL.setUniform1i("tex_shininess", 2);
		Shader.DECAL.setUniform1i("tex_normal", 3);
		Shader.DECAL.setUniform1i("tex_displacement", 4);
		Shader.DECAL.setUniform1i("tex_position", 5);

		Shader.PARTICLE.setUniform1i("tex_diffuse", 0);
		Shader.PARTICLE.setUniform1i("tex_specular", 1);
		Shader.PARTICLE.setUniform1i("tex_shininess", 2);
		Shader.PARTICLE.setUniform1i("tex_normal", 3);
		Shader.PARTICLE.setUniform1i("tex_displacement", 4);
		Shader.PARTICLE.setUniform1i("tex_pos", 5);
		Shader.PARTICLE.setUniform1i("enableParallaxMapping", 0);
		Shader.PARTICLE.setUniform1i("enableTexScaling", 1);

		Shader.RAYTRACING.setUniform1i("render_tex_0", 0);
		Shader.RAYTRACING.setUniform1i("skybox_tex", 1);

		Shader.RAYTRACING_HDR.setUniform1i("tex_color", 0);
		Shader.RAYTRACING_HDR.setUniform1i("tex_bloom", 1);

		Shader.RAYTRACING_EXTRACT_BLOOM.setUniform1i("tex_color", 0);

		Shader.GAUSSIAN_BLUR.setUniform1i("tex_color", 0);

		Shader.RENDER_ALPHA.setUniform1i("tex_color", 0);

		Shader.TEXTURE3D_DISPLAY.setUniform1i("tex_color", 0);

		Shader.TEXTURE_DISPLAY.setUniform1i("tex_color", 0);
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

	public void setUniform2f(String name, Vec2 v) {
		if (!enabled)
			enable();
		glUniform2f(getUniform(name), v.x, v.y);
	}

	public void setUniform3f(String name, Vec3 v) {
		if (!enabled)
			enable();
		glUniform3f(getUniform(name), v.x, v.y, v.z);
	}

	public void setUniformMat4(String name, Mat4 mat) {
		if (!enabled)
			enable();
		glUniformMatrix4fv(getUniform(name), false, BufferUtils.createFloatBuffer(new Mat4[] { mat }));
	}

	public void enable() {
		glUseProgram(ID);
		enabled = true;
	}

	public void disable() {
		glUseProgram(0);
		enabled = false;
	}

	public void kill() {
		glDeleteShader(this.ID);
	}

}
