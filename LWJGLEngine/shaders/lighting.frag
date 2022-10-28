#version 330 core
layout (location = 0) out vec4 lColor;
layout (location = 1) out vec4 lBrightness;

const int DIR_LIGHT = 0;
const int POINT_LIGHT = 1;
const int SPOT_LIGHT = 2;

in vec2 frag_uv;

uniform vec3 view_pos;
uniform sampler2D tex_position;
uniform sampler2D tex_normal;
uniform sampler2D tex_diffuse;
uniform sampler2D tex_specular;

//directional shadows
uniform float shadowMapNear;	// >= near
uniform float shadowMapFar;	// < far
uniform sampler2D shadowMap;
uniform sampler2D shadowBackfaceMap;	//is a backface or not
uniform mat4 lightSpace_matrix;

//point shadows
uniform samplerCube shadowCubemap;
uniform float shadowCubemapFar;

vec3 sampleOffsetDirections[20] = vec3[]
(
   vec3( 1,  1,  1), vec3( 1, -1,  1), vec3(-1, -1,  1), vec3(-1,  1,  1), 
   vec3( 1,  1, -1), vec3( 1, -1, -1), vec3(-1, -1, -1), vec3(-1,  1, -1),
   vec3( 1,  1,  0), vec3( 1, -1,  0), vec3(-1, -1,  0), vec3(-1,  1,  0),
   vec3( 1,  0,  1), vec3(-1,  0,  1), vec3( 1,  0, -1), vec3(-1,  0, -1),
   vec3( 0,  1,  1), vec3( 0, -1,  1), vec3( 0, -1, -1), vec3( 0,  1, -1)
);   

struct Light {
	int type;
	
	vec3 pos;
	vec3 dir;
	vec3 color;
	
	float ambientIntensity;
	
	float cutOff;
	float outerCutOff;
	
	float constant;
	float linear;
	float quadratic;
};	

uniform Light light;

float SampleShadowMap(sampler2D shadowMap, vec2 coords, float compare) {
	return step(texture2D(shadowMap, coords.xy).r, compare);
}

float SampleShadowMapLinear(sampler2D shadowMap, vec2 coords, float compare, vec2 texelSize) {
	vec2 pixelPos = coords/texelSize + vec2(0.5);
	vec2 fracPart = fract(pixelPos);
	vec2 startTexel = (pixelPos - fracPart) * texelSize;
	
	float blTexel = SampleShadowMap(shadowMap, startTexel, compare);
	float brTexel = SampleShadowMap(shadowMap, startTexel + vec2(texelSize.x, 0.0), compare);
	float tlTexel = SampleShadowMap(shadowMap, startTexel + vec2(0.0, texelSize.y), compare);
	float trTexel = SampleShadowMap(shadowMap, startTexel + texelSize, compare);
	
	float mixA = mix(blTexel, tlTexel, fracPart.y);
	float mixB = mix(brTexel, trTexel, fracPart.y);
	
	return mix(mixA, mixB, fracPart.x);
}

void main()
{	
	vec3 fragPos = texture(tex_position, frag_uv).rgb;
	float fragDepth = texture(tex_position, frag_uv).a;
	vec3 fragColor = texture(tex_diffuse, frag_uv).rgb;
	float fragAlpha = texture(tex_diffuse, frag_uv).a;
	float fragShininess = texture(tex_specular, frag_uv).a;
	vec3 fragSpec = texture(tex_specular, frag_uv).rgb;
	vec3 normal = texture(tex_normal, frag_uv).rgb;
	vec3 viewDir = normalize(view_pos - fragPos);
	
	if(fragShininess == 0){
		fragShininess = 1;
	}
	
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
    if(attenuation < 0.001){
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
    float spec = pow(max(dot(normal, halfwayDir), 0.0), fragShininess);	//specular strength
    
    //combine results
    float ambient  = light.ambientIntensity;
    float diffuse  = diff * (1 - light.ambientIntensity);
    vec3 specular = spec * fragSpec;
    
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
   	 	
   	 	float currentDepth = projCoords.z; 
   	 	float backfaceBias = texture(shadowBackfaceMap, projCoords.xy).r == 1? 0 : 0;
   	 	//float backfaceBias = 0;
   	 	
   	 	//float bias = max(0.0003 * (1.0 - dot(normal, lightDir)), 0.0005);  
   	 	float bias = 0.0001;
   	 	
   	 	vec2 texelSize = 1.0 / textureSize(shadowMap, 0);
   	 	int pcfSampleN = 2;
		for(int x = -pcfSampleN; x <= pcfSampleN; ++x) {
		    for(int y = -pcfSampleN; y <= pcfSampleN; ++y) {
		    	//pixeleated pcf shadows
		    	//float pcfDepth = texture(shadowMap, projCoords.xy + vec2(x, y) * texelSize).r; 
		        //shadow += currentDepth - bias > pcfDepth + backfaceBias? 1.0 : 0.0; 
		        
		        //linear soft shadows
		        shadow += SampleShadowMapLinear(shadowMap, projCoords.xy + vec2(x, y) * texelSize, currentDepth - bias, texelSize);
		    }    
		}
		shadow /= 25;
   	 	
   	 	if(projCoords.z > 1.0){
        	shadow = 0.0;
        }
	}
	
	if(light.type == POINT_LIGHT || light.type == SPOT_LIGHT){
		vec3 toFrag = lightDir * -1;
		float currentDepth = distance; 
		float sampledDepth = texture(shadowCubemap, toFrag).r * shadowCubemapFar;
		
		//float bias = 0.05;
		
		//shadow = currentDepth - bias > sampledDepth? 1.0 : 0.0;
		
		float bias   = 0.1;
		int samples  = 20;
		float viewDistance = length(view_pos - fragPos);
		float diskRadius = 0.003;
		for(int i = 0; i < samples; i++) {
		    float closestDepth = texture(shadowCubemap, toFrag + sampleOffsetDirections[i] * diskRadius).r;
		    closestDepth *= shadowCubemapFar;   // undo mapping [0;1]
		    if(currentDepth - bias > closestDepth) {
		        shadow += 1.0;
		    }
		}
		shadow /= float(samples);  
	}
	
	//account for shadows
	diffuse *= (1 - shadow);
	specular *= (1 - shadow);
	
	//compute color vector
	vec3 ambientColor  = light.color * fragColor * ambient;
    vec3 diffuseColor  = light.color * fragColor * diffuse;
    vec3 specularColor = light.color * specular;
    
	lColor = vec4(ambientColor + (diffuseColor + specularColor) * (1 - shadow), fragAlpha);
	//lColor = vec4(vec3(fragShininess), 1);
	lBrightness = vec4(vec3(ambient + diffuse + specular), 1);
    
} 

