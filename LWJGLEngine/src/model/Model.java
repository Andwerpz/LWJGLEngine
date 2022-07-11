package model;

import java.util.ArrayList;

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
	
	public abstract VertexArray create();
	
	public void updateModelMats() {
		this.vao.updateModelMats(modelMats);
	}
	
	public void render(Texture tex) {		
		tex.bind();
		this.vao.render();
	}
	
}
