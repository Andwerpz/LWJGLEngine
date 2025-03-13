package lwjglengine.animation;

import lwjglengine.model.Model;
import lwjglengine.model.ModelInstance;

public class AnimatedModelInstance extends ModelInstance {
	private AnimationHandler animationHandler;
	private int nodeOffset = -1;	//set by AnimatedModel before being sent off to VertexArray

	public AnimatedModelInstance(AnimatedModel model, int scene) {
		super(model, scene);
		this.init();
	}
	
	private void init() {
		this.animationHandler = new AnimationHandler(this, this.getScene());
	}
	
	public AnimationHandler getAnimationHandler() {
		return this.animationHandler;
	}

	public void playAnimation(int ind) {
		this.animationHandler.playAnimation(ind);
	}
	
	public void stopAnimation() {
		this.animationHandler.stopAnimation();
	}
	
	protected void setNodeOffset(int off) {
		this.nodeOffset = off;
	}
	
	public int getNodeOffset() {
		return this.nodeOffset;
	}
}
