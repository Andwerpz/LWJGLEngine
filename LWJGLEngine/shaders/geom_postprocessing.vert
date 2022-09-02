#version 330 core
layout (location = 0) in vec3 pos;
layout (location = 1) in vec2 uv;
layout (location = 2) in vec3 normal;
layout (location = 3) in vec3 tangent;
layout (location = 4) in vec3 bitangent;
layout (location = 5) in mat4 md_matrix;

out vec2 frag_uv;

void main()
{
    gl_Position = vec4(pos, 1.0);
    frag_uv = uv;
}