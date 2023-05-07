#version 330 core
layout (location = 0) out vec4 color;

in vec2 frag_uv;

uniform sampler2D tex_color;

void main() {
	float texAlpha = texture(tex_color, frag_uv).a;
	color = vec4(texAlpha, texAlpha, texAlpha, 1.0);
}


