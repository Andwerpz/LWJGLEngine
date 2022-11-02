#version 330 core
layout (location = 0) in vec3 pos;
layout (location = 1) in vec2 uv;
layout (location = 2) in vec3 normal;
layout (location = 3) in vec3 tangent;
layout (location = 4) in vec3 bitangent;
layout (location = 5) in mat4 md_matrix;
layout (location = 9) in vec3 colorID;
layout (location = 10) in vec4 material_diffuse;
layout (location = 11) in vec4 material_specular;
layout (location = 12) in vec4 material_shininess;

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

void main()
{
    gl_Position = pr_matrix * vw_matrix * md_matrix * vec4(pos, 1.0);
    frag_pos = vec3(md_matrix * vec4(pos, 1.0));
    frag_uv = uv;
    frag_colorID = colorID;
    if(!enableTexScaling){
    	frag_uv = uv * texScaleFactor;
    }
    
    frag_material_diffuse = material_diffuse;
    frag_material_specular = material_specular;
    frag_material_shininess = material_shininess.r;
    
    mat3 normalMatrix = transpose(inverse(mat3(md_matrix)));
    vec3 T = normalize(normalMatrix * tangent);
    vec3 B = normalize(normalMatrix * bitangent);
    vec3 N = normalize(normalMatrix * normal);
    
    //convert from real to tangent space
   	TBN = transpose(mat3(T, B, N));
}