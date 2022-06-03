package util;

import java.util.ArrayList;

import main.Main;

public class MathTools {

	public static double clamp(double low, double high, double val) {
		return val < low ? low : (val > high ? high : val);
	}

	public static double dist(double x1, double y1, double x2, double y2) {
		return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
	}

	public static double distSq(Vec2 a, Vec2 b) {
		Vec2 out = b.sub(a);
		return out.lengthSq();
	}
	
	public static double dist3D(Vec3 a, Vec3 b) {
		return new Vec3(a, b).length();
	}

	public static double slope(double x1, double y1, double x2, double y2) {
		double dx = x1 - x2;
		double dy = y1 - y2;
		return dx / dy;
	}

	public static double radianAngleBetweenVectors(Vec2 a, Vec2 b) {
		return Math.acos(dotProduct(a, b) / (a.length() * b.length()));
	}

	// --------------- Stats -----------

	// approximates a normal distribution
	// in this case, n = 4
	public static double irwinHallDistribution(double x) {
		if (-2 < x && x < -1) {
			return 0.25 * Math.pow(x + 2, 3);
		}
		if (-1 < x && x < 1) {
			return 0.25 * (Math.pow(Math.abs(x), 3) * 3 - Math.pow(x, 2) * 6 + 4);
		}
		if (1 < x && x < 2) {
			return 0.25 * Math.pow(2 - x, 3);
		}
		return 0;
	}

	// --------------- ML --------------

	public static double sigmoid(double x) {
		return (1d / (1d + Math.pow(Math.E, (-1d * x))));
	}

	// derivative of sigmoid function with center at 0

	public static double sigmoidDerivative(double x) {
		return sigmoid(x) * (1d - sigmoid(x));
	}

	public static double relu(double x) {
		return Math.max(0, x);
	}

	// inv sigmoid
	public static double logit(double x) {
		return Math.log(x / (1d - x));
	}

	public static double reluDerivative(double x) {
		return x <= 0 ? 0 : 1;
	}

	// -------------- Linear Algebra -----------

	public static double dotProduct(Vec2 a, Vec2 b) {
		return a.x * b.x + a.y * b.y;
	}

	public static double dotProduct(Vec3 a, Vec3 b) {
		return a.x * b.x + a.y * b.y + a.z * b.z;
	}

	public static double crossProduct(Vec2 a, Vec2 b) {
		return a.x * b.y - a.y * b.x;
	}

	public static Vec2 crossProduct(Vec2 a, double s) {
		return new Vec2(s * a.y, -s * a.x);
	}

	public static Vec2 crossProduct(double s, Vec2 a) {
		return new Vec2(-s * a.y, s * a.x);
	}

	public static Vec3 crossProduct(Vec3 a, Vec3 b) {

		Vec3 normal = new Vec3(0, 0, 0);

		normal.x = a.y * b.z - a.z * b.y;
		normal.y = a.z * b.x - a.x * b.z;
		normal.z = a.x * b.y - a.y * b.x;

		return normal;

	}

	// takes in two line segments, and returns a vector pointing to where they
	// intersect, null otherwise
	public static Vec2 line_lineCollision(double x1, double y1, double x2, double y2, double x3, double y3, double x4,
			double y4) {
		// calculate the distance to intersection point
		double uA = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / ((y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1));
		double uB = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / ((y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1));

		// if uA and uB are between 0-1, lines are colliding
		if (uA >= 0 && uA <= 1 && uB >= 0 && uB <= 1) {

			// calculate the intersection point
			double intersectionX = x1 + (uA * (x2 - x1));
			double intersectionY = y1 + (uA * (y2 - y1));

			return new Vec2(intersectionX, intersectionY);
		}
		return null;
	}

	// takes in a line, lineP + lineVec, and two points, and return if the two
	// points are on the same side of the line

