package lwjglengine.util;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

import lwjglengine.graphics.Shader;
import myutils.file.FileUtils;
import myutils.file.JarUtils;

public class ShaderUtils {

	private static int load(String srcPath, int shaderType) {
		String src = JarUtils.loadString(srcPath);
		int shaderID = glCreateShader(shaderType);
		glShaderSource(shaderID, src);
		glCompileShader(shaderID);
		if (glGetShaderi(shaderID, GL_COMPILE_STATUS) == GL_FALSE) {
			System.err.println("Failed to compile shader");
			System.err.println(glGetShaderInfoLog(shaderID));
			return -1;
		}
		return shaderID;
	}

	public static Shader createShader(String vertPath, String fragPath) {
		int program = glCreateProgram();
		int vertID = load(vertPath, GL_VERTEX_SHADER);
		int fragID = load(fragPath, GL_FRAGMENT_SHADER);

		glAttachShader(program, vertID);
		glAttachShader(program, fragID);
		glLinkProgram(program);
		glValidateProgram(program);

		boolean error = false;
		if (glGetProgrami(program, GL_VALIDATE_STATUS) == GL_FALSE) {
			System.err.println("Failed to link program");
			error = true;
		}
		if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
			System.err.println("Failed to validate program");
			error = true;
		}
		if (error) {
			System.err.println(glGetProgramInfoLog(program));
//			return null;
		}

		glDeleteShader(vertID);
		glDeleteShader(fragID);

		return new Shader(program);
	}

	public static Shader createShader(String srcPath, int shaderType) {
		int program = glCreateProgram();
		int shaderID = load(srcPath, shaderType);

		glAttachShader(program, shaderID);
		glLinkProgram(program);
		glValidateProgram(program);
		
		boolean error = false;
		if (glGetProgrami(program, GL_VALIDATE_STATUS) == GL_FALSE) {
			System.err.println("Failed to link program");
			error = true;
		}
		if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
			System.err.println("Failed to validate program");
			error = true;
		}
		if (error) {
			System.err.println(glGetProgramInfoLog(program));
//			return null;
		}

		glDeleteShader(shaderID);

		return new Shader(program);
	}

}
