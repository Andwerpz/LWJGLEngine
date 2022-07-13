#version 330 core
layout (location = 0) out vec4 color;

in vec2 frag_uv;

uniform sampler2D tex_color;

void main()
{
    color = texture(tex_color, frag_uv);
} 

