package model;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;

import java.util.HashMap;

import graphics.Cubemap;
import graphics.Texture;

public class AssetManager {
	// manages all of our assets

	// when loading, check against the asset manager to make sure you haven't loaded
	// that
	// particular asset before.

	public static HashMap<String, Model> models = new HashMap<>();
	public static HashMap<String, Texture> textures = new HashMap<>();
	public static HashMap<String, Cubemap> skyboxes = new HashMap<>();

	private static HashMap<String, String[]> paths = new HashMap<>();

	// bind all asset names to load paths
	public static void init() {
		paths.put("dust2", new String[] { "/dust2/", "dust2_blend.obj" });
		paths.put("ak47", new String[] { "/ak47/", "ak47.obj" });
		paths.put("m4a4", new String[] { "/m4a4/", "m4a4.obj" });
		paths.put("usps", new String[] { "/usps/", "usps.obj" });
		paths.put("awp", new String[] { "/awp/", "awp.obj" });
		paths.put("sphere", new String[] { "/sphere/", "sphere.obj" });
		paths.put("cylinder", new String[] { "/cylinder/", "cylinder.obj" });

		paths.put("blood_splatter_texture", new String[] { "decal/blood_splatter.png", GL_NEAREST + "" });
		paths.put("bullet_hole_texture", new String[] { "decal/bullet_hole.png", GL_NEAREST + "" });

		paths.put("lake_skybox", new String[] { "/skybox/lake/right.jpg", "/skybox/lake/left.jpg", "/skybox/lake/top.jpg", "/skybox/lake/bottom.jpg", "/skybox/lake/front.jpg", "/skybox/lake/back.jpg" });
		paths.put("stars_skybox", new String[] { "/skybox/stars/right.png", "/skybox/stars/left.png", "/skybox/stars/top.png", "/skybox/stars/bottom.png", "/skybox/stars/front.png", "/skybox/stars/back.png" });
	}

	public static void loadModel(String name) {
		String[] p = paths.get(name);
		models.put(name, new Model(p[0], p[1]));
	}

	public static void loadTexture(String name) {
		String[] p = paths.get(name);
		textures.put(name, new Texture(p[0], false, false, false, Integer.parseInt(p[1])));
	}

	public static void loadSkybox(String name) {
		String[] p = paths.get(name);
		skyboxes.put(name, new Cubemap(p));
	}

	public static Model getModel(String name) {
		if (paths.get(name) == null) {
			return null;
		}
		if (models.get(name) == null) {
			loadModel(name);
		}
		return models.get(name);
	}

	public static Texture getTexture(String name) {
		if (paths.get(name) == null) {
			return null;
		}
		if (textures.get(name) == null) {
			loadTexture(name);
		}
		return textures.get(name);
	}

	public static Cubemap getSkybox(String name) {
		if (paths.get(name) == null) {
			return null;
		}
		if (skyboxes.get(name) == null) {
			loadSkybox(name);
		}
		return skyboxes.get(name);
	}
}
