package entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import graphics.Material;
import model.AssetManager;
import model.Model;
import util.Mat4;

public abstract class Entity {
	// Entities each have their own id

	// When binding model instances to the entity, the entity is responsible for
	// keeping track of the
	// model instance IDs.

	// K : Entity ID
	// V : Entity
	public static HashMap<Long, Entity> entities = new HashMap<>();

	// K : Model Instance ID
	// V : Entity ID
	public static HashMap<Long, Long> modelToEntityID = new HashMap<>();

	// never equal to 0
	private long ID;

	protected HashSet<Long> modelInstanceIDs; // holds all model instance IDs associated with this entity

	public Entity() {
		this.ID = Entity.generateNewID();
		this.modelInstanceIDs = new HashSet<>();
		Entity.entities.put(this.ID, this);
	}

	private static long generateNewID() {
		long ans = 0;
		while (ans == 0 || entities.keySet().contains(ans)) {
			ans = (long) (Math.random() * 1000000000);
		}
		return ans;
	}

	public long getID() {
		return this.ID;
	}

	public static Entity getEntity(long ID) {
		return entities.get(ID);
	}

	protected long addModelInstance(Model model, Mat4 mat4, int scene) {
		long modelInstanceID = Model.addInstance(model, mat4, scene);
		this.modelInstanceIDs.add(modelInstanceID);
		modelToEntityID.put(modelInstanceID, this.ID);
		return modelInstanceID;
	}

	protected void registerModelInstance(long modelInstanceID) {
		this.modelInstanceIDs.add(modelInstanceID);
		modelToEntityID.put(modelInstanceID, this.ID);
	}

	protected void updateModelInstance(long modelInstanceID, Mat4 mat4) {
		Model.updateInstance(modelInstanceID, mat4);
	}

	protected void updateModelInstance(long modelInstanceID, Material material) {
		Model.updateInstance(modelInstanceID, material);
	}

	protected void removeModelInstance(long modelInstanceID) {
		if(!modelInstanceIDs.contains(modelInstanceID)) {
			return;
		}
		this.modelInstanceIDs.remove(modelInstanceID);
		modelToEntityID.remove(modelInstanceID);
		Model.removeInstance(modelInstanceID);
	}

	public static void updateEntities() {
		for (Entity e : entities.values()) {
			e.update();
		}
	}

	public static long getEntityIDFromModelID(long modelID) {
		return Entity.modelToEntityID.get(modelID) == null ? 0 : Entity.modelToEntityID.get(modelID);
	}

	// _kill() is ran at the end, so if you have any models you can kill them here.
	protected abstract void _kill();

	public void kill() {
		// remove all model instances
		for (long id : this.modelInstanceIDs) {
			modelToEntityID.remove(id);
			Model.removeInstance(id);
		}

		// remove entity pointer
		entities.remove(this.ID);

		System.out.println("REMOVE ENTITY " + this.ID);

		this._kill();
	}

	public static void killAll() {
		ArrayList<Entity> arr = new ArrayList<>();
		arr.addAll(entities.values());
		for (Entity e : arr) {
			e.kill();
		}
	}

	protected abstract void update();

}
