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
import java.util.HashMap;
import java.util.HashSet;

import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIMaterialProperty;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIString;
import org.lwjgl.assimp.AITexture;
import org.lwjgl.assimp.AIVector2D;
import org.lwjgl.assimp.AIVector3D;

import graphics.Material;
import graphics.Texture;
import graphics.VertexArray;
import util.GraphicsTools;
import util.Mat4;
import util.SystemUtils;
import util.TargaReader;
import util.Vec2;
import util.Vec3;

public class Model {
	
	//always instanced rendering
	//in order to render with correct model matrices, you must first update the model matrices. 
	//if model matrix updates aren't needed, then you shouldn't update them. 
	
	public static final Material DEFAULT_MATERIAL = new Material(null, null, null, null);
	
	private static ArrayList<Model> models = new ArrayList<>();
	private static HashSet<Long> modelInstanceIDs = new HashSet<>();
	
	//K : entity ID, Can be translated to a color to draw 
	//V : model Mat4, Where to draw each instance of the model
	private HashMap<Long, Mat4> modelMats;
	private boolean modelMatrixUpdateNeeded = false;
	
	public ArrayList<VertexArray> meshes;
	public ArrayList<Material> materials;

	public Model() {
		this.modelMats = new HashMap<Long, Mat4>();
		this.meshes = new ArrayList<>();
		this.materials = new ArrayList<>();
		this.create();
		
		init();
	}
	
	public Model(String filepath, String filename) {
		this.modelMats = new HashMap<Long, Mat4>();
		this.loadModelFile(filepath, filename);
		
		init();
	}
	
	private void init() {
		models.add(this);
	}
	
	public static long generateNewID() {
		long ans = 0;
		while(ans == 0 || modelInstanceIDs.contains(ans)) {
			ans = (long) (Math.random() * 256) + (long) (Math.random() * 256) * 1000l + (long) (Math.random() * 256)  * 1000000l;
		}
		return ans;
	}
	
	public static long convertRGBToID(Vec3 rgb) {
		return (long) rgb.x * 1000000l + (long) rgb.y * 1000l + (long) rgb.z;
	}
	
	public static Vec3 convertIDToRGB(long ID) {
		return new Vec3((ID / 1000000) % 1000, (ID / 1000) % 1000, ID % 1000);
	}
	
	//must have .mtl file to be able to load materials
	private void loadModelFile(String filepath, String filename) {
		System.out.println("LOADING MESH: " + filename);
		
		this.meshes = new ArrayList<>();
		this.materials = new ArrayList<>();
		
		String workingDirectory = SystemUtils.getWorkingDirectory();
		
		AIScene scene = aiImportFile(workingDirectory + "/res" + filepath + filename, aiProcess_Triangulate | aiProcess_JoinIdenticalVertices);
		
		//group meshes with the same material
		ArrayList<ArrayList<Vec3>> vertices = new ArrayList<>();
		ArrayList<ArrayList<Vec2>> uvs = new ArrayList<>();
		ArrayList<ArrayList<Integer>> indices = new ArrayList<>();
		
		for(int i = 0; i < scene.mNumMaterials(); i++) {
			vertices.add(new ArrayList<>());
			uvs.add(new ArrayList<>());
			indices.add(new ArrayList<>());
		}
		
		PointerBuffer buffer = scene.mMeshes();
		
		System.out.println("meshes: " + buffer.limit());
		
		for(int i = 0; i < buffer.limit(); i++) {
			AIMesh mesh = AIMesh.create(buffer.get(i));
			
			int matIndex = mesh.mMaterialIndex();
			int indexOffset = vertices.get(matIndex).size();
			
			AIVector3D.Buffer AIVertices = mesh.mVertices();
			for(int j = 0; j < AIVertices.limit(); j++) {
				AIVector3D v = AIVertices.get(j);
				vertices.get(matIndex).add(new Vec3(v.x(), v.y(), v.z()));
			}
			
			AIVector3D.Buffer AIuvs = mesh.mTextureCoords(0);
			for(int j = 0; j < AIuvs.limit(); j++) {
				AIVector3D v = AIuvs.get(j);
				uvs.get(matIndex).add(new Vec2(v.x(), v.y()));
			}
			
			AIFace.Buffer AIFaces = mesh.mFaces();
			for(int j = 0; j < AIFaces.limit(); j++) {
				AIFace f = AIFaces.get(j);
				indices.get(matIndex).add(f.mIndices().get(0) + indexOffset);
				indices.get(matIndex).add(f.mIndices().get(1) + indexOffset);
				indices.get(matIndex).add(f.mIndices().get(2) + indexOffset);
			}
		}
		
		PointerBuffer materials = scene.mMaterials(); // array of pointers to AIMaterial structs
		for(int i = 0; i < scene.mNumMaterials(); i++) {
			if(vertices.get(i).size() == 0) {	//empty mesh, don't load
				continue;
			}
			
			//put vertex data into buffers
			float[] vArr = new float[vertices.get(i).size() * 3];
			float[] uvArr = new float[uvs.get(i).size() * 2];
			int[] iArr = new int[indices.get(i).size()];
			
			for(int j = 0; j < vertices.get(i).size(); j++) {
				vArr[j * 3 + 0] = vertices.get(i).get(j).x;
				vArr[j * 3 + 1] = vertices.get(i).get(j).y;
				vArr[j * 3 + 2] = vertices.get(i).get(j).z;
			}
			
			for(int j = 0; j < uvs.get(i).size(); j++) {
				uvArr[j * 2 + 0] = uvs.get(i).get(j).x;
				uvArr[j * 2 + 1] = uvs.get(i).get(j).y;
			}
			
			for(int j = 0; j < indices.get(i).size(); j++) {
				iArr[j] = indices.get(i).get(j);
			}
			
			System.out.println("vertices: " + vertices.get(i).size() + " | faces: " + (indices.get(i).size() / 3));
			this.meshes.add(new VertexArray(vArr, uvArr, iArr, GL_TRIANGLES));
			
			//load material data
			AIMaterial AIMat = AIMaterial.create(materials.get(i)); // wrap raw pointer in AIMaterial instance
			Material material = Material.defaultMaterial();
		    AIString path;
		    
		    //map_Kd in .mtl
		    path = AIString.calloc();
		    aiGetMaterialTexture(AIMat, aiTextureType_DIFFUSE, 0, path, (IntBuffer) null, null, null, null, null, null);
		    String diffusePath = path.dataString();
		    if(diffusePath != null && diffusePath.length() != 0) {
		    	Texture diffuseTexture = new Texture(loadImage(workingDirectory + "/res" + filepath + diffusePath), false, false);
		    	material.setTexture(diffuseTexture, Material.DIFFUSE);
		    }
		    
		    //norm in .mtl
		    path = AIString.calloc();
		    aiGetMaterialTexture(AIMat, aiTextureType_NORMALS, 0, path, (IntBuffer) null, null, null, null, null, null);
		    String normalsPath = path.dataString();
		    if(normalsPath != null && normalsPath.length() != 0) {
		    	Texture normalsTexture = new Texture(loadImage(workingDirectory + "/res" + filepath + normalsPath), false, false);
		    	material.setTexture(normalsTexture, Material.NORMAL);
		    }
		    
		    this.materials.add(material);
		}
		
		System.out.println("SUCCESS");
	}
	
