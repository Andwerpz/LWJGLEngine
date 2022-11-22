package util;

import java.util.ArrayList;

public class MathUtils {

	// MathTools v2, made with LWGJL in mind

	// -- GENERAL --

	/**
	 * Takes in a value, and returns it clamped to two other inputs
	 * 
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
	 * 
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
	 * Linearly interpolates between two vec3 points
	 * 
	 * @param 
	 */
	public static Vec3 interpolate(Vec3 v1, float t1, Vec3 v2, float t2, float t3) {
		Vec3 v = (v2.sub(v1)).divi(t2 - t1);
		return v1.add(v.mul(t3 - t1));
	}

	// -- LINEAR ALGEBRA --

	/**
	 * Calculates the normal of a triangle given the three points.
	 * 
	 * @param t0
	 * @param t1
	 * @param t2
	 * @return
	 */
	public static Vec3 computeTriangleNormal(Vec3 t0, Vec3 t1, Vec3 t2) {
		Vec3 d0 = new Vec3(t0, t1);
		Vec3 d1 = new Vec3(t0, t2);
		return d0.cross(d1).normalize();
	}

	/**
	 * Takes in two line segments, and returns the point of intersection, if it exists. Null otherwise.
	 * 
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
	 * Take in two lines in 3D, and returns the shortest distance between them.
	 * @param a_point
	 * @param a_dir
	 * @param b_point
	 * @param b_dir
	 * @return
	 */
	public static float line_lineDistance(Vec3 a_point, Vec3 a_dir, Vec3 b_point, Vec3 b_dir) {
		float[] nm = line_lineDistanceNM(a_point, a_point.add(a_dir), b_point, b_point.add(b_dir));
		Vec3 v = new Vec3(a_point.add(a_dir.mul(nm[0])), b_point.add(b_dir.mul(nm[1])));
		return v.length();
	}

	/**
	 * Takes in a ray and a line segment, and returns the minimum distance between them. 
	 * @param ray_origin
	 * @param ray_dir
	 * @param b0
	 * @param b1
	 * @return
	 */
	public static float ray_lineSegmentDistance(Vec3 ray_origin, Vec3 ray_dir, Vec3 b0, Vec3 b1) {
		float[] nm = line_lineDistanceNM(ray_origin, ray_origin.add(ray_dir), b0, b1);
		nm[0] = Math.max(0, nm[0]);
		nm[1] = clamp(0, 1, nm[1]);
		Vec3 b_dir = new Vec3(b0, b1);
		Vec3 v = new Vec3(ray_origin.add(ray_dir.mul(nm[0])), b0.add(b_dir.mul(nm[1])));
		return v.length();
	}

	/**
	 * Takes in a ray and a line segment, and returns the minimum distance between them. 
	 * @param ray_origin
	 * @param ray_dir
	 * @param b0
	 * @param b1
	 * @return
	 */
	public static float ray_lineSegmentDistance(Vec3 ray_origin, Vec3 ray_dir, Vec3 b0, Vec3 b1, Vec3 out_ray_point, Vec3 out_line_point) {
		float[] nm = line_lineDistanceNM(ray_origin, ray_origin.add(ray_dir), b0, b1);
		nm[0] = Math.max(0, nm[0]);
		nm[1] = clamp(0, 1, nm[1]);
		Vec3 b_dir = new Vec3(b0, b1);
		out_ray_point.set(ray_origin.add(ray_dir.mul(nm[0])));
		out_line_point.set(b0.add(b_dir.mul(nm[1])));
		Vec3 v = new Vec3(out_ray_point, out_line_point);
		return v.length();
	}

