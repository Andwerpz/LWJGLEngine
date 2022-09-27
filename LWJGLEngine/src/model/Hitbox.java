package model;

import static org.lwjgl.opengl.GL11.*;

import graphics.TextureMaterial;
import graphics.Texture;
import graphics.VertexArray;
import main.Main;
import util.Vec3;

public class Hitbox {
	// just an axis aligned bounding box.

	public static TextureMaterial texture = new TextureMaterial("/hitbox.png", null, null, null);

	VertexArray model;
	public Vec3 min, max;

	public Hitbox(Vec3 min, Vec3 max) {
		this.min = new Vec3(min);
		this.max = new Vec3(max);
		createModel();
	}

	public Hitbox(float xMin, float yMin, float zMin, float xMax, float yMax, float zMax) {
		this.min = new Vec3(xMin, yMin, zMin);
		this.max = new Vec3(xMax, yMax, zMax);
		createModel();
	}

	private void createModel() {
		int[] indices = new int[] { 0, 1, 1, 2, 2, 3, 3, 0,

			4, 5, 5, 6, 6, 7, 7, 4,

			0, 4, 1, 5, 2, 6, 3, 7, };

		float[] vertices = new float[] { min.x, min.y, min.z, min.x, min.y, max.z, max.x, min.y, max.z, max.x, min.y, min.z,

			min.x, max.y, min.z, min.x, max.y, max.z, max.x, max.y, max.z, max.x, max.y, min.z };

		float[] tex = new float[] { 0, 0, 0, 0, 0, 0, 0, 0,

			0, 0, 0, 0, 0, 0, 0, 0, };

		model = new VertexArray(vertices, tex, indices, GL_LINES);
	}

	public float getWidth() {
		return this.max.x - this.min.x;
	}

	public float getDepth() {
		return this.max.z - this.min.z;
	}

	public float getHeight() {
		return this.max.y - this.min.y;
	}

	// TODO fix this
	public void render() {
		texture.bind();
		// model.render();
		texture.unbind();
	}

	// returns the result to ray collision after this and input hitbox was
	// translated
	public boolean collision(Vec3 inputTranslate, Vec3 selfTranslate, Hitbox h) {
		Vec3 translate = selfTranslate.subi(inputTranslate);
		Vec3 tempMin = new Vec3(min);
		Vec3 tempMax = new Vec3(max);
		this.min.addi(translate);
		this.max.addi(translate);
		boolean out = this.collision(h);
		this.min = new Vec3(tempMin);
		this.max = new Vec3(tempMax);
		return out;
	}

	// returns the result to ray collision after this hitbox was translated
	public boolean collision(Vec3 selfTranslate, Hitbox h) {
		Vec3 tempMin = new Vec3(min);
		Vec3 tempMax = new Vec3(max);
		this.min.addi(selfTranslate);
		this.max.addi(selfTranslate);
		boolean out = this.collision(h);
		this.min = new Vec3(tempMin);
		this.max = new Vec3(tempMax);
		return out;
	}

	public boolean collision(Hitbox h) {
//		boolean xInt = !(this.min.x > h.max.x || this.max.x < h.min.x);
//		boolean yInt = !(this.min.y > h.max.y || this.max.y < h.min.y);
//		boolean zInt = !(this.min.z > h.max.z || this.max.z < h.min.z);
//		return xInt && yInt && zInt;

		return (min.x <= h.max.x && max.x >= h.min.x) && (min.y <= h.max.y && max.y >= h.min.y) && (min.z <= h.max.z && max.z >= h.min.z);
	}

	// returns the result to ray collision after the hitbox was translated
	public Vec3 collision(Vec3 translate, Vec3 point, Vec3 ray) {
		Vec3 tempMin = new Vec3(min);
		Vec3 tempMax = new Vec3(max);
		this.min.addi(translate);
		this.max.addi(translate);
		Vec3 out = this.collision(point, ray);
		this.min = new Vec3(tempMin);
		this.max = new Vec3(tempMax);
		return out;
	}

