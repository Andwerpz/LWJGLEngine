#version 330 core
layout (location = 0) out vec4 color;

in vec2 frag_uv;

uniform sampler2D tex_color;
uniform float alpha;

void main() {
	vec3 result = texture(tex_color, frag_uv).rgb;
	color = vec4(result, alpha);
}


