package graphics;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL21.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL33.*;

import java.nio.FloatBuffer;

import main.Main;

public class Framebuffer {
	
	private int fbo;
	private int dbo, cbo, rbo;
	
	public Framebuffer() {
		fbo = glGenFramebuffers();
		glBindFramebuffer(GL_FRAMEBUFFER, fbo);
		
		dbo = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, dbo);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, Main.windowWidth, Main.windowHeight, 0, GL_DEPTH_COMPONENT, GL_UNSIGNED_BYTE, (FloatBuffer) null);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);  
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_DEPTH_COMPONENT, dbo, 0); 
		glBindTexture(GL_TEXTURE_2D, 0);
		
		cbo = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, cbo);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, Main.windowWidth, Main.windowHeight, 0, GL_RGB, GL_UNSIGNED_BYTE, (FloatBuffer) null);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);  
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, cbo, 0);  
		glBindTexture(GL_TEXTURE_2D, 0);
		
		rbo = glGenRenderbuffers();
		glBindRenderbuffer(GL_RENDERBUFFER, rbo);
		glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, Main.windowWidth, Main.windowHeight);
		glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, rbo); 
		glBindRenderbuffer(GL_RENDERBUFFER, 0);
		
		if(glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE) {
			System.out.println("Framebuffer " + fbo + " generated successfully");
		}
		
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
	}
	
	public int getColorbuffer() {
		return this.cbo;
	}
	
	public int getDepthbuffer() {
		return this.dbo;
	}
	
	public void bind() {
		glBindFramebuffer(GL_FRAMEBUFFER, fbo);
	}
	
	public void unbind() {
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
	}

}
