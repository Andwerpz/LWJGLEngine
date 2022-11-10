package model;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL33.*;

import static org.lwjgl.assimp.Assimp.*;

import java.awt.image.BufferedImage;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIColor4D;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIMaterialProperty;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIString;
import org.lwjgl.assimp.AITexture;
import org.lwjgl.assimp.AIVector2D;
import org.lwjgl.assimp.AIVector3D;

import graphics.TextureMaterial;
import graphics.Material;
import graphics.Shader;
import graphics.Texture;
import graphics.VertexArray;
import util.FileUtils;
import util.GraphicsTools;
import util.Mat4;
import util.SystemUtils;
import util.TargaReader;
import util.Vec2;
import util.Vec3;

public class Model {
	// always instanced rendering
	// in order to render with correct model matrices, you must first update the model matrices.
	// if model matrix updates aren't needed, then you shouldn't update them.

	// each model will have separate instances per scene.

	// if a model instance is bound to an entity, they will not share the same ID.
	// the entity is responsible for keeping track of the IDs of all the model instances it has.
	// the Entity class should be able to return an entity or entity ID based on a model instance ID.

	public static final Material DEFAULT_MATERIAL = Material.defaultMaterial();
	public static final TextureMaterial DEFAULT_TEXTURE_MATERIAL = TextureMaterial.defaultTextureMaterial();

	private static HashSet<Model> models = new HashSet<>();
	private static HashSet<Long> modelInstanceIDs = new HashSet<>();
	private static HashMap<Long, Integer> IDtoScene = new HashMap<>(); // which scene each instance is in
	private static HashMap<Long, Model> IDtoModel = new HashMap<>();

	// for each scene, which model instances have active collision
	private static HashMap<Integer, HashSet<Long>> activeCollisionMeshes = new HashMap<>();

	// first, specify which scene
	// K : model instance ID, Can be translated to a color to draw
	// V : info for that model instance.
	private HashMap<Integer, HashMap<Long, Mat4>> modelMats;
	private HashMap<Integer, HashMap<Long, ArrayList<Material>>> materials;

	private ArrayList<Integer> scenesNeedingUpdates;

	// per model 3D vertex information
	protected ArrayList<VertexArray> meshes;

	// default per instance traditional blinn-phong Ka, Ks, Kd
	protected ArrayList<Material> defaultMaterials;

	// like material, but uses a texture2D sampler instead. Is multiplied by the instanced material to get the final result.
	protected ArrayList<TextureMaterial> textureMaterials;

	// created in init(), stores the same data that meshes stores, just all in
	// triangles.
	private ArrayList<CollisionMesh> collisionMeshes;

	public Model() {
		this.meshes = new ArrayList<>();
		this.defaultMaterials = new ArrayList<>();
		this.textureMaterials = new ArrayList<>();
		this.create();

		init();
	}

	public Model(String filepath, String filename) {
		this.loadModelFile(filepath, filename);

		init();
	}

	public Model(VertexArray mesh, TextureMaterial material) {
		this.meshes = new ArrayList<VertexArray>(Arrays.asList(mesh));
		this.defaultMaterials = new ArrayList<Material>(Arrays.asList(DEFAULT_MATERIAL));
		this.textureMaterials = new ArrayList<TextureMaterial>(Arrays.asList(material));

		init();
	}

	private void init() {
		this.collisionMeshes = new ArrayList<>();
		for (VertexArray vao : meshes) {
			this.collisionMeshes.add(new CollisionMesh(vao));
		}
		this.scenesNeedingUpdates = new ArrayList<Integer>();
		this.modelMats = new HashMap<Integer, HashMap<Long, Mat4>>();
		this.materials = new HashMap<Integer, HashMap<Long, ArrayList<Material>>>();
		models.add(this);
	}

	public static long generateNewID() {
		long ans = 0;
		while (ans == 0 || modelInstanceIDs.contains(ans)) {
			ans = (long) (Math.random() * 256) + (long) (Math.random() * 256) * 1000l + (long) (Math.random() * 256) * 1000000l;
		}
		return ans;
	}

	public static long convertRGBToID(Vec3 rgb) {
		return (long) rgb.x * 1000000l + (long) rgb.y * 1000l + (long) rgb.z;
	}

