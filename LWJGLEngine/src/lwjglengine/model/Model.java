package lwjglengine.model;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL33.*;

import static org.lwjgl.assimp.Assimp.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

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
import org.lwjgl.assimp.Assimp;

import lwjglengine.graphics.Material;
import lwjglengine.graphics.Shader;
import lwjglengine.graphics.Texture;
import lwjglengine.graphics.TextureMaterial;
import myutils.file.FileUtils;
import myutils.file.SystemUtils;
import myutils.file.TargaReader;
import myutils.graphics.GraphicsTools;
import myutils.math.Mat4;
import myutils.math.Vec2;
import myutils.math.Vec3;

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

	private static long updateModelsMillis; //how long did it take the last tick to update all the models
	private static HashMap<Integer, Long> renderModelsMillis = new HashMap<>(); //likewise but for rendering. 

	private static HashSet<Model> models = new HashSet<>();

	private static HashMap<Long, ModelInstance> IDtoInstance = new HashMap<>();

	private static HashSet<Long> modelInstanceIDs = new HashSet<>();
	private static HashMap<Long, Integer> IDtoScene = new HashMap<>(); // which scene each instance is in
	private static HashMap<Long, Model> IDtoModel = new HashMap<>();
	
	// for each scene, which model instances have active collision
	// this system really needs a rework. 
	private static HashMap<Integer, HashSet<Long>> activeCollisionMeshes = new HashMap<>();

	//for each model, map each scene to a set of model instance ids
	//I use a tree set here to give all the instances some ordering. 
	//this is required for AnimatedModel to work properly. 
	protected HashMap<Integer, TreeSet<Long>> sceneToID;

	protected HashSet<Integer> scenesNeedingUpdates;

	// per mesh 3D vertex information. Should be in the same order that Assimp loaded them. 
	// these should not change after initialization
	protected ArrayList<VertexArray> meshes;

	// default per instance traditional blinn-phong Ka, Ks, Kd
	protected ArrayList<Material> defaultMaterials;

	// like material, but uses a texture2D sampler instead. Is multiplied by the instanced material to get the final result.
	protected ArrayList<TextureMaterial> textureMaterials;

	// created in init(), stores the same data that meshes stores, just all in triangles.
	private ArrayList<CollisionMesh> collisionMeshes;
	
	public Model(AIScene aiscene, String parentFilepath) throws IOException {
		ArrayList<VertexArray> meshes = new ArrayList<>();
		ArrayList<Material> defaultMaterials = new ArrayList<>();
		ArrayList<TextureMaterial> textureMaterials = new ArrayList<>();

		// group meshes with the same material
		ArrayList<ArrayList<Vec3>> vertices = new ArrayList<>();
		ArrayList<ArrayList<Vec2>> uvs = new ArrayList<>();
		ArrayList<ArrayList<Integer>> indices = new ArrayList<>();

		for (int i = 0; i < aiscene.mNumMaterials(); i++) {
			vertices.add(new ArrayList<>());
			uvs.add(new ArrayList<>());
			indices.add(new ArrayList<>());
		}

		PointerBuffer buffer = aiscene.mMeshes();

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
			if (AIuvs == null) {
				System.err.println("Model : Mesh UVS missing");
				for (int j = 0; j < AIVertices.limit(); j++) {
					uvs.get(matIndex).add(new Vec2(Math.random(), Math.random()));
				}
			}
			else {
				for (int j = 0; j < AIuvs.limit(); j++) {
					AIVector3D v = AIuvs.get(j);
					uvs.get(matIndex).add(new Vec2(v.x(), v.y()));
				}
			}

			AIFace.Buffer AIFaces = mesh.mFaces();
			for (int j = 0; j < AIFaces.limit(); j++) {
				AIFace f = AIFaces.get(j);
				indices.get(matIndex).add(f.mIndices().get(0) + indexOffset);
				indices.get(matIndex).add(f.mIndices().get(1) + indexOffset);
				indices.get(matIndex).add(f.mIndices().get(2) + indexOffset);
			}
		}

		PointerBuffer materials = aiscene.mMaterials(); // array of pointers to AIMaterial structs
		for (int i = 0; i < aiscene.mNumMaterials(); i++) {
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
			meshes.add(new VertexArray(vArr, uvArr, iArr, GL_TRIANGLES));

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
			defaultMaterials.add(mat);
			System.out.println(mat);

			// TextureMaterial
			// map_Kd in .mtl
			path = AIString.calloc();
			aiGetMaterialTexture(AIMat, aiTextureType_DIFFUSE, 0, path, (IntBuffer) null, null, null, null, null, null);
			String diffusePath = path.dataString();
			if (diffusePath != null && diffusePath.length() != 0) {
				try {
					Texture diffuseTexture = new Texture(loadImage(parentFilepath + diffusePath));
					material.setTexture(diffuseTexture, TextureMaterial.DIFFUSE);
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}

			// map_Ks in .mtl
			path = AIString.calloc();
			aiGetMaterialTexture(AIMat, aiTextureType_SPECULAR, 0, path, (IntBuffer) null, null, null, null, null, null);
			String specularPath = path.dataString();
			if (specularPath != null && specularPath.length() != 0) {
				try {
					Texture specularTexture = new Texture(loadImage(parentFilepath + specularPath));
					material.setTexture(specularTexture, TextureMaterial.SPECULAR);
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}

			// map_Ns in .mtl
			path = AIString.calloc();
			aiGetMaterialTexture(AIMat, aiTextureType_SHININESS, 0, path, (IntBuffer) null, null, null, null, null, null);
			String shininessPath = path.dataString();
			if (shininessPath != null && shininessPath.length() != 0) {
				try {
					Texture shininessTexture = new Texture(loadImage(parentFilepath + shininessPath));
					material.setTexture(shininessTexture, TextureMaterial.SHININESS);
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}

			// norm or map_Kn in .mtl
			path = AIString.calloc();
			aiGetMaterialTexture(AIMat, aiTextureType_NORMALS, 0, path, (IntBuffer) null, null, null, null, null, null);
			String normalsPath = path.dataString();
			if (normalsPath != null && normalsPath.length() != 0) {
				try {
					Texture normalsTexture = new Texture(loadImage(parentFilepath + normalsPath));
					material.setTexture(normalsTexture, TextureMaterial.NORMAL);
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}

			textureMaterials.add(material);
		}

		this.init(meshes, defaultMaterials, textureMaterials);
	}

	public Model(ArrayList<VertexArray> meshes, ArrayList<Material> defaultMaterials, ArrayList<TextureMaterial> textureMaterials) {
		this.init(meshes, defaultMaterials, textureMaterials);
	}

	public Model(VertexArray mesh) {
		ArrayList<VertexArray> meshes = new ArrayList<VertexArray>(Arrays.asList(mesh));
		ArrayList<Material> defaultMaterials = new ArrayList<Material>(Arrays.asList(DEFAULT_MATERIAL));
		ArrayList<TextureMaterial> textureMaterials = new ArrayList<TextureMaterial>(Arrays.asList(TextureMaterial.defaultTextureMaterial()));
		this.init(meshes, defaultMaterials, textureMaterials);
	}

	public Model(VertexArray mesh, TextureMaterial material) {
		ArrayList<VertexArray> meshes = new ArrayList<VertexArray>(Arrays.asList(mesh));
		ArrayList<Material> defaultMaterials = new ArrayList<Material>(Arrays.asList(DEFAULT_MATERIAL));
		ArrayList<TextureMaterial> textureMaterials = new ArrayList<TextureMaterial>(Arrays.asList(material));
		this.init(meshes, defaultMaterials, textureMaterials);
	}

	private void init(ArrayList<VertexArray> meshes, ArrayList<Material> defaultMaterials, ArrayList<TextureMaterial> textureMaterials) {
		this.meshes = meshes;
		this.defaultMaterials = defaultMaterials;
		this.textureMaterials = textureMaterials;
		
		this.collisionMeshes = new ArrayList<>();
		for (VertexArray vao : meshes) {
			this.collisionMeshes.add(new CollisionMesh(vao));
		}
		this.scenesNeedingUpdates = new HashSet<Integer>();
		this.sceneToID = new HashMap<Integer, TreeSet<Long>>();
		models.add(this);
	}

	public static int getNumInstances() {
		return modelInstanceIDs.size();
	}

	public static int getNumModels() {
		return models.size();
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
	
	public static ModelInstance getModelInstanceFromID(long ID) {
		return IDtoInstance.get(ID);
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

	public ArrayList<VertexArray> getMeshes() {
		return this.meshes;
	}

	/**
	 * When loading .obj files, must have .mtl file to be able to load materials
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static Model loadModelFile(File file, int flags) {
		String filepath = file.getAbsolutePath();
		String parentFilepath = file.getParent() + "\\";

		System.out.println("LOADING MODEL: " + file.getName());

		AIScene scene = aiImportFile(filepath, aiProcess_Triangulate | aiProcess_JoinIdenticalVertices | flags);

		if (scene == null) {
			System.err.println("Failed to load model " + file.getName());
			String error = Assimp.aiGetErrorString();
			System.err.println("Assimp Error: " + error);
			return null;
		}

		Model model;
		try {
			model = new Model(scene, parentFilepath);
		} catch (IOException e) {
			System.out.println("FAILED");
			e.printStackTrace();
			return null;
		}
		System.out.println("SUCCESS");
		
		return model;
	}
	
	public static Model loadModelFile(File file) {
		return Model.loadModelFile(file, 0);
	}

	public static Model loadModelFile(String filepath) throws IOException {
		return Model.loadModelFile(FileUtils.loadFile(filepath), 0);
	}
	
	public static Model loadModelFile(String filepath, int flags) {
		return Model.loadModelFile(FileUtils.loadFile(filepath), flags);
	}

	public static Model loadModelFileRelative(String relativeFilepath) throws IOException {
		return Model.loadModelFile(FileUtils.generateAbsoluteFilepath(relativeFilepath));
	}
	
	public static Model loadModelFileRelative(String relativeFilepath, int flags) {
		return Model.loadModelFile(FileUtils.generateAbsoluteFilepath(relativeFilepath), flags);
	}

	public static BufferedImage loadImage(String path) throws IOException {
		String fileExtension = FileUtils.getFileExtension(path);
		switch (fileExtension) {
		case "png":
			return FileUtils.loadImage(path);

		case "jpg":
			return FileUtils.loadImage(path);

		case "jpeg":
			return FileUtils.loadImage(path);

		case "tga":
			return TargaReader.getImage(path);
		}

		throw new IOException("File extension " + fileExtension + " is not supported");
	}

	public static int getScene(long ID) {
		return IDtoScene.get(ID);
	}

	public static Model getModel(long ID) {
		return IDtoModel.get(ID);
	}

	/**
	 * called by ModelInstance upon construction, will register a new model instance id, and return it. 
	 * @param model
	 * @param mat4
	 * @param scene
	 * @return
	 */
	protected static long addInstance(Model model, ModelTransform transform, int scene, ModelInstance instance) {
		long ID = generateNewID();
		if (model.sceneToID.get(scene) == null) {
			model.sceneToID.put(scene, new TreeSet<Long>());
		}

		IDtoInstance.put(ID, instance);
		IDtoScene.put(ID, scene);
		IDtoModel.put(ID, model);
		modelInstanceIDs.add(ID);

		model.sceneToID.get(scene).add(ID);
		model.scenesNeedingUpdates.add(scene);

		System.out.println("ADD MODEL INSTANCE " + ID);

		return ID;
	}

	protected static void removeInstance(long ID) {
		Model model = IDtoModel.get(ID);
		if (model == null) { // couldn't find model to remove
			// could happen if you try to kill an entity after removing all models from a scene
			return;
		}
		int scene = IDtoScene.get(ID);

		model.sceneToID.get(scene).remove(ID);
		if (model.sceneToID.get(scene).size() == 0) {
			model.sceneToID.remove(scene);
		}

		Model.deactivateCollisionMesh(ID);
		IDtoInstance.remove(ID);
		IDtoScene.remove(ID);
		IDtoModel.remove(ID);
		modelInstanceIDs.remove(ID);

		//if there are still instances left in this scene, then we should update them. 
		if (model.sceneToID.containsKey(scene)) {
			model.scenesNeedingUpdates.add(scene);
		}

		System.out.println("REMOVE MODEL INSTANCE " + ID);
	}

	protected static void updateInstance(long ID) {
		if (IDtoScene.get(ID) == null) {
			System.err.println("Model: Can't find model instance " + ID + " scene when updating");
			System.exit(0);
			return;
		}
		int scene = IDtoScene.get(ID);
		Model model = IDtoModel.get(ID);
		model.scenesNeedingUpdates.add(scene);
	}

	public static Material getMaterial(long ID, int index) {
		return Model.IDtoInstance.get(ID).getMaterials().get(index);
	}

	public static Material getMaterial(long ID) {
		return Model.getMaterial(ID, 0);
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
		long startMillis = System.currentTimeMillis();
		for (Model m : models) {
			if (m.scenesNeedingUpdates.size() != 0) {
				m.updateModelMats();
			}
		}
		updateModelsMillis = System.currentTimeMillis() - startMillis;
	}

	protected void updateModelMats() {
		if (this.scenesNeedingUpdates.size() == 0) {
			return;
		}

		for (int scene : this.scenesNeedingUpdates) {
			if (this.sceneToID.get(scene) == null) {
				continue;
			}
			
			ArrayList<ModelInstance> instances = new ArrayList<>();
			for(long ID : this.sceneToID.get(scene)) {
				instances.add(IDtoInstance.get(ID));
			}
			for(int i = 0; i < this.meshes.size(); i++) {
				VertexArray v = this.meshes.get(i);
				v.updateInstances(instances, i, scene);
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
				System.err.println("Model: something is wrong with " + ID);
				continue;
			}
			Model model = IDtoModel.get(ID);
			Mat4 transform = IDtoInstance.get(ID).getModelTransform().getModelMatrix();
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
				System.err.println("Model: something is wrong " + ID);
				continue;
			}
			Model model = IDtoModel.get(ID);
			Mat4 transform = IDtoInstance.get(ID).getModelTransform().getModelMatrix();
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
				System.err.println("Model: something is wrong " + ID);
				continue;
			}
			Model model = IDtoModel.get(ID);
			Mat4 transform = IDtoInstance.get(ID).getModelTransform().getModelMatrix();
			for (CollisionMesh c : model.collisionMeshes) {
				result.addAll(c.capsuleIntersect(capsule_bottom, capsule_top, capsule_radius, transform));
			}
		}
		return result;
	}

	// removes all model instances from the given scene.
	public static void removeInstancesFromScene(int scene) {
		for (Model m : models) {
			if (m.sceneToID.get(scene) == null) {
				continue;
			}
			ArrayList<Long> instanceIDs = new ArrayList<>();
			instanceIDs.addAll(m.sceneToID.get(scene));
			for (long id : instanceIDs) {
				Model.removeInstance(id);
			}
		}
	}
	
	//TODO only render models that have model instances in that scene
	public static void renderModels(int scene) {
		long startMillis = System.currentTimeMillis();
		for (Model m : models) {
			m.render(scene);
		}
		renderModelsMillis.put(scene, System.currentTimeMillis() - startMillis);
	}

	protected void render(int scene) {
		if (this.sceneToID.get(scene) == null) { // check whether or not this model actually has any instances in the specified scene
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
		//dispose of all model instances
		ArrayList<Long> instanceIDs = new ArrayList<>();
		for (TreeSet<Long> i : this.sceneToID.values()) {
			for (Long id : i) {
				instanceIDs.add(id);
			}
		}

		for (long id : instanceIDs) {
			Model.removeInstance(id);
		}

		//this is fine, i think
		//we should demand that a vertex array can only be used in one model
		for (VertexArray v : this.meshes) {
			v.kill();
		}

		models.remove(this);
	}

	public static void printAliveInstanceIDs() {
		System.out.println("ALIVE INSTANCE IDS : ");
		for (Long id : modelInstanceIDs) {
			System.out.println(id);
		}
	}

	public static long getUpdateModelsMillis() {
		return updateModelsMillis;
	}

	public static long getRenderModelsMillis(int scene) {
		if (renderModelsMillis.containsKey(scene)) {
			return renderModelsMillis.get(scene);
		}
		return -1;
	}
}
