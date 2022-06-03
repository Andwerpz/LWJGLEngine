#version 330 core
layout (location = 0) in vec4 pos;
layout (location = 1) in vec2 uv;

out vec2 frag_uv;

void main()
{
    gl_Position = vec4(pos.x, pos.y, pos.z, 1.0);
    frag_uv = uv;
}