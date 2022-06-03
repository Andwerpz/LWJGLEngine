package graphics;

import static org.lwjgl.opengl.GL20.*;

import java.util.HashMap;
import java.util.Map;

import util.Mat4;
import util.ShaderUtils;
import util.Vec3;

public class Shader {
	
	//location when passed into shaders
	public static final int VERTEX_ATTRIB = 0;
	public static final int TCOORD_ATTRIB = 1;
	
	public static Shader HTRI, PERS;
	
	private boolean enabled = false;
	
	private final int ID;
	private Map<String, Integer> locationCache = new HashMap<>();
	
	public Shader(String vertex, String fragment) {
		ID = ShaderUtils.load(vertex, fragment);
	}
	
	public static void loadAll() {
		HTRI = new Shader("shaders/hellotri.vert", "shaders/hellotri.frag");
		PERS = new Shader("shaders/perspective.vert", "shaders/perspective.frag");
	}
	
	public int getUniform(String name) {
		if(locationCache.containsKey(name)) {
			return locationCache.get(name);
		}
		
		int result = glGetUniformLocation(ID, name);
		if(result == -1) {
			System.err.println("Could not find uniform variable " + name);
		}
		locationCache.put(name, result);
		return result;
	}
	
	public void setUniform1i(String name, int value) {
		if(!enabled) enable();
		glUniform1i(getUniform(name), value);
	}
	
	public void setUniform1f(String name, float value) {
		if(!enabled) enable();
		glUniform1f(getUniform(name), value);
	}
	
	public void setUniform2f(String name, float x, float y) {
		if(!enabled) enable();
		glUniform2f(getUniform(name), x, y);
	}
	
	public void setUniform3f(String name, Vec3 v) {
		if(!enabled) enable();
		glUniform3f(getUniform(name), v.x, v.y, v.z);
	}
	
	public void setUniformMat4(String name, Mat4 mat) {
		if(!enabled) enable();
		glUniformMatrix4fv(getUniform(name), false, mat.toFloatBuffer());
	}
	
	public void enable() {
		glUseProgram(ID);
		enabled = true;
	}
	
	public void disable() {
		glUseProgram(0);
		enabled = false;
	}
	
}
