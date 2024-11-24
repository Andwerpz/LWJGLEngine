package lwjglengine.impulse3d.collision;

import lwjglengine.impulse3d.Body;

public interface CollisionCallback {
	//should populate the given manifold with contact points and other relevant information
	public void handleCollision(Manifold m);
}
