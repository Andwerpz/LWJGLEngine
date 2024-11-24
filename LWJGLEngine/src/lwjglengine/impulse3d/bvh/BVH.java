package lwjglengine.impulse3d.bvh;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Queue;

import myutils.math.MathUtils;
import myutils.math.Vec3;
import myutils.misc.Pair;

public class BVH {
	//bvh that only accepts axis aligned bounding boxes. 
	//should return a list of intersecting bounding box indices

	public BVHNode root = null;

	public BVH(ArrayList<KDOP> aabb_list) {
		ArrayList<Pair<KDOP, Integer>> list = new ArrayList<>();
		for (int i = 0; i < aabb_list.size(); i++) {
			list.add(new Pair<>(aabb_list.get(i), i));
		}

		//		System.out.println("Building BVH");
		//		for (KDOP k : aabb_list) {
		//			System.out.println(k);
		//		}

		this.root = this.buildTree(list);
	}

	private BVHNode buildTree(ArrayList<Pair<KDOP, Integer>> aabb_list) {
		if (aabb_list.size() == 0) {
			return null;
		}

		//compute some stuff about current box list
		float[] bmin = new float[KDOP.axes.length];
		float[] bmax = new float[KDOP.axes.length];
		float[] dim = new float[KDOP.axes.length];
		float[] med = new float[KDOP.axes.length];
		for (int i = 0; i < aabb_list.size(); i++) {
			KDOP k = aabb_list.get(i).first;
			for (int j = 0; j < KDOP.axes.length; j++) {
				if (i == 0) {
					bmin[j] = k.bmin[j];
					bmax[j] = k.bmax[j];
				}
				bmin[j] = Math.min(bmin[j], k.bmin[j]);
				bmax[j] = Math.max(bmax[j], k.bmax[j]);
			}
		}
		for (int i = 0; i < KDOP.axes.length; i++) {
			dim[i] = bmax[i] - bmin[i];
			med[i] = (bmax[i] + bmin[i]) / 2.0f;
		}
		KDOP cur_bb = new KDOP(bmin, bmax);

		//figure out best partition for the bounding boxes
		//just look for maximum separation along all axes	
		int best_sep = -1;
		float max_sep = -1;
		for (int i = 0; i < KDOP.axes.length; i++) {
			if (dim[i] > max_sep) {
				max_sep = dim[i];
				best_sep = i;
			}
		}

		//create partition
		ArrayList<Pair<KDOP, Integer>> alist = new ArrayList<>(), blist = new ArrayList<>();
		for (int i = 0; i < aabb_list.size(); i++) {
			float cmed = (aabb_list.get(i).first.bmin[best_sep] + aabb_list.get(i).first.bmax[best_sep]) / 2.0f;
			if (cmed < med[best_sep]) {
				alist.add(aabb_list.get(i));
			}
			else {
				blist.add(aabb_list.get(i));
			}
		}

		//couldn't figure out partition, just make a leaf
		if (alist.size() == 0 || blist.size() == 0) {
			return new BVHNode(cur_bb, alist.size() != 0 ? alist : blist);
		}

		BVHNode a = buildTree(alist), b = buildTree(blist);
		BVHNode node = new BVHNode(cur_bb, a, b);
		return node;
	}

	public ArrayList<Integer> getIntersections(KDOP aabb) {
		ArrayList<Integer> ans = new ArrayList<>();
		Queue<BVHNode> q = new ArrayDeque<>();
		q.add(this.root);
		while (q.size() != 0) {
			BVHNode cur = q.peek();
			q.poll();

			if (cur == null) {
				continue;
			}

			//check if kdop is intersecting with current bvh node
			if (!aabb.isIntersecting(cur.bounding_box)) {
				continue;
			}

			//if is leaf, just compare against everything in node
			if (cur.is_leaf) {
				for (Pair<KDOP, Integer> p : cur.boxes) {
					if (p.first.isIntersecting(aabb)) {
						ans.add(p.second);
					}
				}
				continue;
			}

			//otherwise, append children to queue
			q.add(cur.a);
			q.add(cur.b);
		}
		return ans;
	}

	class BVHNode {
		public KDOP bounding_box;

		public boolean is_leaf;
		public ArrayList<Pair<KDOP, Integer>> boxes = null;

		public BVHNode a = null, b = null;

		public BVHNode(KDOP _bounding_box, BVHNode _a, BVHNode _b) {
			this.bounding_box = new KDOP(_bounding_box);

			this.is_leaf = false;
			this.a = _a;
			this.b = _b;
		}

		public BVHNode(KDOP _bounding_box, ArrayList<Pair<KDOP, Integer>> _boxes) {
			this.bounding_box = new KDOP(_bounding_box);

			this.is_leaf = true;
			this.boxes = _boxes;
		}
	}
}
