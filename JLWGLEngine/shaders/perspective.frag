#version 330 core
layout (location = 0) out vec4 color;

in vec3 frag_pos;
in vec2 frag_uv;
in vec3 frag_normal;

uniform vec3 view_pos;
uniform sampler2D tex_diffuse;
uniform sampler2D tex_specular;

const int DIR_LIGHT = 0;
const int POINT_LIGHT = 1;
const int SPOT_LIGHT = 2;

const int MAX_NR_LIGHTS = 100;

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
};	

uniform Light lights[MAX_NR_LIGHTS];
uniform Light lights2[MAX_NR_LIGHTS];
uniform Light lights3[MAX_NR_LIGHTS];
uniform Light lights4[MAX_NR_LIGHTS];
uniform Light lights5[MAX_NR_LIGHTS];
uniform int nrLights;

vec3 calculateDirLight(Light light, vec3 viewDir)
{
	vec3 lightDir = normalize(-light.dir);
	
	// diffuse shading
    float diff = max(dot(frag_normal, lightDir), 0.0);
    // specular shading
    vec3 reflectDir = reflect(-lightDir, frag_normal);
    float specularStrength = 64;
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), specularStrength);
    
    // combine results
    vec3 ambient  = light.color * vec3(texture(tex_diffuse, frag_uv)) * 0.1;
    vec3 diffuse  = light.color * diff * vec3(texture(tex_diffuse, frag_uv));
    vec3 specular = light.color * spec * vec3(texture(tex_specular, frag_uv));
    return (ambient + diffuse + specular);
}

vec3 calculatePointLight(Light light, vec3 viewDir)
{
	vec3 lightDir = normalize(light.pos - frag_pos);
	
	//diffuse shading
	float diff = max(dot(frag_normal, lightDir), 0.0);
	//specular shading
	vec3 reflectDir = reflect(-lightDir, frag_normal);
    float specularStrength = 64;
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), specularStrength);
    
    //combine results
    vec3 ambient  = light.color * vec3(texture(tex_diffuse, frag_uv)) * 0.1;
    vec3 diffuse  = light.color * diff * vec3(texture(tex_diffuse, frag_uv));
    vec3 specular = light.color * spec * vec3(texture(tex_specular, frag_uv));
	
	//attenuation
	float distance = length(light.pos - frag_pos);
	float attenuation = 1.0 / (light.constant + light.linear * distance + light.quadratic * (distance * distance));    
	ambient  *= attenuation; 
	diffuse  *= attenuation;
	specular *= attenuation;
	
	return (ambient + diffuse + specular);
}

vec3 calculateSpotLight(Light light, vec3 viewDir)
{
	vec3 lightDir = normalize(light.pos - frag_pos);
	
	float theta = dot(lightDir, normalize(-light.dir));    
  		
	//diffuse shading
	float diff = max(dot(frag_normal, lightDir), 0.0);
	//specular shading
	vec3 reflectDir = reflect(-lightDir, frag_normal);
    float specularStrength = 64;
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), specularStrength);
    
    //combine results
    vec3 ambient  = light.color * vec3(texture(tex_diffuse, frag_uv)) * 0.1;
    vec3 diffuse  = light.color * diff * vec3(texture(tex_diffuse, frag_uv));
    vec3 specular = light.color * spec * vec3(texture(tex_specular, frag_uv));
    
    //calculate cutoff
	float epsilon   = light.cutOff - light.outerCutOff;
	float intensity = clamp((theta - light.outerCutOff) / epsilon, 0.0, 1.0);    
	diffuse *= intensity;
	specular *= intensity;
	
	//attenuation
	float distance = length(light.pos - frag_pos);
	float attenuation = 1.0 / (light.constant + light.linear * distance + light.quadratic * (distance * distance));    
	ambient  *= attenuation; 
	diffuse  *= attenuation;
	specular *= attenuation;
	
	return (ambient + diffuse + specular);
}

void main()
{

	vec4 fragColor = texture(tex_diffuse, frag_uv);
	
	if(fragColor.w == 0){	//alpha = 0
    	discard;
    }
	
	vec3 viewDir = normalize(view_pos - frag_pos);
	
	vec3 result = vec3(0);
	
	for(int i = 0; i < nrLights; i++){
		if(lights[i].type == DIR_LIGHT){
			result += calculateDirLight(lights[i], viewDir);
		}
		if(lights[i].type == POINT_LIGHT){
			result += calculatePointLight(lights[i], viewDir);
		}
		if(lights[i].type == SPOT_LIGHT){
			result += calculateSpotLight(lights[i], viewDir);
		}
	}	
	
    color = vec4(result, 1.0);
} 

