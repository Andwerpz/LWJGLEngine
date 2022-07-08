package util;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ByteOrder;

public class BufferUtils {
	
	//FloatBuffer.flip() is deprecated, need to cast as Buffer to use. 

	public static ByteBuffer createByteBuffer(byte[] array) {
		ByteBuffer result = ByteBuffer.allocateDirect(array.length).order(ByteOrder.nativeOrder());
		((Buffer) result.put(array)).flip();
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
}
