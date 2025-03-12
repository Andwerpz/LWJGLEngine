package lwjglengine.animation;

import lwjglengine.model.Model;
import lwjglengine.model.ModelInstance;

public class AnimatedModelInstance extends ModelInstance {
	private AnimationHandler animationHandler;

	public AnimatedModelInstance(AnimatedModel model, int scene) {
		super(model, scene);
		this.init();
	}
	
	private void init() {
		this.animationHandler = new AnimationHandler((AnimatedModel) this.getModel(), this.getScene());
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
}
