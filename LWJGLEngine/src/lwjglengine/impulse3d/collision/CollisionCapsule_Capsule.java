package lwjglengine.impulse3d.collision;

import java.util.ArrayList;

import lwjglengine.impulse3d.Body;
import lwjglengine.impulse3d.shape.Capsule;
import myutils.math.MathUtils;
import myutils.math.Vec3;
import myutils.misc.Pair;

public class CollisionCapsule_Capsule implements CollisionCallback {
	public static final CollisionCapsule_Capsule instance = new CollisionCapsule_Capsule();
	//capsule - capsule collision is literally collision between two line segments, but they have radius. 
	//check both endcaps against other line segment, then both line segments against each other. 

	@Override
	public void handleCollision(Manifold m) {
		Body b1 = m.a;
		Body b2 = m.b;

		Capsule s1 = (Capsule) b1.shape;
		Capsule s2 = (Capsule) b2.shape;

		Vec3 u1 = new Vec3(0, 0, s1.length / 2);
		Vec3 u2 = new Vec3(0, 0, -s1.length / 2);
		u1 = MathUtils.quaternionRotateVec3(b1.orient, u1);
		u2 = MathUtils.quaternionRotateVec3(b1.orient, u2);
		u1.addi(b1.pos);
		u2.addi(b1.pos);

		Vec3 v1 = new Vec3(0, 0, s2.length / 2);
		Vec3 v2 = new Vec3(0, 0, -s2.length / 2);
		v1 = MathUtils.quaternionRotateVec3(b2.orient, v1);
		v2 = MathUtils.quaternionRotateVec3(b2.orient, v2);
		v1.addi(b2.pos);
		v2.addi(b2.pos);

		m.contacts = new ArrayList<>();

		//special case if both capsules are just spheres
		if (s1.length == 0 && s2.length == 0) {
			if (MathUtils.dist(u1, v1) < s1.radius + s2.radius) {
				Vec3 norm = new Vec3(v1, u1).normalize();
				float pen = (s1.radius + s2.radius) - MathUtils.dist(u1, v1);
				Vec3 pt = (u1.add(v1)).div(2.0f);
				m.contacts.add(new Contact(pt, norm, pen));
			}
		}

		//see what the ends are doing
		if (s2.length != 0) {
			Vec3 proj = MathUtils.point_lineSegmentProjectClamped(u1, v1, v2);
			if (MathUtils.dist(u1, proj) < s1.radius + s2.radius) {
				Vec3 norm = new Vec3(proj, u1).normalize();
				float pen = (s1.radius + s2.radius) - MathUtils.dist(u1, proj);
				Vec3 pt = (proj.add(u1)).div(2.0f);
				m.contacts.add(new Contact(pt, norm, pen));
			}
		}
		if (s2.length != 0) {
			Vec3 proj = MathUtils.point_lineSegmentProjectClamped(u2, v1, v2);
			if (MathUtils.dist(u2, proj) < s1.radius + s2.radius) {
				Vec3 norm = new Vec3(proj, u2).normalize();
				float pen = (s1.radius + s2.radius) - MathUtils.dist(u2, proj);
				Vec3 pt = (proj.add(u2)).div(2.0f);
				m.contacts.add(new Contact(pt, norm, pen));
			}
		}
		if (s1.length != 0) {
			Vec3 proj = MathUtils.point_lineSegmentProjectClamped(v1, u1, u2);
			if (MathUtils.dist(v1, proj) < s1.radius + s2.radius) {
				Vec3 norm = new Vec3(v1, proj).normalize();
				float pen = (s1.radius + s2.radius) - MathUtils.dist(v1, proj);
				Vec3 pt = (proj.add(v1)).div(2.0f);
				m.contacts.add(new Contact(pt, norm, pen));
			}
		}
		if (s1.length != 0) {
			Vec3 proj = MathUtils.point_lineSegmentProjectClamped(v2, u1, u2);
			if (MathUtils.dist(v2, proj) < s1.radius + s2.radius) {
				Vec3 norm = new Vec3(v2, proj).normalize();
				float pen = (s1.radius + s2.radius) - MathUtils.dist(v2, proj);
				Vec3 pt = (proj.add(v2)).div(2.0f);
				m.contacts.add(new Contact(pt, norm, pen));
			}
		}

		//are the middles colliding?
		//ignore the middles if they are parallel (or if the length is 0). 
		if (MathUtils.cross(new Vec3(u1, u2), new Vec3(v1, v2)).lengthSq() != 0) {
			Pair<Vec3, Vec3> pts = MathUtils.lineSegment_lineSegmentClosestPoints(u1, u2, v1, v2);
			Vec3 p1 = pts.first;
			Vec3 p2 = pts.second;

			float dst_to_end1 = Math.min(MathUtils.dist(p1, u1), MathUtils.dist(p1, u2));
			float dst_to_end2 = Math.min(MathUtils.dist(p2, v1), MathUtils.dist(p2, v2));

			if (MathUtils.dist(p1, p2) < s1.radius + s2.radius && Math.min(dst_to_end1, dst_to_end2) > 0.001) {
				Vec3 norm = new Vec3(p2, p1).normalize();
				float pen = (s1.radius + s2.radius) - MathUtils.dist(p1, p2);
				Vec3 pt = (p1.add(p2)).div(2.0f);
				m.contacts.add(new Contact(pt, norm, pen));
			}
		}

		if (m.contacts.size() != 0) {
			m.didCollide = true;

			//find contact with most penetration, and label that as the normal
			float mx_pen = -1;
			int mx_pen_ind = -1;
			for (int i = 0; i < m.contacts.size(); i++) {
				if (m.contacts.get(i).penetration > mx_pen) {
					mx_pen = m.contacts.get(i).penetration;
					mx_pen_ind = i;
				}
			}
			m.penetration = mx_pen;
			m.separating_axis = m.contacts.get(mx_pen_ind).norm;
		}
	}

}
