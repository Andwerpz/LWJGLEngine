package lwjglengine.animation;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;

import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIAnimation;
import org.lwjgl.assimp.AIBone;
import org.lwjgl.assimp.AIMatrix4x4;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AINode;
import org.lwjgl.assimp.AINodeAnim;
import org.lwjgl.assimp.AIQuatKey;
import org.lwjgl.assimp.AIQuaternion;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.assimp.AIVectorKey;
import org.lwjgl.assimp.AIVertexWeight;

import lwjglengine.animation.AnimatedModel.Animation;
import lwjglengine.animation.AnimatedModel.Node;
import lwjglengine.animation.AnimatedModel.NodeAnimation;
import lwjglengine.graphics.Material;
import lwjglengine.model.Line;
import lwjglengine.model.ModelInstance;
import lwjglengine.model.ModelTransform;
import myutils.math.Mat4;
import myutils.math.MathUtils;
import myutils.math.Quaternion;
import myutils.math.Vec3;
import myutils.misc.Pair;

public class AnimationHandler {
	//keeps track of the current animation being played, and the respective pointers. 
	
	private final int DEBUG_SCENE;
	
	private AnimatedModel model;
	private AnimatedModelInstance modelInstance;
	private Mat4[] nodeTransforms;
	
	//if curAnimation != null, then we're currently playing one.
	private Animation curAnimation = null;
	private long prevTimeMillis;
	private float animationTime;
	
	private boolean doLooping = false;
	
	//TODO figure out how to set this variable automatically
	//if false, will discard the default pose when playing the animation. 
	//if true, will use the default pose as a base when applying the animation. 
	private boolean applyAnimationToDefaultPose = false;
	
	//while playing an animation, these are what keep track of what keyframes to interpolate
	private int[] posptrs, orientptrs, scaleptrs;
	
	//debug skeleton rendering. 
	private boolean renderSkeleton = false;
	private ModelInstance[] skeletonInstances;
	
	public AnimationHandler(AnimatedModelInstance _modelInstance, int _DEBUG_SCENE) {
		this.DEBUG_SCENE = _DEBUG_SCENE;
		this.modelInstance = _modelInstance;
		this.model = (AnimatedModel) _modelInstance.getModel();
		
		int node_cnt = this.model.getNodeCount();
		this.nodeTransforms = new Mat4[node_cnt];
		for(int i = 0; i < node_cnt; i++) {
			this.nodeTransforms[i] = Mat4.identity();
		}
		this.posptrs = new int[node_cnt];
		this.orientptrs = new int[node_cnt];
		this.scaleptrs = new int[node_cnt];
		
		this.computeNodeTransforms();
	}
	
	public void stopAnimation() {
		this.curAnimation = null;
		this.computeNodeTransforms();
	}
	
	public void playAnimation(int ind) {
		if(ind < 0 || ind >= this.model.getAnimationCount()) {
			System.err.println("AnimationHandler : tried to play animation index out of bounds, " + ind);
		}
		
		this.curAnimation = this.model.getAnimation(ind);
		this.prevTimeMillis = System.currentTimeMillis();
		this.animationTime = 0;
		
		//reset all keyframe pointers
		for(int i = 0; i < this.model.getNodeCount(); i++) {
			this.posptrs[i] = 0;
			this.orientptrs[i] = 0;
			this.scaleptrs[i] = 0;
		}
	
		this.computeNodeTransforms();
	}
	
	public boolean isPlayingAnimation() {
		return this.curAnimation != null;
	}
	
	public void setApplyAnimationToDefaultPose(boolean b) {
		this.applyAnimationToDefaultPose = b;
	}
	
	public boolean getApplyAnimationToDefaultPose() {
		return this.applyAnimationToDefaultPose;
	}
	
	public void setDoLooping(boolean b) {
		this.doLooping = b;
	}
	
	public boolean getRenderSkeleton() {
		return this.renderSkeleton;
	}
	
	public void setRenderSkeleton(boolean b) {
		if(this.renderSkeleton == b) {
			return;
		}
		this.renderSkeleton = b;
		if(this.renderSkeleton) {
			this.skeletonInstances = new ModelInstance[this.model.getNodeCount()];
			for(int i = 0; i < this.model.getNodeCount(); i++) {
				this.skeletonInstances[i] = Line.addDefaultLine(DEBUG_SCENE);
				this.skeletonInstances[i].setMaterial(new Material(Color.WHITE));
			}
			this.generateSkeleton();
		}
		else {
			for(ModelInstance m : this.skeletonInstances) {
				m.kill();
			}
			this.skeletonInstances = null;
		}
	}
	
	public Mat4[] getNodeTransforms() {
		return this.nodeTransforms;
	}
	
	private void generateSkeleton() {
		if(this.skeletonInstances == null ) {
			System.err.println("AnimationHandler : tried to generate skeleton while skeletonInstances == null");
			return;
		}
		for(int i = 0; i < this.model.getNodeCount(); i++) {
			Vec3 cpos = this.nodeTransforms[i].mul(new Vec3(0), 1);
			Vec3 ppos = new Vec3(cpos);
			if(i != 0) {
				ppos = this.nodeTransforms[this.model.getNode(i).parent.id].mul(new Vec3(0), 1);
			}
			ModelTransform t = Line.generateLineModelTransform(ppos, cpos);
			this.skeletonInstances[i].setModelTransform(t);
		}
	}
	
