#version 330 core
layout (location = 0) out vec4 color;

const int DIR_LIGHT = 0;
const int POINT_LIGHT = 1;
const int SPOT_LIGHT = 2;
const int MAX_NR_LIGHTS = 100;

in vec2 frag_uv;

uniform vec3 view_pos;
uniform sampler2D tex_position;
uniform sampler2D tex_normal;
uniform sampler2D tex_diffuse;

struct Light {
	int type;
	
	vec3 pos;
	vec3 dir;
	vec3 color;
	
	float cutOff;
	float outerCutOff;
	
	float constant;
	float linear;
	float quadratic;
	
	sampler2D depthMap;
};	

uniform Light lights[MAX_NR_LIGHTS];
uniform int nrLights;

vec3 calculateLight(Light light, vec3 viewDir, vec3 normal, vec3 fragColor, float fragSpec, vec3 fragPos){

	vec3 lightDir = normalize(light.pos - fragPos);
	if(light.type == DIR_LIGHT){
		lightDir = light.dir;
	}
	
	//diffuse shading
	float diff = max(dot(normal, lightDir), 0.0);
	//specular shading
	vec3 halfwayDir = normalize(lightDir + viewDir); 
	float specularStrength = 64; 
    float spec = pow(max(dot(normal, halfwayDir), 0.0), 16.0);
	//vec3 reflectDir = reflect(-lightDir, normal);
    //float spec = pow(max(dot(viewDir, reflectDir), 0.0), specularStrength);
    
    //combine results
    vec3 ambient  = light.color * fragColor * 0.1;
    vec3 diffuse  = light.color * diff * fragColor;
    vec3 specular = light.color * spec * fragSpec;
    
    if(light.type == SPOT_LIGHT){
    	//calculate cutoff
    	float theta = dot(lightDir, normalize(-light.dir)); 
		float epsilon   = light.cutOff - light.outerCutOff;
		float intensity = clamp((theta - light.outerCutOff) / epsilon, 0.0, 1.0);    
		diffuse *= intensity;
		specular *= intensity;
    }
    
    if(light.type == SPOT_LIGHT || light.type == POINT_LIGHT){
    	//attenuation
		float distance = length(light.pos - fragPos);
		float attenuation = 1.0 / (light.constant + light.linear * distance + light.quadratic * (distance * distance));    
		ambient  *= attenuation; 
		diffuse  *= attenuation;
		specular *= attenuation;
    }
    
    return (ambient + diffuse + specular);
}

void main()
{
	if(texture(tex_position, frag_uv).w == 0.0){
		discard;
	}
	
	vec3 fragPos = texture(tex_position, frag_uv).rgb;
	vec3 fragColor = texture(tex_diffuse, frag_uv).rgb;
	float fragSpec = texture(tex_diffuse, frag_uv).a;
	vec3 normal = texture(tex_normal, frag_uv).rgb;
	vec3 viewDir = normalize(view_pos - fragPos);
	
	//proceed with lighting
	vec3 result = vec3(0);
	for(int i = 0; i < nrLights; i++){
		result += calculateLight(lights[i], viewDir, normal, fragColor, fragSpec, fragPos);
	}	
	
    color = vec4(result, 1.0);
} 

