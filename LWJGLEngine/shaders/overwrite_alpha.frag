#version 330 core
layout (location = 0) out vec4 color;

in vec2 frag_uv;

uniform sampler2D tex_color;
uniform sampler2D tex_alpha;
uniform float alpha;

void main() {
	vec3 result = texture(tex_color, frag_uv).rgb;
	float texAlpha = texture(tex_alpha, frag_uv).a;
	color = vec4(result, texAlpha * alpha);
}


