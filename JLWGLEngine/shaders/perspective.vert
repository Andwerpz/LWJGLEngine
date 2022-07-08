#version 330 core
layout (location = 0) in vec3 pos;
layout (location = 1) in vec2 uv;
layout (location = 2) in vec3 normal;
layout (location = 3) in mat4 md_matrix;

uniform mat4 pr_matrix;	//projection
uniform mat4 vw_matrix;	//view

out vec3 frag_pos;
out vec2 frag_uv;
out vec3 frag_normal;

void main()
{
    gl_Position = pr_matrix * vw_matrix * md_matrix * vec4(pos, 1.0);
    frag_pos = vec3(md_matrix * vec4(pos, 1.0));
    frag_uv = uv;
   	frag_normal = normalize(vec3(md_matrix * vec4(normal, 0.0)));
}