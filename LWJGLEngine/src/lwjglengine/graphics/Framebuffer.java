package lwjglengine.graphics;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL21.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL33.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import lwjglengine.input.MouseInput;
import lwjglengine.main.Main;
import lwjglengine.model.Model;
import lwjglengine.util.BufferUtils;
import myutils.math.Vec2;
import myutils.math.Vec3;
import myutils.misc.Triple;

public class Framebuffer {
	// after creating a new instance, you need to add either one color buffer, or a render buffer
	// this is so that the gpu can actually write to the buffer. 
	// check if complete using the isComplete function.

	private int fbo;
	private HashMap<Integer, Triple<Integer, Integer, Integer>> buffers; //bound location, {textureType, textureID, mipLevel}

	private int width, height;

	public Framebuffer(int width, int height) {
		this.width = width;
		this.height = height;

		this.buffers = new HashMap<>();

		this.fbo = glGenFramebuffers();
	}

	public Framebuffer(int framebufferID) {
		this.fbo = framebufferID;
		this.buffers = new HashMap<>();
	}

	public int getWidth() {
		return this.width;
	}

	public int getHeight() {
		return this.height;
	}

	public int addRenderBuffer() {
		this.bind();
		int renderBuffer = glGenRenderbuffers();
		glBindRenderbuffer(GL_RENDERBUFFER, renderBuffer);
		glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);
		glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, renderBuffer);
		glBindRenderbuffer(GL_RENDERBUFFER, 0);
		this.unbind();

		this.buffers.put(GL_RENDERBUFFER, new Triple<>(GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, renderBuffer));
		return renderBuffer;
	}

	public int addColorBuffer(int internalFormat, int dataFormat, int dataType, int layoutLocation) {
		this.bind();
		int id = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, id);
		glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, dataFormat, dataType, (FloatBuffer) null);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glFramebufferTexture2D(GL_FRAMEBUFFER, layoutLocation, GL_TEXTURE_2D, id, 0);
		glBindTexture(GL_TEXTURE_2D, 0);
		this.unbind();

		this.buffers.put(layoutLocation, new Triple<>(GL_TEXTURE_2D, id, 0));
		return id;
	}

	public int addDepthBuffer() {
		this.bind();
		int depthBuffer = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, depthBuffer);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, width, height, 0, GL_DEPTH_COMPONENT, GL_FLOAT, (FloatBuffer) null);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
		float borderColor[] = { 1.0f, 1.0f, 1.0f, 1.0f };
		glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, borderColor);
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthBuffer, 0);
		glBindTexture(GL_TEXTURE_2D, 0);
		this.unbind();

		this.buffers.put(GL_DEPTH_ATTACHMENT, new Triple<>(GL_TEXTURE_2D, depthBuffer, 0));
		return depthBuffer;
	}

	public void setDrawBuffers(int[] which) {
		this.bind();
		glDrawBuffers(BufferUtils.createIntBuffer(which));
	}

	public void bindTextureToBuffer(int bufferType, int textureType, int textureID) {
		this.bind();
		glFramebufferTexture2D(GL_FRAMEBUFFER, bufferType, textureType, textureID, 0);
		this.buffers.put(bufferType, new Triple<>(textureType, textureID, 0));
	}

	public void bindTextureToBuffer(int bufferType, int textureType, int textureID, int mipLevel) {
		this.bind();
		glFramebufferTexture2D(GL_FRAMEBUFFER, bufferType, textureType, textureID, mipLevel);
		this.buffers.put(bufferType, new Triple<>(textureType, textureID, mipLevel));
	}

	public void unbindTextureAtBuffer(int bufferType) {
		if (!this.buffers.containsKey(bufferType)) {
			return;
		}

		int textureType = this.buffers.get(bufferType).first;
		int mipLevel = this.buffers.get(bufferType).third;
		this.bind();
		glFramebufferTexture2D(GL_FRAMEBUFFER, bufferType, textureType, 0, mipLevel);
		if (this.buffers.containsKey(bufferType)) {
			this.buffers.remove(bufferType);
		}
	}

	public boolean isComplete() {
		this.bind();
		boolean ans = glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE;
		if (ans) {
			System.out.println("Framebuffer " + fbo + " generated successfully");
		}
		else {
			System.err.println("Framebuffer " + fbo + " generation unsuccessful");
		}
		return ans;
	}

	public Vec3 sampleColorAtPoint(int x, int y, int attachmentID) {
		this.bind();
		glReadBuffer(attachmentID);
		ByteBuffer pixels = BufferUtils.createByteBuffer(4);
		glReadPixels(x, y, 1, 1, GL_RGB, GL_UNSIGNED_BYTE, pixels);
		return new Vec3((pixels.get(0) & 0xFF), (pixels.get(1) & 0xFF), (pixels.get(2) & 0xFF));
	}

	public int getRenderBuffer() {
		return this.buffers.get(GL_RENDERBUFFER).second;
	}

	public int getDepthBuffer() {
		return this.buffers.get(GL_DEPTH_ATTACHMENT).second;
	}

	public void bind() {
		glBindFramebuffer(GL_FRAMEBUFFER, fbo);
	}

	public void unbind() {
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
	}

	//also kills any textures associated with it. 
	public void kill() {
		for (int layoutLocation : this.buffers.keySet()) {
			int id = this.buffers.get(layoutLocation).second;
			glDeleteTextures(BufferUtils.createIntBuffer(new int[] { id }));
		}
		glDeleteFramebuffers(BufferUtils.createIntBuffer(new int[] { this.fbo }));
		System.out.println("KILL FRAMEBUFFER " + this.fbo);
	}

}
