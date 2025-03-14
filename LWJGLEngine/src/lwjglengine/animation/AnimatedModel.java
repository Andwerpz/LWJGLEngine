package lwjglengine.animation;

import static org.lwjgl.assimp.Assimp.aiImportFile;
import static org.lwjgl.assimp.Assimp.aiProcess_JoinIdenticalVertices;
import static org.lwjgl.assimp.Assimp.aiProcess_Triangulate;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL21.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL41.*;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.opengl.GL44.*;
import static org.lwjgl.opengl.GL45.*;
import static org.lwjgl.opengl.GL46.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;

import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIAnimation;
import org.lwjgl.assimp.AIBone;
import org.lwjgl.assimp.AIMatrix4x4;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIMetaData;
import org.lwjgl.assimp.AIMetaDataEntry;
import org.lwjgl.assimp.AIMetaDataEntry.Buffer;
import org.lwjgl.assimp.AINode;
import org.lwjgl.assimp.AINodeAnim;
import org.lwjgl.assimp.AIQuatKey;
import org.lwjgl.assimp.AIQuaternion;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIString;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.assimp.AIVectorKey;
import org.lwjgl.assimp.AIVertexWeight;
import org.lwjgl.assimp.Assimp;

import lwjglengine.graphics.Material;
import lwjglengine.graphics.ShaderStorageBuffer;
import lwjglengine.graphics.TextureMaterial;
import lwjglengine.model.Model;
import lwjglengine.model.ModelInstance;
import lwjglengine.model.VertexArray;
import lwjglengine.util.BufferUtils;
import myutils.file.FileUtils;
import myutils.math.Mat4;
import myutils.math.MathUtils;
import myutils.math.Quaternion;
import myutils.math.Vec3;
import myutils.misc.Pair;

public class AnimatedModel extends Model {
	
	private Node[] nodes;	//root is nodes[0]
	private Animation[] animations;
	
	//bind before rendering
	private HashMap<Integer, ShaderStorageBuffer> nodeTransformBuffers;
	
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
		
		//for each mesh, figure out bones and stuff
		//each vertex needs to find its top 4 bone (node) weights. 
		//after you find it out, save it in the VertexArray
		//vertex ids are per mesh?
		PointerBuffer aimeshes = aiscene.mMeshes();
		for(int i = 0; i < aimeshes.limit(); i++) {
			AIMesh aimesh = AIMesh.create(aimeshes.get(i));
			int vertex_cnt = aimesh.mNumVertices();
			
			//load bones
			PointerBuffer aibones = aimesh.mBones();
			if(aibones == null) {
				continue;
			}
			Bone[] bones = new Bone[aibones.limit()];
			Mat4[] offset_mats = new Mat4[aibones.limit()];
			for(int j = 0; j < aibones.limit(); j++) {
				AIBone aibone = AIBone.create(aibones.get(j));
				bones[j] = new Bone(aibone, name_to_id, j);
				offset_mats[j] = bones[j].boneSpaceTransform;
			}
			
			//for each vertex, find which bones influence it
			ArrayList<VertexWeight>[] weights = new ArrayList[vertex_cnt];
			for(int j = 0; j < vertex_cnt; j++) {
				weights[j] = new ArrayList<>();
			}
			for(Bone b : bones) {
				for(VertexWeight w : b.weights) {
					weights[w.vertex_id].add(w); 
				}
			}
			
			//sort by weight, and take the top 4. Normalize their sum to 1
			//build BONE_IND_ATTRIB, BONE_WEIGHT_ATTRIB render buffers
			int[] bone_ind_attrib = new int[vertex_cnt * 4];
			int[] bone_node_ind_attrib = new int[vertex_cnt * 4];
			float[] bone_weight_attrib = new float[vertex_cnt * 4];
			for(int j = 0; j < vertex_cnt * 4; j++) {
				bone_ind_attrib[j] = -1;
				bone_node_ind_attrib[j] = -1;
			}
			for(int j = 0; j < vertex_cnt; j++) {
				if(weights[j].size() == 0) continue;
				weights[j].sort((a, b) -> -Float.compare(a.weight, b.weight));
				float sum = 0;
				for(int k = 0; k < Math.min(4, weights[j].size()); k++) {
					sum += weights[j].get(k).weight;
				}
				for(int k = 0; k < Math.min(4, weights[j].size()); k++) {
					bone_ind_attrib[j * 4 + k] = weights[j].get(k).bone_id;
					bone_node_ind_attrib[j * 4 + k] = weights[j].get(k).node_id;
					bone_weight_attrib[j * 4 + k] = weights[j].get(k).weight / sum;
				}
			}
			
			//tell the current mesh to update their render buffers
			this.getMeshes().get(i).setBoneBuffers(bone_ind_attrib, bone_node_ind_attrib, bone_weight_attrib, offset_mats);
		}
		
