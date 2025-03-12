package lwjglengine.animation;

import static org.lwjgl.assimp.Assimp.aiImportFile;
import static org.lwjgl.assimp.Assimp.aiProcess_JoinIdenticalVertices;
import static org.lwjgl.assimp.Assimp.aiProcess_Triangulate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
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
import org.lwjgl.assimp.Assimp;

import lwjglengine.graphics.Material;
import lwjglengine.graphics.TextureMaterial;
import lwjglengine.model.Model;
import lwjglengine.model.VertexArray;
import myutils.file.FileUtils;
import myutils.math.Mat4;
import myutils.math.MathUtils;
import myutils.math.Quaternion;
import myutils.math.Vec3;
import myutils.misc.Pair;

public class AnimatedModel extends Model {
	
	private Node[] nodes;	//root is nodes[0]
	private Animation[] animations;
	
	public AnimatedModel(AIScene aiscene, String parentFilepath) throws IOException {
		super(aiscene, parentFilepath);
		this.anim_init(aiscene);
	}
	
	private void anim_init(AIScene aiscene) {
		if(aiscene.mRootNode() == null) {
			//doesn't have skeleton. 
			System.err.println("AnimatedModel : provided model doesn't have skeleton");
			return;
		}
		
		//extract nodes
		HashMap<String, Integer> name_to_id = new HashMap<>();
		{
			AINode airoot = aiscene.mRootNode();
			Node root = new Node(airoot, null, name_to_id);
			this.nodes = new Node[name_to_id.size()];
			Queue<Node> q = new ArrayDeque<>();
			q.add(root);
			while(q.size() != 0){
				Node cur = q.poll();
				this.nodes[cur.id] = cur;
				for(Node next : cur.children) {
					q.add(next);
				}
			}
		}
		
		//debug:
		{
			System.out.println("Name to id :");
			for(String name : name_to_id.keySet()) {
				System.out.println(name + " -> " + name_to_id.get(name));
			}
			int hip_id = 1;
		}
		
		//extract animations
		if(aiscene.mAnimations() != null) {
			PointerBuffer aianimations = aiscene.mAnimations();
			this.animations = new Animation[aianimations.limit()];
			System.out.println("NR ANIMATIONS : " + aianimations.limit());
			for(int i = 0; i < aianimations.limit(); i++) {
				AIAnimation anim = AIAnimation.create(aianimations.get(i));
				this.animations[i] = new Animation(anim, name_to_id);
				System.out.println("Animation " + i + ", duration : " + anim.mDuration() + ", tps : " + anim.mTicksPerSecond());
			}
		}
		else {
			this.animations = new Animation[0];
		}
		
		//TODO
		//for each mesh, figure out bones and stuff
		//each vertex needs to find its top 4 bone (node) weights. 
		//after you find it out, save it in the VertexArray
		PointerBuffer aimeshes = aiscene.mMeshes();
		for(int i = 0; i < aimeshes.limit(); i++) {
			AIMesh aimesh = AIMesh.create(aimeshes.get(i));
			PointerBuffer aibones = aimesh.mBones();
			for(int j = 0; j < aibones.limit(); j++) {
				
			}
		}
	}
	
	public Node getNode(int ind) {
		return this.nodes[ind];
	}
	
	public Animation getAnimation(int ind) {
		return this.animations[ind];
	}
	
	public int getNodeCount() {
		return this.nodes.length;
	}
	
	public int getAnimationCount() {
		return this.animations.length;
	}
	
	public static AnimatedModel loadAnimatedModelFile(File file) {
		String filepath = file.getAbsolutePath();
		String parentFilepath = file.getParent() + "\\";

		System.out.println("LOADING ANIMATED MODEL: " + file.getName());

		AIScene scene = aiImportFile(filepath, aiProcess_Triangulate | aiProcess_JoinIdenticalVertices);

		if (scene == null) {
			System.err.println("Failed to load model " + file.getName());
			String error = Assimp.aiGetErrorString();
			System.err.println("Assimp Error: " + error);
			return null;
		}

		AnimatedModel model;
		try {
			model = new AnimatedModel(scene, parentFilepath);
		} catch (IOException e) {
			System.out.println("FAILED");
			e.printStackTrace();
			return null;
		}
		System.out.println("SUCCESS");
		
		return model;
	}
	
