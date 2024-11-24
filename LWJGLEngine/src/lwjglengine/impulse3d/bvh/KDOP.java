package lwjglengine.impulse3d.bvh;

import myutils.math.MathUtils;
import myutils.math.Vec3;

public class KDOP {
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

	public static Vec3[] axes = dop18;
	public float[] bmin, bmax;

	public KDOP(Vec3[] pts) {
		this.bmin = new float[axes.length];
		this.bmax = new float[axes.length];
		for (int i = 0; i < pts.length; i++) {
			for (int j = 0; j < axes.length; j++) {
				float val = MathUtils.dot(pts[i], axes[j]);
				if (i == 0) {
					this.bmin[j] = val;
					this.bmax[j] = val;
				}
				this.bmin[j] = Math.min(this.bmin[j], val);
				this.bmax[j] = Math.max(this.bmax[j], val);
			}
		}
	}

	public KDOP(float[] _bmin, float[] _bmax) {
		this.bmin = _bmin;
		this.bmax = _bmax;
	}

	public KDOP(KDOP first, KDOP... rest) {
		this.bmin = new float[axes.length];
		this.bmax = new float[axes.length];
		for (int i = 0; i < axes.length; i++) {
			this.bmin[i] = first.bmin[i];
			this.bmax[i] = first.bmax[i];
		}

		for (KDOP k : rest) {
			for (int i = 0; i < axes.length; i++) {
				this.bmin[i] = Math.min(this.bmin[i], k.bmin[i]);
				this.bmax[i] = Math.max(this.bmax[i], k.bmax[i]);
			}
		}
	}

	public boolean isIntersecting(KDOP other) {
		for (int i = 0; i < axes.length; i++) {
			if ((this.bmin[i] <= other.bmin[i] && other.bmin[i] <= this.bmax[i]) || (other.bmin[i] <= this.bmin[i] && this.bmin[i] <= other.bmax[i])) {
				continue;
			}
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		String ans = "Bounding KDOP : \n";
		for (int i = 0; i < KDOP.axes.length; i++) {
			ans += this.bmin[i] + ", " + this.bmax[i] + "\n";
		}
		return ans;
	}
}
