package entity;

import java.util.HashMap;

import graphics.Shader;
import model.Hitbox;
import model.Model;
import scene.World;
import util.Mat4;
import util.Vec3;

public abstract class Entity {
	
	public static HashMap<Long, Entity> entities = new HashMap<>();
	
	//of the form [0 - 255][0 - 255][0 - 255] : RGB
	//never equal to 0
	public long ID;	
	
	public static float friction = 0.7f;
	public static float cushion = 0.001f;
	public static float gravity = 0.025f;
	
	public Vec3 pos, vel;
	public float xRot, yRot, zRot;
	public boolean onGround = false;
	
	private Model model;
	private Mat4 modelMat4;
	
	public Entity(Vec3 pos, float xRot, float yRot, float zRot, Model model, int scene) {
		this.ID = Model.generateNewID();
		this.pos = new Vec3(pos);
		this.vel = new Vec3(0);
		this.xRot = xRot;
		this.yRot = yRot;
		this.zRot = zRot;
		this.model = model;
		
		this.model.addInstance(this.getModelMat4(), scene);
		Entity.entities.put(this.ID, this);
	}
	
	protected void switchModel(Model newModel) {
		int scene = Model.getScene(this.ID);
		
		//old model
		this.model.removeInstance(this.ID);
		
		//new model
		newModel.addInstance(this.getModelMat4(), ID, scene);
		this.model = newModel;
	}
	
	public Model getModel() {
		return this.model;
	}
	
	public Mat4 getModelMat4() {
		return Mat4.rotateZ(this.zRot).mul(Mat4.rotateX(xRot)).mul(Mat4.rotateY(yRot)).mul(Mat4.translate(this.pos));
	}
	
	public void updateModelMat4() {
		this.model.updateInstance(this.getModelMat4(), this.ID);
	}
	
	public abstract Hitbox getHitbox();
	
	public abstract void update();
	
	public static void updateEntities() {
		for(Entity e : entities.values()) {
			e.update();
		}
	}
	
	//remove model instance
	public void kill() {
		this.model.removeInstance(this.ID);
	}
	
	public boolean collision(Vec3 translate, Hitbox h) {
		return this.getHitbox().collision(translate, this.pos, h);
	}
	
	public void move() {
		move_noclip();
	}
	
	//ignores all collision
	public void move_noclip() {
		this.vel.x *= friction;
		this.vel.z *= friction;
		this.vel.y *= friction;
		
		this.pos = nextPos_noclip();
	}
	
	public Vec3 nextPos_noclip() {
		return this.pos.add(this.vel);
	}
	
