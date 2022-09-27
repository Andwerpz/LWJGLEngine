package scene;

import graphics.Shader;
import util.Vec3;

public class SpotLight extends Light {

	public SpotLight(Vec3 pos, Vec3 dir, Vec3 color, float ambientIntensity, float cutOff, float outerCutOff, float constant, float linear, float quadratic) {
		this.type = Light.SPOT_LIGHT;
		this.pos = new Vec3(pos);
		this.dir = new Vec3(dir);
		this.color = new Vec3(color);
		this.ambientIntensity = ambientIntensity;
		this.cutOff = cutOff;
		this.outerCutOff = outerCutOff;
		this.constant = constant;
		this.linear = linear;
		this.quadratic = quadratic;
	}

	@Override
	public void bind(Shader shader, int index) {
		shader.setUniform1i("light.type", Light.SPOT_LIGHT);
		shader.setUniform3f("light.pos", this.pos);
		shader.setUniform3f("light.dir", this.dir);
		shader.setUniform3f("light.color", this.color);
		shader.setUniform1f("light.ambientIntensity", this.ambientIntensity);
		shader.setUniform1f("light.cutOff", (float) Math.cos(Math.toRadians(this.cutOff)));
		shader.setUniform1f("light.outerCutOff", (float) Math.cos(Math.toRadians(this.outerCutOff)));
		shader.setUniform1f("light.constant", this.constant);
		shader.setUniform1f("light.linear", this.linear);
		shader.setUniform1f("light.quadratic", this.quadratic);
	}

}