	/**
	 * Takes in two line segments in 3D, and returns the values for n and m.
	 * Where v is the vector between the two closest points of a and b,
	 * v.x = (a0.x + n * d0.x) - (b0.x + m * d1.x)
	 * v.y = (a0.y + n * d0.y) - (b0.y + m * d1.y)
	 * v.z = (a0.z + n * d0.z) - (b0.z + m * d1.z)
	 * 
	 * d0 is the direction vector of a, d0 is for b. 
	 * v.dot(d0) = 0
	 * v.dot(d1) = 0
	 * 
	 * thus we can get the two equations: 
	 * ((a0.x + n * d0.x) - (b0.x + m * d1.x)) * d0.x + 
	 * ((a0.y + n * d0.y) - (b0.y + m * d1.y)) * d0.y + 
	 * ((a0.z + n * d0.z) - (b0.z + m * d1.z)) * d0.z = 0
	 * 
	 * ((a0.x + n * d0.x) - (b0.x + m * d1.x)) * d1.x + 
	 * ((a0.y + n * d0.y) - (b0.y + m * d1.y)) * d1.y + 
	 * ((a0.z + n * d0.z) - (b0.z + m * d1.z)) * d1.z = 0
	 * 
	 * finally, solve for n and m. 
	 * 
	 * closest point on line a : a0 + n * d0. 
	 * for line b : b0 + m * d0. 
	 * 
	 * @param a0
	 * @param a1
	 * @param b0
	 * @param b1
	 * @return
	 */
	private static float[] line_lineDistanceNM(Vec3 a0, Vec3 a1, Vec3 b0, Vec3 b1) {
		Vec3 d0 = new Vec3(a0, a1);
		Vec3 d1 = new Vec3(b0, b1);

		float n = 0;
		float m = 0;

		float nExp0 = 0;
		float mExp0 = 0;
		float cnst0 = 0;

		float nExp1 = 0;
		float mExp1 = 0;
		float cnst1 = 0;

		//v is the vector between the two closest points of a and b. 
		//v.x = (a0.x + n * d0.x) - (b0.x + m * d1.x);
		//v.y = (a0.y + n * d0.y) - (b0.y + m * d1.y);
		//v.z = (a0.z + n * d0.z) - (b0.z + m * d1.z);

		//v.dot(d0) = 0
		//v.dot(d1) = 0

		//((a0.x + n * d0.x) - (b0.x + m * d1.x)) * d0.x + 
		//((a0.y + n * d0.y) - (b0.y + m * d1.y)) * d0.y + 
		//((a0.z + n * d0.z) - (b0.z + m * d1.z)) * d0.z = 0

		nExp0 += d0.x * d0.x + d0.y * d0.y + d0.z * d0.z;
		mExp0 -= d1.x * d0.x + d1.y * d0.y + d1.z * d0.z;
		cnst0 -= a0.x * d0.x + a0.y * d0.y + a0.z * d0.z;
		cnst0 += b0.x * d0.x + b0.y * d0.y + b0.z * d0.z;

		//((a0.x + n * d0.x) - (b0.x + m * d1.x)) * d1.x + 
		//((a0.y + n * d0.y) - (b0.y + m * d1.y)) * d1.y + 
		//((a0.z + n * d0.z) - (b0.z + m * d1.z)) * d1.z = 0

		nExp1 += d0.x * d1.x + d0.y * d1.y + d0.z * d1.z;
		mExp1 -= d1.x * d1.x + d1.y * d1.y + d1.z * d1.z;
		cnst1 -= a0.x * d1.x + a0.y * d1.y + a0.z * d1.z;
		cnst1 += b0.x * d1.x + b0.y * d1.y + b0.z * d1.z;

		//nExp0 * n + mExp0 * m = cnst0
		//nExp1 * n + mExp1 * m = cnst1

		//cancel out nExp0
		float ratio = nExp0 / nExp1;
		nExp0 -= nExp1 * ratio; //nExp1 * (nExp0 / nExp1) = nExp0
		mExp0 -= mExp1 * ratio;
		cnst0 -= cnst1 * ratio;

		//now, nExp0 should = 0. Solve for m. 
		m = cnst0 / mExp0;

		//now solve for n. n = (cnst1 - mExp1 * m) / nExp1;
		n = (cnst1 - mExp1 * m) / nExp1;

		return new float[] { n, m };
	}

	/**
	 * Take in a ray and a plane, and returns the intersection if it exists.
	 * 
	 * @param ray_origin
	 * @param ray_dir
	 * @param plane_origin
	 * @param plane_normal
	 * @return
	 */
	public static Vec3 ray_planeIntersect(Vec3 ray_origin, Vec3 ray_dir, Vec3 plane_origin, Vec3 plane_normal) {
		float ray_dirStepRatio = plane_normal.dot(ray_dir); // for each step in ray_dir, you go ray_dirStepRatio steps
		// in plane_normal
		if (ray_dirStepRatio == 0) {
			// ray is parallel to plane, no intersection
			return null;
		}
		float t = plane_origin.sub(ray_origin).dot(plane_normal) / ray_dirStepRatio;
		if (t < 0) {
			// the plane intersection is behind the ray origin
			return null;
		}
		return ray_origin.add(ray_dir.mul(t));
	}

