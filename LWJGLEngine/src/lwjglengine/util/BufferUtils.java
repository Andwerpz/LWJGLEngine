package lwjglengine.util;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import lwjglengine.graphics.Material;
import myutils.math.Mat4;
import myutils.math.Vec3;
import myutils.math.Vec4;

import java.nio.ByteOrder;

public class BufferUtils {

	// FloatBuffer.flip() is deprecated, need to cast as Buffer to use.

	public static ByteBuffer createByteBuffer(byte[] array) {
		if (array == null) {
			return null;
		}
		ByteBuffer result = ByteBuffer.allocateDirect(array.length).order(ByteOrder.nativeOrder());
		((Buffer) result.put(array)).flip();
		return result;
	}

	public static ByteBuffer createByteBuffer(int capacity) {
		ByteBuffer result = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
		return result;
	}

	public static FloatBuffer createFloatBuffer(float[] array) {
		if (array == null) {
			return null;
		}
		FloatBuffer result = ByteBuffer.allocateDirect(array.length << 2).order(ByteOrder.nativeOrder()).asFloatBuffer();
		((Buffer) result.put(array)).flip();
		return result;
	}

	public static FloatBuffer createFloatBuffer(int capacity) {
		FloatBuffer result = ByteBuffer.allocateDirect(capacity << 2).order(ByteOrder.nativeOrder()).asFloatBuffer();
		return result;
	}

	public static IntBuffer createIntBuffer(int[] array) {
		if (array == null) {
			return null;
		}
		IntBuffer result = ByteBuffer.allocateDirect(array.length << 2).order(ByteOrder.nativeOrder()).asIntBuffer();
		((Buffer) result.put(array)).flip();
		return result;
	}

	public static IntBuffer createIntBuffer(int capacity) {
		IntBuffer result = ByteBuffer.allocateDirect(capacity << 2).order(ByteOrder.nativeOrder()).asIntBuffer();
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
//		float[] elements = new float[array.length * (4 + 4 + 4 + 4)]; // diffuse, specular, shininess
//		int eSize = 16;
//		for (int i = 0; i < array.length; i++) {
//			elements[i * eSize + 0] = array[i].getDiffuse().x;
//			elements[i * eSize + 1] = array[i].getDiffuse().y;
//			elements[i * eSize + 2] = array[i].getDiffuse().z;
//			elements[i * eSize + 3] = array[i].getDiffuse().w;
//			elements[i * eSize + 4] = array[i].getSpecular().x;
//			elements[i * eSize + 5] = array[i].getSpecular().y;
//			elements[i * eSize + 6] = array[i].getSpecular().z;
//			elements[i * eSize + 7] = array[i].getSpecular().w;
//			elements[i * eSize + 8] = array[i].getSpecularExponent();
//			elements[i * eSize + 9] = array[i].getRoughness();
//			elements[i * eSize + 10] = array[i].getMetalness();
//			elements[i * eSize + 11] = array[i].getRefractiveIndex();
//			elements[i * eSize + 12] = array[i].getEmissive().x;
//			elements[i * eSize + 13] = array[i].getEmissive().y;
//			elements[i * eSize + 14] = array[i].getEmissive().z;
//			elements[i * eSize + 15] = array[i].getEmissive().w;
//		}
//		return createFloatBuffer(elements);
		
		float[] elements = new float[array.length * (4 + 4 + 4)]; // diffuse, specular, shininess
		int eSize = 12;
		for (int i = 0; i < array.length; i++) {
			elements[i * eSize + 0] = array[i].getDiffuse().x;
			elements[i * eSize + 1] = array[i].getDiffuse().y;
			elements[i * eSize + 2] = array[i].getDiffuse().z;
			elements[i * eSize + 3] = array[i].getDiffuse().w;
			elements[i * eSize + 4] = array[i].getSpecular().x;
			elements[i * eSize + 5] = array[i].getSpecular().y;
			elements[i * eSize + 6] = array[i].getSpecular().z;
			elements[i * eSize + 7] = array[i].getSpecular().w;
			elements[i * eSize + 8] = array[i].getSpecularExponent();
			elements[i * eSize + 9] = array[i].getRoughness();
			elements[i * eSize + 10] = array[i].getMetalness();
			elements[i * eSize + 11] = array[i].getRefractiveIndex();
		}
		return createFloatBuffer(elements);
	}

	public static FloatBuffer createFloatBuffer(Vec4[] hues) {
		float[] elements = new float[hues.length * 4];
		for (int i = 0; i < hues.length; i++) {
			elements[i * 4 + 0] = hues[i].x;
			elements[i * 4 + 1] = hues[i].y;
			elements[i * 4 + 2] = hues[i].z;
			elements[i * 4 + 3] = hues[i].w;
		}
		return createFloatBuffer(elements);
	}
}
