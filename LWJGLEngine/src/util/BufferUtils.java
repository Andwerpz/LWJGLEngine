package util;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import graphics.Material;

import java.nio.ByteOrder;

public class BufferUtils {

	// FloatBuffer.flip() is deprecated, need to cast as Buffer to use.

	public static ByteBuffer createByteBuffer(byte[] array) {
		ByteBuffer result = ByteBuffer.allocateDirect(array.length).order(ByteOrder.nativeOrder());
		((Buffer) result.put(array)).flip();
		return result;
	}

	public static ByteBuffer createByteBuffer(int capacity) {
		ByteBuffer result = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
		return result;
	}

	public static FloatBuffer createFloatBuffer(float[] array) {
		FloatBuffer result = ByteBuffer.allocateDirect(array.length << 2).order(ByteOrder.nativeOrder()).asFloatBuffer();
		((Buffer) result.put(array)).flip();
		return result;
	}

	public static IntBuffer createIntBuffer(int[] array) {
		IntBuffer result = ByteBuffer.allocateDirect(array.length << 2).order(ByteOrder.nativeOrder()).asIntBuffer();
		((Buffer) result.put(array)).flip();
		return result;
	}

	public static FloatBuffer createFloatBuffer(Mat4[] array) {
		float[] elements = new float[array.length * 16];
		// have to convert to column major order
		for (int k = 0; k < array.length; k++) {
			for (int i = 0; i < 4; i++) {
				for (int j = 0; j < 4; j++) {
					elements[i + j * 4 + k * 16] = array[k].mat[i][j];
				}
			}
		}
		return createFloatBuffer(elements);
	}

	public static FloatBuffer createFloatBuffer(Vec3[] array) {
		float[] elements = new float[array.length * 3];
		for (int i = 0; i < array.length; i++) {
			elements[i * 3 + 0] = array[i].x;
			elements[i * 3 + 1] = array[i].y;
			elements[i * 3 + 2] = array[i].z;
		}
		return createFloatBuffer(elements);
	}

	public static FloatBuffer createFloatBuffer(Material[] array) {
		float[] elements = new float[array.length * (4 + 4 + 1)]; // diffuse, specular, shininess
		int eSize = 9;
		for (int i = 0; i < array.length; i++) {
			elements[i * eSize + 0] = array[i].getDiffuse().x;
			elements[i * eSize + 1] = array[i].getDiffuse().y;
			elements[i * eSize + 2] = array[i].getDiffuse().z;
			elements[i * eSize + 3] = array[i].getDiffuse().w;
			elements[i * eSize + 4] = array[i].getSpecular().x;
			elements[i * eSize + 5] = array[i].getSpecular().y;
			elements[i * eSize + 6] = array[i].getSpecular().z;
			elements[i * eSize + 7] = array[i].getSpecular().w;
			elements[i * eSize + 8] = array[i].getShininess();
		}
		return createFloatBuffer(elements);
	}
}
