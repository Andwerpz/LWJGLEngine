package graphics;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;

public class Cubemap {
	
	private int cubemapID;
	
	public Cubemap(String right, String left, String up, String down, String back, String front) {
		String[] sides = new String[] {right, left, up, down, back, front};
		cubemapID = load(sides);
	}
	
	public int load(String[] sides) {
		int id = glGenTextures();
		glBindTexture(GL_TEXTURE_CUBE_MAP, id);
		
		for(int i = 0; i < 6; i++) {
			int[] outWH = new int[2];
			int[] data = Texture.getDataFromImage(sides[i], false, (i != 2 && i != 3), outWH);
			glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL_RGBA, outWH[0], outWH[1], 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
		}
		
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE); 
		glBindTexture(GL_TEXTURE_CUBE_MAP, 0);
		
		return id;
	}
	
	public void bind(int textureID) {
		glActiveTexture(textureID);
		glBindTexture(GL_TEXTURE_CUBE_MAP, cubemapID);
	}
	
	public void unbind(int textureID) {
		glActiveTexture(textureID);
		glBindTexture(GL_TEXTURE_CUBE_MAP, 0);
	}
	
}
