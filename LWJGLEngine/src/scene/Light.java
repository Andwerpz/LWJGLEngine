package scene;

import java.util.ArrayList;
import java.util.HashMap;

import graphics.Shader;
import util.Vec3;

public abstract class Light {

	public static final int DIR_LIGHT = 0;
	public static final int POINT_LIGHT = 1;
	public static final int SPOT_LIGHT = 2;

	public static HashMap<Integer, ArrayList<Light>> lights = new HashMap<>();

	public int type;

	public Vec3 pos, dir, color;

	public float ambientIntensity; // how bright ambient is compared to diffuse. 1.0 for the same.
	public float cutOff, outerCutOff;
	public float constant, linear, quadratic;

	public Light() {

	}

	public static void addLight(int scene, Light l) {
		if(lights.get(scene) == null) {
			lights.put(scene, new ArrayList<Light>());
		}
		lights.get(scene).add(l);
	}

	public static void removeLightsFromScene(int scene) {
		if(lights.get(scene) != null) {
			lights.get(scene).clear();
		}
	}

	public abstract void bind(Shader s, int index);

}