	public static boolean pointOnSameSideOfLine(Vec2 lineP, Vec2 lineVec, Vec2 a, Vec2 b) {

		// copy a and b
		Vec2 p1 = new Vec2(a);
		Vec2 p2 = new Vec2(b);

		// first offset lineP, p1, and p2 so that lineP is at the origin
		p1.subi(lineP);
		p2.subi(lineP);

		// calculate the line perpendicular to the input line
		// rotate the line 90 deg
		Vec2 perpendicular = new Vec2(lineVec.y, -lineVec.x);

		// now take dot product of the perpendicular vector with both points
		// if both dot products are negative or positive, then the points are on the
		// same side of the line
		Vec2 v1 = new Vec2(p1);
		Vec2 v2 = new Vec2(p2);
		double d1 = MathTools.dotProduct(perpendicular, v1);
		double d2 = MathTools.dotProduct(perpendicular, v2);

		if (d1 * d2 >= 0) {
			return true;
		}

		return false;

	}

	// assuming that the points are arranged into a convex hull.
	// get the centroid of each triangle, then take the weighted average of the
	// centroids, using the area of each triangle as the weight.
	public static Vec2 getCentroid(ArrayList<Vec2> points) {
		double accumulatedArea = 0.0f;
		double centerX = 0.0f;
		double centerY = 0.0f;

		for (int i = 0, j = points.size() - 1; i < points.size(); j = i++) {
			double temp = points.get(i).x * points.get(j).y - points.get(j).x * points.get(i).y;
			accumulatedArea += temp;
			centerX += (points.get(i).x + points.get(j).x) * temp;
			centerY += (points.get(i).y + points.get(j).y) * temp;
		}

		if (Math.abs(accumulatedArea) < 1E-7f) {
			return new Vec2(0, 0);
		}

		accumulatedArea *= 3f;
		return new Vec2(centerX / accumulatedArea, centerY / accumulatedArea);
	}

	// -------------- 3D graphics --------------

	// camera settings
	public static double aspectRatio = (double) Main.height / (double) Main.width;
	public static double fov = Math.toRadians(90);
	public static double fovScalingFactor = 1d / Math.tan(fov * 0.5d);
	public static double zNear = 0.1;
	public static double zFar = 1000;
	public static double zNormal = zFar / (zFar - zNear);

	public static double[][] projectionMatrix = new double[][] { { aspectRatio * fovScalingFactor, 0, 0, 0 },
			{ 0, fovScalingFactor, 0, 0 }, { 0, 0, zFar / (zFar - zNear), 1 },
			{ 0, 0, (-zNear * zNear) / (zFar - zNear), 0 }, };

	public static double[][] pointAtMatrix = new double[][] {};
	public static double[][] lookAtMatrix = new double[][] {};

	// xRot and yRot in radians
	// assumes default camera position is the +z axis
	// transforms into camera space, with the point relative to a camera facing down
	// the z-axis.
	public static Vec3 cameraTransform(Vec3 p, Vec3 camera, double xRot, double yRot) {
		Vec3 ans = new Vec3(p);
		ans.x -= camera.x;
		ans.y -= camera.y;
		ans.z -= camera.z;

		ans.rotateY((float) -yRot);
		ans.rotateX((float) -xRot);

		return ans;
	}

	// used for scaling a point up to the window size after projection
	// also flips the y coordinate vertically about the center of the screen, or
	// MainPanel.HEIGHT / 2 to correct for inverted y coordinate while
	// drawing to screen.
	public static Vec3 scaleVector(Vec3 p) {
		Vec3 ans = new Vec3(p);

		ans.x = (float) ((p.x + 1d) * (0.5 * Main.width));
		ans.y = (float) ((p.y + 1d) * (0.5 * Main.height));

		ans.y = Main.height - ans.y;

		return ans;
	}

	// calculates the intersection point between a line and a plane

	public static Vec3 lineIntersectPlane(Vec3 planeVector, Vec3 planeNormal, Vec3 lineStart,
			Vec3 lineEnd, double[] tRef) {
		planeNormal.normalize();
		double planeD = -MathTools.dotProduct(planeNormal, new Vec3(planeVector));
		double ad = dotProduct(planeNormal, new Vec3(lineStart));
		double bd = dotProduct(planeNormal, new Vec3(lineEnd));
		double t = (-planeD - ad) / (bd - ad);
		Vec3 lineStartToEnd = new Vec3(lineStart, lineEnd);
		Vec3 lineIntersect = new Vec3(lineStartToEnd);
		lineIntersect.muli((float) t);
		Vec3 intersect = new Vec3(lineStart);
		intersect.addi(lineIntersect);
		tRef[0] = t;
		return intersect;
	}

