package lwjglengine.v10.impulse2d;

public interface CollisionCallback {
	public void handleCollision(Manifold m, Body a, Body b);
}
