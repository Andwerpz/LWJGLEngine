package player;

import static org.lwjgl.glfw.GLFW.*;

import java.nio.DoubleBuffer;

import org.lwjgl.BufferUtils;

import main.Main;
import util.Mat4;
import util.MathTools;
import util.Vec2;
import util.Vec3;

public class Camera {

	public Vec3 pos;
	public float xRot, yRot, zRot;

	public Camera() {
		xRot = 0;
		yRot = 0;
		zRot = 0;
		pos = new Vec3(0, 0, 0);
	}

	//convert from real space to camera space
	
	// inverting camera transform:
	// -translate, -yrot, -xrot, -zrot
	public Mat4 getViewMatrix() {
		Mat4 out = Mat4.identity();
		out = out.mul(Mat4.translate(pos.mul(-1f)));
		out = out.mul(Mat4.rotateY(-yRot));
		out = out.mul(Mat4.rotateX(-xRot));
		out = out.mul(Mat4.rotateZ(-zRot));
		
		return out;
	}
	
	public Mat4 getViewRotMatrix() {
		Mat4 out = Mat4.identity();
		out = out.mul(Mat4.rotateY(-yRot));
		out = out.mul(Mat4.rotateX(-xRot));
		out = out.mul(Mat4.rotateZ(-zRot));
		
		return out;
	}
	
	//convert from camera space to real space
	public Mat4 getInvViewMatrix() {
		Mat4 out = Mat4.identity();
		//out = out.mul(Mat4.rotateY((float) Math.PI));
		out = out.mul(Mat4.rotateZ(zRot));
		out = out.mul(Mat4.rotateX(xRot));
		out = out.mul(Mat4.rotateY(yRot));
		out = out.mul(Mat4.translate(pos));
		
		return out;
	}
	
	public Vec3 getFacing() {
		Vec3 output = new Vec3(0, 0, -1);
		output.rotateZ(zRot);
		output.rotateX(xRot);
		output.rotateY(yRot);
		return output;
	}

}
