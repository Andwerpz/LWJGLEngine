#version 330 core
layout (location = 0) out vec4 color_id;

in vec2 frag_uv;

uniform sampler2D tex_id;

uniform vec3 cur_color_id;

void main() {
	vec4 srcColorID = texture(tex_id, frag_uv).rgba;
	if(srcColorID.r == 0 && srcColorID.g == 0 && srcColorID.b == 0) {
		srcColorID = vec4(cur_color_id, 1);
	}
	color_id = srcColorID;
}


