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

uniform mat4 pr_matrix;	//projection
uniform mat4 vw_matrix;	//view

uniform bool enableTexScaling;
uniform float texScaleFactor;

out vec3 frag_pos;
out vec2 frag_uv;
out mat3 TBN;

out vec4 frag_material_diffuse;
out vec4 frag_material_specular;
out float frag_material_shininess;

out vec3 frag_colorID;

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
    frag_pos = vec3(vert_transform * vec4(pos, 1.0));
    frag_uv = uv;
    frag_colorID = colorID.xyz;
    if(!enableTexScaling){
    	frag_uv = uv * texScaleFactor;
    }
    
    frag_material_diffuse = material_diffuse;
    frag_material_specular = material_specular;
    frag_material_shininess = material_shininess.r;
    
    mat3 normalMatrix = transpose(inverse(mat3(vert_transform)));
    vec3 T = normalize(normalMatrix * tangent);
    vec3 B = normalize(normalMatrix * bitangent);
    vec3 N = normalize(normalMatrix * normal);
    
    float det = determinant(normalMatrix);
    if(det < 0){	
    	//md_matrix somehow switched from left to right hand axes. 
    	//this should never really happen, but just in case ig. 
   		T *= -1;
   		N *= -1;
   	}
    
    //convert from real to tangent space
   	TBN = transpose(mat3(T, B, N));
   	
}