	/**
	 * Take in a line and a plane, and returns the intersection if it exists.
	 * 
	 * @param ray_origin
	 * @param ray_dir
	 * @param plane_origin
	 * @param plane_normal
	 * @return
	 */
	public static Vec3 line_planeIntersect(Vec3 line_origin, Vec3 line_dir, Vec3 plane_origin, Vec3 plane_normal) {
		float line_dirStepRatio = plane_normal.dot(line_dir); // for each step in line_dir, you go line_dirStepRatio
		// steps in plane_normal
		if (line_dirStepRatio == 0) {
			// line is parallel to plane, no intersection
			return null;
		}
		// Conceptually, a line is equivalent to a double-sided ray. This means that
		// it's fine if t is negative.
		float t = plane_origin.sub(line_origin).dot(plane_normal) / line_dirStepRatio;
		return line_origin.add(line_dir.mul(t));
	}

	/**
	 * Take in a point and a plane, and returns the point projected onto the plane
	 * 
	 * @param point
	 * @param plane_origin
	 * @param plane_normal
	 * @return
	 */
	public static Vec3 point_planeProject(Vec3 point, Vec3 plane_origin, Vec3 plane_normal) {
		return point.sub(new Vec3(plane_origin, point).projectOnto(plane_normal));
	}

	/**
	 * Take in a point and a line, and returns the point projected onto the line
	 * 
	 * @param point
	 * @param line_origin
	 * @param line_dir
	 * @return
	 */
	public static Vec3 point_lineProject(Vec3 point, Vec3 line_origin, Vec3 line_dir) {
		Vec3 lineToPoint = new Vec3(line_origin, point);
		return line_origin.add(lineToPoint.projectOnto(line_dir));
	}

	/**
	 * Take in a point and a line segment, and if the projection of the point onto the line is within the segment, 
	 * returns the point projected onto the line, else returns null.
	 * 
	 * @param point
	 * @param line_a
	 * @param line_b
	 * @return
	 */
	public static Vec3 point_lineSegmentProject(Vec3 point, Vec3 line_a, Vec3 line_b) {
		Vec3 line_ab = new Vec3(line_a, line_b);
		Vec3 lineToPoint = new Vec3(line_a, point);
		float mul = lineToPoint.dot(line_ab) / line_ab.dot(line_ab);
		if (mul < 0 || mul > 1) {
			return null;
		}
		return line_a.add(line_ab.mul(mul));
	}

	/**
	 * Take in a point and a line segment, and if the projection of the point onto the line is within the segment, 
	 * returns the point projected onto the line, else clamps the point to the line segment, and returns the clamped point.
	 * 
	 * @param point
	 * @param line_a
	 * @param line_b
	 * @return
	 */
	public static Vec3 point_lineSegmentProjectClamped(Vec3 point, Vec3 line_a, Vec3 line_b) {
		Vec3 line_ab = new Vec3(line_a, line_b);
		Vec3 lineToPoint = new Vec3(line_a, point);
		float mul = lineToPoint.dot(line_ab) / line_ab.dot(line_ab);
		mul = clamp(0f, 1f, mul);
		return line_a.add(line_ab.mul(mul));
	}

	/**
	 * Take in a ray and a triangle, and returns the intersection if it exists.
	 * 
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
		if (plane_intersect == null) {
			// if it doesn't intersect the plane, then theres no way it intersects the
			// triangle.
			return null;
		}

		// now, we just have to make sure that the intersection point is inside the
		// triangle.
		Vec3 n0 = d0.cross(plane_normal);
		Vec3 n1 = d1.cross(plane_normal);
		Vec3 n2 = d2.cross(plane_normal);

		if (n0.dot(t0.sub(plane_intersect)) < 0 || n1.dot(t1.sub(plane_intersect)) < 0 || n2.dot(t2.sub(plane_intersect)) < 0) {
			// intersection point is outside of the triangle.
			return null;
		}

		return plane_intersect;
	}

	/**
	 * Take in a line and a triangle, and returns the intersection if it exists.
	 * 
	 * @param line_origin
	 * @param line_dir
	 * @param t0
	 * @param t1
	 * @param t2
	 * @return
	 */
	public static Vec3 line_triangleIntersect(Vec3 line_origin, Vec3 line_dir, Vec3 t0, Vec3 t1, Vec3 t2) {
		Vec3 d0 = new Vec3(t0, t1).normalize();
		Vec3 d1 = new Vec3(t1, t2).normalize();
		Vec3 d2 = new Vec3(t2, t0).normalize();

		Vec3 plane_origin = new Vec3(t0);
		Vec3 plane_normal = d0.cross(d1).normalize();

		Vec3 plane_intersect = line_planeIntersect(line_origin, line_dir, plane_origin, plane_normal);
		if (plane_intersect == null) {
			// if it doesn't intersect the plane, then theres no way it intersects the
			// triangle.
			return null;
		}

		// now, we just have to make sure that the intersection point is inside the
		// triangle.
		Vec3 n0 = d0.cross(plane_normal);
		Vec3 n1 = d1.cross(plane_normal);
		Vec3 n2 = d2.cross(plane_normal);

		if (n0.dot(t0.sub(plane_intersect)) < 0 || n1.dot(t1.sub(plane_intersect)) < 0 || n2.dot(t2.sub(plane_intersect)) < 0) {
			// intersection point is outside of the triangle.
			return null;
		}

		return plane_intersect;
	}

