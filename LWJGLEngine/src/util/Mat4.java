package util;

import java.nio.FloatBuffer;

import main.Main;

public class Mat4 {

	public float[][] mat = new float[4][4];

	public Mat4() {

	}

	public Mat4(Mat4 mat) {
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				this.mat[i][j] = mat.mat[i][j];
			}
		}
	}

	public Mat4(Vec3 row1, Vec3 row2, Vec3 row3) {
		mat[0][0] = row1.x;
		mat[0][1] = row1.y;
		mat[0][2] = row1.z;

		mat[1][0] = row2.x;
		mat[1][1] = row2.y;
		mat[1][2] = row2.z;

		mat[2][0] = row3.x;
		mat[2][1] = row3.y;
		mat[2][2] = row3.z;
	}

	public Mat4 transpose() {
		float[][] nextMat = new float[4][4];
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				nextMat[i][j] = this.mat[j][i];
			}
		}
		this.mat = nextMat;
		return this;
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
		result.mat[2][2] = -2f / (far - near);
		result.mat[0][3] = -(right + left) / (right - left);
		result.mat[1][3] = -(top + bottom) / (top - bottom);
		result.mat[2][3] = -(far + near) / (far - near);

		return result;
	}

	/**
	 * Makes a perspective projection matrix
	 * 
	 * @param viewAngleRad
	 * @param width
	 * @param height
	 * @param nearClippingPlaneDistance
	 * @param farClippingPlaneDistance
	 * @return
	 */

	public static Mat4 perspective(float viewAngleRad, float width, float height, float nearClippingPlaneDistance, float farClippingPlaneDistance) {
		final float radians = viewAngleRad;

		float halfHeight = (float) (Math.tan(radians / 2) * nearClippingPlaneDistance);

		float halfScaledAspectRatio = halfHeight * (width / height);

		Mat4 projection = perspectiveFrustum(-halfScaledAspectRatio, halfScaledAspectRatio, -halfHeight, halfHeight, nearClippingPlaneDistance, farClippingPlaneDistance);

		return projection;
	}

	private static Mat4 perspectiveFrustum(float left, float right, float bottom, float top, float near, float far) {
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

	/**
	 * Essentially the same as glm::lookAt(). Eye is the viewing position. Center is
	 * the position that you are looking at.
	 * 
	 * This matrix transforms space so that eye is at the origin, and the vector
	 * from eye to center looks down the -z axis.
	 * 
	 * @param eye
	 * @param pos
	 * @param up
	 * @return
	 */

	public static Mat4 lookAt(Vec3 eye, Vec3 center, Vec3 up) {
		// define our 3 basis vectors for the new space
		Vec3 z = new Vec3(center, eye).normalize(); // Z
		Vec3 x = up.cross(z).normalize();
		Vec3 y = z.cross(x).normalize();

		Mat4 result = Mat4.translate(eye.mul(-1f));
		Mat4 viewSpace = new Mat4(x, y, z);
		viewSpace.mat[3][3] = 1;
		result.muli(viewSpace);

		return result;
	}

	public static Mat4 translate(Vec3 vec) {
		Mat4 result = identity();

		result.mat[0][3] = vec.x;
		result.mat[1][3] = vec.y;
		result.mat[2][3] = vec.z;

		return result;
	}

	public static Mat4 translate(float x, float y, float z) {
		return Mat4.translate(new Vec3(x, y, z));
	}

	/**
	 * Returns a matrix that will rotate around the z axis
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

	/**
	 * Returns a matrix that will rotate around the x axis
	 * 
	 * @param rad
	 * @return
	 */

	public static Mat4 rotateX(float rad) {
		Mat4 result = identity();
		float cos = (float) Math.cos(rad);
		float sin = (float) Math.sin(rad);

		result.mat[1][1] = cos;
		result.mat[2][1] = -sin;
		result.mat[1][2] = sin;
		result.mat[2][2] = cos;

		return result;
	}

	/**
	 * Returns a matrix that will rotate around the y axis
	 * 
	 * @param rad
	 * @return
	 */

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

	public static Mat4 scale(float xAmt, float yAmt, float zAmt) {
		Mat4 result = Mat4.identity();
		result.mat[0][0] = xAmt;
		result.mat[1][1] = yAmt;
		result.mat[2][2] = zAmt;

		return result;
	}

	/**
	 * Returns a new matrix equal to the product between itself and the input
	 * 
	 * @param matrix
	 * @return
	 */
	public Mat4 mul(Mat4 matrix) {
		Mat4 result = new Mat4();

		for (int y = 0; y < 4; y++) {
			for (int x = 0; x < 4; x++) {
				float sum = 0f;
				for (int e = 0; e < 4; e++) {
					sum += this.mat[e][y] * matrix.mat[x][e];
				}
				result.mat[x][y] = sum;
			}
		}

		return result;
	}

	/**
	 * Sets itself equal to the product from a multiplication with itself and the
	 * input
	 * 
	 * @param matrix
	 * @return
	 */
	public Mat4 muli(Mat4 matrix) {
		this.mat = this.mul(matrix).mat;
		return this;
	}

	public Vec3 mul(Vec3 vec, float w) {
		//		return new Vec3(
		//			vec.x * mat[0][0] + vec.y * mat[1][0] + vec.z * mat[2][0] + w * mat[3][0],
		//			vec.x * mat[0][1] + vec.y * mat[1][1] + vec.z * mat[2][1] + w * mat[3][1],
		//			vec.x * mat[0][2] + vec.y * mat[1][2] + vec.z * mat[2][2] + w * mat[3][2]
		//		);

		return new Vec3(vec.x * mat[0][0] + vec.y * mat[0][1] + vec.z * mat[0][2] + w * mat[0][3], vec.x * mat[1][0] + vec.y * mat[1][1] + vec.z * mat[1][2] + w * mat[1][3], vec.x * mat[2][0] + vec.y * mat[2][1] + vec.z * mat[2][2] + w * mat[2][3]);
	}

	public FloatBuffer toFloatBuffer() {
		// have to convert to column major order
		float[] elements = new float[4 * 4];
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				elements[i + j * 4] = this.mat[i][j];
			}
		}

		return BufferUtils.createFloatBuffer(elements);
	}

	@Override
	public String toString() {
		String out = "";
		for (float[] i : mat) {
			for (float j : i) {
				out += j + " ";
			}
			out += "\n";
		}
		return out;
	}

}
