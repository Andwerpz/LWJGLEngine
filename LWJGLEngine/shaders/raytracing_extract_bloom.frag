#version 330 core
layout (location = 0) out vec4 brightColor;

in vec2 frag_uv;

uniform sampler2D tex_color;

uniform float bloomThreshold;

void main() {
	vec3 sampledColor = vec3(texture(tex_color, frag_uv).rgb);
	
	float brightness = dot(sampledColor.rgb, vec3(0.2126, 0.7152, 0.0722));	//dot with magic bloom vector
	if(brightness > bloomThreshold) {
		brightColor = vec4(sampledColor.rgb, 1);
	}
	else {
		brightColor = vec4(0);
	}
}


