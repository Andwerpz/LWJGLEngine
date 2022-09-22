package graphics;

import util.Vec3;

public class Material {
	
	private Vec3 diffuse, specular;
	private float shininess;	//also known as specular exponent
	
	public static Material defaultMaterial() {
		return new Material(new Vec3(1), new Vec3(1), 32f);
	}
	
	public Material(Vec3 diffuse, Vec3 specular, float shininess) {
		this.diffuse = new Vec3(diffuse);
		this.specular = new Vec3(specular);
		this.shininess = shininess;
	}
	
	public Vec3 getDiffuse() {
		return this.diffuse;
	}
	
	public Vec3 getSpecular() {
		return this.specular;
	}
	
	public float getShininess() {
		return this.shininess;
	}
	
}
