package lwjglengine.v10.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import lwjglengine.v10.graphics.Material;
import lwjglengine.v10.model.Model;
import lwjglengine.v10.model.ModelInstance;
import myutils.v10.math.Mat4;

public abstract class Entity {
	// Entities each have their own id

	// When binding model instances to the entity, the entity is responsible for
	// keeping track of the
	// model instance IDs.

	// K : Entity ID
	// V : Entity
	private static HashMap<Long, Entity> entities = new HashMap<>();

	// K : Model Instance ID
	// V : Entity ID
	private static HashMap<Long, Long> modelToEntityID = new HashMap<>();

	// never equal to 0
	private long ID;

	protected HashSet<ModelInstance> modelInstances; // holds all model instance objects associated with this entity

	private boolean isAlive = true;

	public Entity() {
		this.ID = Entity.generateNewID();
		this.modelInstances = new HashSet<>();
		Entity.entities.put(this.ID, this);
	}

	public static int getNumEntities() {
		return entities.size();
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

	protected void registerModelInstance(ModelInstance instance) {
		long modelInstanceID = instance.getID();
		this.modelInstances.add(instance);
		modelToEntityID.put(modelInstanceID, this.ID);
	}

	protected void removeModelInstance(ModelInstance instance) {
		long modelInstanceID = instance.getID();
		if (!modelInstances.contains(instance)) {
			return;
		}
		this.modelInstances.remove(instance);
		modelToEntityID.remove(modelInstanceID);
		instance.kill();
	}

	public static void updateEntities() {
		for (Entity e : entities.values()) {
			e.update();
		}
	}

	public static long getEntityIDFromModelID(long modelID) {
		return Entity.modelToEntityID.get(modelID) == null ? 0 : Entity.modelToEntityID.get(modelID);
	}

	public boolean isAlive() {
		return this.isAlive;
	}

	// _kill() is ran at the end, so if you have any models you can kill them here.
	protected abstract void _kill();

	public void kill() {
		this.isAlive = false;

		// remove all model instances currently registered to this entity
		for (ModelInstance i : this.modelInstances) {
			modelToEntityID.remove(i.getID());
			i.kill();
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