	// takes a triangle and clips it against the plane given.
	// can return 0, 1, or 2 triangles.

	// also handles texture coordinates with depth information

	// as of now, doesn't really work completely. This will either completely clip a
	// triangle, or leave it untouched. I think it's due to the bad inputs
	// FIXED yes, it was the bad inputs. When checking the normals to render, always
	// check in real space.

	public static ArrayList<Vec3[]> triangleClipAgainstPlane(Vec3 planeVector, Vec3 planeNormal,
			Vec3[] inTri, Vec2[] inTex, ArrayList<Vec2[]> outTex, double[] inW, ArrayList<double[]> outW) {

		planeNormal.normalize();

		ArrayList<Vec3> insideVectors = new ArrayList<Vec3>();
		ArrayList<Vec3> outsideVectors = new ArrayList<Vec3>();

		ArrayList<Vec2> insideTex = new ArrayList<Vec2>();
		ArrayList<Vec2> outsideTex = new ArrayList<Vec2>();

		ArrayList<Double> insideW = new ArrayList<Double>();
		ArrayList<Double> outsideW = new ArrayList<Double>();

		for (int i = 0; i < 3; i++) {

			Vec3 n = new Vec3(inTri[i]);
			n.normalize();

			// returns signed distance from the given triangle vertice to the plane.
			// if the distance is positive, then the point lies on the "inside" of the plane
			double dist = planeNormal.x * inTri[i].x + planeNormal.y * inTri[i].y + planeNormal.z * inTri[i].z
					- dotProduct(planeNormal, new Vec3(planeVector));

			if (dist >= 0) {
				insideVectors.add(inTri[i]);
				insideTex.add(inTex[i]);
				insideW.add(inW[i]);
			} else {
				outsideVectors.add(inTri[i]);
				outsideTex.add(inTex[i]);
				outsideW.add(inW[i]);
			}

		}

		ArrayList<Vec3[]> ans = new ArrayList<Vec3[]>();

		// no points are on the inside of the plane; the triangle ceases to exist.
		if (insideVectors.size() == 0) {
			return ans;
		}

		// the entire triangle is on the "inside" of the plane. No action needed
		if (insideVectors.size() == 3) {
			ans.add(inTri);
			outTex.add(inTex);
			outW.add(inW);
			return ans;
		}

		// we need to clip the triangle. As only one point lies inside the plane, this
		// triangle can be clipped into a smaller triangle
		if (insideVectors.size() == 1) {

			double[] tRef1 = new double[1];
			double[] tRef2 = new double[1];

			// output 3d space points
			Vec3[] newTri = new Vec3[3];
			newTri[0] = insideVectors.get(0);
			newTri[1] = lineIntersectPlane(planeVector, planeNormal, insideVectors.get(0), outsideVectors.get(0), tRef1);
			newTri[2] = lineIntersectPlane(planeVector, planeNormal, insideVectors.get(0), outsideVectors.get(1), tRef2);

			// output texture space points
			Vec2[] newTex = new Vec2[3];

			Vec2 ab = new Vec2(insideTex.get(0), outsideTex.get(0));
			ab.muli((float) tRef1[0]);
			Vec2 ac = new Vec2(insideTex.get(0), outsideTex.get(1));
			ac.muli((float) tRef2[0]);

			newTex[0] = insideTex.get(0);
			newTex[1] = new Vec2(insideTex.get(0));
			newTex[1].add(ab);
			newTex[2] = new Vec2(insideTex.get(0));
			newTex[2].add(ac);

			// output w values
			double[] newW = new double[3];

			// System.out.println("TREF: " + tRef1[0]);

			newW[0] = insideW.get(0);
			newW[1] = insideW.get(0) + ((outsideW.get(0) - insideW.get(0)) * tRef1[0]);
			newW[2] = insideW.get(0) + ((outsideW.get(1) - insideW.get(0)) * tRef2[0]);

			outTex.add(newTex);
			outW.add(newW);
			ans.add(newTri);
			return ans;
		}

		// this triangle needs clipping
		// as two points lie inside the plane, we need to return 2 new triangles.
		if (insideVectors.size() == 2) {

			double[] tRef1 = new double[1];
			double[] tRef2 = new double[2];

			// output new 3d space points
			Vec3[] newTri1 = new Vec3[3];
			newTri1[0] = insideVectors.get(0);
			newTri1[1] = insideVectors.get(1);
			newTri1[2] = lineIntersectPlane(planeVector, planeNormal, insideVectors.get(0), outsideVectors.get(0), tRef1);

			Vec3[] newTri2 = new Vec3[3];
			newTri2[0] = insideVectors.get(1);
			newTri2[1] = newTri1[2];
			newTri2[2] = lineIntersectPlane(planeVector, planeNormal, insideVectors.get(1), outsideVectors.get(0), tRef2);

			ans.add(newTri1);
			ans.add(newTri2);

			// output new texture points
			Vec2[] newTex1 = new Vec2[3];

			Vec2 ab = new Vec2(insideTex.get(0), outsideTex.get(0));
			ab.mul((float) tRef1[0]);
			Vec2 cb = new Vec2(insideTex.get(1), outsideTex.get(0));
			cb.mul((float) tRef2[0]);

			newTex1[0] = new Vec2(insideTex.get(0));
			newTex1[1] = new Vec2(insideTex.get(1));
			newTex1[2] = new Vec2(insideTex.get(0));
			newTex1[2].add(ab);

			Vec2[] newTex2 = new Vec2[3];
			newTex2[0] = new Vec2(insideTex.get(1));
			newTex2[1] = new Vec2(newTex1[2]);
			newTex2[2] = new Vec2(insideTex.get(1));
			newTex2[2].add(cb);

			outTex.add(newTex1);
			outTex.add(newTex2);

			// output w values
			double[] newW1 = new double[3];
			newW1[0] = insideW.get(0);
			newW1[1] = insideW.get(1);
			newW1[2] = insideW.get(0) + (outsideW.get(0) - insideW.get(0)) * tRef1[0];

			double[] newW2 = new double[3];
			newW2[0] = insideW.get(1);
			newW2[1] = newW1[2];
			newW2[2] = insideW.get(1) + (outsideW.get(0) - insideW.get(1)) * tRef2[0];

			outW.add(newW1);
			outW.add(newW2);

			return ans;
		}

		// something weird happened
		return null;

	}

