package lwjglengine.v10.scene;

import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.graphics.Shader;
import myutils.v10.math.Vec3;

public class DirLight extends Light {

	public DirLight(Vec3 dir, Vec3 color, float ambientIntensity) {
		this.type = Light.DIR_LIGHT;
		this.dir = new Vec3(dir).normalize();
		this.color = new Vec3(color);
		this.ambientIntensity = ambientIntensity;
	}

	@Override
	public void bind(Shader shader, int index) {
		shader.setUniform1i("light.type", Light.DIR_LIGHT);
		shader.setUniform3f("light.dir", dir);
		shader.setUniform3f("light.color", color);
		shader.setUniform1f("light.ambientIntensity", this.ambientIntensity);
	}

}
