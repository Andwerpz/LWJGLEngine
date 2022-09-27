package scene;

import graphics.Framebuffer;
import graphics.Shader;
import util.Vec3;

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
