#version 330 core
layout (location = 3) out vec4 gColor;

uniform sampler2D tex_diffuse;
uniform sampler2D tex_specular;
uniform sampler2D tex_normal;
uniform sampler2D tex_displacement;
uniform sampler2D tex_position;

in mat4 inv_md_matrix;

in vec2 frag_uv;

void main()
{
	vec2 texelSize = 1.0 / vec2(textureSize(tex_position, 0));
  	vec2 screen_uv = gl_FragCoord.xy * texelSize;
  	
  	vec4 frag_pos = texture(tex_position, screen_uv).rgba;
  	vec4 frag_norm_pos = inv_md_matrix * vec4(frag_pos.rgb, 1.0);
  	
  	if(frag_norm_pos.x < 0 || frag_norm_pos.x > 1 ||
  		frag_norm_pos.y < 0 || frag_norm_pos.y > 1 ||
  		frag_norm_pos.z < 0 || frag_norm_pos.z > 1) {
  		discard;	
  	}
  	
  	vec2 uv = vec2(frag_norm_pos.x, frag_norm_pos.y);
  	vec3 frag_color = texture(tex_diffuse, uv).rgb;
	
	gColor = vec4(frag_color, 1.0);
} 

