#version 330 core
layout (location = 0) in vec3 pos;
layout (location = 1) in vec2 uv;
layout (location = 2) in vec3 normal;
layout (location = 3) in vec3 tangent;
layout (location = 4) in vec3 bitangent;
layout (location = 5) in mat4 md_matrix;

uniform mat4 pr_matrix;
uniform mat4 vw_matrix;

out vec3 frag_pos;

void main()
{
    gl_Position = pr_matrix * vw_matrix * md_matrix * vec4(pos, 1.0);
    frag_pos = vec3(vw_matrix * md_matrix * vec4(pos, 1.0));
}