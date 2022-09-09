package util;

import java.util.ArrayList;

public class MathUtils {
	
	//MathTools v2, made with LWGJL in mind
	
	// -- GENERAL --
	
	/**
	 * Takes in a value, and returns it clamped to two other inputs
	 * @param low
	 * @param high
	 * @param val
	 * @return
	 */
	public static float clamp(float low, float high, float val) {
		return val < low ? low : (val > high ? high : val);
	}
	
	/**
	 * Linearly interpolates between two points
	 * @param x1
	 * @param t1
	 * @param x2
	 * @param t2
	 * @param t3
	 * @return
	 */
	public static float interpolate(float x1, float t1, float x2, float t2, float t3) {
		float v = (x2 - x1) / (t2 - t1);
		return x1 + (t3 - t1) * v;
	}
	
	/**
	 * Calculates the distance between two 2D points. 
	 * 
	 * You can also do this by defining a vector between the two points, and querying the length. 
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return
	 */
	public static float dist(float x1, float y1, float x2, float y2) {
		return (float) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
	}
	
	// -- LINEAR ALGEBRA --
	
	/**
	 * Takes in two line segments, and returns the point of intersection, if it exists. Null otherwise. 
	 * @param a0
	 * @param a1
	 * @param b0
	 * @param b1
	 * @return
	 */
	public static Vec2 line_lineIntersect(Vec2 a0, Vec2 a1, Vec2 b0, Vec2 b1) {
		float x1 = a0.x;
		float x2 = a1.x;
		float x3 = b0.x;
		float x4 = b1.x;
		
		float y1 = a0.y;
		float y2 = a1.y;
		float y3 = b0.y;
		float y4 = b1.y;
		
		// calculate the distance to intersection point
		float uA = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / ((y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1));
		float uB = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / ((y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1));

		// if uA and uB are between 0-1, lines are colliding
		if (uA >= 0 && uA <= 1 && uB >= 0 && uB <= 1) {
			// calculate the intersection point
			float intersectionX = x1 + (uA * (x2 - x1));
			float intersectionY = y1 + (uA * (y2 - y1));

			return new Vec2(intersectionX, intersectionY);
		}
		return null;
	}
	
	/**
	 * Take in a ray and a plane, and returns the intersection if it exists. 
	 * @param ray_origin
	 * @param ray_dir
	 * @param plane_origin
	 * @param plane_normal
	 * @return
	 */
	public static Vec3 ray_planeIntersect(Vec3 ray_origin, Vec3 ray_dir, Vec3 plane_origin, Vec3 plane_normal) {
		float ray_dirStepRatio = plane_normal.dot(ray_dir);	//for each step in ray_dir, you go ray_dirStepRatio steps in plane_normal
		if(ray_dirStepRatio == 0) {
			//ray is parallel to plane, no intersection
			return null;
		}
		float t = plane_origin.sub(ray_origin).dot(plane_normal) / ray_dirStepRatio;
		return ray_origin.add(ray_dir.mul(t));
	}
	
	/**
	 * Take in a ray and a triangle, and returns the intersection if it exists. 
	 * @param ray_origin
	 * @param ray_dir
	 * @param t0
	 * @param t1
	 * @param t2
	 * @return
	 */
	public static Vec3 ray_triangleIntersect(Vec3 ray_origin, Vec3 ray_dir, Vec3 t0, Vec3 t1, Vec3 t2) {
		Vec3 d0 = new Vec3(t0, t1).normalize();
		Vec3 d1 = new Vec3(t1, t2).normalize();
		Vec3 d2 = new Vec3(t2, t0).normalize();
		
		Vec3 plane_origin = new Vec3(t0);
		Vec3 plane_normal = d0.cross(d1).normalize();
		
		Vec3 plane_intersect = ray_planeIntersect(ray_origin, ray_dir, plane_origin, plane_normal);
		if(plane_intersect == null) {
			//if it doesn't intersect the plane, then theres no way it intersects the triangle. 
			return null;
		}
		
		//now, we just have to make sure that the intersection point is inside the triangle. 
		Vec3 n0 = d0.cross(plane_normal);
		Vec3 n1 = d1.cross(plane_normal);
		Vec3 n2 = d2.cross(plane_normal);
		
		if(n0.dot(t0.sub(plane_intersect)) < 0 || n1.dot(t1.sub(plane_intersect)) < 0 || n2.dot(t2.sub(plane_intersect)) < 0) {
			//intersection point is outside of the triangle. 
			return null;
		}
		
		return plane_intersect;
	}

	/**
	 * Takes a line and two points, and returns true if the two points are on the same side of the line. 
	 * @param lineP
	 * @param lineVec
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean pointsOnSameSideOfLine(Vec2 lineP, Vec2 lineVec, Vec2 a, Vec2 b) {
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
		float d1 = (float) perpendicular.dot(v1);
		float d2 = (float) perpendicular.dot(v2);

		if (d1 * d2 >= 0) {
			return true;
		}

		return false;

	}

	/**
	 * Assuming that the points are arranged in a convex hull, it calculates the centroid of the points. 
	 * @param points
	 * @return
	 */
	public static Vec2 computeCentroid(ArrayList<Vec2> points) {
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
	
	// -- ML --
	
	/**
	 * Regular sigmoid function
	 * @param x
	 * @return
	 */
	public static double sigmoid(double x) {
		return (1d / (1d + Math.pow(Math.E, (-1d * x))));
	}
	
	/**
	 * Derivative of regular sigmoid function, with center at 0
	 * @param x
	 * @return
	 */
	public static double sigmoidDerivative(double x) {
		return sigmoid(x) * (1d - sigmoid(x));
	}
	
	/**
	 * Regular ReLU (Rectified Linear Unit) function
	 * @param x
	 * @return
	 */
	public static double relu(double x) {
		return Math.max(0, x);
	}

	/**
	 * Regular Logit or inverse sigmoid function
	 * @param x
	 * @return
	 */
	public static double logit(double x) {
		return Math.log(x / (1d - x));
	}
	
	/**
	 * Derivative of regular ReLU function
	 * @param x
	 * @return
	 */
	public static double reluDerivative(double x) {
		return x <= 0 ? 0 : 1;
	}
	
	// -- STATS --
	
	/**
	 * Approximates a normal distribution, in this case, n = 4. 
	 * @param x
	 * @return
	 */
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

}
