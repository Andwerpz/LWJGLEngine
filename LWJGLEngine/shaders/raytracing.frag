#version 440 core
layout (location = 0) out vec4 out_tex_0;

uniform sampler2D render_tex_0;
uniform samplerCube skybox_tex;

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

struct Triangle {
	vec4 a;
	vec4 b;
	vec4 c;
	Material material;
};

uniform int numTriangles;
layout(std140, binding = 1) readonly buffer Triangles {
	Triangle triangles[];
};

uniform int numRenderedFrames;
uniform int windowWidth;
uniform int windowHeight;

uint rngState = uint(gl_FragCoord.x * windowWidth) * windowWidth * 13 + uint(gl_FragCoord.y * windowHeight) * 1203 + numRenderedFrames * 1838411;
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

HitInfo rayTriangle(Ray ray, Triangle triangle) {
	HitInfo ret = createHitInfo();
	
	vec3 t0 = triangle.a.xyz;
	vec3 t1 = triangle.b.xyz;
	vec3 t2 = triangle.c.xyz;
	
	vec3 d0 = normalize(t1 - t0);
	vec3 d1 = normalize(t2 - t1);
	vec3 d2 = normalize(t0 - t2);
	
	vec3 plane_origin = t0;
	vec3 plane_normal = normalize(cross(d0, d1));
	
	if(dot(plane_normal, ray.dir) > 0) {
		//ray dir and plane normal are facing in the same direction, so we shouldn't be able to see this 
		return ret;
	}

	//calculate intersection point between ray and plane defined by triangle
	float ray_dirStepRatio = dot(plane_normal, ray.dir);	// for each step in ray_dir, you go ray_dirStepRatio steps towards the plane
	// in plane_normal
	if (ray_dirStepRatio == 0) {
		// ray is parallel to plane, no intersection
		return ret;
	}
	
	float t = dot(plane_origin - ray.origin, plane_normal) / ray_dirStepRatio;
	if (t < 0) {
		// the plane intersection is behind the ray origin
		return ret;
	}
	
	vec3 plane_intersect = ray.origin + (ray.dir * t);

	// now, we just have to make sure that the intersection point is inside the triangle.
	vec3 n0 = cross(d0, plane_normal);
	vec3 n1 = cross(d1, plane_normal);
	vec3 n2 = cross(d2, plane_normal);
	
	if(dot(n0, t0 - plane_intersect) < 0 || dot(n1, t1 - plane_intersect) < 0 || dot(n2, t2 - plane_intersect) < 0) {
		//intersection point is outside of the triangle
		return ret;
	}
	
	ret.didHit = true;
	ret.dist = t;	//ray.dir has to be normalized on function call for this to work
	ret.hitPoint = plane_intersect;
	ret.hitNormal = plane_normal;
	ret.hitMaterial = triangle.material;

	return ret;
}

HitInfo calculateRayCollision(Ray ray) {
	HitInfo closestHit = createHitInfo();
	closestHit.dist = 10000000;	//very large number
	
	ray.dir = normalize(ray.dir);
	
	for(int i = 0; i < numSpheres; i++) {
		HitInfo hit = raySphere(ray, spheres[i]);
		if(hit.didHit && hit.dist < closestHit.dist) {
			closestHit = hit;
		}
	}
	
	for(int i = 0; i < numTriangles; i++){
		HitInfo hit = rayTriangle(ray, triangles[i]);
		if(hit.didHit && hit.dist < closestHit.dist) {
			closestHit = hit;
		}
	}	
	
	return closestHit;
}

uniform int maxBounceCount;

uniform vec3 sunDir;	//which direction do you have to face to see the sun
uniform float sunStrength; //how big and powerful is the sun? owo

uniform float ambientStrength;

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
			//sample skybox texture
			vec3 emittedLight = texture(skybox_tex, ray.dir).xyz * ambientStrength;	//skybox 'emits' ambient light
			
			vec3 sunLight = max(0, (dot(ray.dir, sunDir) - 0.99) * sunStrength) * vec3(1);
			emittedLight += sunLight;
			
			incomingLight += emittedLight * rayColor;
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

