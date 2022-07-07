package scene;

import graphics.Shader;
import util.Vec3;

public class PointLight extends Light{
	
	public PointLight(Vec3 pos, Vec3 color, float constant, float linear, float quadratic) {
		this.pos = new Vec3(pos);
		this.color = new Vec3(color);
		this.constant = constant;
		this.linear = linear;
		this.quadratic = quadratic;
	}
	
	@Override
	public void bind(Shader shader, int index) {
		shader.setUniform1i("lights[" + index + "].type", Light.POINT_LIGHT);
		shader.setUniform3f("lights[" + index + "].pos", this.pos);
		shader.setUniform3f("lights[" + index + "].color", this.color);
		shader.setUniform1f("lights[" + index + "].constant", this.constant);
		shader.setUniform1f("lights[" + index + "].linear", this.constant);
		shader.setUniform1f("lights[" + index + "].quadratic", this.quadratic);
	}
	
}
