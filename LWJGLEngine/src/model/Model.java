package model;

import static org.lwjgl.assimp.Assimp.*;

import java.util.ArrayList;

import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;

import graphics.Texture;
import graphics.VertexArray;
import util.Mat4;

public abstract class Model {
	
	//always instanced rendering
	//in order to render with correct model matrices, you must first update the model matrices. 
	//if model matrix updates aren't needed, then you shouldn't update them. 
	
	public ArrayList<Mat4> modelMats;
	public VertexArray vao;

	public Model() {
		this.modelMats = new ArrayList<Mat4>();
		this.vao = this.create();
	}
	
	private static void loadModelFile(String filepath) {
		AIScene scene = aiImportFile(filepath, aiProcess_Triangulate);
		
		PointerBuffer buffer = scene.mMeshes();
		
		for(int i = 0; i < buffer.limit(); i++) {
			AIMesh mesh = AIMesh.create(buffer.get(i));
		}
	}
	
	public abstract VertexArray create();
	
	public void updateModelMats() {
		this.vao.updateModelMats(modelMats);
	}
	
	public void render() {		
		this.vao.render();
	}
	
}
