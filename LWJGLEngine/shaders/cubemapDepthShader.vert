#version 430 core
layout (location = 0) in vec3 pos;
layout (location = 1) in vec2 uv;
layout (location = 2) in vec3 normal;
layout (location = 3) in vec3 tangent;
layout (location = 4) in vec3 bitangent;
layout (location = 5) in mat4 md_matrix;
layout (location = 9) in vec4 colorID;
layout (location = 10) in vec4 material_diffuse;
layout (location = 11) in vec4 material_specular;
layout (location = 12) in vec4 material_shininess;
layout (location = 13) in ivec4 bone_ind;
layout (location = 14) in ivec4 bone_node_ind;
layout (location = 15) in vec4 bone_weight;

layout(binding = 0) buffer boneOffsetBuffer {
	mat4[] boneOffsets;
};

layout(binding = 1) buffer nodeOffsetBuffer {
	mat4[] nodeOffsets;
};	

uniform mat4 pr_matrix;
uniform mat4 vw_matrix;

out vec3 frag_pos;

void main() {
	int node_offset = floatBitsToInt(colorID.w);
	mat4 vert_transform = md_matrix;
	if(node_offset != -1){
		mat4 anim_transform = mat4(0.0);
		for(int i = 0; i < 4; i++){
			if(bone_ind[i] == -1) break;
			mat4 bone_transform = boneOffsets[bone_ind[i]];
			mat4 node_transform = nodeOffsets[node_offset + bone_node_ind[i]];
			anim_transform += bone_weight[i] * (node_transform * bone_transform);
		}
		vert_transform = md_matrix * anim_transform;
	}

    gl_Position = pr_matrix * vw_matrix * vert_transform * vec4(pos, 1.0);
    frag_pos = vec3(vw_matrix * vert_transform * vec4(pos, 1.0));
}