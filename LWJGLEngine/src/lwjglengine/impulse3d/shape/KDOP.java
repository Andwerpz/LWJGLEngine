package lwjglengine.impulse3d.shape;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import myutils.math.Mat3;
import myutils.math.MathUtils;
import myutils.math.Quaternion;
import myutils.math.Vec2;
import myutils.math.Vec3;

public class KDOP extends Shape {

	// @formatter:off
	public static Vec3[] dop6 = new Vec3[] {
		new Vec3(1, 0, 0),
		new Vec3(0, 1, 0),
		new Vec3(0, 0, 1),
	};
	
	public static Vec3[] dop18 = new Vec3[] {
		new Vec3(1, 0, 0),
		new Vec3(0, 1, 0),
		new Vec3(0, 0, 1),
		new Vec3(1, 1, 0).normalize(),
		new Vec3(-1, 1, 0).normalize(),
		new Vec3(0, 1, 1).normalize(),
		new Vec3(0, 1, -1).normalize(),
		new Vec3(1, 0, 1).normalize(),
		new Vec3(-1, 0, 1).normalize(),
	};
	// @formatter:on

	private static final float EPSILON = 0.00001f;

	protected Vec3[] axes;
	protected float[] elow, ehigh;

	protected Vec3[] vertices;
	protected int[][] edges;

	//faces are wound facing outwards
	private Vec3[][] faces;
	private Vec3[] face_normals;

	private Vec3 com_correction = null;

	protected KDOP() {

	}

	public KDOP(Vec3[] pts) {
		this(pts, dop18);
	}

	public KDOP(Vec3[] pts, Vec3[] _axes) {
		this.type = Type.KDOP;

		this.axes = new Vec3[_axes.length];
		for (int i = 0; i < _axes.length; i++) {
			this.axes[i] = new Vec3(_axes[i]);
		}

		//COM correction
		this.computeBVE(pts);
		this.computeFaces();
		Vec3 com = this.computeCenterOfMass();
		this.com_correction = new Vec3(com);
		System.out.println("CENTER OF MASS : " + com);
		for (int i = 0; i < pts.length; i++) {
			pts[i].subi(com);
		}

		//OK, now the real stuff can begin
		this.computeBVE(pts);
		this.computeFaces();
		this.computeMass();
	}

