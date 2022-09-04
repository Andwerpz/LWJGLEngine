package entity;

import java.util.HashMap;
import java.util.HashSet;

import model.Model;
import scene.Scene;
import util.Mat4;

public abstract class Entity {
	//Entities each have their own id
	
	//When binding model instances to the entity, the entity is responsible for keeping track of the 
	//model instance IDs. 
	
	//K : Entity ID
	//V : Entity
	public static HashMap<Long, Entity> entities = new HashMap<>();
	
	//K : Model Instance ID
	//V : Entity ID
	public static HashMap<Long, Long> modelToEntityID = new HashMap<>();
	
	//never equal to 0
	public long ID;	
	
	protected HashSet<Long> modelInstanceIDs;	//holds all model instance IDs associated with this entity
	
	public Entity() {
		this.ID = Entity.generateNewID();
		this.modelInstanceIDs = new HashSet<>();
		Entity.entities.put(this.ID, this);
	}
	
	private static long generateNewID() {
		long ans = 0;
		while(ans == 0 || entities.keySet().contains(ans)) {
			ans = (long) (Math.random() * 1000000000);
		}
		return ans;
	}
	
	protected void addModelInstance(Model model, Mat4 mat4, int scene) {
		this.modelInstanceIDs.add(Model.addInstance(model, mat4, scene));
	}
	
	protected void removeModelInstance(long modelInstanceID) {
		if(!modelInstanceIDs.contains(modelInstanceID)) {
			return;
		}
		this.modelInstanceIDs.remove(modelInstanceID);
		Model.removeInstance(modelInstanceID);
	}
	
	public static void updateEntities() {
		for(Entity e : entities.values()) {
			e.update();
		}
	}
	
	public void kill() {
		//remove all model instances
		for(long id : this.modelInstanceIDs) {
			Model.removeInstance(id);
		}
		
		//remove entity pointer 
		entities.remove(this.ID);
		
		System.out.println("REMOVE ENTITY " + this.ID);
	}
	
	public abstract void update();
	
}