	public static AnimatedModel loadAnimatedModelFile(String filepath) throws IOException {
		return AnimatedModel.loadAnimatedModelFile(FileUtils.loadFile(filepath));
	}

	public static AnimatedModel loadAnimatedModelFileRelative(String relativeFilepath) throws IOException {
		return AnimatedModel.loadAnimatedModelFile(FileUtils.generateAbsoluteFilepath(relativeFilepath));
	}
	
	class Animation {
		float duration, tps;
		NodeAnimation[] channels;	//one channel per node
		
		public Animation(AIAnimation aianimation, HashMap<String, Integer> name_to_id) {
			this.tps = (float) aianimation.mTicksPerSecond();
			if(this.tps <= 0) this.tps = 1;
			this.duration = (float) aianimation.mDuration() / this.tps;
			PointerBuffer aichannels = aianimation.mChannels();
			this.channels = new NodeAnimation[nodes.length];
			for(int i = 0; i < aichannels.limit(); i++) {
				AINodeAnim ainodeanim = AINodeAnim.create(aichannels.get(i));
				String name = ainodeanim.mNodeName().dataString();
				this.channels[name_to_id.get(name)] = new NodeAnimation(ainodeanim, this.tps, name_to_id.get(name));
			}
			for(int i = 0; i < nodes.length; i++) {
				if(this.channels[i] == null) {
					this.channels[i] = new NodeAnimation(i);
				}
			}
		}
	}
	
	class NodeAnimation {
		int node_id;
		Pair<Float, Vec3>[] poskeys, scalekeys;
		Pair<Float, Quaternion>[] orientkeys;
		
		public NodeAnimation(AINodeAnim ainodeanimation, float tps, int _node_id) {
			this.node_id = _node_id;
			AIVectorKey.Buffer aiposkeys = ainodeanimation.mPositionKeys();
			this.poskeys = new Pair[aiposkeys.limit()];
			for(int i = 0; i < aiposkeys.limit(); i++) {
				AIVectorKey aiposkey = aiposkeys.get(i);
				AIVector3D aivec = aiposkey.mValue();
				this.poskeys[i] = new Pair<Float, Vec3>((float) aiposkey.mTime() / tps, new Vec3(aivec.x(), aivec.y(), aivec.z()));
			}
			AIQuatKey.Buffer aiorientkeys = ainodeanimation.mRotationKeys();
			this.orientkeys = new Pair[aiorientkeys.limit()];
			for(int i = 0; i < aiorientkeys.limit(); i++) {
				AIQuatKey aiquatkey = aiorientkeys.get(i);
				AIQuaternion aiquat = aiquatkey.mValue();
				this.orientkeys[i] = new Pair<Float, Quaternion>((float) aiquatkey.mTime() / tps, new Quaternion(aiquat.w(), aiquat.x(), aiquat.y(), aiquat.z()));
			}
			AIVectorKey.Buffer aiscalekeys = ainodeanimation.mScalingKeys();
			this.scalekeys = new Pair[aiscalekeys.limit()];
			for(int i = 0; i < aiscalekeys.limit(); i++) {
				AIVectorKey aiscalekey = aiscalekeys.get(i);
				AIVector3D aivec = aiscalekey.mValue();
				this.scalekeys[i] = new Pair<Float, Vec3>((float) aiscalekey.mTime() / tps, new Vec3(aivec.x(), aivec.y(), aivec.z()));
			}
		}
		
		//just initializes an identity animation
		public NodeAnimation(int _node_id) {
			this.node_id = _node_id;
			this.poskeys = new Pair[1];
			this.poskeys[0] = new Pair<Float, Vec3>(0f, new Vec3(0));
			this.orientkeys = new Pair[1];
			this.orientkeys[0] = new Pair<Float, Quaternion>(0f, Quaternion.identity());
			this.scalekeys = new Pair[1];
			this.scalekeys[0] = new Pair<Float, Vec3>(0f, new Vec3(1));
		}
	}
	
