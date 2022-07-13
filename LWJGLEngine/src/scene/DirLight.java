package scene;

import graphics.Framebuffer;
import graphics.Shader;
import util.Vec3;

public class DirLight extends Light{
	
	private Framebuffer depthMap;
	
	public DirLight(Vec3 dir, Vec3 color) {
		this.dir = new Vec3(dir);
		this.color = new Vec3(color);
		this.depthMap = new Framebuffer(1024, 1024);
	}
	
	@Override
	public void bind(Shader shader, int index) {
		shader.setUniform1i("lights[" + index + "].type", Light.DIR_LIGHT);
		shader.setUniform3f("lights[" + index + "].dir", dir);
		shader.setUniform3f("lights[" + index + "].color", color);
		
	}
	
}
