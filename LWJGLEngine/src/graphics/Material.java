package graphics;

import static org.lwjgl.assimp.Assimp.*;

import java.awt.Color;

import org.lwjgl.assimp.AIColor4D;

import util.Vec3;
import util.Vec4;

public class Material {

	private Vec4 diffuse; //RGBA
	private Vec4 specular; //Reflection RGB, 

	private float shininess; // also known as specular exponent

	public static Material defaultMaterial() {
		return new Material(new Vec3(1f), new Vec3(1), 64f);
	}

	public Material(Material m) {
		this.diffuse = new Vec4(m.diffuse);
		this.specular = new Vec4(m.specular);
		this.shininess = m.shininess;
	}

	public Material(Vec3 diffuse) {
		this.diffuse = new Vec4(diffuse, 1);
		this.specular = new Vec4(1);
		this.shininess = 64f;
	}

	public Material(Vec4 diffuse) {
		this.diffuse = new Vec4(diffuse);
		this.specular = new Vec4(1);
		this.shininess = 64f;
	}

	public Material(Color diffuse) {
		this.diffuse = new Vec4(diffuse.getRed() / 255f, diffuse.getGreen() / 255f, diffuse.getBlue() / 255f, 1);
		this.specular = new Vec4(1);
		this.shininess = 64f;
	}

	public Material(Vec3 diffuse, Vec3 specular, float shininess) {
		this.diffuse = new Vec4(diffuse, 1);
		this.specular = new Vec4(specular, 1);
		this.shininess = shininess;
	}

	public Material(Vec4 diffuse, Vec4 specular, float shininess) {
		this.diffuse = new Vec4(diffuse);
		this.specular = new Vec4(specular);
		this.shininess = shininess;
	}

	public Material(AIColor4D diffuse, AIColor4D specular, AIColor4D shininess) {
		this.diffuse = new Vec4(diffuse.r(), diffuse.g(), diffuse.b(), diffuse.a());
		this.specular = new Vec4(specular.r(), specular.g(), specular.b(), specular.a());
		this.shininess = shininess.r();
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

	public void setAlpha(float alpha) {
		this.diffuse.w = alpha;
	}

	@Override
	public String toString() {
		String ans = "Diffuse : " + this.diffuse + "\n";
		ans += "Specular : " + this.specular + "\n";
		ans += "Shininess : " + this.shininess;
		return ans;
	}

}
