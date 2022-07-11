package scene;

import graphics.Shader;
import util.Vec3;

public class SpotLight extends Light{
	
	public SpotLight(Vec3 pos, Vec3 dir, Vec3 color, float cutOff, float outerCutOff, float constant, float linear, float quadratic) {
		this.pos = new Vec3(pos);
		this.dir = new Vec3(dir);
		this.color = new Vec3(color);
		this.cutOff = cutOff;
		this.outerCutOff = outerCutOff;
		this.constant = constant;
		this.linear = linear;
		this.quadratic = quadratic;
	}
	
	@Override
	public void bind(Shader shader, int index) {
		shader.setUniform1i("lights[" + index + "].type", Light.SPOT_LIGHT);
		shader.setUniform3f("lights[" + index + "].pos", this.pos);
		shader.setUniform3f("lights[" + index + "].dir", this.dir);
		shader.setUniform3f("lights[" + index + "].color", this.color);
		shader.setUniform1f("lights[" + index + "].cutOff", (float) Math.cos(Math.toRadians(this.cutOff)));
		shader.setUniform1f("lights[" + index + "].outerCutOff", (float) Math.cos(Math.toRadians(this.outerCutOff)));
		shader.setUniform1f("lights[" + index + "].constant", this.constant);
		shader.setUniform1f("lights[" + index + "].linear", this.linear);
		shader.setUniform1f("lights[" + index + "].quadratic", this.quadratic);
	}
	
}