	//(bounds, vertices, edges)
	protected void computeBVE(Vec3[] pts) {
		this.elow = new float[this.axes.length];
		this.ehigh = new float[this.axes.length];
		for (int i = 0; i < pts.length; i++) {
			for (int j = 0; j < this.axes.length; j++) {
				if (i == 0) {
					this.elow[j] = MathUtils.dot(this.axes[j], pts[i]);
					this.ehigh[j] = MathUtils.dot(this.axes[j], pts[i]);
				}
				this.elow[j] = Math.min(MathUtils.dot(this.axes[j], pts[i]), this.elow[j]);
				this.ehigh[j] = Math.max(MathUtils.dot(this.axes[j], pts[i]), this.ehigh[j]);
			}
		}

		//find vertices that compose the hull. 
		//vertices are where three planes intersect. 
		//for each hull vertex, store what planes it's adjacent to. 
		HashMap<Vec3, HashSet<Integer>> hull_vertices = new HashMap<>(); //maps hull vertices to the set of faces its touching. 
		for (int ia = 0; ia < this.axes.length; ia++) {
			for (int ib = ia + 1; ib < this.axes.length; ib++) {
				for (int ic = ib + 1; ic < this.axes.length; ic++) {
					for (int bits = 0; bits < 8; bits++) {
						Vec3 a_origin = this.axes[ia].mul((bits & (1 << 0)) == 0 ? this.elow[ia] : this.ehigh[ia]);
						Vec3 b_origin = this.axes[ib].mul((bits & (1 << 1)) == 0 ? this.elow[ib] : this.ehigh[ib]);
						Vec3 c_origin = this.axes[ic].mul((bits & (1 << 2)) == 0 ? this.elow[ic] : this.ehigh[ic]);
						Vec3 isect_pt = MathUtils.plane_plane_planeIntersect(a_origin, this.axes[ia], b_origin, this.axes[ib], c_origin, this.axes[ic]);
						if (isect_pt == null) {
							continue;
						}

						//check if this point is inside the kdop
						boolean is_inside = true;
						for (int i = 0; i < this.axes.length; i++) {
							float d = MathUtils.dot(isect_pt, this.axes[i]);
							if (this.elow[i] <= d + EPSILON && d - EPSILON <= this.ehigh[i]) {
								continue;
							}
							is_inside = false;
						}
						if (!is_inside) {
							continue;
						}

						//save this point as a hull vertex
						//see if we can find an already existing hull vertex close enough to this one
						Vec3 hull_vertex = null;
						for (Vec3 v : hull_vertices.keySet()) {
							if (MathUtils.dist(v, isect_pt) < EPSILON) {
								hull_vertex = v;
							}
						}
						if (hull_vertex == null) {
							//can't find pre-existing one. Create new one. 
							hull_vertex = isect_pt;
							hull_vertices.put(isect_pt, new HashSet<>());
						}

						//insert face indexes
						hull_vertices.get(hull_vertex).add(ia + ((bits & (1 << 0)) == 0 ? 0 : this.axes.length));
						hull_vertices.get(hull_vertex).add(ib + ((bits & (1 << 1)) == 0 ? 0 : this.axes.length));
						hull_vertices.get(hull_vertex).add(ic + ((bits & (1 << 2)) == 0 ? 0 : this.axes.length));
					}
				}
			}
		}
		this.vertices = new Vec3[hull_vertices.size()];
		ArrayList<HashSet<Integer>> adj_faces = new ArrayList<>();
		{
			int ptr = 0;
			for (Vec3 v : hull_vertices.keySet()) {
				this.vertices[ptr] = v;
				adj_faces.add(hull_vertices.get(v));
				ptr++;
			}
		}

		//find edges that compose the hull
		//edges are the line segments between edges where two planes intersect
		//just need to find pairs of vertices which share at least 2 faces. 
		ArrayList<int[]> edge_list = new ArrayList<>();
		for (int ia = 0; ia < this.vertices.length; ia++) {
			for (int ib = ia + 1; ib < this.vertices.length; ib++) {
				int share_cnt = 0;
				for (int eind : adj_faces.get(ia)) {
					if (adj_faces.get(ib).contains(eind)) {
						share_cnt++;
					}
				}
				if (share_cnt < 2) {
					continue;
				}

				edge_list.add(new int[] { ia, ib });
			}
		}
		this.edges = new int[edge_list.size()][];
		for (int i = 0; i < edge_list.size(); i++) {
			this.edges[i] = edge_list.get(i);
		}
	}

	protected Vec3 computeCenterOfMass() {
		float volume = 0;
		Vec3 com = new Vec3(0);
		for (int _i = 0; _i < this.faces.length; _i++) {
			Vec3[] f = this.faces[_i];

			//add to volume and com
			for (int i = 2; i < f.length; i++) {
				Vec3 a = f[0];
				Vec3 b = f[1];
				Vec3 c = f[i];
				float cvol = Math.abs(MathUtils.signedTetrahedronVolume(a, b, c, new Vec3(0)));
				Vec3 ccom = (a.add(b).add(c)).div(4.0f);

				volume += cvol;
				com.addi(ccom.mul(cvol));
			}
		}
		com.divi(volume);
		return com;
	}

