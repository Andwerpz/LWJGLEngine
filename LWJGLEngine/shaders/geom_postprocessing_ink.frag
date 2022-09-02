#version 330 core
layout (location = 0) out vec4 color;

in vec2 frag_uv;

uniform sampler2D tex_color;
uniform sampler2D tex_position;	//RGB: pos, A: depth
uniform sampler2D skybox;

float kernel[9] = float[](
    1, 2, 1,
    2, -12, 2,
    1, 2, 1  
);

int numColors = 6;

float bayer4[4 * 4] = float[] (
	0, 8, 2, 10,
	12, 4, 14, 6,
	3, 11, 1, 9,
	15, 7, 13, 5
);

float noiseScale = 0.5;

float windowWidth = 1920.0;
float windowHeight = 1080.0;
float aspectRatio = windowWidth / windowHeight;
float xOffset = 1.0 / 800.0;
float yOffset = xOffset * aspectRatio;

vec2 offsets[9] = vec2[](
    vec2(-xOffset,  yOffset), // top-left
    vec2( 0.0f,    yOffset), // top-center
    vec2( xOffset,  yOffset), // top-right
    vec2(-xOffset,  0.0f),   // center-left
    vec2( 0.0f,    0.0f),   // center-center
    vec2( xOffset,  0.0f),   // center-right
    vec2(-xOffset, -yOffset), // bottom-left
    vec2( 0.0f,   -yOffset), // bottom-center
    vec2( xOffset, -yOffset)  // bottom-right    
);

//returns geometry or skybox color depending on depth of texel
vec3 sampleColor(vec2 uv){
	float depth = texture(tex_position, uv).a;
	if(depth == 0){	//if fragment is part of the background
		return texture(skybox, uv).rgb;
	}
	return texture(tex_color, uv).rgb;
}

float sampleDepth(vec2 uv){
	uv.x = clamp(uv.x, 0, 1);
	uv.y = clamp(uv.y, 0, 1);
	return texture(tex_position, uv).a;
}

void main()
{
	float depth = texture(tex_position, frag_uv).a;
	vec3 sampledColor = sampleColor(frag_uv);
	
	int pixelX = int(frag_uv.x * windowWidth);
	int pixelY = int(frag_uv.y * windowHeight);
	float noise = bayer4[int(mod(pixelX, 4) + mod(pixelY, 4) * 4)] / 16.0;
	
	float r = floor(sampledColor.x * (numColors - 1) + (noise * noiseScale) + 0.5) / (numColors - 1);
	float g = floor(sampledColor.y * (numColors - 1) + (noise * noiseScale) + 0.5) / (numColors - 1);
	float b = floor(sampledColor.z * (numColors - 1) + (noise * noiseScale) + 0.5) / (numColors - 1);
	
	vec3 result = vec3(r, g, b);
	
	float edgeDetColor = 0;
	for(int i = 0; i < 9; i++){
		edgeDetColor += sampleDepth(frag_uv + offsets[i]) * kernel[i];
	}
	edgeDetColor = clamp(edgeDetColor, 0, 1);
	result -= vec3(edgeDetColor);
	
	color = vec4(result, 1.0);
}