	public static BufferedImage loadImage(String path) {
		String fileExtension = getFileExtension(path);
		switch(fileExtension) {
		case "png": 
			return GraphicsTools.loadImageFromRoot(path);
			
		case "jpg":
			return GraphicsTools.loadImageFromRoot(path);
			
		case "tga":
			return TargaReader.getImage(path);
		}
		
		System.err.println("File extension " + fileExtension + " is not supported");
		return null;
	}
	
	public static String getFileExtension(String path) {
		int lastPeriod = path.lastIndexOf('.');
		return path.substring(lastPeriod + 1);
	}
	
	public static String removeFileExtension(String path) {
		int lastPeriod = path.lastIndexOf('.');
		return path.substring(0, lastPeriod);
	}
	
	public void create() {}
	
	//adds the mat4, and returns a new id
	public long addInstance(Mat4 model) {
		long ID = generateNewID();
		modelInstanceIDs.add(ID);
		this.addInstance(model, ID);
		return ID;
	}
	
	public void addInstance(Mat4 model, long ID) {
		modelMats.put(ID, model);
		modelMatrixUpdateNeeded = true;
	}
	
	public void removeInstance(long ID) {
		modelMats.remove(ID);
		modelMatrixUpdateNeeded = true;
	}
	
	public void updateInstance(Mat4 model, long ID) {
		modelMats.put(ID, model);
		modelMatrixUpdateNeeded = true;
	}
	
	public static void updateModels() {
		for(Model m : models) {
			if(m.modelMatrixUpdateNeeded) {
				m.updateModelMats();
				m.modelMatrixUpdateNeeded = false;
			}
		}
	}
	
	private void updateModelMats() {
		for(VertexArray v : meshes) {
			v.updateInstances(modelMats);
		}
	}
	
	public static void renderModels() {
		for(Model m : models) {
			m.render();
		}
	}
	
	protected void render() {		
		for(int i = 0; i < meshes.size(); i++) {
			if(i < this.materials.size() && this.materials.get(i) != null) {
				this.materials.get(i).bind();
			}
			else {
				DEFAULT_MATERIAL.bind();
			}
			this.meshes.get(i).render();
		}
	}
	
	private void render(ArrayList<Material> materials) {
		for(int i = 0; i < meshes.size(); i++) {
			if(i < materials.size() && materials.get(i) != null) {
				materials.get(i).bind();
			}
			else {
				DEFAULT_MATERIAL.bind();
			}
			this.meshes.get(i).render();
		}
	}
}
