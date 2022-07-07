package model;

import static org.lwjgl.opengl.GL11.*;

import java.util.ArrayList;

import graphics.Shader;
import graphics.Texture;
import graphics.VertexArray;
import util.Vec3;

public class Cube extends Model {
	
	static VertexArray v_top, v_bottom, v_north, v_south, v_east, v_west;

	public static void create() {
		byte[] indices = new byte[] {
			0, 1, 2,
			0, 2, 3,
		};
		
		float[] tex = new float[] {
			0f, 0f,
			0f, 1f,
			1f, 1f,
			1f, 0f,
		};
		
		v_top = new VertexArray(
			new float[] {
				0f, 1f, 0f,
				0f, 1f, 1f,
				1f, 1f, 1f,
				1f, 1f, 0f,
			},
			indices, tex, GL_TRIANGLES
		);
		
		v_bottom = new VertexArray(
			new float[] {
				0f, 0f, 1f,
				0f, 0f, 0f,
				1f, 0f, 0f,
				1f, 0f, 1f,
			},
			indices, tex, GL_TRIANGLES
		);
		
		v_north = new VertexArray(
			new float[] {
				0f, 0f, 0f,
				0f, 1f, 0f,
				1f, 1f, 0f,
				1f, 0f, 0f,
			},
			indices, tex, GL_TRIANGLES
		);
		
		v_south = new VertexArray(
			new float[] {
				1f, 0f, 1f,
				1f, 1f, 1f,
				0f, 1f, 1f,
				0f, 0f, 1f,
			},
			indices, tex, GL_TRIANGLES
		);
		
		v_east = new VertexArray(
			new float[] {
				0f, 0f, 1f,
				0f, 1f, 1f,
				0f, 1f, 0f,
				0f, 0f, 0f,
			},
			indices, tex, GL_TRIANGLES
		);
		
		v_west = new VertexArray(
			new float[] {
				1f, 0f, 0f,
				1f, 1f, 0f,
				1f, 1f, 1f,
				1f, 0f, 1f,
			},
			indices, tex, GL_TRIANGLES
		);
	}
	
	public static void render(Texture tex) {
		VertexArray[] vert = new VertexArray[] {
			v_top, v_bottom, v_north, v_south, v_east, v_west
		};
		
		tex.bind();
		for(int i = 0; i < 6; i++) {
			vert[i].render();
		}
		tex.bind();
	}
	
}