	protected void computeFaces() {
		//first, identify all the faces. This can be done by identifying all vertices that touch the same axis
		ArrayList<ArrayList<Vec3>> face_list = new ArrayList<>();
		ArrayList<Vec3> face_normal_list = new ArrayList<>(); //should face inwards
		for (int i = 0; i < this.axes.length; i++) {
			ArrayList<Vec3> fmin = new ArrayList<>();
			ArrayList<Vec3> fmax = new ArrayList<>();
			for (int j = 0; j < this.vertices.length; j++) {
				float d = MathUtils.dot(this.vertices[j], this.axes[i]);
				if (Math.abs(d - this.elow[i]) < EPSILON) {
					fmin.add(new Vec3(this.vertices[j]));
				}
				if (Math.abs(d - this.ehigh[i]) < EPSILON) {
					fmax.add(new Vec3(this.vertices[j]));
				}
			}
			if (fmin.size() >= 3) {
				face_list.add(fmin);
				face_normal_list.add(new Vec3(this.axes[i]).mul(-1));
			}
			if (fmax.size() >= 3) {
				face_list.add(fmax);
				face_normal_list.add(new Vec3(this.axes[i]).mul(1));
			}
		}

		//for each face, give it a CCW winding order with respect to the face normal. 
		this.faces = new Vec3[face_list.size()][];
		this.face_normals = new Vec3[face_list.size()];
		for (int _i = 0; _i < face_list.size(); _i++) {
			ArrayList<Vec3> f = face_list.get(_i);
			Vec3 norm = face_normal_list.get(_i);

			//create local coordinate system on the plane
			Vec3 u = null, v = null;
			{
				Vec3 not_parl = null;
				if (Math.abs(norm.x) < 0.707)
					not_parl = new Vec3(1, 0, 0);
				else if (Math.abs(norm.y) < 0.707)
					not_parl = new Vec3(0, 1, 0);
				else
					not_parl = new Vec3(0, 0, 1);

				//right handed coordinate system
				u = MathUtils.cross(not_parl, norm);
				u.normalize();
				v = MathUtils.cross(norm, u);
				v.normalize();
			}
			float dist = MathUtils.dot(norm, f.get(0));

			//transform face into local coordinate system
			Vec2[] ft = new Vec2[f.size()];
			for (int i = 0; i < f.size(); i++) {
				ft[i] = new Vec2(0);
				ft[i].x = MathUtils.dot(u, f.get(i));
				ft[i].y = MathUtils.dot(v, f.get(i));
			}

			//get nice winding, and transform back to world space
			ArrayList<Vec2> hull = MathUtils.calculateConvexHull(ft);
			if (hull.size() != f.size()) {
				System.err.println("KDOP : SOMETHING BAD HAPPENED WITH CONVEX HULL");
			}
			Vec3[] face = new Vec3[hull.size()];
			for (int i = 0; i < hull.size(); i++) {
				Vec2 a = hull.get(i);
				Vec3 cur = new Vec3(0);
				cur.addi(u.mul(a.x));
				cur.addi(v.mul(a.y));
				cur.addi(norm.mul(dist));
				face[i] = cur;
			}

			this.faces[_i] = face;
			this.face_normals[_i] = norm;
		}
	}

	protected void computeMass() {
		//TODO compute moment. Sum up moments of all tetrahedra
		//need to use parallel axis theorem or smth.
		//https://docsdrive.com/pdfs/sciencepublications/jmssp/2005/8-11.pdf

		//ok, for now, just use parl axis theorem. Will do complicated math later
		this.moment = new Mat3();
		float volume = 0;
		for (int _i = 0; _i < this.faces.length; _i++) {
			Vec3[] f = this.faces[_i];
			for (int i = 2; i < f.length; i++) {
				Vec3 a = f[0];
				Vec3 b = f[1];
				Vec3 c = f[i];
				float cvol = Math.abs(MathUtils.signedTetrahedronVolume(a, b, c, new Vec3(0)));
				volume += cvol;

				Vec3 centroid = (a.add(b).add(c)).div(4.0f);
				float x = centroid.x;
				float y = centroid.y;
				float z = centroid.z;
				Mat3 com_tensor = new Mat3();
				com_tensor.mat[0][0] = cvol * (y * y + z * z);
				com_tensor.mat[0][1] = -cvol * x * y;
				com_tensor.mat[0][2] = -cvol * x * z;
				com_tensor.mat[1][0] = -cvol * x * y;
				com_tensor.mat[1][1] = cvol * (x * x + z * z);
				com_tensor.mat[1][2] = -cvol * y * z;
				com_tensor.mat[2][0] = -cvol * x * z;
				com_tensor.mat[2][1] = -cvol * y * z;
				com_tensor.mat[2][2] = cvol * (x * x + y * y);
				this.moment.addi(com_tensor);
			}
		}
		this.mass = volume;
	}

	public Vec3[] getVertices() {
		return this.vertices;
	}

	public int[][] getEdges() {
		return this.edges;
	}

	public Vec3[] getAxes() {
		return this.axes;
	}

	public float[] getELow() {
		return this.elow;
	}

	public float[] getEHigh() {
		return this.ehigh;
	}

	public Vec3[][] getFaces() {
		return this.faces;
	}

	public Vec3[] getFaceNormals() {
		return this.face_normals;
	}

	public Vec3 getCOMCorrection() {
		return this.com_correction;
	}

	@Override
	public lwjglengine.impulse3d.bvh.KDOP calcBoundingBox(Quaternion orient, Vec3 pos) {
		//just run through all the vertices 
		Vec3[] vlist = new Vec3[this.vertices.length];
		for (int i = 0; i < this.vertices.length; i++) {
			vlist[i] = MathUtils.quaternionRotateVec3(orient, this.vertices[i]).add(pos);
		}
		return new lwjglengine.impulse3d.bvh.KDOP(vlist);
	}

}