	public static Vec3 convertIDToRGB(long ID) {
		return new Vec3((ID / 1000000) % 1000, (ID / 1000) % 1000, ID % 1000);
	}

	public void setTextureMaterial(TextureMaterial m, int index) {
		if (index >= this.textureMaterials.size()) {
			System.err.println("Texture material index out of bounds");
			return;
		}
		this.textureMaterials.get(index).kill();
		this.textureMaterials.set(index, m);
	}

	public void setTextureMaterial(TextureMaterial m) {
		this.setTextureMaterial(m, 0);
	}

	public void create() {
	}

	// must have .mtl file to be able to load materials
	private void loadModelFile(String filepath, String filename) {
		System.out.println("LOADING MESH: " + filename);

		this.meshes = new ArrayList<>();
		this.defaultMaterials = new ArrayList<>();
		this.textureMaterials = new ArrayList<>();

		String workingDirectory = SystemUtils.getWorkingDirectory();

		AIScene scene = aiImportFile(workingDirectory + "/res" + filepath + filename, aiProcess_Triangulate | aiProcess_JoinIdenticalVertices);

		// group meshes with the same material
		ArrayList<ArrayList<Vec3>> vertices = new ArrayList<>();
		ArrayList<ArrayList<Vec2>> uvs = new ArrayList<>();
		ArrayList<ArrayList<Integer>> indices = new ArrayList<>();

		for (int i = 0; i < scene.mNumMaterials(); i++) {
			vertices.add(new ArrayList<>());
			uvs.add(new ArrayList<>());
			indices.add(new ArrayList<>());
		}

		PointerBuffer buffer = scene.mMeshes();

		System.out.println("meshes: " + buffer.limit());

		for (int i = 0; i < buffer.limit(); i++) {
			AIMesh mesh = AIMesh.create(buffer.get(i));

			int matIndex = mesh.mMaterialIndex();
			int indexOffset = vertices.get(matIndex).size();

			AIVector3D.Buffer AIVertices = mesh.mVertices();
			for (int j = 0; j < AIVertices.limit(); j++) {
				AIVector3D v = AIVertices.get(j);
				vertices.get(matIndex).add(new Vec3(v.x(), v.y(), v.z()));
			}

			AIVector3D.Buffer AIuvs = mesh.mTextureCoords(0);
			for (int j = 0; j < AIuvs.limit(); j++) {
				AIVector3D v = AIuvs.get(j);
				uvs.get(matIndex).add(new Vec2(v.x(), v.y()));
			}

			AIFace.Buffer AIFaces = mesh.mFaces();
			for (int j = 0; j < AIFaces.limit(); j++) {
				AIFace f = AIFaces.get(j);
				indices.get(matIndex).add(f.mIndices().get(0) + indexOffset);
				indices.get(matIndex).add(f.mIndices().get(1) + indexOffset);
				indices.get(matIndex).add(f.mIndices().get(2) + indexOffset);
			}
		}

		PointerBuffer materials = scene.mMaterials(); // array of pointers to AIMaterial structs
		for (int i = 0; i < scene.mNumMaterials(); i++) {
			if (vertices.get(i).size() == 0) { // empty mesh, don't load
				continue;
			}

			// put vertex data into buffers
			float[] vArr = new float[vertices.get(i).size() * 3];
			float[] uvArr = new float[uvs.get(i).size() * 2];
			int[] iArr = new int[indices.get(i).size()];

			for (int j = 0; j < vertices.get(i).size(); j++) {
				vArr[j * 3 + 0] = vertices.get(i).get(j).x;
				vArr[j * 3 + 1] = vertices.get(i).get(j).y;
				vArr[j * 3 + 2] = vertices.get(i).get(j).z;
			}

			for (int j = 0; j < uvs.get(i).size(); j++) {
				uvArr[j * 2 + 0] = uvs.get(i).get(j).x;
				uvArr[j * 2 + 1] = uvs.get(i).get(j).y;
			}

			for (int j = 0; j < indices.get(i).size(); j++) {
				iArr[j] = indices.get(i).get(j);
			}

			System.out.println("vertices: " + vertices.get(i).size() + " | faces: " + (indices.get(i).size() / 3));
			this.meshes.add(new VertexArray(vArr, uvArr, iArr, GL_TRIANGLES));

			// load material data
			AIMaterial AIMat = AIMaterial.create(materials.get(i)); // wrap raw pointer in AIMaterial instance
			TextureMaterial material = TextureMaterial.defaultTextureMaterial();
			AIString path;

			// traditional phong-blinn Material
			boolean errorExtractingMaterial = false;
			AIColor4D diffuseColor = AIColor4D.create();
			if (aiGetMaterialColor(AIMat, AI_MATKEY_COLOR_DIFFUSE, aiTextureType_NONE, 0, diffuseColor) != 0) {
				errorExtractingMaterial = true;
			}

			AIColor4D specularColor = AIColor4D.create();
			if (aiGetMaterialColor(AIMat, AI_MATKEY_COLOR_SPECULAR, aiTextureType_NONE, 0, specularColor) != 0) {
				errorExtractingMaterial = true;
			}

			AIColor4D shininessColor = AIColor4D.create();
			if (aiGetMaterialColor(AIMat, AI_MATKEY_SHININESS, aiTextureType_NONE, 0, shininessColor) != 0) {
				errorExtractingMaterial = true;
			}

			Material mat = Material.defaultMaterial();
			if (!errorExtractingMaterial) {
				mat = new Material(diffuseColor, specularColor, shininessColor);
			}
			this.defaultMaterials.add(mat);
			System.out.println(mat);

			// TextureMaterial
			// map_Kd in .mtl
			path = AIString.calloc();
			aiGetMaterialTexture(AIMat, aiTextureType_DIFFUSE, 0, path, (IntBuffer) null, null, null, null, null, null);
			String diffusePath = path.dataString();
			if (diffusePath != null && diffusePath.length() != 0) {
				Texture diffuseTexture = new Texture(loadImage(filepath + diffusePath));
				material.setTexture(diffuseTexture, TextureMaterial.DIFFUSE);
			}

			// map_Ks in .mtl
			path = AIString.calloc();
			aiGetMaterialTexture(AIMat, aiTextureType_SPECULAR, 0, path, (IntBuffer) null, null, null, null, null, null);
			String specularPath = path.dataString();
			if (specularPath != null && specularPath.length() != 0) {
				Texture specularTexture = new Texture(loadImage(filepath + specularPath));
				material.setTexture(specularTexture, TextureMaterial.SPECULAR);
			}

			// norm in .mtl
			path = AIString.calloc();
			aiGetMaterialTexture(AIMat, aiTextureType_NORMALS, 0, path, (IntBuffer) null, null, null, null, null, null);
			String normalsPath = path.dataString();
			if (normalsPath != null && normalsPath.length() != 0) {
				Texture normalsTexture = new Texture(loadImage(filepath + normalsPath));
				material.setTexture(normalsTexture, TextureMaterial.NORMAL);
			}

			this.textureMaterials.add(material);
		}

		System.out.println("SUCCESS");
	}

