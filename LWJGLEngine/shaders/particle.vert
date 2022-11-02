#version 330 core
layout (location = 0) in vec3 pos;
layout (location = 1) in vec2 uv;
layout (location = 2) in vec3 normal;
layout (location = 3) in vec3 tangent;
layout (location = 4) in vec3 bitangent;
layout (location = 5) in mat4 md_matrix;
layout (location = 9) in vec3 colorID;
layout (location = 10) in vec4 material_diffuse;
layout (location = 11) in vec4 material_specular;
layout (location = 12) in vec4 material_shininess;

uniform mat4 pr_matrix;	//projection
uniform mat4 vw_matrix;	//view

uniform bool enableTexScaling;
uniform float texScaleFactor;

out vec3 frag_pos;
out vec2 frag_uv;
out mat3 TBN;

out vec4 frag_material_diffuse;
out vec4 frag_material_specular;
out float frag_material_shininess;

out vec3 frag_colorID;

noperspective out vec2 frag_screen_uv;

void main()
{
	//saving z rotation and scaling effects from original model matrix
	mat4 scale_rot_matrix = mat4(
		vec4(md_matrix[0].xyz, 0),
		vec4(md_matrix[1].xyz, 0),
		vec4(md_matrix[2].xyz, 0),
		0, 0, 0, 1
	);
	
	//we want to adjust the model matrix, so that the rotation part is the transpose of the view matrix, while preserving scale
	mat4 adjusted_md_matrix = md_matrix;
	
	mat3 transpose_view_rot = transpose(mat3(vw_matrix[0].xyz, vw_matrix[1].xyz, vw_matrix[2].xyz));
	adjusted_md_matrix[0].xyz = transpose_view_rot[0].xyz;
	adjusted_md_matrix[1].xyz = transpose_view_rot[1].xyz;
	adjusted_md_matrix[2].xyz = transpose_view_rot[2].xyz;
	
    gl_Position = pr_matrix * vw_matrix * adjusted_md_matrix * scale_rot_matrix * vec4(pos, 1.0);
    frag_screen_uv = ((gl_Position.xy / gl_Position.w) + vec2(1)) / 2;
    frag_pos = vec3(adjusted_md_matrix * vec4(pos, 1.0));
    frag_uv = uv;
    frag_colorID = colorID;
    if(!enableTexScaling){
    	frag_uv = uv * texScaleFactor;
    }
    
    frag_material_diffuse = material_diffuse;
    frag_material_specular = material_specular;
    frag_material_shininess = material_shininess.r;
    
    mat3 normalMatrix = transpose(inverse(mat3(adjusted_md_matrix)));
    vec3 T = normalize(normalMatrix * tangent);
    vec3 B = normalize(normalMatrix * bitangent);
    vec3 N = normalize(normalMatrix * normal);
    
    //convert from real to tangent space
   	TBN = transpose(mat3(T, B, N));
}