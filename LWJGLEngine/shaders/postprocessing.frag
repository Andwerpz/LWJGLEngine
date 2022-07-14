#version 330 core
layout (location = 0) out vec4 color;

in vec2 frag_uv;

uniform sampler2D tex_color;
uniform sampler2D tex_position;	//RGB: pos, A: depth
uniform sampler2D skybox;

void main()
{
	float depth = texture(tex_position, frag_uv).a;
	
	vec3 result = texture(tex_color, frag_uv).rgb;
	
	if(depth == 0){	//if fragment is part of the background
		result = texture(skybox, frag_uv).rgb;
	}
	
	color = vec4(result, 1.0);
} 

