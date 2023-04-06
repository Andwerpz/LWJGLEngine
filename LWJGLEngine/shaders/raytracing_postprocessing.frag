#version 330 core
layout (location = 0) out vec4 color;

in vec2 frag_uv;

uniform sampler2D tex_color;

uniform float exposure;
uniform float gamma;

void main() {
	vec3 sampledColor = vec3(texture(tex_color, frag_uv).rgb);
	
    vec3 hdrColor = sampledColor;
    hdrColor = vec3(1.0) - exp(-hdrColor * exposure); // exposure tone mapping
    hdrColor = pow(hdrColor, vec3(1.0 / gamma)); // gamma correction 
	
	color = vec4(hdrColor, 1);
}