	public static double[][] matrixPointAt(Vec3 target, Vec3 pos, Vec3 up) {

		// calculate new forward direction
		Vec3 newForward = new Vec3(pos, target);
		newForward.normalize();

		// calculate new up direction
		Vec3 a = new Vec3(newForward);
		a.muli((float) dotProduct(up, newForward));
		Vec3 newUp = new Vec3(a, up);
		newUp.normalize();

		// calculate new right direction
		Vec3 newRight = crossProduct(newUp, newForward);

		double[][] mat = new double[][] { { newRight.x, newRight.y, newRight.z, 0 }, { newUp.x, newUp.y, newUp.z, 0 },
				{ newForward.x, newForward.y, newForward.z, 0 }, { pos.x, pos.y, pos.z, 1 } };

		return mat;

	}

	// takes in a 4x4 rotation or translation matrix and returns its inverse

	public static double[][] invertTransformMatrix(double[][] mat) {

		/*
		 * matrix.m[0][0] = m.m[0][0]; matrix.m[0][1] = m.m[1][0]; matrix.m[0][2] =
		 * m.m[2][0]; matrix.m[0][3] = 0.0f; matrix.m[1][0] = m.m[0][1]; matrix.m[1][1]
		 * = m.m[1][1]; matrix.m[1][2] = m.m[2][1]; matrix.m[1][3] = 0.0f;
		 * matrix.m[2][0] = m.m[0][2]; matrix.m[2][1] = m.m[1][2]; matrix.m[2][2] =
		 * m.m[2][2]; matrix.m[2][3] = 0.0f; matrix.m[3][0] = -(m.m[3][0] *
		 * matrix.m[0][0] + m.m[3][1] * matrix.m[1][0] + m.m[3][2] * matrix.m[2][0]);
		 * matrix.m[3][1] = -(m.m[3][0] * matrix.m[0][1] + m.m[3][1] * matrix.m[1][1] +
		 * m.m[3][2] * matrix.m[2][1]); matrix.m[3][2] = -(m.m[3][0] * matrix.m[0][2] +
		 * m.m[3][1] * matrix.m[1][2] + m.m[3][2] * matrix.m[2][2]); matrix.m[3][3] =
		 * 1.0f;
		 */

		double[][] ans = new double[][] { { mat[0][0], mat[1][0], mat[2][0], 0 },
				{ mat[0][1], mat[1][1], mat[2][1], 0 }, { mat[0][2], mat[2][1], mat[2][2], 0 }, { 0, 0, 0, 0 } };

		ans[3][0] = -(mat[3][0] * ans[0][0] + mat[3][1] * ans[1][0] + mat[3][2] * ans[2][0]);
		ans[3][1] = -(mat[3][0] * ans[0][1] + mat[3][1] * ans[1][1] + mat[3][2] * ans[2][1]);
		ans[3][2] = -(mat[3][0] * ans[0][2] + mat[3][1] * ans[1][2] + mat[3][2] * ans[2][2]);
		ans[3][3] = 1;

		return ans;

	}

