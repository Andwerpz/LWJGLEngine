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

flat out mat4 inv_md_matrix;

out vec2 frag_uv;
noperspective out vec2 frag_screen_uv;

out vec4 frag_material_diffuse;
out vec4 frag_material_specular;
out float frag_material_shininess;

void main()
{
    gl_Position = pr_matrix * vw_matrix * md_matrix * vec4(pos, 1.0);
    inv_md_matrix = inverse(md_matrix);
    
    frag_uv = uv;
    frag_screen_uv = ((gl_Position.xy / gl_Position.w) + vec2(1)) / 2;
    
    frag_material_diffuse = material_diffuse;
    frag_material_specular = material_specular;
    frag_material_shininess = material_shininess.r;
}