	public static BufferedImage loadImage(String path) {
		String fileExtension = FileUtils.getFileExtension(path);
		switch (fileExtension) {
		case "png":
			return GraphicsTools.verticalFlip(FileUtils.loadImage(path));

		case "jpg":
			return GraphicsTools.verticalFlip(FileUtils.loadImage(path));

		case "jpeg":
			return GraphicsTools.verticalFlip(FileUtils.loadImage(path));

		case "tga":
			return TargaReader.getImage(SystemUtils.getWorkingDirectory() + "\\res" + path);
		}

		System.err.println("File extension " + fileExtension + " is not supported");
		return null;
	}

	public static int getScene(long ID) {
		return IDtoScene.get(ID);
	}

	public static Model getModel(long ID) {
		return IDtoModel.get(ID);
	}

	// creates a new instance, and returns the id associated with that instance
	public static long addInstance(Model model, Mat4 mat4, int scene) {
		long ID = generateNewID();
		if (model.modelMats.get(scene) == null) {
			model.modelMats.put(scene, new HashMap<Long, Mat4>());
		}
		if (model.materials.get(scene) == null) {
			model.materials.put(scene, new HashMap<Long, ArrayList<Material>>());
		}
		IDtoScene.put(ID, scene);
		IDtoModel.put(ID, model);
		modelInstanceIDs.add(ID);
		model.modelMats.get(scene).put(ID, mat4);
		model.materials.get(scene).put(ID, new ArrayList<Material>(model.defaultMaterials));
		model.scenesNeedingUpdates.add(scene);

		System.out.println("ADD MODEL INSTANCE " + ID);

		return ID;
	}

