#version 330 core
layout (location = 0) in vec3 pos;
layout (location = 1) in vec2 uv;

uniform mat4 pr_matrix;	//projection
uniform mat4 md_matrix;	//model
uniform mat4 vw_matrix;	//view

out vec2 frag_uv;

void main()
{
    //gl_Position = ((pos * md_matrix) * vw_matrix) * pr_matrix;
    gl_Position = pr_matrix * vw_matrix * md_matrix * vec4(pos, 1.0);
    frag_uv = uv;
}