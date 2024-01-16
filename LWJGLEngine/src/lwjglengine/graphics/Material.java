package lwjglengine.graphics;

import static org.lwjgl.assimp.Assimp.*;

import java.awt.Color;

import org.lwjgl.assimp.AIColor4D;

import myutils.math.Vec3;
import myutils.math.Vec4;

public class Material {
	//generally, these material attributes will just scale whatever attributes the texture has. 

	private Vec4 diffuse; //RGBA
	private Vec4 specular; //Reflection RGB,
	private Vec4 emissive; //xyz = emissive rgb, a = strength 

	private float specularExponent = 64f;
	private float roughness = 1; //between 0 and 1, 0 is like mirror, and 1 is ceramic
	private float refractiveIndex = 0; //if greater than 1, the material is transmissive
	private float metalness = 0; //0 is not metal (dielectric) and 1 is metal

	public static Material defaultMaterial() {
		return new Material(new Vec3(1f), new Vec3(1f), 64f);
	}

	public static Material transparent() {
		return new Material(new Vec4(0f));
	}

	public Material(Material m) {
		this.diffuse = new Vec4(m.diffuse);
		this.specular = new Vec4(m.specular);
		this.emissive = new Vec4(m.emissive);
		this.specularExponent = m.specularExponent;
		this.roughness = m.roughness;
		this.refractiveIndex = m.refractiveIndex;
		this.metalness = m.metalness;
	}

	public Material(Vec3 diffuse) {
		this.diffuse = new Vec4(diffuse, 1);
		this.specular = new Vec4(1f);
		this.emissive = new Vec4(0);
		this.specularExponent = 64f;
	}

	public Material(Vec4 diffuse) {
		this.diffuse = new Vec4(diffuse);
		this.specular = new Vec4(1);
		this.emissive = new Vec4(0);
		this.specularExponent = 64f;
	}

	public Material(Color diffuse) {
		this.diffuse = new Vec4(diffuse.getRed() / 255f, diffuse.getGreen() / 255f, diffuse.getBlue() / 255f, 1);
		this.specular = new Vec4(1);
		this.emissive = new Vec4(0);
		this.specularExponent = 64f;
	}

	public Material(Vec3 diffuse, Vec3 specular, float specularExponent) {
		this.diffuse = new Vec4(diffuse, 1);
		this.specular = new Vec4(specular, 1);
		this.emissive = new Vec4(0);
		this.specularExponent = specularExponent;
	}

	public Material(Vec4 diffuse, Vec4 specular, float specularExponent) {
		this.diffuse = new Vec4(diffuse);
		this.specular = new Vec4(specular);
		this.emissive = new Vec4(0);
		this.specularExponent = specularExponent;
	}

	public Material(AIColor4D diffuse, AIColor4D specular, AIColor4D specularExponent) {
		this.diffuse = new Vec4(diffuse.r(), diffuse.g(), diffuse.b(), diffuse.a());
		this.specular = new Vec4(specular.r(), specular.g(), specular.b(), specular.a());
		this.emissive = new Vec4(0);
		this.specularExponent = specularExponent.r();
	}

	public void set(Material m) {
		this.diffuse.set(m.getDiffuse());
		this.specular.set(m.getSpecular());
		this.emissive.set(m.getEmissive());
		this.specularExponent = m.getSpecularExponent();
		this.roughness = m.getRoughness();
		this.refractiveIndex = m.getRefractiveIndex();
		this.metalness = m.getMetalness();
	}

	public Vec4 getDiffuse() {
		return this.diffuse;
	}

	public Vec4 getSpecular() {
		return this.specular;
	}

	public float getSpecularExponent() {
		return this.specularExponent;
	}

	public float getMetalness() {
		return this.metalness;
	}

	public void setDiffuse(Vec4 diffuse) {
		this.diffuse.set(diffuse);
	}

	public void setDiffuse(Vec3 diffuse) {
		this.diffuse.x = diffuse.x;
		this.diffuse.y = diffuse.y;
		this.diffuse.z = diffuse.z;
	}

	public void setSpecular(Vec3 specular) {
		this.specular.x = specular.x;
		this.specular.y = specular.y;
		this.specular.z = specular.z;
	}

	public void setAlpha(float alpha) {
		this.diffuse.w = alpha;
	}

	public void setRoughness(float s) {
		this.roughness = s;
	}

	public void setSpecularExponent(float s) {
		this.specularExponent = s;
	}

	public void setRefractiveIndex(float s) {
		this.refractiveIndex = s;
	}

	public void setEmissive(Vec4 e) {
		this.emissive.set(e);
	}

	public float getRoughness() {
		return this.roughness;
	}

	public float getRefractiveIndex() {
		return this.refractiveIndex;
	}

	public Vec4 getEmissive() {
		return this.emissive;
	}

	public void setMetalness(float m) {
		this.metalness = m;
	}

	@Override
	public String toString() {
		String ans = "Diffuse : " + this.diffuse + "\n";
		ans += "Specular : " + this.specular + "\n";
		ans += "Specular Exponent : " + this.specularExponent + "\n";
		ans += "Roughness : " + this.roughness + "\n";
		ans += "Metalness : " + this.metalness + "\n";
		ans += "Refractive Index : " + this.refractiveIndex + "\n";
		ans += "Emissive : " + this.emissive + "\n";
		return ans;
	}

	//this is only used in raytracing, vertex array manually extracts stuff. 
	public float[] toFloatArr() {
		float[] res = new float[4 + 4 + 4 + 1 + 1 + 1 + 1];
		res[0] = this.diffuse.x;
		res[1] = this.diffuse.y;
		res[2] = this.diffuse.z;
		res[3] = this.diffuse.w;
		res[4] = this.specular.x;
		res[5] = this.specular.y;
		res[6] = this.specular.z;
		res[7] = this.specular.w;
		res[8] = this.emissive.x;
		res[9] = this.emissive.y;
		res[10] = this.emissive.z;
		res[11] = this.emissive.w;
		res[12] = this.specularExponent;
		res[13] = this.roughness;
		res[14] = this.metalness;
		res[15] = this.refractiveIndex;
		return res;
	}

}
