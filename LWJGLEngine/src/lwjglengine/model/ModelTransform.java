package lwjglengine.model;

import java.util.StringTokenizer;

import myutils.math.Mat4;
import myutils.math.MathUtils;
import myutils.math.Quaternion;
import myutils.math.Vec3;

public class ModelTransform implements Comparable<ModelTransform> {
	//just a utility class for creating a model matrix. 

	//transformations are applied in the order:
	// - scale
	// - rotation
	// - translation

	//scale in x, y, z, directions. 
	public Vec3 scale;

	//this should always be a unit quaternion. 
	public Quaternion rotation;

	public Vec3 translate;

	public boolean doPropogateScale = true;

	//this is jank. How fix D:
	//ideally, we would do all model transform stuff within this class, and not have to manipulate matrices outside. 
	//currently, this is only used by the ui element class. 
	public boolean doCustomModelMat4 = false;
	public Mat4 customModelMat4 = Mat4.identity();

	public ModelTransform() {
		this.init();
	}

	public ModelTransform(ModelTransform m) {
		this.init();
		this.set(m);
	}

	public ModelTransform(Vec3 scale, Quaternion rotation, Vec3 translate) {
		this.init();
		this.scale = new Vec3(scale);
		this.rotation = new Quaternion(rotation);
		this.translate = new Vec3(translate);
	}

	public ModelTransform(Mat4 customMat4) {
		this.init();
		this.doCustomModelMat4 = true;
		this.customModelMat4.set(customMat4);
	}

	private void init() {
		this.scale = new Vec3(1);
		this.rotation = Quaternion.identity();
		this.translate = new Vec3(0);
		this.doCustomModelMat4 = false;
		this.customModelMat4 = Mat4.identity();
	}

	/**
	 * The rotation quaternion will represent the rotation from (0, 0, -1) to orient. 
	 * Orient is basically the facing vector for this model transform. 
	 * @param scale
	 * @param orient
	 * @param translate
	 */
	public ModelTransform(Vec3 scale, Vec3 orient, Vec3 translate) {
		this.scale = new Vec3(scale);
		this.rotation = MathUtils.quaternionRotationUToV(new Vec3(0, 0, -1), orient);
		this.rotation.normalize();
		this.translate = new Vec3(translate);
	}

	public Mat4 getModelMatrix() {
		if (this.doCustomModelMat4) {
			return this.customModelMat4;
		}
		Mat4 ret = Mat4.identity();
		ret.muli(Mat4.scale(this.scale.x, this.scale.y, this.scale.z));
		ret.muli(MathUtils.quaternionToRotationMat4(this.rotation));
		ret.muli(Mat4.translate(this.translate));
		return ret;
	}

	public void set(ModelTransform m) {
		this.setRotation(m.rotation);
		this.setTranslation(m.translate);
		this.setScale(m.scale);
		if (m.doCustomModelMat4) {
			this.setCustomMat4(m.customModelMat4);
		}
	}

	public void setRotation(Quaternion q) {
		this.rotation.set(q);
	}

	public void setTranslation(Vec3 v) {
		this.translate.set(v);
	}

	public void setScale(Vec3 s) {
		this.scale.set(s);
	}

	public void setScale(float s) {
		this.scale.set(new Vec3(s));
	}

	public void setCustomMat4(Mat4 m) {
		this.doCustomModelMat4 = true;
		this.customModelMat4.set(m);
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
		Vec3 scale = Vec3.parseVec3(st.nextToken());
		Quaternion rotation = Quaternion.parseQuaternion(st.nextToken());
		Vec3 translate = Vec3.parseVec3(st.nextToken());
		return new ModelTransform(scale, rotation, translate);
	}

	@Override
	public int compareTo(ModelTransform o) {
		return 0;
	}
}
