#version 330 core
layout (location = 0) out vec4 color;

const int DIR_LIGHT = 0;
const int POINT_LIGHT = 1;
const int SPOT_LIGHT = 2;
const int MAX_NR_LIGHTS = 100;

in vec3 frag_pos;
in vec2 frag_uv;
in mat3 TBN;

uniform vec3 view_pos;
uniform sampler2D tex_diffuse;
uniform sampler2D tex_specular;
uniform sampler2D tex_normal;
uniform sampler2D tex_displacement;

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
uniform int nrLights;

vec3 calculateLight(Light light, vec3 viewDir, vec3 normal, vec2 texCoords){

	vec3 tangentLightPos = TBN * light.pos;
	vec3 tangentFragPos = TBN * frag_pos;
	vec3 tangentLightDir = normalize(TBN * light.dir);

	vec3 lightDir = normalize(tangentLightPos - tangentFragPos);
	if(light.type == DIR_LIGHT){
		lightDir = tangentLightDir;
	}
	
	//diffuse shading
	float diff = max(dot(normal, lightDir), 0.0);
	//specular shading
	vec3 reflectDir = reflect(-lightDir, normal);
    float specularStrength = 64;
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), specularStrength);
    
    //combine results
    vec3 ambient  = light.color * vec3(texture(tex_diffuse, texCoords)) * 0.1;
    vec3 diffuse  = light.color * diff * vec3(texture(tex_diffuse, texCoords));
    vec3 specular = light.color * spec * vec3(texture(tex_specular, texCoords));
    
    if(light.type == SPOT_LIGHT){
    	//calculate cutoff
    	float theta = dot(lightDir, normalize(-tangentLightDir)); 
		float epsilon   = light.cutOff - light.outerCutOff;
		float intensity = clamp((theta - light.outerCutOff) / epsilon, 0.0, 1.0);    
		diffuse *= intensity;
		specular *= intensity;
    }
    
    if(light.type == SPOT_LIGHT || light.type == POINT_LIGHT){
    	//attenuation
		float distance = length(tangentLightPos - tangentFragPos);
		float attenuation = 1.0 / (light.constant + light.linear * distance + light.quadratic * (distance * distance));    
		ambient  *= attenuation; 
		diffuse  *= attenuation;
		specular *= attenuation;
    }
    
    return (ambient + diffuse + specular);
}

vec2 ParallaxMapping(vec2 texCoords, vec3 viewDir)
{ 
	float height_scale = 0.2;
    //float height =  texture(tex_displacement, texCoords).r;    
    //vec2 p = viewDir.xy / viewDir.z * (height * height_scale);
    //return texCoords - p;    
    
    
    // number of depth layers
    const float numLayers = 320;
    // calculate the size of each layer
    float layerDepth = 1.0 / numLayers;
    // depth of current layer
    float currentLayerDepth = 0.0;
    // the amount to shift the texture coordinates per layer (from vector P)
    vec2 P = viewDir.xy * height_scale; 
    vec2 deltaTexCoords = P / numLayers;
    
    // get initial values
	vec2  currentTexCoords     = texCoords;
	float currentDepthMapValue = texture(tex_displacement, currentTexCoords).r;
	  
	while(currentLayerDepth < currentDepthMapValue)
	{
	    // shift texture coordinates along direction of P
	    currentTexCoords -= deltaTexCoords;
	    // get depthmap value at current texture coordinates
	    currentDepthMapValue = texture(tex_displacement, currentTexCoords).r;  
	    // get depth of next layer
	    currentLayerDepth += layerDepth;  
	}
	
	// get texture coordinates before collision (reverse operations)
	vec2 prevTexCoords = currentTexCoords + deltaTexCoords;
	
	// get depth after and before collision for linear interpolation
	float afterDepth  = currentDepthMapValue - currentLayerDepth;
	float beforeDepth = texture(tex_displacement, prevTexCoords).r - currentLayerDepth + layerDepth;
	 
	// interpolation of texture coordinates
	float weight = afterDepth / (afterDepth - beforeDepth);
	vec2 finalTexCoords = prevTexCoords * weight + currentTexCoords * (1.0 - weight);
	
	return finalTexCoords; 
} 

void main()
{
	//all calculations done in tangent space now
	vec3 tangentViewPos = TBN * view_pos;
	vec3 tangentFragPos = TBN * frag_pos;

	//offset texture coordinates with parallax mapping
	vec3 viewDir = normalize(tangentViewPos - tangentFragPos);
	vec2 texCoords = ParallaxMapping(frag_uv, viewDir);
	//vec2 texCoords = frag_uv;
	
	//proceed with lighting
	vec4 fragColor = texture(tex_diffuse, texCoords);
	vec3 normal = texture(tex_normal, texCoords).xyz;
	normal = normalize(normal * 2.0 - 1.0);
	
	if(fragColor.w == 0){	//alpha = 0
    	discard;
    }
	
	vec3 result = vec3(0);
	
	for(int i = 0; i < nrLights; i++){
		result += calculateLight(lights[i], viewDir, normal, texCoords);
	}	
	
    color = vec4(result, 1.0);
} 

