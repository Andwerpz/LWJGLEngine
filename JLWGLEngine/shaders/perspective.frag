#version 330 core
layout (location = 0) out vec4 color;

in vec3 frag_pos;
in vec2 frag_uv;
in vec3 frag_normal;

uniform sampler2D tex;
uniform vec3 view_pos;

void main()
{
	vec3 lightPos = vec3(1.2, 1.1, -1.5);

	vec4 fragColor = texture(tex, frag_uv);
	
	if(fragColor.w == 0){	//alpha = 0
    	discard;
    }
	
	float ambientStrength = 0.1;
	vec4 ambient = fragColor * ambientStrength;
	
	vec3 norm = normalize(frag_normal);
	vec3 lightDir = normalize(lightPos - frag_pos);  
	float diffuseStrength = max(dot(norm, lightDir), 0.0);
	vec4 diffuse = fragColor * diffuseStrength;
	
	float specularStrength = 0.5;
	vec3 viewDir = normalize(view_pos - frag_pos);
	vec3 reflectDir = reflect(-lightDir, norm);  
	float spec = pow(max(dot(viewDir, reflectDir), 0.0), 32);
	vec4 specular = specularStrength * spec * fragColor;
	
    color = ambient + diffuse + specular;
} 