	/**
	 * Take in a sphere and a triangle, and returns the point on the triangle, p, where dist(p, sphere_origin) is minimal.
	 * 
	 * @param sphere_origin
	 * @param sphere_radius
	 * @param t0
	 * @param t1
	 * @param t2
	 * @return
	 */
	public static Vec3 sphere_triangleIntersect(Vec3 sphere_origin, float sphere_radius, Vec3 t0, Vec3 t1, Vec3 t2) {
		Vec3 d0 = new Vec3(t0, t1).normalize();
		Vec3 d1 = new Vec3(t1, t2).normalize();
		Vec3 d2 = new Vec3(t2, t0).normalize();

		Vec3 plane_origin = new Vec3(t0);
		Vec3 plane_normal = d0.cross(d1).normalize();

		// first check if the sphere intersects the plane the triangle defines
		Vec3 plane_intersect = line_planeIntersect(sphere_origin, plane_normal, plane_origin, plane_normal);
		if (new Vec3(plane_intersect, sphere_origin).length() > sphere_radius) {
			// sphere doesn't intersect the plane
			return null;
		}

		// check if sphere_origin projects to a point in the triangle.
		// If true, it means that the intersection point isn't a corner or edge of the
		// triangle.
		Vec3 triangle_intersect = line_triangleIntersect(sphere_origin, plane_normal, t0, t1, t2);
		if (triangle_intersect != null) {
			if (new Vec3(triangle_intersect, sphere_origin).length() < sphere_radius) {
				return triangle_intersect;
			}
			return null;
		}

		// else, check if sphere_origin projects onto a line segment of the triangle.
		// if we project the point clamped onto the line segment, then we don't have to
		// check the vertices.
		Vec3 minS_p = null;
		float minDist = -1f;
		Vec3 s0_p = point_lineSegmentProjectClamped(sphere_origin, t0, t1);
		Vec3 s1_p = point_lineSegmentProjectClamped(sphere_origin, t1, t2);
		Vec3 s2_p = point_lineSegmentProjectClamped(sphere_origin, t2, t0);
		if (s0_p != null) {
			float dist = new Vec3(s0_p, sphere_origin).length();
			if (minS_p == null || dist < minDist) {
				minS_p = s0_p;
				minDist = dist;
			}
		}
		if (s1_p != null) {
			float dist = new Vec3(s1_p, sphere_origin).length();
			if (minS_p == null || dist < minDist) {
				minS_p = s1_p;
				minDist = dist;
			}
		}
		if (s2_p != null) {
			float dist = new Vec3(s2_p, sphere_origin).length();
			if (minS_p == null || dist < minDist) {
				minS_p = s2_p;
				minDist = dist;
			}
		}
		if (minS_p != null && minDist < sphere_radius) {
			return minS_p;
		}

		// else, the sphere doesn't intersect the triangle
		return null;
	}