	public void update() {
		if(this.curAnimation != null) {
			long cur_time_millis = System.currentTimeMillis();
			long delta_millis = cur_time_millis - this.prevTimeMillis;
			float delta_seconds = ((float) delta_millis) / 1000.0f;
			this.prevTimeMillis = cur_time_millis;
			this.advance(delta_seconds);
		}
	}
	
	private void computeNodeTransforms() {
		this.computeTransform(this.model.getNode(0));
		if(this.renderSkeleton) {
			this.generateSkeleton();
			//zero out all the model transforms so we can actually see the skeleton
			for(int i = 0; i < this.nodeTransforms.length; i++) {
				this.nodeTransforms[i] = new Mat4();
			}
		}
		this.modelInstance.updateInstance();
	}
	
	//recompute node positions. 
	//position of node is calculated by
	// - start with node default transform, currently in limb space
	// - then apply the animation transform in limb space
	// - finally, apply the parent transform, bringing node into model space
	//animation transform is calculated by first scaling, then rotating, then translating. 
	//before first keyframe and after last one, we just use the keyframe as anim transform
	//everywhere else, we will interpolate
	private void computeTransform(Node cur) {
		//default transform
		Mat4 transform = new Mat4(cur.defaultTransform);
		
		//compute animation transform
		if(curAnimation != null){
			NodeAnimation na = this.curAnimation.channels[cur.id];
			Vec3 scale = new Vec3(na.scalekeys[scaleptrs[cur.id]].second);
			if(animationTime >= na.scalekeys[0].first && scaleptrs[cur.id] + 1 < na.scalekeys.length) {
				float t1 = na.scalekeys[scaleptrs[cur.id]].first;
				float t2 = na.scalekeys[scaleptrs[cur.id] + 1].first;
				Vec3 s1 = na.scalekeys[scaleptrs[cur.id]].second;
				Vec3 s2 = na.scalekeys[scaleptrs[cur.id] + 1].second;
				scale = MathUtils.lerp(s1, t1, s2, t2, animationTime);
			}
			Quaternion orient = new Quaternion(na.orientkeys[orientptrs[cur.id]].second);
			if(animationTime >= na.orientkeys[0].first && orientptrs[cur.id] + 1 < na.orientkeys.length) {
				float t1 = na.orientkeys[orientptrs[cur.id]].first;
				float t2 = na.orientkeys[orientptrs[cur.id] + 1].first;
				Quaternion q1 = na.orientkeys[orientptrs[cur.id]].second;
				Quaternion q2 = na.orientkeys[orientptrs[cur.id] + 1].second;
				orient = MathUtils.slerp(q1, t1, q2, t2, animationTime);
			}
			Vec3 pos = new Vec3(na.poskeys[posptrs[cur.id]].second);
			if(animationTime >= na.poskeys[0].first && posptrs[cur.id] + 1 < na.poskeys.length) {
				float t1 = na.poskeys[posptrs[cur.id]].first;
				float t2 = na.poskeys[posptrs[cur.id] + 1].first;
				Vec3 v1 = na.poskeys[posptrs[cur.id]].second;
				Vec3 v2 = na.poskeys[posptrs[cur.id] + 1].second;
				pos = MathUtils.lerp(v1, t1, v2, t2, animationTime);
			}
			Mat4 anim_transform = Mat4.identity();
			anim_transform.muli(Mat4.scale(scale));
			anim_transform.muli(MathUtils.quaternionToRotationMat4(orient));
			anim_transform.muli(Mat4.translate(pos));
			if(applyAnimationToDefaultPose) {
				transform.muli(anim_transform);
			}
			else {
				transform = anim_transform;
			}
		}

		//parent transform
		if(cur.parent != null) {
			transform.muli(nodeTransforms[cur.parent.id]);
		}
		
		nodeTransforms[cur.id] = transform;
		
		for(Node next : cur.children) {
			this.computeTransform(next);
		}
	}
	
	//advances the current animation by delta_seconds.
	private void advance(float delta_seconds) {		
		if(this.curAnimation == null) {
			return;
		}
		
		this.animationTime += delta_seconds;
		
		//see if we need to stop animation
		if(this.animationTime > this.curAnimation.duration) {
			if(!this.doLooping) {
				this.stopAnimation();
				return;
			}
			
			//update animation time, reset all keyframe pointers
			while(this.animationTime > this.curAnimation.duration) {
				this.animationTime -= this.curAnimation.duration;
			}
			for(int i = 0; i < this.model.getNodeCount(); i++) {
				this.posptrs[i] = 0;
				this.orientptrs[i] = 0;
				this.scaleptrs[i] = 0;
			}
		}
		
		//for each node, figure out new positions of pointers
		NodeAnimation[] channels = this.curAnimation.channels;
		for(int i = 0; i < this.model.getNodeCount(); i++) {
			NodeAnimation na = channels[i];
			while(this.posptrs[i] + 1 < na.poskeys.length && this.animationTime > na.poskeys[this.posptrs[i] + 1].first) {
				this.posptrs[i] ++;
			}
			while(this.orientptrs[i] + 1 < na.orientkeys.length && this.animationTime > na.orientkeys[this.orientptrs[i] + 1].first) {
				this.orientptrs[i] ++;
			}
			while(this.scaleptrs[i] + 1 < na.scalekeys.length && this.animationTime > na.scalekeys[this.scaleptrs[i] + 1].first) {
				this.scaleptrs[i] ++;
			}
		}
		
		this.computeNodeTransforms();
	}
}
