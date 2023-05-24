package lwjglengine.v10.model;

import java.util.StringTokenizer;

import myutils.v10.math.Mat4;
import myutils.v10.math.MathUtils;
import myutils.v10.math.Quaternion;
import myutils.v10.math.Vec3;

public class ModelTransform {
	//just a utility class for creating a model matrix. 

	//transformations are applied in the order:
	// - scale
	// - rotation
	// - translation

	public float scale;

	//this should always be a unit quaternion. 
	public Quaternion rotation;

	public Vec3 translate;

	public ModelTransform() {
		this.scale = 1;
		this.rotation = Quaternion.identity();
		this.translate = new Vec3(0);
	}

	public ModelTransform(float scale, Quaternion rotation, Vec3 translate) {
		this.scale = scale;
		this.rotation = new Quaternion(rotation);
		this.translate = new Vec3(translate);
	}

	/**
	 * The rotation quaternion will represent the rotation from (0, 0, -1) to orient. 
	 * Orient is basically the facing vector for this model transform. 
	 * @param scale
	 * @param orient
	 * @param translate
	 */
	public ModelTransform(float scale, Vec3 orient, Vec3 translate) {
		this.scale = scale;
		this.rotation = MathUtils.quaternionRotationUToV(new Vec3(0, 0, -1), orient);
		this.rotation.normalize();
		this.translate = new Vec3(translate);
	}

	public Mat4 getModelMatrix() {
		Mat4 ret = Mat4.identity();
		ret.muli(Mat4.scale(this.scale));
		ret.muli(MathUtils.quaternionToRotationMat4(this.rotation));
		ret.muli(Mat4.translate(this.translate));
		return ret;
	}

	/**
	 * Propogates the model transform as if this were a joint of m, or m was the parent of this. 
	 * @param m
	 */
	public void add(ModelTransform m) {
		//multiply scale
		this.scale *= m.scale;

		//multiply rotation
		Quaternion newRotation = new Quaternion(this.rotation);
		newRotation.muli(m.rotation);
		newRotation.muli(this.rotation.inv());
		newRotation.normalize();
		this.rotation.set(newRotation);

		//apply parent rotation to current translation vector
		Mat4 parentRotationMat4 = MathUtils.quaternionToRotationMat4(m.rotation);
		this.translate = parentRotationMat4.mul(this.translate, 1);

		//add parent translation to current translation
		this.translate.addi(m.translate);
	}

	public void setRotation(Quaternion q) {
		this.rotation.set(q);
	}

	public void setTranslation(Vec3 v) {
		this.translate.set(v);
	}

	public void setScale(float s) {
		this.scale = s;
	}

	@Override
	public String toString() {
		String ret = "";
		ret += this.scale + "\n";
		ret += this.rotation.toString() + "\n";
		ret += this.translate.toString();
		return ret;
	}

	public static ModelTransform parseModelTransform(String s) {
		StringTokenizer st = new StringTokenizer(s, "\n");
		float scale = Float.parseFloat(st.nextToken());
		Quaternion rotation = Quaternion.parseQuaternion(st.nextToken());
		Vec3 translate = Vec3.parseVec3(st.nextToken());
		return new ModelTransform(scale, rotation, translate);
	}
}
