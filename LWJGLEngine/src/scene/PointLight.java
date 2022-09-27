package scene;

import graphics.Shader;
import util.Vec3;

public class PointLight extends Light {

	public PointLight(Vec3 pos, Vec3 color, float ambientIntensity, float constant, float linear, float quadratic) {
		this.type = Light.POINT_LIGHT;
		this.pos = new Vec3(pos);
		this.color = new Vec3(color);
		this.ambientIntensity = ambientIntensity;
		this.constant = constant;
		this.linear = linear;
		this.quadratic = quadratic;
	}

	@Override
	public void bind(Shader shader, int index) {
		shader.setUniform1i("light.type", Light.POINT_LIGHT);
		shader.setUniform3f("light.pos", this.pos);
		shader.setUniform3f("light.color", this.color);
		shader.setUniform1f("light.ambientIntensity", this.ambientIntensity);
		shader.setUniform1f("light.constant", this.constant);
		shader.setUniform1f("light.linear", this.linear);
		shader.setUniform1f("light.quadratic", this.quadratic);
	}

}