	//WIP
	//assumes that objects in the world are arranged in a gridlike fashion
	/*
	public void move_grid() {
		this.vel.x *= friction;
		this.vel.z *= friction;
		this.vel.y -= gravity;
		
		this.pos = nextPos_grid();
	}
	
	public Vec3 nextPos_grid() {
		Hitbox h = this.getHitbox();
		Vec3 nextPos = new Vec3(this.pos);
		
		//north / south movement
		nextPos.z += vel.z;
		if(vel.z < 0) {
			boolean collision = false;
			for(int x = (int) Math.floor(nextPos.x); x <= (int) Math.floor(nextPos.x + h.getWidth()); x++) {
				for(int y = (int) Math.floor(nextPos.y); y <= (int) Math.floor(nextPos.y + h.getHeight()); y++) {
					for(int z = (int) Math.floor(nextPos.z); z <= (int) Math.floor(nextPos.z + h.getDepth()); z++) {
						Block b = World.getBlock(x, y, z);
						if(b.collision(nextPos, h)) {
							collision = true;
							nextPos.z = Math.max(nextPos.z, b.getHitbox().max.z + z + cushion);
						}
					}
				}
			}
			if(collision) {
				vel.z = 0;
			}
		}
		else if(vel.z > 0) {
			boolean collision = false;
			for(int x = (int) Math.floor(nextPos.x); x <= (int) Math.floor(nextPos.x + h.getWidth()); x++) {
				for(int y = (int) Math.floor(nextPos.y); y <= (int) Math.floor(nextPos.y + h.getHeight()); y++) {
					for(int z = (int) Math.floor(nextPos.z); z <= (int) Math.floor(nextPos.z + h.getDepth()); z++) {
						Block b = World.getBlock(x, y, z);
						if(b.collision(nextPos, h)) {
							collision = true;
							nextPos.z = Math.min(nextPos.z, b.getHitbox().min.z + z - h.getDepth() - cushion);
						}
					}
				}
			}
			if(collision) {
				vel.z = 0;
			}
		}
		
		//east / west movement
		nextPos.x += vel.x;
		if(vel.x < 0) {
			boolean collision = false;
			for(int x = (int) Math.floor(nextPos.x); x <= (int) Math.floor(nextPos.x + h.getWidth()); x++) {
				for(int y = (int) Math.floor(nextPos.y); y <= (int) Math.floor(nextPos.y + h.getHeight()); y++) {
					for(int z = (int) Math.floor(nextPos.z); z <= (int) Math.floor(nextPos.z + h.getDepth()); z++) {
						Block b = World.getBlock(x, y, z);
						if(b.collision(nextPos, h)) {
							collision = true;
							nextPos.x = Math.max(nextPos.x, b.getHitbox().max.x + x + cushion);
						}
					}
				}
			}
			if(collision) {
				vel.x = 0;
			}
		}
		else if(vel.x > 0) {
			boolean collision = false;
			for(int x = (int) Math.floor(nextPos.x); x <= (int) Math.floor(nextPos.x + h.getWidth()); x++) {
				for(int y = (int) Math.floor(nextPos.y); y <= (int) Math.floor(nextPos.y + h.getHeight()); y++) {
					for(int z = (int) Math.floor(nextPos.z); z <= (int) Math.floor(nextPos.z + h.getDepth()); z++) {
						Block b = World.getBlock(x, y, z);
						if(b.collision(nextPos, h)) {
							collision = true;
							nextPos.x = Math.min(nextPos.x, b.getHitbox().min.x + x - h.getWidth() - cushion);
						}
					}
				}
			}
			if(collision) {
				vel.x = 0;
			}
		}
		
		//up / down movement
		nextPos.y += vel.y;
		if(vel.y < 0) { 
			boolean collision = false;
			for(int x = (int) Math.floor(nextPos.x); x <= (int) Math.floor(nextPos.x + h.getWidth()); x++) {
				for(int y = (int) Math.floor(nextPos.y); y <= (int) Math.floor(nextPos.y + h.getHeight()); y++) {
					for(int z = (int) Math.floor(nextPos.z); z <= (int) Math.floor(nextPos.z + h.getDepth()); z++) {
						Block b = World.getBlock(x, y, z);
						if(b.collision(nextPos, h)) {
							collision = true;
							nextPos.y = Math.max(nextPos.y, b.getHitbox().max.y + y + cushion);
						}
					}
				}
			}
			if(collision) {
				onGround = true;
				vel.y = 0;
			}
		}
		else if(vel.y > 0) {
			onGround = false;
			boolean collision = false;
			for(int x = (int) Math.floor(nextPos.x); x <= (int) Math.floor(nextPos.x + h.getWidth()); x++) {
				for(int y = (int) Math.floor(nextPos.y); y <= (int) Math.floor(nextPos.y + h.getHeight()); y++) {
					for(int z = (int) Math.floor(nextPos.z); z <= (int) Math.floor(nextPos.z + h.getDepth()); z++) {
						Block b = World.getBlock(x, y, z);
						if(b.collision(nextPos, h)) {
							collision = true;
							nextPos.y = Math.min(nextPos.y, b.getHitbox().min.y + y - h.getHeight() - cushion);
						}
					}
				}
			}
			if(collision) {
				vel.y = 0;
			}
		}
	}
	*/
	
	//TODO : replace or rework these
	public void applyModelTransform() {
		Mat4 md_matrix = Mat4.translate(pos);
		Shader.GEOMETRY.setUniformMat4("md_matrix", md_matrix);
	}
	
	public void renderHitbox() {
		applyModelTransform();
		this.getHitbox().render();
	}
	
}