		this.nodeTransformBuffers = new HashMap<>();
	}
	
	@Override
	public void kill() {
		if(this.nodeTransformBuffers != null) {
			for(ShaderStorageBuffer ssbo : this.nodeTransformBuffers.values()) {
				ssbo.kill();
			}
			this.nodeTransformBuffers = null;
		}
		
		super.kill();
	}
	
	@Override
	protected void updateModelMats() {
		if(this.nodeTransformBuffers == null) {
			System.err.println("AnimatedModel : tried to update model mats of dead model");
			return;
		}
		
		//retrieve per-instance node transforms
		if (this.scenesNeedingUpdates.size() == 0) {
			return;
		}

		for (int scene : this.scenesNeedingUpdates) {
			if (this.sceneToID.get(scene) == null) {
				continue;
			}
			
			ArrayList<ModelInstance> instances = new ArrayList<>();
			for(long ID : this.sceneToID.get(scene)) {
				instances.add(Model.getModelInstanceFromID(ID));
			}
			
			Mat4[] node_transforms = new Mat4[this.getNodeCount() * instances.size()];
			for(int i = 0; i < instances.size(); i++) {
				if(!(instances.get(i) instanceof AnimatedModelInstance)) {
					continue;
				}
				AnimatedModelInstance inst = (AnimatedModelInstance) instances.get(i);
				Mat4[] cur_transforms = inst.getAnimationHandler().getNodeTransforms();
				if(cur_transforms == null) {
					for(int j = 0; j < this.getNodeCount(); j++) {
						node_transforms[i * this.getNodeCount() + j] = new Mat4();
					}
					inst.setNodeOffset(-1);
					continue;
				}
				for(int j = 0; j < cur_transforms.length; j++) {
					node_transforms[i * this.getNodeCount() + j] = cur_transforms[j];
				}
				inst.setNodeOffset(i * this.getNodeCount());
			}
			
			float[] data = new float[node_transforms.length * 16];
			FloatBuffer buf = BufferUtils.createFloatBuffer(node_transforms);
			for(int i = 0; i < data.length; i++) {
				data[i] = buf.get();
			}
			
			if(!this.nodeTransformBuffers.containsKey(scene)) {
				ShaderStorageBuffer buffer = new ShaderStorageBuffer();
				buffer.setUsage(GL_DYNAMIC_DRAW);
				this.nodeTransformBuffers.put(scene, buffer);
			}
			
			ShaderStorageBuffer transform_buffer = this.nodeTransformBuffers.get(scene);
			if(transform_buffer.getSize() / 4 == data.length) {
				transform_buffer.setSubData(data, 0);
			}
			else {
				transform_buffer.setData(data);
			}
			
			for(int i = 0; i < this.meshes.size(); i++) {
				VertexArray v = this.meshes.get(i);
				v.updateInstances(instances, i, scene);
			}
		}
		
		super.updateModelMats();
	}
	
	@Override
	protected void render(int scene) {
		if(!this.nodeTransformBuffers.containsKey(scene)) {
			//doesn't have anything to render
			return;
		}
		ShaderStorageBuffer node_transform_buffer = this.nodeTransformBuffers.get(scene);
		node_transform_buffer.bindToBase(VertexArray.NODE_TRANSFORM_SSBO_LOC);
		super.render(scene);
		node_transform_buffer.unbind();
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
	
	public static AnimatedModel loadAnimatedModelFile(File file, int flags) {
		String filepath = file.getAbsolutePath();
		String parentFilepath = file.getParent() + "\\";

		System.out.println("LOADING ANIMATED MODEL: " + file.getName());

		AIScene scene = aiImportFile(filepath, aiProcess_Triangulate | aiProcess_JoinIdenticalVertices | flags);

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
	
	public static AnimatedModel loadAnimatedModel(File file) {
		return AnimatedModel.loadAnimatedModelFile(file, 0);
	}
	
	public static AnimatedModel loadAnimatedModelFile(String filepath) throws IOException {
		return AnimatedModel.loadAnimatedModelFile(FileUtils.loadFile(filepath), 0);
	}
	
	public static AnimatedModel loadAnimatedModelFile(String filepath, int flags) {
		return AnimatedModel.loadAnimatedModelFile(FileUtils.loadFile(filepath), flags);
	}

	public static AnimatedModel loadAnimatedModelFileRelative(String relativeFilepath) throws IOException {
		return AnimatedModel.loadAnimatedModelFile(FileUtils.generateAbsoluteFilepath(relativeFilepath));
	}
	
	public static AnimatedModel loadAnimatedModelFileRelative(String relativeFilepath, int flags) throws IOException {
		return AnimatedModel.loadAnimatedModelFile(FileUtils.generateAbsoluteFilepath(relativeFilepath), flags);
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
			this.id = name_to_id.size();
			this.parent = _parent;
			name_to_id.put(ainode.mName().dataString(), this.id);
			System.out.println("AINODE : " + this.id + " " + ainode.mName().dataString());
			
			AIMetaData aimeta = ainode.mMetadata();
			if(aimeta != null) {
				System.out.println("METADATA");
				Buffer aiproperties = aimeta.mValues();
				AIString.Buffer aikeys = aimeta.mKeys();
				for (int i = 0; i < aiproperties.limit(); i++) {
			        AIMetaDataEntry entry = aiproperties.get(i);
			        AIString key = aimeta.mKeys().get(i); 
			        int type = entry.mType();          
			        ByteBuffer buf;
			        System.out.print(key.dataString() + " : ");

			        switch (type) {
			            case 0: // bool
			            {
			            	buf = ByteBuffer.allocate(1);
			            	entry.mData(buf);
			            	boolean val = buf.get() != 0;
			                System.out.println(val? "True" : "False");
			                break;
			            }
			            case 1: // int32
			            {
			            	buf = ByteBuffer.allocate(4);
			            	entry.mData(buf);
			            	int val = buf.getInt();
			                System.out.println(val);
			                break;
			            }
			            case 5: // String
			            {
			            	buf = ByteBuffer.allocate(64);
			            	entry.mData(buf);
			            	StringBuilder val = new StringBuilder();
			            	char c;
			            	while(buf.hasRemaining() && (c = buf.getChar()) != '\0') { 
			            		val.append(c);
			            	}
			                System.out.println(val.toString());
			                break;
			            }
			            default:
			                System.out.println("Unsupported metadata type: " + type);
			                break;
			        }
			    }
			}
			System.out.println();
			
			
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
		int node_id, bone_id;
		Bone[] children;
		VertexWeight[] weights;	//{vertex_id, weight}
		Mat4 boneSpaceTransform;	//converts a vertex from model to bone space
		
		public Bone(AIBone aibone, HashMap<String, Integer> name_to_id, int _bone_id) {
			this.node_id = name_to_id.get(aibone.mName().dataString());
			this.bone_id = _bone_id;
			
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
			this.weights = new VertexWeight[weights.limit()];
			for(int k = 0; k < weights.limit(); k++) {
				AIVertexWeight aiweight = weights.get(k);
				this.weights[k] = new VertexWeight(aiweight, this.node_id, this.bone_id);
			}
		}
	}
	
	class VertexWeight {
		int node_id, vertex_id, bone_id;
		float weight;
		
		public VertexWeight(AIVertexWeight aiweight, int _node_id, int _bone_id) {;
			this.node_id = _node_id;
			this.bone_id = _bone_id;
			this.vertex_id = aiweight.mVertexId();
			this.weight = aiweight.mWeight();
		}
	}
	
}
