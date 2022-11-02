package main;

import static org.lwjgl.glfw.GLFW.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.*;

import static org.lwjgl.openal.ALC10.*;

import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;

import graphics.Shader;
import input.KeyboardInput;
import input.MouseInput;
import model.AssetManager;
import scene.Scene;
import screen.Screen;
import screen.ScreenQuad;
import state.StateManager;
import ui.UIElement;
import util.FontUtils;
import util.Mat4;

import static org.lwjgl.opengl.GL.*;

public class Main implements Runnable {
	// seems like the maximum size the viewport can be is equal to the dimensions of the window

	private static int windowedWidth = 1280;
	private static int windowedHeight = 720;

	public static int windowWidth = 1280;
	public static int windowHeight = 720;

	private Thread thread;
	private boolean running = false;

	public static long window;
	private boolean fullscreen = false;

	public static final float ASPECT_RATIO = (float) Main.windowWidth / (float) Main.windowHeight;
	public static final float FOV = (float) Math.toRadians(90f); // vertical FOV

	public static long selectedEntityID = 0;

	public long deltaMillis = 0;
	public int lastSecondUpdates = 0;
	public int lastSecondFrames = 0;

	private StateManager sm;

	private long audioContext;
	private long audioDevice;

	public void start() {
		running = true;
		thread = new Thread(this, "Game");
		thread.start();
	}

	private void init() {
		//initializing audio device. 
		//make sure to do this before createCapabilities();
		String defaultDeviceName = alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER);
		audioDevice = alcOpenDevice(defaultDeviceName);
		int[] attributes = { 0 };
		audioContext = alcCreateContext(audioDevice, attributes);
		alcMakeContextCurrent(audioContext);

		ALCCapabilities alcCapabilities = ALC.createCapabilities(audioDevice);
		ALCapabilities alCapabilities = AL.createCapabilities(alcCapabilities);

		assert alCapabilities.OpenAL10 : "Audio library not supported";

		if (!glfwInit()) {
			// window failed to init
			return;
		}

		glfwWindowHint(GLFW_RESIZABLE, GL_FALSE);
		long primaryMonitor = glfwGetPrimaryMonitor();
		window = glfwCreateWindow(windowWidth, windowHeight, "LWJGL", fullscreen ? primaryMonitor : NULL, NULL);

		if (window == NULL) {
			return;
		}

		GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
		glfwSetWindowPos(window, (vidmode.width() - windowWidth) / 2, (vidmode.height() - windowHeight) / 2);
		glfwMakeContextCurrent(window);
		glfwShowWindow(window);

		glfwSetKeyCallback(window, new KeyboardInput());
		glfwSetMouseButtonCallback(window, new MouseInput());

		createCapabilities();
		glClearColor(0f, 0f, 0f, 0f);
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);
		System.out.println("OpenGL : " + glGetString(GL_VERSION));

		// INIT
		FontUtils.loadFonts();
		Shader.init();
		AssetManager.init();
		Scene.init();
		this.sm = new StateManager();
	}

	public void toggleFullscreen() {
		GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
		long primaryMonitor = glfwGetPrimaryMonitor();
		if (this.fullscreen) {
			Main.windowWidth = Main.windowedWidth;
			Main.windowHeight = Main.windowedHeight;
			glfwSetWindowMonitor(window, NULL, 0, 0, Main.windowWidth, Main.windowHeight, vidmode.refreshRate());
			glfwSetWindowSize(window, Main.windowWidth, Main.windowHeight);
			glfwSetWindowPos(window, (vidmode.width() - windowWidth) / 2, (vidmode.height() - windowHeight) / 2);
		}
		else {
			glfwSetWindowMonitor(window, primaryMonitor, 0, 0, vidmode.width(), vidmode.height(), vidmode.refreshRate());
			Main.windowWidth = vidmode.width();
			Main.windowHeight = vidmode.height();
		}

		System.out.println("NEW RESOLUTION : " + Main.windowWidth + " " + Main.windowHeight);

		UIElement.shouldAlignUIElements = true;

		Screen.rebuildAllBuffers();
		this.sm.buildBuffers();
		glViewport(0, 0, Main.windowWidth, Main.windowHeight);
		this.fullscreen = !this.fullscreen;
	}

	public static void lockCursor() {
		glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
	}

	public static void unlockCursor() {
		glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
	}

	@Override
	public void run() {
		init();

		long lastTime = System.nanoTime();
		long lastUpdateTime = System.nanoTime();
		double delta = 0.0;
		double ns = 1000000000.0 / 60;
		long timer = System.currentTimeMillis();

		int updates = 0;
		int frames = 0;
		while (running) {
			long now = System.nanoTime();
			delta += (now - lastTime) / ns;
			lastTime = now;
			if (delta >= 1.0) {
				this.deltaMillis = (System.nanoTime() - lastUpdateTime) / 1000000;
				lastUpdateTime = now;
				update();
				delta--;
				delta = Math.min(delta, 1);
				updates++;
			}

			render();
			frames++;

			if (System.currentTimeMillis() - timer > 1000) {
				timer += 1000;
				this.lastSecondFrames = frames;
				this.lastSecondUpdates = updates;
				System.out.println(frames + " fps \\ " + updates + " ups");
				updates = 0;
				frames = 0;
			}

			if (glfwWindowShouldClose(window)) {
				running = false;
			}
		}

		glfwDestroyWindow(window);
		glfwTerminate();
	}

	private void update() {
		glfwPollEvents();

		if (UIElement.shouldAlignUIElements) {
			UIElement.alignAllUIElements();
		}

		this.sm.update();
	}

	private void render() {
		this.sm.render();

		glfwSwapBuffers(window);

		int error = glGetError();
		if (error != GL_NO_ERROR) {
			System.out.println(error);
		}
	}

	public void exit() {
		this.running = false;

		//destroy audio context
		alcDestroyContext(audioContext);
		alcCloseDevice(audioDevice);
	}

	public static Main main;

	public static void main(String[] args) {
		main = new Main();
		main.start();
	}

	public void mousePressed(int button) {
		this.sm.mousePressed(button);
	}

	public void mouseReleased(int button) {
		this.sm.mouseReleased(button);
	}

	public void keyPressed(int key) {
		this.sm.keyPressed(key);
	}

	public void keyReleased(int key) {
		this.sm.keyReleased(key);
	}

}