#version 330 core
layout (location = 0) out vec4 color;

const int DIR_LIGHT = 0;
const int POINT_LIGHT = 1;
const int SPOT_LIGHT = 2;

in vec2 frag_uv;

uniform vec3 view_pos;
uniform sampler2D tex_position;
uniform sampler2D tex_normal;
uniform sampler2D tex_diffuse;

uniform float shadowMapNear;	// >= near
uniform float shadowMapFar;	// < far
uniform sampler2D shadowMap;
uniform mat4 lightSpace_matrix;

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

uniform Light light;

void main()
{	
	vec3 fragPos = texture(tex_position, frag_uv).rgb;
	float fragDepth = texture(tex_position, frag_uv).a;
	vec3 fragColor = texture(tex_diffuse, frag_uv).rgb;
	float fragSpec = texture(tex_diffuse, frag_uv).a;
	vec3 normal = texture(tex_normal, frag_uv).rgb;
	vec3 viewDir = normalize(view_pos - fragPos);
	
	//we do cascaded shadows in multiple passes to render the whole scene
	if(light.type == DIR_LIGHT && (fragDepth < shadowMapNear || fragDepth >= shadowMapFar)){
		discard;
	}
	
	//check if we can discard
	if(texture(tex_position, frag_uv).w == 0.0){
		discard;
	}
	
	float distance = 0;
	float attenuation = 1;
	if(light.type == SPOT_LIGHT || light.type == POINT_LIGHT){
    	//attenuation
		distance = length(light.pos - fragPos);
		attenuation = 1.0 / (light.constant + light.linear * distance + light.quadratic * (distance * distance));    
    }
    if(attenuation < 0.01){
		discard;
	}	
	
	//proceed with lighting
	vec3 lightDir = normalize(light.pos - fragPos);	//direction from fragment to light source
	if(light.type == DIR_LIGHT){
		lightDir = -light.dir;
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
    vec3 ambient  = light.color * fragColor * 0.15;
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
		ambient  *= attenuation; 
		diffuse  *= attenuation;
		specular *= attenuation;
    }
    
	//test if frag is in shadow
	float shadow = 0.0;
	if(light.type == DIR_LIGHT){
		vec4 lightSpace_frag_pos = lightSpace_matrix * vec4(fragPos, 1.0);
		// perform perspective divide
   	 	vec3 projCoords = lightSpace_frag_pos.xyz / lightSpace_frag_pos.w;
   	 	projCoords = projCoords * 0.5 + 0.5; //transform from [-1, 1] to [0, 1]
   	 	
   	 	float closestDepth = texture(shadowMap, projCoords.xy).r;   
   	 	float currentDepth = projCoords.z; 
   	 	
   	 	float bias = max(0.05 * (1.0 - dot(normal, lightDir)), 0.005);  
   	 	shadow = currentDepth - bias > closestDepth  ? 1.0 : 0.0;
   	 	
   	 	vec2 texelSize = 1.0 / textureSize(shadowMap, 0);
		for(int x = -1; x <= 1; ++x)
		{
		    for(int y = -1; y <= 1; ++y)
		    {
		        float pcfDepth = texture(shadowMap, projCoords.xy + vec2(x, y) * texelSize).r; 
		        shadow += currentDepth - bias > pcfDepth ? 1.0 : 0.0;        
		    }    
		}
		shadow /= 9.0;
   	 	
   	 	if(projCoords.z > 1.0){
        	shadow = 0.0;
        }
	}
	
	color = vec4(ambient + (diffuse + specular) * (1 - shadow), 1.0);
    
} 

