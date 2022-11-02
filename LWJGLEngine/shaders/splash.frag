#version 330 core
layout (location = 0) out vec4 color;

in vec2 frag_uv;

uniform sampler2D tex_color;
uniform float alpha;

void main() {
	vec3 result = vec3(texture(tex_color, frag_uv).rgb);
	float texAlpha = texture(tex_color, frag_uv).a;
	color = vec4(result, texAlpha * alpha);
}