	class Node {
		int id;
		Node parent;
		Node[] children;
		Mat4 defaultTransform;
		
		public Node(AINode ainode, Node _parent, HashMap<String, Integer> name_to_id) {
			System.out.println("AINODE : " + ainode.mName().dataString());
			this.id = name_to_id.size();
			this.parent = _parent;
			name_to_id.put(ainode.mName().dataString(), this.id);
			
			this.defaultTransform = new Mat4();
			AIMatrix4x4 aimat = ainode.mTransformation();
			this.defaultTransform.mat[0][0] = aimat.a1();
			this.defaultTransform.mat[0][1] = aimat.a2();
			this.defaultTransform.mat[0][2] = aimat.a3();
			this.defaultTransform.mat[0][3] = aimat.a4();
			
			this.defaultTransform.mat[1][0] = aimat.b1();
			this.defaultTransform.mat[1][1] = aimat.b2();
			this.defaultTransform.mat[1][2] = aimat.b3();
			this.defaultTransform.mat[1][3] = aimat.b4();
			
			this.defaultTransform.mat[2][0] = aimat.c1();
			this.defaultTransform.mat[2][1] = aimat.c2();
			this.defaultTransform.mat[2][2] = aimat.c3();
			this.defaultTransform.mat[2][3] = aimat.c4();
			
			this.defaultTransform.mat[3][0] = aimat.d1();
			this.defaultTransform.mat[3][1] = aimat.d2();
			this.defaultTransform.mat[3][2] = aimat.d3();
			this.defaultTransform.mat[3][3] = aimat.d4();
			
			PointerBuffer aichildren = ainode.mChildren();
			if(aichildren != null) {
				this.children = new Node[aichildren.limit()];
				for(int i = 0; i < aichildren.limit(); i++) {
					this.children[i] = new Node(AINode.create(aichildren.get(i)), this, name_to_id);
				}
			}
			else {
				this.children = new Node[0];
			}
		}
	}
	
	class Bone {
		int node_id;
		Bone[] children;
		AIVertexWeight[] weights;
		Mat4 boneSpaceTransform;	//converts a vertex from model to bone space
		
		public Bone(AIBone aibone, HashMap<String, Integer> name_to_id) {
			this.node_id = name_to_id.get(aibone.mName().dataString());
			
			this.boneSpaceTransform = new Mat4();
			AIMatrix4x4 aimat = aibone.mOffsetMatrix();
			this.boneSpaceTransform.mat[0][0] = aimat.a1();
			this.boneSpaceTransform.mat[0][1] = aimat.a2();
			this.boneSpaceTransform.mat[0][2] = aimat.a3();
			this.boneSpaceTransform.mat[0][3] = aimat.a4();
			
			this.boneSpaceTransform.mat[1][0] = aimat.b1();
			this.boneSpaceTransform.mat[1][1] = aimat.b2();
			this.boneSpaceTransform.mat[1][2] = aimat.b3();
			this.boneSpaceTransform.mat[1][3] = aimat.b4();
			
			this.boneSpaceTransform.mat[2][0] = aimat.c1();
			this.boneSpaceTransform.mat[2][1] = aimat.c2();
			this.boneSpaceTransform.mat[2][2] = aimat.c3();
			this.boneSpaceTransform.mat[2][3] = aimat.c4();
			
			this.boneSpaceTransform.mat[3][0] = aimat.d1();
			this.boneSpaceTransform.mat[3][1] = aimat.d2();
			this.boneSpaceTransform.mat[3][2] = aimat.d3();
			this.boneSpaceTransform.mat[3][3] = aimat.d4();
			
			AIVertexWeight.Buffer weights = aibone.mWeights();	
			this.weights = new AIVertexWeight[weights.limit()];
			for(int k = 0; k < weights.limit(); k++) {
				this.weights[k] = weights.get(k);
			}
		}
	}
	
}
