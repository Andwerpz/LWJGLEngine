#version 330 core
layout (location = 0) out vec4 color;

in vec3 frag_dir;

uniform samplerCube skybox;

void main() {	
	color = vec4(texture(skybox, normalize(frag_dir)).rgb, 1.0);
	//color = vec4(textureLod(skybox, normalize(frag_dir), 1).rgb, 1.0);
} 

