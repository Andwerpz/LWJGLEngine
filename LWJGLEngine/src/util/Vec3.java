package util;

public class Vec3 {
	public float x, y, z;
	
	public Vec3() {
		this.x = 0;
		this.y = 0;
		this.z = 0;
	}
	
	public Vec3(float val) {
		this.x = val;
		this.y = val;
		this.z = val;
	}

	public Vec3(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Vec3(Vec3 a, Vec3 b) {
		this.x = b.x - a.x;
		this.y = b.y - a.y;
		this.z = b.z - a.z;
	}
	
	public Vec3(Vec3 v) {
		this.x = v.x;
		this.y = v.y;
		this.z = v.z;
	}
	
	public Vec3 addi(Vec3 v) {
		this.x += v.x;
		this.y += v.y;
		this.z += v.z;
		return this;
	}
	
	public Vec3 add(Vec3 v) {
		Vec3 result = new Vec3(this);
		result.addi(v);
		return result;
	}
	
	public Vec3 subi(Vec3 v) {
		this.x -= v.x;
		this.y -= v.y;
		this.z -= v.z;
		return this;
	}
	
	public float length() {
		float xyDist = (float) MathTools.dist(0, 0, x, y);
		return (float) MathTools.dist(0, 0, xyDist, z);
	}
	
	public Vec3 muli(float val) {
		this.x *= val;
		this.y *= val;
		this.z *= val;
		return this;
	}
	
	public Vec3 mul(float val) {
		Vec3 result = new Vec3(this);
		result.x *= val;
		result.y *= val;
		result.z *= val;
		return result;
	}
	
	public Vec3 normalize() {
		float mag = this.length();
		this.x /= mag;
		this.y /= mag;
		this.z /= mag;
		return this;
	}
	
	public Vec3 setLength(float mag) {
		this.normalize();
		this.x *= mag;
		this.y *= mag;
		this.z *= mag;
		return this;
	}
	
	public Vec3 rotateX(float xRot) {
		float x = this.x;
		float y = this.y;
		float z = this.z;
		this.x = x;
		this.y = (float) ((y * Math.cos(xRot)) + (z * Math.sin(xRot)));
		this.z = (float) ((y * -Math.sin(xRot)) + (z * Math.cos(xRot)));
		return this;
	}
	
	public Vec3 rotateY(float yRot) {
		float x = this.x;
		float y = this.y;
		float z = this.z;
		this.x = (float) ((x * Math.cos(yRot)) + (z * -Math.sin(yRot)));
		this.y = y;
		this.z = (float) ((x * Math.sin(yRot)) + (z * Math.cos(yRot)));
		return this;
	}
	
	public Vec3 rotateZ(float zRot) {
		float x = this.x;
		float y = this.y;
		float z = this.z;
		this.x = (float) ((x * Math.cos(zRot)) + (y * Math.sin(zRot)));
		this.y = (float) ((x * -Math.sin(zRot)) + (y * Math.cos(zRot)));
		this.z = z;
		return this;
	}
	
	public String toString() {
		return this.x + ", " + this.y + ", " + this.z;
	}
}
