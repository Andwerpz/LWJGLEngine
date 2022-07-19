#version 330 core
layout (location = 0) out vec4 shadowBackfaceMap;

void main()
{	
	//gl_FragDepth = gl_FragCoord.z;
	
	shadowBackfaceMap.r = gl_FrontFacing? 0 : 1;
	shadowBackfaceMap.gb = vec2(0);
	shadowBackfaceMap.a = 1;
} 