	// projects point from 3d onto the 2d screen.
	// assumes the camera is pointing in the +z direction
	// stores the z buffer into the z dimension

	public static Vec3 projectVector(Vec3 p, double[] wOut) {
		return multiplyMatrixVector(projectionMatrix, p, wOut);
	}

	// Multiplies a 3D vector with a 4x4 projection matrix

	// it's implied that the 4th element of the vector is 1

	public static Vec3 multiplyMatrixVector(double[][] mat, Vec3 p, double[] wOut) {
		Vec3 ans = new Vec3(0, 0, 0);

		ans.x = (float) (p.x * mat[0][0] + p.y * mat[1][0] + p.z * mat[2][0] + mat[3][0]);
		ans.y = (float) (p.x * mat[0][1] + p.y * mat[1][1] + p.z * mat[2][1] + mat[3][1]);
		ans.z = (float) (p.x * mat[0][2] + p.y * mat[1][2] + p.z * mat[2][2] + mat[3][2]);
		double w = p.x * mat[0][3] + p.y * mat[1][3] + p.z * mat[2][3] + mat[3][3];

		if (w != 0) {
			ans.x /= w;
			ans.y /= w;
			ans.z /= w;
		}

		wOut[0] = w;

		return ans;
	}

	public static Vec3 rotateVector(Vec3 p, double xRot, double yRot, double zRot) {
		Vec3 p1 = new Vec3(p.x, p.y, p.z);

		rotateX(p1, xRot);
		rotateY(p1, yRot);
		rotateZ(p1, zRot);

		return p1;
	}

	public static void rotateX(Vec3 p, double xRot) {
		double x = p.x;
		double y = p.y;
		double z = p.z;
		p.x = (float) x;
		p.y = (float) ((y * Math.cos(xRot)) + (z * -Math.sin(xRot)));
		p.z = (float) ((y * Math.sin(xRot)) + (z * Math.cos(xRot)));
	}

	public static void rotateY(Vec3 p, double yRot) {
		double x = p.x;
		double y = p.y;
		double z = p.z;
		p.x = (float) ((x * Math.cos(yRot)) + (z * Math.sin(yRot)));
		p.y = (float) y;
		p.z = (float) ((x * -Math.sin(yRot)) + (z * Math.cos(yRot)));
	}

	public static void rotateZ(Vec3 p, double zRot) {
		double x = p.x;
		double y = p.y;
		double z = p.z;
		p.x = (float) ((x * Math.cos(zRot)) + (y * -Math.sin(zRot)));
		p.y = (float) ((x * Math.sin(zRot)) + (y * Math.cos(zRot)));
		p.z = (float) z;
	}

}