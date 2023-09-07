#version 330 core
layout (location = 0) out vec4 color;

in vec2 frag_uv;

uniform int display_red;
uniform int display_green;
uniform int display_blue;
uniform int display_alpha;

uniform sampler2D tex_color;

void main() {
	vec4 sample_color = texture(tex_color, frag_uv).rgba;
	vec3 display_color = sample_color.rgb;
	if(display_alpha == 1){
		display_color = vec3(sample_color.a);
	}
	else {
		display_color *= vec3(display_red, display_green, display_blue);
	}
	color = vec4(display_color, 1);
}


