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
	
	public float ambientIntensity;
	public float cutOff, outerCutOff;
	public float constant, linear, quadratic;
	
	public Light() {
		
	}

	public abstract void bind(Shader s, int index);
	
}
