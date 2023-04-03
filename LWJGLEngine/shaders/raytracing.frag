#version 440 core
layout (location = 0) out vec4 out_tex_0;

uniform sampler2D render_tex_0;

uniform vec3 camera_pos;
//uniform vec3 sun_dir;

in vec3 frag_dir;

struct Ray {
	vec3 origin;
	vec3 dir;
};

struct Material {
	vec4 diffuse;
	vec4 specular;
	vec4 emissive;
	vec4 attr;	//x = shininess, y = smoothness, z = specularProbability
};

struct HitInfo {
	bool didHit;
	float dist;
	vec3 hitPoint;
	vec3 hitNormal;
	Material hitMaterial;
};

struct Sphere {
	vec4 attr;	//xyz = center, w = radius;
	Material material;
};

uniform int numSpheres;
layout(std140, binding = 0) readonly buffer Spheres {
	Sphere spheres[];
};

uniform int numRenderedFrames;
uniform int windowWidth;
uniform int windowHeight;

uint rngState = uint(gl_FragCoord.x * windowWidth) * windowWidth + uint(gl_FragCoord.y * windowHeight) + numRenderedFrames * 18381391;
//https://www.shadertoy.com/view/XlGcRh
float randomValue() {
	rngState = rngState * 747796405 + 2891336453;
	uint result = ((rngState >> ((rngState >> 28u) + 4u)) ^ rngState) * 277803737u;
	result = (result >> 22) ^ result;
	return result / 4294967295.0;
}

float randomValueNormal() {
	float theta = 2 * 3.1415926 * randomValue();
	float rho = sqrt(-2 * log(randomValue()));
	return rho * cos(theta);
}

vec3 randomDirection() {
	float x = randomValueNormal();
	float y = randomValueNormal();
	float z = randomValueNormal();
	return normalize(vec3(x, y, z));
}

vec3 randomPointInSphere() {
	vec3 ret = randomDirection();
	ret *= sqrt(randomValue());
	return ret;
}

vec3 randomHemisphereDirection(vec3 normal) {
	vec3 dir = randomDirection();
	return dir * sign(dot(normal, dir));
}

vec2 randomPointInCircle() {
	float angle = randomValue() * 2 * 3.1415926;
	vec2 pointOnCircle = vec2(cos(angle), sin(angle));
	pointOnCircle *= sqrt(randomValue());
	return pointOnCircle;
}

Material createMaterial() {
	return Material(vec4(0), vec4(0), vec4(0), vec4(0));
}

HitInfo createHitInfo() {
	return HitInfo(false, 0, vec3(0), vec3(0), createMaterial());
}

HitInfo raySphere(Ray ray, Sphere sphere) {
	HitInfo ret = createHitInfo();
	vec3 sphereCenter = sphere.attr.xyz;
	float sphereRadius = sphere.attr.w;
	vec3 offsetRayOrigin = ray.origin - sphereCenter;
	
	float a = dot(ray.dir, ray.dir);
	float b = 2 * dot(offsetRayOrigin, ray.dir);
	float c = dot(offsetRayOrigin, offsetRayOrigin) - sphereRadius * sphereRadius;
	
	float d = b * b - 4 * a * c;
	
	if(d >= 0){
		float dist = (-b - sqrt(d)) / (2 * a);
		
		if(dist > 0) {
			ret.didHit = true;
			ret.dist = dist;
			ret.hitPoint = ray.origin + ray.dir * dist;
			ret.hitNormal = normalize(ret.hitPoint - sphereCenter);
			ret.hitMaterial = sphere.material;
		}
	}
	return ret;
}

HitInfo calculateRayCollision(Ray ray) {
	HitInfo closestHit = createHitInfo();
	closestHit.dist = 10000000;	//very large number
	
	for(int i = 0; i < numSpheres; i++) {
		HitInfo hit = raySphere(ray, spheres[i]);
		if(hit.didHit && hit.dist < closestHit.dist) {
			closestHit = hit;
		}
	}
	
	return closestHit;
}

uniform int maxBounceCount;
vec3 traceRay(Ray ray) {
	vec3 incomingLight = vec3(0);
	vec3 rayColor = vec3(1);

	for(int i = 0; i < maxBounceCount; i++){
		HitInfo hit = calculateRayCollision(ray);
		if(hit.didHit) {
			ray.origin = hit.hitPoint;
			
			vec3 diffuseDir = normalize(hit.hitNormal + randomDirection());
			vec3 specularDir = reflect(ray.dir, hit.hitNormal);
			
			Material m = hit.hitMaterial;
			float smoothness = m.attr.y;
			float specularProbability = m.attr.z;
			
			bool isSpecularBounce = specularProbability >= randomValue();
			
			ray.dir = mix(diffuseDir, specularDir, smoothness * float(isSpecularBounce));
			
			vec3 emittedLight = m.emissive.xyz * m.emissive.w;
			incomingLight += emittedLight * rayColor;
			rayColor *= mix(m.diffuse.xyz, m.specular.xyz, isSpecularBounce);
		}
		else {
			break;
		}
	}	
	
	return incomingLight;
}
uniform int numRaysPerPixel;
uniform float blurStrength;
uniform float defocusStrength;
uniform float focusDist;
uniform vec3 cameraRight;
uniform vec3 cameraUp;
void main() {   
	vec3 traceColor = vec3(0);
	for(int i = 0; i < numRaysPerPixel; i++) {
		vec3 focusPos = camera_pos + frag_dir * focusDist; 
		
		vec2 defocusJitter = randomPointInCircle() * defocusStrength / windowWidth;
		vec3 rayOrigin = camera_pos + cameraRight * defocusJitter.x + cameraUp * defocusJitter.y;
		
		vec2 blurJitter = randomPointInCircle() * blurStrength / windowWidth;
		vec3 rayDir = normalize(focusPos - rayOrigin) + cameraRight * blurJitter.x + cameraUp * blurJitter.y;
		
		Ray fragRay = Ray(rayOrigin, rayDir);
		
		traceColor += traceRay(fragRay);
	}
	traceColor /= numRaysPerPixel;
	
	vec4 oldColor = texture(render_tex_0, vec2(gl_FragCoord.x / windowWidth, gl_FragCoord.y / windowHeight)).xyzw;
	
	vec4 newColor = vec4(traceColor, 1);
	
	float weight = 1.0 / (numRenderedFrames + 1);
	vec4 avg = oldColor * (1.0 - weight) + newColor * weight;
	
	out_tex_0.rgba = avg;
} 

