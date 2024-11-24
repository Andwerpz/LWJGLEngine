package lwjglengine.impulse3d.collision;

import java.util.ArrayList;

import lwjglengine.impulse3d.Body;
import lwjglengine.impulse3d.shape.Capsule;
import lwjglengine.impulse3d.shape.KDOP;
import myutils.math.MathUtils;
import myutils.math.Vec3;
import myutils.misc.Pair;

public class CollisionCapsule_KDOP implements CollisionCallback {
	public static final CollisionCapsule_KDOP instance = new CollisionCapsule_KDOP();

	private Body b1, b2;

	private Capsule s1;
	private KDOP s2;

	//rotated and stuff
	private Vec3[] vertices;
	private int[][] edges;
	private Vec3[] axes;
	private Vec3[][] faces;
	private Vec3[] face_normals;

	private Vec3 e1, e2;

	@Override
	public void handleCollision(Manifold m) {
		this.b1 = m.a;
		this.b2 = m.b;

		this.s1 = (Capsule) b1.shape;
		this.s2 = (KDOP) b2.shape;

		this.axes = new Vec3[s2.getAxes().length];
		for (int i = 0; i < axes.length; i++) {
			axes[i] = MathUtils.quaternionRotateVec3(b2.orient, s2.getAxes()[i]);
		}

		this.vertices = new Vec3[s2.getVertices().length];
		for (int i = 0; i < vertices.length; i++) {
			vertices[i] = MathUtils.quaternionRotateVec3(b2.orient, s2.getVertices()[i]);
			vertices[i].addi(b2.pos);
		}

		this.edges = s2.getEdges();

		this.faces = new Vec3[s2.getFaces().length][];
		this.face_normals = new Vec3[s2.getFaces().length];
		for (int i = 0; i < faces.length; i++) {
			Vec3[] face = new Vec3[s2.getFaces()[i].length];
			for (int j = 0; j < face.length; j++) {
				face[j] = MathUtils.quaternionRotateVec3(b2.orient, s2.getFaces()[i][j]);
				face[j].addi(b2.pos);
			}
			faces[i] = face;
			face_normals[i] = MathUtils.quaternionRotateVec3(b2.orient, s2.getFaceNormals()[i]);
		}

		Pair<Vec3, Vec3> ends = s1.getEnds(b1.orient, b1.pos);
		this.e1 = ends.first;
		this.e2 = ends.second;

		//do contact point generation. First see if every feature can be matched to the middle, and if not
		//match it to one of the endcaps. 
		m.contacts = new ArrayList<>();

		//drastic penetration correction. Center line of capsule is somehow inside of KDOP
		//just project center of KDOP onto capsule center line and see if it's inside the KDOP
		{
			Vec3 proj = MathUtils.point_lineSegmentProjectClamped(this.b2.pos, e1, e2);
			Contact c = null;
			boolean is_inside = true;
			for (int i = 0; i < axes.length; i++) {
				float val = MathUtils.dot(axes[i], proj) - MathUtils.dot(axes[i], b2.pos);
				if (s2.getELow()[i] <= val && val <= s2.getEHigh()[i]) {
					float pen1 = val - s2.getELow()[i] + s1.radius;
					float pen2 = s2.getEHigh()[i] - val + s1.radius;
					if (c == null || pen1 < c.penetration) {
						c = new Contact(proj, new Vec3(b2.pos, proj), pen1);
					}
					if (c == null || pen2 < c.penetration) {
						c = new Contact(proj, new Vec3(b2.pos, proj), pen2);
					}
				}
				else {
					is_inside = false;
				}
			}

			if (is_inside) {
				m.contacts.add(c);
			}
		}

		//test against every face. Only have to consider endcaps
		{
			Contact c1 = null;
			Contact c2 = null;
			for (int i = 0; i < faces.length; i++) {
				Contact cur1 = this.endcapFaceContact(e1, faces[i], face_normals[i]);
				Contact cur2 = this.endcapFaceContact(e2, faces[i], face_normals[i]);
				if (cur1 != null && (c1 == null || c1.penetration > cur1.penetration))
					c1 = cur1;
				if (cur2 != null && (c2 == null || c2.penetration > cur2.penetration))
					c2 = cur2;
			}
			if (c1 != null)
				m.contacts.add(c1);
			if (c2 != null)
				m.contacts.add(c2);
		}

		//test against every edge
		for (int i = 0; i < edges.length; i++) {
			Vec3 v1 = new Vec3(vertices[edges[i][0]]);
			Vec3 v2 = new Vec3(vertices[edges[i][1]]);
			Contact c = null;

			//middle
			if (s1.length != 0) {
				//skip if they're exactly parallel
				//(either endcap or cylinder-vertex will handle this case)
				if (MathUtils.cross(new Vec3(v1, v2), new Vec3(e1, e2)).lengthSq() != 0) {
					Pair<Vec3, Vec3> pts = MathUtils.lineSegment_lineSegmentClosestPoints(e1, e2, v1, v2);
					Vec3 p1 = pts.first;
					Vec3 p2 = pts.second;

					//make sure the closest points are in the middle of the edge, not on the endpoints
					float md1 = Math.min(MathUtils.distSq(p1, e1), MathUtils.distSq(p1, e2));
					float md2 = Math.min(MathUtils.distSq(p2, v1), MathUtils.distSq(p2, v2));
					if (Math.min(md1, md2) != 0) {
						float pen = s1.radius - MathUtils.dist(p1, p2);
						if (pen > 0) {
							Vec3 norm = (new Vec3(p2, p1)).normalize();
							c = new Contact(p2, norm, pen);
						}
					}
				}
			}

			//endcaps
			if (c == null) {
				Vec3 cpt1 = MathUtils.point_lineSegmentProjectClamped(e1, v1, v2);
				Vec3 cpt2 = MathUtils.point_lineSegmentProjectClamped(e2, v1, v2);
				float md1 = Math.min(MathUtils.distSq(cpt1, v1), MathUtils.distSq(cpt1, v2));
				float md2 = Math.min(MathUtils.distSq(cpt2, v1), MathUtils.distSq(cpt2, v2));
				if (Math.min(md1, md2) != 0) { //this will be handled by testing vertices
					float pen1 = s1.radius - MathUtils.dist(cpt1, e1);
					float pen2 = s1.radius - MathUtils.dist(cpt2, e2);
					if (Math.max(pen1, pen2) > 0) {
						if (md2 == 0 || (md1 != 0 && pen1 > pen2)) {
							//collide with e1
							Vec3 norm = (new Vec3(cpt1, e1)).normalize();
							c = new Contact(cpt1, norm, pen1);
						}
						else {
							//collide with e2
							Vec3 norm = (new Vec3(cpt2, e2)).normalize();
							c = new Contact(cpt2, norm, pen2);
						}
					}
				}
			}

			if (c != null) {
				m.contacts.add(c);
			}
		}

		//test against every vertex
		for (int i = 0; i < vertices.length; i++) {
			Vec3 v = new Vec3(vertices[i]);
			Contact c = null;

			//middle
			if (s1.length != 0) {
				//does this vertex even project to the segment
				Vec3 proj = MathUtils.point_lineSegmentProject(v, e1, e2);
				if (proj != null) {
					//ok, it does.
					float pen = s1.radius - MathUtils.dist(proj, v);
					if (pen > 0) {
						Vec3 norm = (new Vec3(v, proj)).normalize();
						c = new Contact(v, norm, pen);
					}
				}
			}

			//ends
			if (c == null) {
				float pen1 = s1.radius - MathUtils.dist(v, e1);
				float pen2 = s1.radius - MathUtils.dist(v, e2);
				if (Math.max(pen1, pen2) > 0) {
					if (pen1 > pen2) {
						Vec3 norm = (new Vec3(v, e1)).normalize();
						c = new Contact(v, norm, pen1);
					}
					else {
						Vec3 norm = (new Vec3(v, e2)).normalize();
						c = new Contact(v, norm, pen2);
					}
				}
			}

			if (c != null) {
				m.contacts.add(c);
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

	//works if endcap is outside of the face
	public Contact endcapFaceContact(Vec3 e, Vec3[] face, Vec3 face_normal) {
		//see if e is too far away from or inside the face plane
		float pen = s1.radius - (MathUtils.dot(e, face_normal) - MathUtils.dot(face[0], face_normal));
		if (pen <= 0 || pen > s1.radius) { //too far away || inside
			return null;
		}

		//check if the end center, when projected down into the face plane, is inside the face. 
		//since the face is guaranteed to be convex, can just check if the signed area of each triangle is positive
		Vec3 e_proj = MathUtils.point_planeProject(e, face[0], face_normal);
		for (int i = 0; i < face.length; i++) {
			Vec3 v1 = face[i];
			Vec3 v2 = face[(i + 1) % face.length];
			Vec3 cross = MathUtils.cross(new Vec3(e_proj, v1), new Vec3(e_proj, v2));
			if (MathUtils.dot(cross, face_normal) < 0) {
				return null;
			}
		}

		//ok, take this as contact
		Vec3 norm = new Vec3(face_normal);
		return new Contact(e_proj, norm, pen);
	}

}
