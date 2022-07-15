#version 330 core

in vec3 frag_pos;

uniform float far;

void main()
{	
	float depth = length(frag_pos) / far;
	gl_FragDepth = depth;
} 

