package screen;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

import graphics.Framebuffer;
import graphics.Shader;
import graphics.Texture;
import main.Main;
import model.Model;

public class UIScreen extends Screen {
	
	private Framebuffer geometryBuffer;
	
	private Texture geometryPositionMap;	//RGB: pos, A: depth
	private Texture geometryNormalMap;		//RGB: normal, A: specular
	private Texture geometryColorMap;		//RGB: color, A: alpha
	private Texture geometryColorIDMap;		//RGB: colorID
	
	public UIScreen() {
		super();
		
		this.geometryBuffer = new Framebuffer(Main.windowWidth, Main.windowHeight);
		this.geometryPositionMap = new Texture(GL_RGBA16F, Main.windowWidth, Main.windowHeight, GL_RGBA, GL_FLOAT);
		this.geometryNormalMap = new Texture(GL_RGBA16F, Main.windowWidth, Main.windowHeight, GL_RGBA, GL_FLOAT);
		this.geometryColorMap = new Texture(GL_RGBA, Main.windowWidth, Main.windowHeight, GL_RGBA, GL_FLOAT);
		this.geometryColorIDMap = new Texture(GL_RGBA, Main.windowWidth, Main.windowHeight, GL_RGBA, GL_FLOAT);
		this.geometryBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.geometryPositionMap.getID());
		this.geometryBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, this.geometryNormalMap.getID());
		this.geometryBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT2, GL_TEXTURE_2D, this.geometryColorMap.getID());
		this.geometryBuffer.bindTextureToBuffer(GL_COLOR_ATTACHMENT3, GL_TEXTURE_2D, this.geometryColorIDMap.getID());
		this.geometryBuffer.addDepthBuffer();
		this.geometryBuffer.setDrawBuffers(new int[] {GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1, GL_COLOR_ATTACHMENT2, GL_COLOR_ATTACHMENT3});
		this.geometryBuffer.isComplete();
	}

	@Override
	public void render(Framebuffer outputBuffer, int scene) {
		this.geometryBuffer.bind();
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_CULL_FACE);
		glDisable(GL_BLEND);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer
		
		Shader.GEOMETRY.enable();
		Shader.GEOMETRY.setUniformMat4("pr_matrix", camera.getProjectionMatrix());
		Shader.GEOMETRY.setUniformMat4("vw_matrix", camera.getViewMatrix());
		Shader.GEOMETRY.setUniform3f("view_pos", camera.getPos());
		Model.renderModels(scene);
		
		outputBuffer.bind();
		glDisable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA); 
		
		Shader.SPLASH.enable();
		Shader.SPLASH.setUniform1f("alpha", 1f);
		geometryColorMap.bind(GL_TEXTURE0);
		screenQuad.render();
	}

}
