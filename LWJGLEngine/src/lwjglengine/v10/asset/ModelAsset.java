package lwjglengine.v10.asset;

import static org.lwjgl.assimp.Assimp.AI_MATKEY_COLOR_DIFFUSE;
import static org.lwjgl.assimp.Assimp.AI_MATKEY_COLOR_SPECULAR;
import static org.lwjgl.assimp.Assimp.AI_MATKEY_SHININESS;
import static org.lwjgl.assimp.Assimp.aiGetMaterialColor;
import static org.lwjgl.assimp.Assimp.aiGetMaterialTexture;
import static org.lwjgl.assimp.Assimp.aiImportFile;
import static org.lwjgl.assimp.Assimp.aiProcess_JoinIdenticalVertices;
import static org.lwjgl.assimp.Assimp.aiProcess_Triangulate;
import static org.lwjgl.assimp.Assimp.aiTextureType_DIFFUSE;
import static org.lwjgl.assimp.Assimp.aiTextureType_NONE;
import static org.lwjgl.assimp.Assimp.aiTextureType_NORMALS;
import static org.lwjgl.assimp.Assimp.aiTextureType_SPECULAR;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;

import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;

import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIColor4D;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIString;
import org.lwjgl.assimp.AIVector3D;

import lwjglengine.v10.graphics.Material;
import lwjglengine.v10.graphics.Texture;
import lwjglengine.v10.graphics.TextureMaterial;
import lwjglengine.v10.model.Model;
import lwjglengine.v10.model.VertexArray;
import lwjglengine.v10.project.Project;
import myutils.v10.math.Vec2;
import myutils.v10.math.Vec3;

public class ModelAsset extends Asset {
	//this should use assimp to load the vertex arrays, and figure out what texture dependencies it has. 

	//should assets try to compute dependencies when loading or when saving?
	//it doesn't really make sense to compute dependencies when loading, because you need to load dependencies first
	//before loading the thing you want to load. 

	//if a texture dependency is already present within the project, then it should autolink 

	private Model model;

	public ModelAsset(File file, long id, String name, Project project) {
		super(file, id, name, project);
	}

	@Override
	protected void _load() throws IOException {
		File file = this.getFile();

		String filepath = this.getFile().getAbsolutePath();
		String parentFilepath = this.getFile().getParent();

		System.out.println("LOADING MESH: " + this.getName());

		ArrayList<VertexArray> meshes = new ArrayList<>();
		ArrayList<Material> defaultMaterials = new ArrayList<>();
		ArrayList<TextureMaterial> textureMaterials = new ArrayList<>();

		AIScene scene = aiImportFile(filepath, aiProcess_Triangulate | aiProcess_JoinIdenticalVertices);

		if (scene == null) {
			throw new IOException("Failed to load model " + file.getName());
		}

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
			String diffusePath = File.separator + path.dataString();
			if (diffusePath != null && diffusePath.length() > 1) {
				long assetID = this.getProject().findAssetFromRelativeFilepath(diffusePath);
				Texture diffuseTexture = this.getProject().getTexture(assetID);

				if (diffuseTexture != null) {
					material.setTexture(diffuseTexture, TextureMaterial.DIFFUSE);
				}
			}

			// map_Ks in .mtl
			path = AIString.calloc();
			aiGetMaterialTexture(AIMat, aiTextureType_SPECULAR, 0, path, (IntBuffer) null, null, null, null, null, null);
			String specularPath = File.separator + path.dataString();
			if (specularPath != null && specularPath.length() > 1) {
				long assetID = this.getProject().findAssetFromRelativeFilepath(specularPath);
				Texture specularTexture = this.getProject().getTexture(assetID);

				if (specularTexture != null) {
					material.setTexture(specularTexture, TextureMaterial.SPECULAR);
				}
			}

			// norm in .mtl
			path = AIString.calloc();
			aiGetMaterialTexture(AIMat, aiTextureType_NORMALS, 0, path, (IntBuffer) null, null, null, null, null, null);
			String normalsPath = File.separator + path.dataString();
			if (normalsPath != null && normalsPath.length() > 1) {
				long assetID = this.getProject().findAssetFromRelativeFilepath(normalsPath);
				Texture normalsTexture = this.getProject().getTexture(assetID);

				if (normalsTexture != null) {
					material.setTexture(normalsTexture, TextureMaterial.NORMAL);
				}
			}

			textureMaterials.add(material);
		}

		this.model = new Model(meshes, defaultMaterials, textureMaterials);

		System.out.println("SUCCESS");
	}

	@Override
	protected void _unload() {
		this.model.kill();
		this.model = null;
	}

	@Override
	protected void _save() throws IOException {
		//should do nothing. 
	}

	@Override
	protected void _computeDependencies() {
		//find all texture dependencies within the project. 
		//the dependencies don't actually change during the course of editing, they depend on the properly named files being there. 
		//perhaps, we can later add a .mtl file asset and add it as a component of the model asset.

		ArrayList<String> textureRelativePaths = new ArrayList<>();

		String filepath = this.getFile().getAbsolutePath();
		AIScene scene = aiImportFile(filepath, aiProcess_Triangulate | aiProcess_JoinIdenticalVertices);

		PointerBuffer materials = scene.mMaterials(); // array of pointers to AIMaterial structs

		for (int i = 0; i < scene.mNumMaterials(); i++) {
			// TextureMaterial
			// map_Kd in .mtl
			AIMaterial AIMat = AIMaterial.create(materials.get(i)); // wrap raw pointer in AIMaterial instance
			AIString path;

			path = AIString.calloc();
			aiGetMaterialTexture(AIMat, aiTextureType_DIFFUSE, 0, path, (IntBuffer) null, null, null, null, null, null);
			String diffusePath = File.separator + path.dataString();
			if (diffusePath != null && diffusePath.length() > 1) {
				textureRelativePaths.add(diffusePath);
			}

			// map_Ks in .mtl
			path = AIString.calloc();
			aiGetMaterialTexture(AIMat, aiTextureType_SPECULAR, 0, path, (IntBuffer) null, null, null, null, null, null);
			String specularPath = File.separator + path.dataString();
			if (specularPath != null && specularPath.length() > 1) {
				textureRelativePaths.add(specularPath);
			}

			// norm in .mtl
			path = AIString.calloc();
			aiGetMaterialTexture(AIMat, aiTextureType_NORMALS, 0, path, (IntBuffer) null, null, null, null, null, null);
			String normalsPath = File.separator + path.dataString();
			if (normalsPath != null && normalsPath.length() > 1) {
				textureRelativePaths.add(normalsPath);
			}
		}

		for (String i : textureRelativePaths) {
			long assetID = this.project.findAssetFromRelativeFilepath(i);
			if (assetID != -1) {
				this.addDependency(assetID);
			}
		}
	}

	public Model getModel() {
		return this.model;
	}

}
