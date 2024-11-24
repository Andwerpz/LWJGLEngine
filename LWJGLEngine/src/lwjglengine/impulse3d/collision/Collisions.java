package lwjglengine.impulse3d.collision;

public class Collisions {
	// @formatter:off
	public static CollisionCallback[][] dispatch = { 
			{ CollisionKDOP_KDOP.instance },
			{ CollisionCapsule_KDOP.instance, CollisionCapsule_Capsule.instance },
	};
	// @formatter:on
}