	public static void removeInstance(long ID) {
		Model model = IDtoModel.get(ID);
		if (model == null) { // couldn't find model to remove
			// could happen if you try to kill an entity after removing all models from a scene
			return;
		}
		int scene = IDtoScene.get(ID);

		model.modelMats.get(scene).remove(ID);
		if (model.modelMats.get(scene).size() == 0) {
			model.modelMats.remove(scene);
		}

		model.materials.get(scene).remove(ID);
		if (model.materials.get(scene).size() == 0) {
			model.materials.remove(scene);
		}

		deactivateCollisionMesh(ID);
		IDtoScene.remove(ID);
		IDtoModel.remove(ID);
		modelInstanceIDs.remove(ID);

		if (model.modelMats.containsKey(scene)) {
			model.scenesNeedingUpdates.add(scene);
		}

		System.out.println("REMOVE MODEL INSTANCE " + ID);
	}

	public static void updateInstance(long ID, Mat4 mat4) {
		Model model = IDtoModel.get(ID);
		if (IDtoScene.get(ID) == null) {
			System.out.println(ID);
		}
		int scene = IDtoScene.get(ID);
		model.modelMats.get(scene).put(ID, mat4);
		model.scenesNeedingUpdates.add(scene);
	}

	public static void updateInstance(long ID, Material material, int index) {
		Model model = IDtoModel.get(ID);
		int scene = IDtoScene.get(ID);
		if (model.materials.get(scene).get(ID).size() <= index) {
			System.err.println("Material index " + index + " out of bounds");
			return;
		}
		model.materials.get(scene).get(ID).set(index, new Material(material));
		model.scenesNeedingUpdates.add(scene);
	}

	public static void updateInstance(long ID, Material material) {
		Model.updateInstance(ID, material, 0);
	}

	public static void activateCollisionMesh(long ID) {
		if (!modelInstanceIDs.contains(ID)) {
			return;
		}
		int scene = IDtoScene.get(ID);
		if (activeCollisionMeshes.get(scene) == null) {
			activeCollisionMeshes.put(scene, new HashSet<Long>());
		}
		activeCollisionMeshes.get(scene).add(ID);
	}

	public static void deactivateCollisionMesh(long ID) {
		if (!IDtoScene.containsKey(ID)) {
			return;
		}
		int scene = IDtoScene.get(ID);
		if (activeCollisionMeshes.get(scene) == null) {
			return;
		}
		activeCollisionMeshes.get(scene).remove(ID);
		if (activeCollisionMeshes.get(scene).size() == 0) {
			activeCollisionMeshes.remove(scene);
		}
	}

	public static void updateModels() {
		for (Model m : models) {
			if (m.scenesNeedingUpdates.size() != 0) {
				m.updateModelMats();
			}
		}
	}

	private void updateModelMats() {
		if (this.scenesNeedingUpdates.size() == 0) {
			return;
		}

		for (int scene : scenesNeedingUpdates) {
			if (this.modelMats.get(scene) == null) {
				continue;
			}

			// which vertex array, model instance ID, material for the model instance ID for
			// that vertex array.
			ArrayList<HashMap<Long, Material>> instancedMaterials = new ArrayList<>();
			int vertexArrayAmt = this.meshes.size();
			for (int i = 0; i < vertexArrayAmt; i++) {
				instancedMaterials.add(new HashMap<Long, Material>());
			}

			for (long ID : this.modelMats.get(scene).keySet()) {
				ArrayList<Material> matArr = this.materials.get(scene).get(ID);
				if (matArr == null) {
					System.out.println("DIDNT SET MATERIALS " + ID);
					matArr = this.defaultMaterials;
				}
				for (int i = 0; i < vertexArrayAmt; i++) {
					instancedMaterials.get(i).put(ID, matArr.get(i));
				}
			}

			for (int i = 0; i < this.meshes.size(); i++) {
				VertexArray v = this.meshes.get(i);
				v.updateInstances(this.modelMats.get(scene), instancedMaterials.get(i), scene);
			}
		}

		this.scenesNeedingUpdates.clear();
	}

