#version 330 core
layout (location = 0) out vec4 color;

in vec2 frag_uv;

uniform sampler2D tex_color;

//TODO maybe implement a blur size uniform

uniform bool horizontal;
uniform float weight[5] = float[] (0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216);

void main() {
	vec2 tex_offset = 1.0 / textureSize(tex_color, 0); // gets size of single texel
    vec3 result = texture(tex_color, frag_uv).rgb * weight[0]; // current fragment's contribution
    if(horizontal) {
        for(int i = 1; i < 5; i++) {
            result += texture(tex_color, frag_uv + vec2(tex_offset.x * i, 0.0)).rgb * weight[i];
            result += texture(tex_color, frag_uv - vec2(tex_offset.x * i, 0.0)).rgb * weight[i];
        }
    }
    else {
        for(int i = 1; i < 5; i++) {
            result += texture(tex_color, frag_uv + vec2(0.0, tex_offset.y * i)).rgb * weight[i];
            result += texture(tex_color, frag_uv - vec2(0.0, tex_offset.y * i)).rgb * weight[i];
        }
    }
    color = vec4(result, 1.0);
}