	// returns the closest intersection point if one exists, null otherwise
	public Vec3 collision(Vec3 point, Vec3 ray) {
		if(point.y > max.y && ray.y < 0) { // top
			float axisDist = (max.y - point.y) / ray.y;
			float xInt = point.x + axisDist * ray.x;
			float zInt = point.z + axisDist * ray.z;
			if(xInt > min.x && xInt < max.x && zInt > min.z && zInt < max.z) {
				return new Vec3(xInt, max.y, zInt);
			}
		}
		if(point.y < min.y && ray.y > 0) { // bottom
			float axisDist = (max.y - point.y) / ray.y;
			float xInt = point.x + axisDist * ray.x;
			float zInt = point.z + axisDist * ray.z;
			if(xInt > min.x && xInt < max.x && zInt > min.z && zInt < max.z) {
				return new Vec3(xInt, min.y, zInt);
			}
		}

		if(point.z > max.z && ray.z < 0) { // south
			float axisDist = (max.z - point.z) / ray.z;
			float xInt = point.x + axisDist * ray.x;
			float yInt = point.y + axisDist * ray.y;
			if(xInt > min.x && xInt < max.x && yInt > min.y && yInt < max.y) {
				return new Vec3(xInt, yInt, max.z);
			}
		}
		if(point.z < min.z && ray.z > 0) { // north
			float axisDist = (min.z - point.z) / ray.z;
			float xInt = point.x + axisDist * ray.x;
			float yInt = point.y + axisDist * ray.y;
			if(xInt > min.x && xInt < max.x && yInt > min.y && yInt < max.y) {
				return new Vec3(xInt, yInt, min.z);
			}
		}

		if(point.x > max.x && ray.x < 0) { // west
			float axisDist = (max.x - point.x) / ray.x;
			float yInt = point.y + axisDist * ray.y;
			float zInt = point.z + axisDist * ray.z;
			if(yInt > min.y && yInt < max.y && zInt > min.z && zInt < max.z) {
				return new Vec3(max.x, yInt, zInt);
			}
		}
		if(point.x < min.x && ray.x > 0) { // east
			float axisDist = (min.x - point.x) / ray.x;
			float yInt = point.y + axisDist * ray.y;
			float zInt = point.z + axisDist * ray.z;
			if(yInt > min.y && yInt < max.y && zInt > min.z && zInt < max.z) {
				return new Vec3(min.x, yInt, zInt);
			}
		}

		return null;
	}

	// returns which side the ray intersects this hitbox after the hitbox was
	// translated
	public int collisionSide(Vec3 translate, Vec3 point, Vec3 ray) {
		Vec3 tempMin = new Vec3(min);
		Vec3 tempMax = new Vec3(max);
		this.min.addi(translate);
		this.max.addi(translate);
		int out = this.collisionSide(point, ray);
		this.min = new Vec3(tempMin);
		this.max = new Vec3(tempMax);
		return out;
	}

	// returns which side the ray intersects this hitbox
	public int collisionSide(Vec3 point, Vec3 ray) {
		if(point.y > max.y && ray.y < 0) { // top
			float axisDist = (max.y - point.y) / ray.y;
			float xInt = point.x + axisDist * ray.x;
			float zInt = point.z + axisDist * ray.z;
			if(xInt > min.x && xInt < max.x && zInt > min.z && zInt < max.z) {
				return Main.UP;
			}
		}
		if(point.y < min.y && ray.y > 0) { // bottom
			float axisDist = (min.y - point.y) / ray.y;
			float xInt = point.x + axisDist * ray.x;
			float zInt = point.z + axisDist * ray.z;
			if(xInt > min.x && xInt < max.x && zInt > min.z && zInt < max.z) {
				return Main.DOWN;
			}
		}

		if(point.z > max.z && ray.z < 0) { // south
			float axisDist = (max.z - point.z) / ray.z;
			float xInt = point.x + axisDist * ray.x;
			float yInt = point.y + axisDist * ray.y;
			if(xInt > min.x && xInt < max.x && yInt > min.y && yInt < max.y) {
				return Main.SOUTH;
			}
		}
		if(point.z < min.z && ray.z > 0) { // north
			float axisDist = (min.z - point.z) / ray.z;
			float xInt = point.x + axisDist * ray.x;
			float yInt = point.y + axisDist * ray.y;
			if(xInt > min.x && xInt < max.x && yInt > min.y && yInt < max.y) {
				return Main.NORTH;
			}
		}

		if(point.x > max.x && ray.x < 0) { // west
			float axisDist = (max.x - point.x) / ray.x;
			float yInt = point.y + axisDist * ray.y;
			float zInt = point.z + axisDist * ray.z;
			if(yInt > min.y && yInt < max.y && zInt > min.z && zInt < max.z) {
				return Main.WEST;
			}
		}
		if(point.x < min.x && ray.x > 0) { // east
			float axisDist = (min.x - point.x) / ray.x;
			float yInt = point.y + axisDist * ray.y;
			float zInt = point.z + axisDist * ray.z;
			if(yInt > min.y && yInt < max.y && zInt > min.z && zInt < max.z) {
				return Main.EAST;
			}
		}

		return -1;
	}

}
