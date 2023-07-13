package lwjglengine.v10.model;

import java.util.ArrayList;

import lwjglengine.v10.graphics.Material;
import myutils.v10.math.Mat4;

public class ModelInstance {
	//wrapper class to more easily manage model instances

	//this will just call the model static functions to update. 

	private long id;
	private int scene;

	private boolean alive = true;

	private Model model;
	private ModelTransform modelTransform;

	private ArrayList<Material> materials;

	public ModelInstance(Model model, ModelTransform modelTransform, int scene) {
		this.init(model, modelTransform, scene);
	}

	public ModelInstance(Model model, Mat4 mat4, int scene) {
		this.init(model, new ModelTransform(mat4), scene);
	}

	public ModelInstance(Model model, int scene) {
		this.init(model, new ModelTransform(), scene);
	}

	private void init(Model model, ModelTransform transform, int scene) {
		this.model = model;
		this.modelTransform = new ModelTransform(transform);
		this.scene = scene;

		this.materials = new ArrayList<>();
		ArrayList<Material> modelDefaultMaterials = this.model.defaultMaterials;
		for (int i = 0; i < modelDefaultMaterials.size(); i++) {
			this.materials.add(new Material(modelDefaultMaterials.get(i)));
		}

		this.id = Model.addInstance(model, modelTransform, scene, this);
	}

	public long getID() {
		return this.id;
	}

	public int getScene() {
		return this.scene;
	}

	public Model getModel() {
		return this.model;
	}

	public ModelTransform getModelTransform() {
		return this.modelTransform;
	}

	public ArrayList<Material> getMaterials() {
		return this.materials;
	}

	public Material getMaterial() {
		return this.materials.get(0);
	}

	public void updateInstance() {
		Model.updateInstance(this.id);
	}

	public void setModelTransform(ModelTransform m) {
		this.modelTransform.set(m);
		this.updateInstance();
	}

	/**
	 * Replaces all materials with the given one
	 * @param m
	 */
	public void setMaterial(Material m) {
		for (int i = 0; i < this.materials.size(); i++) {
			this.materials.get(i).set(m);
		}
		this.updateInstance();
	}

	/**
	 * Replaces materials array
	 * 
	 * Input array must be same size as this materials array
	 * @param m
	 */
	public void setMaterials(ArrayList<Material> m) {
		if (m.size() != this.materials.size()) {
			System.err.println("Model Instance Warning : Input materials list is wrong size");
			return;
		}

		for (int i = 0; i < this.materials.size(); i++) {
			this.materials.get(i).set(m.get(i));
		}
		this.updateInstance();
	}

	public void kill() {
		Model.removeInstance(this.id);
		this.alive = false;
	}

	public boolean isAlive() {
		return this.alive;
	}
}
