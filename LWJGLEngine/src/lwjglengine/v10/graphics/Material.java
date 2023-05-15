package lwjglengine.v10.graphics;

import static org.lwjgl.assimp.Assimp.*;

import java.awt.Color;

import org.lwjgl.assimp.AIColor4D;

import myutils.v10.math.Vec3;
import myutils.v10.math.Vec4;

public class Material {
	private Vec4 diffuse; //RGBA
	private Vec4 specular; //Reflection RGB,
	private Vec4 emissive; //xyz = emissive rgb, a = strength 

	private float shininess; // also known as specular exponent
	private float smoothness = 0; //between 0 and 1, 0 is like ceramic, and 1 is mirror
	private float specularProbability = 0; //between 0 and 1, probability of ray bouncing specularly off of the surface. 
	private float metallic = 0; //0 is not metal (dielectric) and 1 is metal

	public static Material defaultMaterial() {
		return new Material(new Vec3(1f), new Vec3(1), 64f);
	}

	public Material(Material m) {
		this.diffuse = new Vec4(m.diffuse);
		this.specular = new Vec4(m.specular);
		this.emissive = new Vec4(m.emissive);
		this.shininess = m.shininess;
		this.smoothness = m.smoothness;
		this.specularProbability = m.specularProbability;
	}

	public Material(Vec3 diffuse) {
		this.diffuse = new Vec4(diffuse, 1);
		this.specular = new Vec4(1);
		this.emissive = new Vec4(0);
		this.shininess = 64f;
	}

	public Material(Vec4 diffuse) {
		this.diffuse = new Vec4(diffuse);
		this.specular = new Vec4(1);
		this.emissive = new Vec4(0);
		this.shininess = 64f;
	}

	public Material(Color diffuse) {
		this.diffuse = new Vec4(diffuse.getRed() / 255f, diffuse.getGreen() / 255f, diffuse.getBlue() / 255f, 1);
		this.specular = new Vec4(1);
		this.emissive = new Vec4(0);
		this.shininess = 64f;
	}

	public Material(Vec3 diffuse, Vec3 specular, float shininess) {
		this.diffuse = new Vec4(diffuse, 1);
		this.specular = new Vec4(specular, 1);
		this.emissive = new Vec4(0);
		this.shininess = shininess;
	}

	public Material(Vec4 diffuse, Vec4 specular, float shininess) {
		this.diffuse = new Vec4(diffuse);
		this.specular = new Vec4(specular);
		this.emissive = new Vec4(0);
		this.shininess = shininess;
	}

	public Material(AIColor4D diffuse, AIColor4D specular, AIColor4D shininess) {
		this.diffuse = new Vec4(diffuse.r(), diffuse.g(), diffuse.b(), diffuse.a());
		this.specular = new Vec4(specular.r(), specular.g(), specular.b(), specular.a());
		this.emissive = new Vec4(0);
		this.shininess = shininess.r();
	}
	
	public void set(Material m) {
		this.diffuse.set(m.getDiffuse());
		this.specular.set(m.getSpecular());
		this.emissive.set(m.getEmissive());
		this.shininess = m.getShininess();
		this.smoothness = m.getSmoothness();
		this.specularProbability = m.getSpecularProbability();
		this.metallic = m.getMetallic();
	}

	public Vec4 getDiffuse() {
		return this.diffuse;
	}

	public Vec4 getSpecular() {
		return this.specular;
	}

	public float getShininess() {
		return this.shininess;
	}
	
	public float getMetallic() {
		return this.metallic;
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

	public void setSmoothness(float s) {
		this.smoothness = s;
	}

	public void setSpecularProbability(float s) {
		this.specularProbability = s;
	}

	public void setEmissive(Vec4 e) {
		this.emissive.set(e);
	}

	public float getSmoothness() {
		return this.smoothness;
	}

	public float getSpecularProbability() {
		return this.specularProbability;
	}

	public Vec4 getEmissive() {
		return this.emissive;
	}
	
	public void setMetallic(float m) {
		this.metallic = m;
	}

	@Override
	public String toString() {
		String ans = "Diffuse : " + this.diffuse + "\n";
		ans += "Specular : " + this.specular + "\n";
		ans += "Emissive : " + this.emissive + "\n";
		ans += "Shininess : " + this.shininess + "\n";
		ans += "Smoothness : " + this.smoothness + "\n";
		ans += "Specular Probability : " + this.specularProbability;
		return ans;
	}

	public float[] toFloatArr() {
		float[] res = new float[4 + 4 + 4 + 1 + 1 + 1];
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
		res[12] = this.shininess;
		res[13] = this.smoothness;
		res[14] = this.specularProbability;
		return res;
	}

}