	public static ArrayList<Vec3[]> rayIntersect(int scene, Vec3 ray_origin, Vec3 ray_dir) {
		ArrayList<Vec3[]> result = new ArrayList<>();
		if (activeCollisionMeshes.get(scene) == null) {
			return result;
		}
		for (long ID : activeCollisionMeshes.get(scene)) {
			if (!Model.modelInstanceIDs.contains(ID)) {
				System.out.println("something is wrong " + ID);
				continue;
			}
			Model model = IDtoModel.get(ID);
			Mat4 transform = model.modelMats.get(scene).get(ID);
			for (CollisionMesh c : model.collisionMeshes) {
				result.addAll(c.rayIntersect(ray_origin, ray_dir, transform));
			}
		}
		return result;
	}

	public static ArrayList<Vec3[]> sphereIntersect(int scene, Vec3 sphere_origin, float sphere_radius) {
		ArrayList<Vec3[]> result = new ArrayList<>();
		if (activeCollisionMeshes.get(scene) == null) {
			return result;
		}
		for (long ID : activeCollisionMeshes.get(scene)) {
			if (!Model.modelInstanceIDs.contains(ID)) {
				System.out.println("something is wrong " + ID);
				continue;
			}
			Model model = IDtoModel.get(ID);
			Mat4 transform = model.modelMats.get(scene).get(ID);
			for (CollisionMesh c : model.collisionMeshes) {
				result.addAll(c.sphereIntersect(sphere_origin, sphere_radius, transform));
			}
		}
		return result;
	}

	public static ArrayList<Vec3[]> capsuleIntersect(int scene, Vec3 capsule_bottom, Vec3 capsule_top, float capsule_radius) {
		ArrayList<Vec3[]> result = new ArrayList<>();
		if (activeCollisionMeshes.get(scene) == null) {
			return result;
		}
		for (long ID : activeCollisionMeshes.get(scene)) {
			if (!Model.modelInstanceIDs.contains(ID)) {
				System.out.println("something is wrong " + ID);
				continue;
			}
			Model model = IDtoModel.get(ID);
			Mat4 transform = model.modelMats.get(scene).get(ID);
			for (CollisionMesh c : model.collisionMeshes) {
				result.addAll(c.capsuleIntersect(capsule_bottom, capsule_top, capsule_radius, transform));
			}
		}
		return result;
	}

	// removes all model instances from the given scene.
	public static void removeInstancesFromScene(int scene) {
		for (Model m : models) {
			if (m.modelMats.get(scene) == null) {
				continue;
			}
			ArrayList<Long> instanceIDs = new ArrayList<>();
			instanceIDs.addAll(m.modelMats.get(scene).keySet());
			for (long id : instanceIDs) {
				Model.removeInstance(id);
			}
		}
	}

	public static void renderModels(int scene) {
		for (Model m : models) {
			m.render(scene);
		}
	}

	protected void render(int scene) {
		if (modelMats.get(scene) == null) { // check whether or not this model actually has any instances in the
			// specified scene
			return;
		}
		for (int i = 0; i < meshes.size(); i++) {
			if (i < this.textureMaterials.size() && this.textureMaterials.get(i) != null) {
				this.textureMaterials.get(i).bind();
			}
			else {
				DEFAULT_TEXTURE_MATERIAL.bind();
			}

			this.meshes.get(i).render(scene);
		}
	}

	public void kill() {
		ArrayList<Long> instanceIDs = new ArrayList<>();
		for (HashMap<Long, Mat4> i : this.modelMats.values()) {
			for (Long id : i.keySet()) {
				instanceIDs.add(id);
			}
		}

		for (long id : instanceIDs) {
			modelInstanceIDs.remove(id);
			IDtoScene.remove(id);
			IDtoModel.remove(id);
		}

		for (TextureMaterial t : this.textureMaterials) {
			t.kill();
		}

		for (VertexArray v : this.meshes) {
			v.kill();
		}

		models.remove(this);
	}
}
