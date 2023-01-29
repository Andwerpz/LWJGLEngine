package player;

import static org.lwjgl.glfw.GLFW.*;

import java.nio.DoubleBuffer;

import org.lwjgl.BufferUtils;

import main.Main;
import util.Mat4;
import util.Vec2;
import util.Vec3;

public class Camera {

	private Vec3 pos, facing, up;

	public Mat4 projectionMatrix;

	private boolean isPerspective;
	private boolean isOrthographic;

	private float verticalFOV, width, height, near, far;
	private float left, right, bottom, top;

	public Camera(float verticalFOV, float width, float height, float near, float far) {
		init();
		this.isPerspective = true;
		this.verticalFOV = verticalFOV;
		this.width = width;
		this.height = height;
		this.near = near;
		this.far = far;
		this.projectionMatrix = Mat4.perspective(verticalFOV, width, height, near, far);
	}

	public Camera(float left, float right, float bottom, float top, float near, float far) {
		init();
		this.isOrthographic = true;
		this.left = left;
		this.right = right;
		this.bottom = bottom;
		this.top = top;
		this.near = near;
		this.far = far;
		this.projectionMatrix = Mat4.orthographic(left, right, bottom, top, near, far);
	}

	public Camera(Mat4 projectionMatrix) {
		init();
		this.projectionMatrix = new Mat4(projectionMatrix);
	}

	public void init() {
		facing = new Vec3(0, 0, -1);
		up = new Vec3(0, 1, 0);
		pos = new Vec3(0, 0, 0);
	}
	
	public void setProjectionMatrix(Mat4 m) {
		this.projectionMatrix = m;
	}

	public Mat4 getProjectionMatrix() {
		return this.projectionMatrix;
	}

	// convert from real space to camera space
	public Mat4 getViewMatrix() {
		return Mat4.lookAt(this.pos, this.pos.add(this.getFacing()), up);
	}

	// convert from camera space to real space
	public Mat4 getInvViewMatrix() {
		Mat4 ans = Mat4.lookAt(new Vec3(0, 0, 0), this.getFacing(), up).transpose();
		ans.muli(Mat4.translate(pos.mul(1f)));
		return ans;
	}

	public Mat4 getInvRotMatrix() {
		Mat4 ans = this.getInvViewMatrix();

		ans.mat[0][3] = 0;
		ans.mat[1][3] = 0;
		ans.mat[2][3] = 0;

		return ans;
	}

	public void setVerticalFOV(float fov) {
		if (!this.isPerspective) {
			System.err.println("WRONG CAMERA TYPE");
			return;
		}
		this.verticalFOV = fov;
		this.projectionMatrix = Mat4.perspective(verticalFOV, width, height, near, far);
	}

	public float getVerticalFOV() {
		return this.verticalFOV;
	}

	public Vec3 getPos() {
		return this.pos;
	}

	public void setPos(Vec3 pos) {
		this.pos = new Vec3(pos);
	}

	public void setUp(Vec3 up) {
		this.up = new Vec3(up).normalize();
	}

	public void setUp(float zRot) {
		this.up = new Vec3(0, 1, 0).rotateZ(zRot);
	}

	public Vec3 getUp() {
		return this.up;
	}

	public void setFacing(float xRot, float yRot) {
		this.facing = new Vec3(0, 0, -1);
		this.facing.rotateX(xRot);
		this.facing.rotateY(yRot);
	}

	public void setFacing(Vec3 facing) {
		this.facing = new Vec3(facing).normalize();
	}

	public Vec3 getFacing() {
		return this.facing;
	}

}
