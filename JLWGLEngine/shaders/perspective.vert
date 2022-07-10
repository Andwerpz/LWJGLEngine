#version 330 core
layout (location = 0) in vec3 pos;
layout (location = 1) in vec2 uv;
layout (location = 2) in vec3 normal;
layout (location = 3) in vec3 tangent;
layout (location = 4) in vec3 bitangent;
layout (location = 5) in mat4 md_matrix;

uniform mat4 pr_matrix;	//projection
uniform mat4 vw_matrix;	//view

out vec3 frag_pos;
out vec2 frag_uv;
out mat3 TBN;

void main()
{
    gl_Position = pr_matrix * vw_matrix * md_matrix * vec4(pos, 1.0);
    frag_pos = vec3(md_matrix * vec4(pos, 1.0));
    frag_uv = uv;
   	
   	//convert from tangent to real space. 
   	mat3 normalMatrix = transpose(inverse(mat3(md_matrix)));
    vec3 T = normalize(normalMatrix * tangent);
    vec3 N = normalize(normalMatrix * normal);
    vec3 B = normalize(normalMatrix * bitangent);
    
    //convert from real to tangent space
    //vec3 T = normalize(mat3(md_matrix) * tangent);
    //vec3 B = normalize(mat3(md_matrix) * bitangent);
    //vec3 N = normalize(mat3(md_matrix) * normal);
    
   	TBN = transpose(mat3(T, B, N));
}