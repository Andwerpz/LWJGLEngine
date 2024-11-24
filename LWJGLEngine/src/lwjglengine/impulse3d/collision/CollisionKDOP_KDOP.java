package lwjglengine.impulse3d.collision;

import java.util.ArrayList;

import lwjglengine.impulse3d.Body;
import lwjglengine.impulse3d.shape.AABB;
import lwjglengine.impulse3d.shape.KDOP;
import myutils.math.MathUtils;
import myutils.math.Vec3;
import myutils.misc.Pair;

public class CollisionKDOP_KDOP implements CollisionCallback {
	public static final CollisionKDOP_KDOP instance = new CollisionKDOP_KDOP();

	private Body ba, bb;
	private KDOP sa, sb;

	private Vec3[] axes_a, axes_b;
	private Vec3[] pts_a, pts_b;

	@Override
	public void handleCollision(Manifold m) {
		this.ba = m.a;
		this.bb = m.b;

		this.sa = (KDOP) ba.shape;
		this.sb = (KDOP) bb.shape;

		//generate primary axes rotated to world space
		this.axes_a = new Vec3[this.sa.getAxes().length];
		this.axes_b = new Vec3[this.sb.getAxes().length];
		for (int i = 0; i < this.sa.getAxes().length; i++) {
			this.axes_a[i] = MathUtils.quaternionRotateVec3(ba.orient, this.sa.getAxes()[i]);
		}
		for (int i = 0; i < this.sb.getAxes().length; i++) {
			this.axes_b[i] = MathUtils.quaternionRotateVec3(bb.orient, this.sb.getAxes()[i]);
		}

		//generate vertices in world space
		this.pts_a = new Vec3[sa.getVertices().length];
		this.pts_b = new Vec3[sb.getVertices().length];
		for (int i = 0; i < this.sa.getVertices().length; i++) {
			this.pts_a[i] = MathUtils.quaternionRotateVec3(ba.orient, this.sa.getVertices()[i]);
			this.pts_a[i].addi(ba.pos);
		}
		for (int i = 0; i < this.sb.getVertices().length; i++) {
			this.pts_b[i] = MathUtils.quaternionRotateVec3(bb.orient, this.sb.getVertices()[i]);
			this.pts_b[i].addi(bb.pos);
		}

		//do SAT test
		Vec3 least_axis = new Vec3(0);
		float least_pen = findLeastPenetration(least_axis);
		if (least_pen < 0) {
			return;
		}

		//force separating axis to point from b to a
		if (MathUtils.dot(least_axis, new Vec3(this.bb.pos, this.ba.pos)) < 0) {
			least_axis.muli(-1);
		}

		m.didCollide = true;
		m.penetration = least_pen;
		m.separating_axis = new Vec3(least_axis);
		m.contacts = new ArrayList<>();

		float[] elow_a = sa.getELow();
		float[] ehigh_a = sa.getEHigh();
		float[] elow_b = sb.getELow();
		float[] ehigh_b = sb.getEHigh();

		//for each point, check if its colliding with the point in the other shape. 
		ArrayList<Contact> all_contacts = new ArrayList<>();
		for (int i = 0; i < this.pts_a.length; i++) { //compare points in a to b
			Vec3 pt = new Vec3(this.bb.pos, this.pts_a[i]);

			//see if the point is actually inside the other box
			boolean inside = true;
			for (int j = 0; j < this.axes_b.length; j++) {
				float dot = MathUtils.dot(pt, this.axes_b[j]);
				if (dot < elow_b[j] || ehigh_b[j] < dot) {
					inside = false;
					break;
				}
			}
			if (!inside) {
				continue;
			}

			//for each face, generate a contact. 
			for (int j = 0; j < this.axes_b.length; j++) {
				float dot = MathUtils.dot(pt, this.axes_b[j]);
				Vec3 normal = new Vec3(this.axes_b[j]);
				float pen = 0;
				if (dot < 0) {
					normal.muli(-1);
					pen = dot - elow_b[j];
				}
				else {
					pen = ehigh_b[j] - dot;
				}

				Contact c = new Contact(this.pts_a[i], normal, pen);
				all_contacts.add(c);
			}
		}
		for (int i = 0; i < this.pts_b.length; i++) { //compare points in b to a
			Vec3 pt = new Vec3(this.ba.pos, this.pts_b[i]);

			//see if the point is actually inside the other box
			boolean inside = true;
			for (int j = 0; j < this.axes_a.length; j++) {
				float dot = MathUtils.dot(pt, this.axes_a[j]);
				if (dot < elow_a[j] || ehigh_a[j] < dot) {
					inside = false;
					break;
				}
			}
			if (!inside) {
				continue;
			}

			//for each face, generate a contact. 
			for (int j = 0; j < this.axes_a.length; j++) {
				float dot = MathUtils.dot(pt, this.axes_a[j]);
				Vec3 normal = new Vec3(this.axes_a[j]);
				float pen = 0;
				if (dot < 0) {
					normal.muli(-1);
					pen = dot - elow_a[j];
				}
				else {
					pen = ehigh_a[j] - dot;
				}

				//force normal to be oriented from A's perspective
				normal.muli(-1);

				Contact c = new Contact(this.pts_b[i], normal, pen);
				all_contacts.add(c);
			}
		}

		//edge vs. edge collisions
		int[][] edges_a = this.sa.getEdges();
		int[][] edges_b = this.sb.getEdges();
		for (int eia = 0; eia < edges_a.length; eia++) {
			for (int eib = 0; eib < edges_b.length; eib++) {
				Vec3 a0 = new Vec3(this.pts_a[edges_a[eia][0]]);
				Vec3 a1 = new Vec3(this.pts_a[edges_a[eia][1]]);

				Vec3 b0 = new Vec3(this.pts_b[edges_b[eib][0]]);
				Vec3 b1 = new Vec3(this.pts_b[edges_b[eib][1]]);

				//compute coll normal
				Vec3 coll_norm = MathUtils.cross(new Vec3(a0, a1), new Vec3(b0, b1));
				coll_norm.normalize();

				//ensure that collision normal is pointing from b -> a
				if (MathUtils.dot(coll_norm, new Vec3(bb.pos, ba.pos)) < 0) {
					coll_norm.muli(-1);
				}

				//prune out if collision normal isn't facing roughly in direction of SAT normal
				if (MathUtils.dot(coll_norm, least_axis) < 0.25) {
					continue;
				}

				//check if they are parallel
				float parl_dot = Math.abs(MathUtils.dot(new Vec3(a0, a1).normalize(), new Vec3(b0, b1).normalize()));
				if (Math.abs(parl_dot - 1.0) < 0.001) {
					continue;
				}

				Pair<Vec3, Vec3> cpts = MathUtils.lineSegment_lineSegmentClosestPoints(a0, a1, b0, b1);

				//see if halfway between them is inside both a and b
				Vec3 coll_pt = MathUtils.lerp(cpts.first, 0, cpts.second, 1, 0.5f);
				if (Float.isNaN(cpts.first.x)) {
					System.exit(0);
				}

				boolean inside = true;
				for (int j = 0; j < this.axes_a.length; j++) {
					float dot = MathUtils.dot(new Vec3(this.ba.pos, coll_pt), this.axes_a[j]);
					if (dot < elow_a[j] || ehigh_a[j] < dot) {
						inside = false;
						break;
					}
				}
				for (int j = 0; j < this.axes_b.length; j++) {
					float dot = MathUtils.dot(new Vec3(this.bb.pos, coll_pt), this.axes_b[j]);
					if (dot < elow_b[j] || ehigh_b[j] < dot) {
						inside = false;
						break;
					}
				}
				if (!inside) {
					continue;
				}

				//ok, we can take this one as a collision point
				Contact c = new Contact(coll_pt, coll_norm, MathUtils.dist(cpts.first, cpts.second));
				all_contacts.add(c);
			}
		}

		//prune out any contacts that are not roughly facing in the correct direction
		ArrayList<Contact> pruned_contacts = new ArrayList<>();
		for (Contact c : all_contacts) {
			if (MathUtils.dot(c.norm, least_axis) < 0.25) {
				continue;
			}
			pruned_contacts.add(c);
		}

		//at this point, accept any contact. 
		for (Contact c : pruned_contacts) {
			m.contacts.add(c);
		}
	}

