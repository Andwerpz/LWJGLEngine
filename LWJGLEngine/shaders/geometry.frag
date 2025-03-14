#version 430 core
layout (location = 0) out vec4 gPosition;
layout (location = 1) out vec4 gNormal;
layout (location = 2) out vec4 gSpecular;
layout (location = 3) out vec4 gColor;
layout (location = 4) out vec4 gColorID;

in vec3 frag_pos;
in vec2 frag_uv;
in vec3 frag_colorID;
in mat3 TBN;

in vec4 frag_material_diffuse;
in vec4 frag_material_specular;
in float frag_material_shininess;

uniform vec3 view_pos;
uniform sampler2D tex_diffuse;
uniform sampler2D tex_specular;
uniform sampler2D tex_normal;
uniform sampler2D tex_displacement;
uniform bool enableParallaxMapping;

vec2 ParallaxMapping(vec2 texCoords, vec3 viewDir)
{ 
	float height_scale = 0.2;
    //float height =  texture(tex_displacement, texCoords).r;    
    //vec2 p = viewDir.xy / viewDir.z * (height * height_scale);
    //return texCoords - p;    
    
    // number of depth layers
    const float numLayers = 8;
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

vec4 scaleWithMaterial(vec4 color, vec4 material) {
	vec4 ans = vec4(0);
	ans.x = color.r * material.r;
	ans.y = color.g * material.g;
	ans.z = color.b * material.b;
	ans.w = color.a * material.a;
	
	//premultiply alpha, texture already has premultiplied
	ans.x *= material.a;
	ans.y *= material.a;
	ans.z *= material.a;
	
	return ans;
}

void main()
{
	mat3 invTBN = transpose(TBN);

	//parallax mapping done in tangent space
	vec3 tangentViewPos = TBN * view_pos;	
	vec3 tangentFragPos = TBN * frag_pos;

	//offset texture coordinates with parallax mapping
	vec3 viewDir = normalize(tangentViewPos - tangentFragPos);
	vec2 texCoords = frag_uv;
	if(enableParallaxMapping){
		texCoords = ParallaxMapping(frag_uv, viewDir);
	}
	
	//calculate normal
	vec4 fragColor = texture(tex_diffuse, texCoords);
	vec3 normal = texture(tex_normal, texCoords).xyz;
	normal = normal * 2.0 - 1.0;
	normal = normalize(invTBN * normal);	//transform normal into world space
	
	if(fragColor.w == 0.0){	//alpha = 0
    	discard;
    }
	
    gColor.rgba = scaleWithMaterial(texture(tex_diffuse, texCoords).rgba, frag_material_diffuse.rgba).rgba;
    gPosition.rgb = frag_pos;
    gPosition.a = gl_FragCoord.z;
    gSpecular.rgb = scaleWithMaterial(texture(tex_specular, texCoords).rgba, frag_material_specular.rgba).rgb;
    gSpecular.a = frag_material_shininess;
    gNormal.rgb = normalize(normal);
    gNormal.a = 1;
    gColorID = vec4(frag_colorID / 255, 1);
} 