	/**
	 * Takes a capsule and a triangle, and determines the point on the triangle, p, where dist(p, c) is minimal. 
	 * Point c is a point in the capsule bound to the line segment defined by capsule_top and capsule_bottom.
	 * 
	 * @param capsule_top
	 * @param capsule_bottom
	 * @param capsule_radius
	 * @param t0
	 * @param t1
	 * @param t2
	 * @return
	 */
	public static Vec3 capsule_triangleIntersect(Vec3 capsule_bottom, Vec3 capsule_top, float capsule_radius, Vec3 t0, Vec3 t1, Vec3 t2) {
		Vec3 capsule_tangent = new Vec3(capsule_bottom, capsule_top).normalize();
		Vec3 capsule_a = capsule_bottom.add(capsule_tangent.mul(capsule_radius));
		Vec3 capsule_b = capsule_top.sub(capsule_tangent.mul(capsule_radius));

		Vec3 d0 = new Vec3(t0, t1).normalize();
		Vec3 d1 = new Vec3(t1, t2).normalize();
		Vec3 d2 = new Vec3(t2, t0).normalize();

		Vec3 plane_normal = d0.cross(d1).normalize();

		Vec3 n0 = d0.cross(plane_normal);
		Vec3 n1 = d1.cross(plane_normal);
		Vec3 n2 = d2.cross(plane_normal);

		Vec3 referencePoint = new Vec3(0);
		Vec3 plane_intersect = line_planeIntersect(capsule_bottom, capsule_tangent, t0, plane_normal);
		if (plane_intersect == null) {
			// capsule_tangent is parallel to the plane, plane_intersect doesn't exist.
			referencePoint = new Vec3(t0);
		}
		else if (n0.dot(t0.sub(plane_intersect)) < 0 || n1.dot(t1.sub(plane_intersect)) < 0 || n2.dot(t2.sub(plane_intersect)) < 0) {
			// plane_intersect point is outside of the triangle.
			// find closest point to plane_intersect that is on the triangle.
			Vec3 minS_p = null;
			float minDist = -1f;
			Vec3 s0_p = point_lineSegmentProjectClamped(plane_intersect, t0, t1);
			Vec3 s1_p = point_lineSegmentProjectClamped(plane_intersect, t1, t2);
			Vec3 s2_p = point_lineSegmentProjectClamped(plane_intersect, t2, t0);

			float dist = new Vec3(s0_p, plane_intersect).length();
			if (minS_p == null || dist < minDist) {
				minS_p = s0_p;
				minDist = dist;
			}

			dist = new Vec3(s1_p, plane_intersect).length();
			if (minS_p == null || dist < minDist) {
				minS_p = s1_p;
				minDist = dist;
			}

			dist = new Vec3(s2_p, plane_intersect).length();
			if (minS_p == null || dist < minDist) {
				minS_p = s2_p;
				minDist = dist;
			}

			referencePoint = minS_p;
		}
		else {
			// plane intersection is inside the triangle
			referencePoint = plane_intersect;
		}

		Vec3 capsule_c = point_lineSegmentProjectClamped(referencePoint, capsule_a, capsule_b);
		return sphere_triangleIntersect(capsule_c, capsule_radius, t0, t1, t2);
	}

	/**
	 * Takes a ray and a capsule, and returns the point on the ray that is the deepest inside the capsule if they intersect. 
	 * @param ray_origin
	 * @param ray_dir
	 * @param capsule_bottom
	 * @param capsule_top
	 * @param capsule_radius
	 * @return
	 */
	public static Vec3 ray_capsuleIntersect(Vec3 ray_origin, Vec3 ray_dir, Vec3 capsule_bottom, Vec3 capsule_top, float capsule_radius) {
		Vec3 capsule_tangent = new Vec3(capsule_bottom, capsule_top).normalize();
		Vec3 capsule_a = capsule_bottom.add(capsule_tangent.mul(capsule_radius));
		Vec3 capsule_b = capsule_top.sub(capsule_tangent.mul(capsule_radius));

		Vec3 ray_close = new Vec3(0);
		Vec3 capsule_close = new Vec3(0);

		ray_lineSegmentDistance(ray_origin, ray_dir, capsule_a, capsule_b, ray_close, capsule_close);

		float dist = new Vec3(ray_close, capsule_close).length();
		if (dist < capsule_radius) {
			return ray_close;
		}
		return null;
	}

	/**
	 * Takes a line and two points, and returns true if the two points are on the same side of the line.
	 * 
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
		float d1 = perpendicular.dot(v1);
		float d2 = perpendicular.dot(v2);

		if (d1 * d2 >= 0) {
			return true;
		}

		return false;

	}

	/**
	 * Assuming that the points are arranged in a convex hull, it calculates the centroid of the points.
	 * 
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
	 * 
	 * @param x
	 * @return
	 */
	public static double sigmoid(double x) {
		return (1d / (1d + Math.pow(Math.E, (-1d * x))));
	}

	/**
	 * Derivative of regular sigmoid function, with center at 0
	 * 
	 * @param x
	 * @return
	 */
	public static double sigmoidDerivative(double x) {
		return sigmoid(x) * (1d - sigmoid(x));
	}

	/**
	 * Regular ReLU (Rectified Linear Unit) function
	 * 
	 * @param x
	 * @return
	 */
	public static double relu(double x) {
		return Math.max(0, x);
	}

	/**
	 * Regular Logit or inverse sigmoid function
	 * 
	 * @param x
	 * @return
	 */
	public static double logit(double x) {
		return Math.log(x / (1d - x));
	}

	/**
	 * Derivative of regular ReLU function
	 * 
	 * @param x
	 * @return
	 */
	public static double reluDerivative(double x) {
		return x <= 0 ? 0 : 1;
	}

	// -- STATS --

	/**
	 * Approximates a normal distribution, in this case, n = 4.
	 * 
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
