#version 330 core
layout (location = 0) out vec4 color;

in vec2 frag_uv;

uniform sampler2D tex_color;
uniform sampler2D tex_bloom;

uniform float exposure;
uniform float gamma;

float bloomThreshold = 1.0;

void main() {
	vec3 sampledColor = texture(tex_color, frag_uv).rgb;
	sampledColor += texture(tex_bloom, frag_uv).rgb;
	
    vec3 hdrColor = sampledColor;
    hdrColor = vec3(1.0) - exp(-hdrColor * exposure); // exposure tone mapping
    hdrColor = pow(hdrColor, vec3(1.0 / gamma)); // gamma correction 
	
	color = vec4(hdrColor, 1);
}