	private float findLeastPenetration(Vec3 out_axis) {
		Vec3[] sat_axes = new Vec3[axes_a.length + axes_b.length + axes_a.length * axes_b.length];
		{
			int ptr = 0;
			for (int i = 0; i < axes_a.length; i++) {
				sat_axes[ptr++] = new Vec3(axes_a[i]);
			}
			for (int i = 0; i < axes_b.length; i++) {
				sat_axes[ptr++] = new Vec3(axes_b[i]);
			}
			for (int i = 0; i < axes_a.length; i++) {
				for (int j = 0; j < axes_b.length; j++) {
					sat_axes[ptr++] = MathUtils.cross(axes_a[i], axes_b[j]);
				}
			}
		}

		Vec3 origin = ba.pos.add(bb.pos).mul(0.5f);

		float ans = 1e18f;
		out_axis.set(0, 0, 0);
		for (int i = 0; i < sat_axes.length; i++) {
			Vec3 axis = sat_axes[i];
			if (axis.lengthSq() < 0.001) {
				continue;
			}
			axis.normalize();

			//project points onto axis
			float mina = 1e18f;
			float maxa = -1e18f;
			float minb = 1e18f;
			float maxb = -1e18f;
			for (int j = 0; j < this.pts_a.length; j++) {
				float cur = MathUtils.dot(axis, this.pts_a[j].sub(origin));
				mina = Math.min(mina, cur);
				maxa = Math.max(maxa, cur);
			}
			for (int j = 0; j < this.pts_b.length; j++) {
				float cur = MathUtils.dot(axis, this.pts_b[j].sub(origin));
				minb = Math.min(minb, cur);
				maxb = Math.max(maxb, cur);
			}

			//enforce mina < minb
			if (mina > minb) {
				float tmp = mina;
				mina = minb;
				minb = tmp;
				tmp = maxa;
				maxa = maxb;
				maxb = tmp;
			}

			float pen = maxa - minb;
			if (pen < ans) {
				ans = pen;
				out_axis.set(axis);
			}
		}

		return ans;
	}

}
