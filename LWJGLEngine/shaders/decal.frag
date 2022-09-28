#version 330 core
layout (location = 2) out vec4 gSpecular;
layout (location = 3) out vec4 gColor;

uniform sampler2D tex_diffuse;
uniform sampler2D tex_specular;
uniform sampler2D tex_normal;
uniform sampler2D tex_displacement;
uniform sampler2D tex_position;

flat in mat4 inv_md_matrix;

in vec2 frag_uv;
noperspective in vec2 frag_screen_uv;

in vec4 frag_material_diffuse;
in vec4 frag_material_specular;
in float frag_material_shininess;

//i changed the gbuffers to be 32bit floats instead of 16 bit due to the decals not having high enough precision

vec4 scaleWithMaterial(vec4 color, vec4 material) {
	vec4 ans = vec4(0);
	ans.x = color.r * material.r;
	ans.y = color.g * material.g;
	ans.z = color.b * material.b;
	ans.w = color.a * material.a;
	return ans;
}

void main() {
  	vec2 screen_uv = frag_screen_uv;
  	
  	vec4 frag_pos = texture(tex_position, screen_uv).rgba;
  	vec4 frag_norm_pos = inv_md_matrix * vec4(frag_pos.rgb, 1.0);
  	
  	if(frag_norm_pos.x < 0 || frag_norm_pos.x > 1 ||
  		frag_norm_pos.y < 0 || frag_norm_pos.y > 1 ||
  		frag_norm_pos.z < 0 || frag_norm_pos.z > 1) {
  		discard;	
  	}
  	
  	vec2 uv = vec2(frag_norm_pos.x, frag_norm_pos.y);
  	vec4 frag_color = texture(tex_diffuse, uv).rgba;
  	
  	if(frag_color.a == 0) {
  		discard;
  	}
	
	gColor.rgba = scaleWithMaterial(texture(tex_diffuse, uv).rgba, frag_material_diffuse.rgba).rgba;
	gSpecular.rgb = scaleWithMaterial(texture(tex_specular, uv).rgba, frag_material_specular.rgba).rgb;
	gSpecular.a = frag_material_shininess;
} 

