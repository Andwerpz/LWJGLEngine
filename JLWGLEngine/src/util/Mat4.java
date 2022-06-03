package util;

import java.nio.FloatBuffer;

import main.Main;

public class Mat4 {

	public float[][] mat = new float[4][4];

	public Mat4() {

	}

	public static Mat4 identity() {
		Mat4 result = new Mat4();

		for (int i = 0; i < 4; i++) {
			result.mat[i][i] = 1;
		}

		return result;
	}

	/**
	 * Makes an orthographic projection matrix.
	 * 
	 * @param left
	 * @param right
	 * @param bottom
	 * @param top
	 * @param near
	 * @param far
	 * @return
	 */

	public static Mat4 orthographic(float left, float right, float bottom, float top, float near, float far) {
		Mat4 result = identity();

		result.mat[0][0] = 2f / (right - left);
		result.mat[1][1] = 2f / (top - bottom);
		result.mat[2][2] = 2f / (near - far);
		result.mat[0][3] = (left + right) / (left - right);
		result.mat[1][3] = (bottom + top) / (bottom - top);
		result.mat[2][3] = (far + near) / (far - near);

		return result;
	}

	public static Mat4 perspective(float viewAngleRad, float width, float height, float nearClippingPlaneDistance,
			float farClippingPlaneDistance) {
		// convert angle from degree to radians
		final float radians = viewAngleRad;

		float halfHeight = (float) (Math.tan(radians / 2) * nearClippingPlaneDistance);

		float halfScaledAspectRatio = halfHeight * (width / height);

		Mat4 projection = perspectiveFrustum(-halfScaledAspectRatio, halfScaledAspectRatio, -halfHeight, halfHeight,
				nearClippingPlaneDistance, farClippingPlaneDistance);

		return projection;
	}

	public static Mat4 perspectiveFrustum(float left, float right, float bottom, float top, float near, float far) {
		Mat4 result = new Mat4();

		result.mat[0][0] = (2f * near) / (right - left);
		result.mat[2][0] = (right + left) / (right - left);

		result.mat[1][1] = (2 * near) / (top - bottom);
		result.mat[2][1] = (top + bottom) / (top - bottom);

		result.mat[2][2] = -(far + near) / (far - near);
		result.mat[2][3] = -2 * (far * near) / (far - near);

		result.mat[3][2] = -1;
		result.mat[3][3] = 0;

		return result;
	}

	public static Mat4 translate(Vec3 vec) {
		Mat4 result = identity();

		result.mat[0][3] = vec.x;
		result.mat[1][3] = vec.y;
		result.mat[2][3] = vec.z;

		return result;
	}

	/**
	 * For use in 2D rotation returns a matrix that will rotate around the z axis
	 * 
	 * @param rad
	 * @return
	 */

	public static Mat4 rotateZ(float rad) {
		Mat4 result = identity();
		float cos = (float) Math.cos(rad);
		float sin = (float) Math.sin(rad);

		result.mat[0][0] = cos;
		result.mat[1][0] = -sin;
		result.mat[0][1] = sin;
		result.mat[1][1] = cos;

		return result;
	}
	
	public static Mat4 rotateX(float rad) {
		Mat4 result = identity();
		float cos = (float) Math.cos(rad);
		float sin = (float) Math.sin(rad);

		result.mat[1][1] = cos;
		result.mat[2][1] = sin;
		result.mat[1][2] = -sin;
		result.mat[2][2] = cos;

		return result;
	}
	
	public static Mat4 rotateY(float rad) {
		Mat4 result = identity();
		float cos = (float) Math.cos(rad);
		float sin = (float) Math.sin(rad);

		result.mat[0][0] = cos;
		result.mat[2][0] = sin;
		result.mat[0][2] = -sin;
		result.mat[2][2] = cos;

		return result;
	}

	public static Mat4 scale(float amt) {
		Mat4 result = Mat4.identity();
		for (int i = 0; i < 3; i++) {
			result.mat[i][i] = amt;
		}

		return result;
	}

	public Mat4 mul(Mat4 matrix) {
		Mat4 result = new Mat4();

		for (int y = 0; y < 4; y++) {
			for (int x = 0; x < 4; x++) {
				float sum = 0f;
				for (int e = 0; e < 4; e++) {
					sum += this.mat[x][e] * matrix.mat[e][y];
				}
				result.mat[x][y] = sum;
			}
		}

		return result;
	}

	public FloatBuffer toFloatBuffer() {
		//have to convert to column major order
		float[] elements = new float[4 * 4];
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				elements[i + j * 4] = this.mat[i][j];
			}
		}

		return BufferUtils.createFloatBuffer(elements);
	}
	
	public String toString() {
		String out = "";
		for(float[] i : mat) {
			for(float j : i) {
				out += j + " ";
			}
			out += "\n";
		}
		return out;
	}

}
