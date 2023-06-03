package lwjglengine.v10.particle;

import static org.lwjgl.opengl.GL11.*;

import java.util.ArrayList;
import java.util.HashSet;

import lwjglengine.v10.entity.Entity;
import lwjglengine.v10.graphics.Material;
import lwjglengine.v10.graphics.TextureMaterial;
import lwjglengine.v10.main.Main;
import lwjglengine.v10.model.FilledRectangle;
import lwjglengine.v10.model.Model;
import lwjglengine.v10.model.ModelInstance;
import lwjglengine.v10.model.ModelTransform;
import lwjglengine.v10.model.VertexArray;
import myutils.v10.math.Mat4;
import myutils.v10.math.MathUtils;
import myutils.v10.math.Vec3;

public class Particle extends Entity {
	//each particle holds information for all instances of a unique particle. 

	private HashSet<ParticleInstance> particles;

	private Model rectangleModel;
	private TextureMaterial textureMaterial;
	private Material defaultMaterial; //every particle's material will be set to this by default. 

	private long lifeLengthMillis;
	private float scale;

	public Particle(TextureMaterial textureMaterial, long lifeLengthMillis, float scale) {
		this.lifeLengthMillis = lifeLengthMillis;
		this.scale = scale;

		this.textureMaterial = TextureMaterial.defaultTextureMaterial();
		this.defaultMaterial = Material.defaultMaterial();

		this.particles = new HashSet<ParticleInstance>();

		//we want to have a 2d unit rectangle centered on the origin, so we can apply scale first before translation
		float[] vertices = new float[] { -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f };
		float[] uvs = new float[] { 0, 0, 1, 0, 1, 1, 0, 1, };
		int[] indices = new int[] { 0, 1, 2, 0, 2, 3, };

		VertexArray vertexArray = new VertexArray(vertices, uvs, indices, GL_TRIANGLES);

		this.rectangleModel = new Model(vertexArray, this.textureMaterial);
	}

	public void addInstance(Vec3 pos, Vec3 vel, float rot, int scene) {
		ModelInstance instance = new ModelInstance(this.rectangleModel, scene);
		this.particles.add(new ParticleInstance(pos, vel, this.lifeLengthMillis, rot, this.scale, this.defaultMaterial, instance));
	}

	@Override
	protected void update() {
		ArrayList<ParticleInstance> needRemoving = new ArrayList<>();
		for (ParticleInstance p : particles) {
			if (!p.update()) {
				needRemoving.add(p);
			}
		}

		for (ParticleInstance p : needRemoving) {
			p.kill();
			this.particles.remove(p);
		}
	}

	@Override
	protected void _kill() {
		for (ParticleInstance p : particles) {
			p.kill();
		}

		this.rectangleModel.kill();
	}

	public void setTextureMaterial(TextureMaterial textureMaterial) {
		this.textureMaterial = textureMaterial;
		this.rectangleModel.setTextureMaterial(this.textureMaterial);
	}

	public void setDefaultMaterial(Material m) {
		this.defaultMaterial = m;
	}

}

class ParticleInstance {

	private ModelInstance modelInstance;

	private Material material;

	private Vec3 pos, vel;
	private long lifeLengthMillis;
	private float rot; //z rotation
	private float scale;

	private long elapsedTime = 0;

	public ParticleInstance(Vec3 pos, Vec3 vel, long lifeLengthMillis, float rot, float scale, Material material, ModelInstance modelInstance) {
		this.pos = new Vec3(pos);
		this.vel = new Vec3(vel);
		this.lifeLengthMillis = lifeLengthMillis;
		this.rot = rot;
		this.scale = scale;

		this.material = material;

		this.modelInstance = modelInstance;
	}

	//returns false if this particle needs to be removed. 
	protected boolean update() {
		this.elapsedTime += Main.main.deltaMillis;
		if (this.elapsedTime >= this.lifeLengthMillis) {
			return false;
		}

		this.pos.addi(this.vel);

		float alpha = 1f - ((float) this.elapsedTime / this.lifeLengthMillis);
		this.material.setAlpha((float) this.elapsedTime / (float) this.lifeLengthMillis);

		this.updateModelInstance();

		return true;
	}

	private void updateModelInstance() {
		Mat4 modelMat4 = Mat4.identity();
		modelMat4.muli(Mat4.scale(this.scale));
		modelMat4.muli(Mat4.rotateZ(this.rot));
		modelMat4.muli(Mat4.translate(this.pos));

		ModelTransform transform = this.modelInstance.getModelTransform();
		transform.setScale(this.scale);
		transform.setRotation(MathUtils.quaternionFromRotationMat4(Mat4.rotateZ(this.rot)));
		transform.setTranslation(this.pos);

		this.modelInstance.updateInstance();
	}

	public void kill() {
		this.modelInstance.kill();
	}

}
