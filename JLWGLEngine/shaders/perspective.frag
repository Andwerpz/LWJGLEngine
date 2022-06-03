#version 330 core
layout (location = 0) out vec4 color;

in vec2 frag_uv;

uniform sampler2D tex;

void main()
{
    color = texture(tex, frag_uv);
    //color = vec4(1.0, 1.0, 1.0, 1.0);
} 