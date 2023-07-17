package lwjglengine.main;

import static org.lwjgl.glfw.GLFW.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.*;

import static org.lwjgl.openal.ALC10.*;

import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;

import lwjglengine.entity.Entity;
import lwjglengine.graphics.Framebuffer;
import lwjglengine.graphics.Shader;
import lwjglengine.input.KeyboardInput;
import lwjglengine.input.MouseInput;
import lwjglengine.input.ScrollInput;
import lwjglengine.model.Model;
import lwjglengine.networking.Client;
import lwjglengine.networking.Server;
import lwjglengine.scene.Scene;
import lwjglengine.screen.Screen;
import lwjglengine.screen.ScreenQuad;
import lwjglengine.state.SplashState;
import lwjglengine.state.StateFactory;
import lwjglengine.state.StateManagerWindow;
import lwjglengine.state.TestState;
import lwjglengine.state.TestStateFactory;
import lwjglengine.ui.UIElement;
import myutils.v10.graphics.FontUtils;
import myutils.v10.math.Mat4;
import myutils.v10.math.Vec2;

import static org.lwjgl.opengl.GL.*;

public class Main implements Runnable {
	// seems like the maximum size the viewport can be is equal to the dimensions of the window

	private static int windowedWidth = 1280;
	private static int windowedHeight = 720;

	public static int windowWidth = 1280;
	public static int windowHeight = 720;

	public static boolean allowWindowResizing = false;

	public static Main main;

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

	public static StateFactory initialStateFactory = new TestStateFactory();
	public static String windowTitle = "LWJGL Engine";

	private StateManagerWindow sm;

	private long audioContext;
	private long audioDevice;

	private static boolean cursorLocked = false;

	private Framebuffer outputBuffer;

	public static void main(String[] args) {
		Main main = new Main();
		main.start();
	}

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

		glfwWindowHint(GLFW_RESIZABLE, Main.allowWindowResizing ? GL_TRUE : GL_FALSE);
		long primaryMonitor = glfwGetPrimaryMonitor();
		window = glfwCreateWindow(windowWidth, windowHeight, windowTitle, fullscreen ? primaryMonitor : NULL, NULL);

		if (window == NULL) {
			return;
		}

		GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
		glfwSetWindowPos(window, (vidmode.width() - windowWidth) / 2, (vidmode.height() - windowHeight) / 2);
		glfwMakeContextCurrent(window);
		glfwShowWindow(window);

		glfwSetKeyCallback(window, new KeyboardInput());
		glfwSetMouseButtonCallback(window, new MouseInput());
		glfwSetScrollCallback(window, new ScrollInput());
		glfwSetWindowSizeCallback(window, new WindowSizeCallback());

		createCapabilities();
		glClearColor(0f, 0f, 0f, 0f);
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);
		System.out.println("OpenGL : " + glGetString(GL_VERSION));

		// INIT
		Shader.init();
		this.sm = new StateManagerWindow(Main.initialStateFactory);

		Main.main = this;

		this.outputBuffer = new Framebuffer(0);
	}

	class WindowSizeCallback extends GLFWWindowSizeCallback {
		@Override
		public void invoke(long window, int width, int height) {
			//for now, assume that the window is referring to the main game window
			Main.windowWidth = width;
			Main.windowHeight = height;
			Main.main.sm.setDimensions(width, height);
		}
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

		glViewport(0, 0, Main.windowWidth, Main.windowHeight);
		this.fullscreen = !this.fullscreen;
	}

	public void setWindowDimensions(int width, int height) {
		glfwSetWindowSize(window, width, height);
	}

	public void setWindowPosition(int x, int y) {
		glfwSetWindowPos(window, x, y);
	}

	public static void lockCursor() {
		cursorLocked = true;
		glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
	}

	public static void unlockCursor() {
		cursorLocked = false;
		glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
	}

	public static boolean isCursorLocked() {
		return cursorLocked;
	}

	@Override
	public void run() {
		this.init();

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

		exit();

		glfwDestroyWindow(window);
		glfwTerminate();
	}

	private void update() {
		glfwPollEvents();

		if (UIElement.shouldAlignUIElements) {
			UIElement.alignAllUIElements();
		}

		this.sm.update();

		//normal updating stuff
		//we want to update this stuff after we modify them in sm.update so that they render correctly. 
		Entity.updateEntities();
		Model.updateModels();
	}

	private void render() {
		this.outputBuffer.bind();
		glClear(GL_COLOR_BUFFER_BIT);
		this.sm.render(this.outputBuffer);

		glfwSwapBuffers(window);
		
		int error = glGetError();
		while(error != GL_NO_ERROR) {
			if (error != GL_NO_ERROR) {
				System.out.println("Main: GL ERROR : " + error);
				error = glGetError();
			}
		}
		
	}

	public void exit() {
		this.running = false;

		this.sm.kill();

		//terminate any networking threads
		for (Server s : Server.servers) {
			s.exit();
		}

		for (Client c : Client.clients) {
			c.exit();
		}

		//destroy audio context
		alcDestroyContext(audioContext);
		alcCloseDevice(audioDevice);
	}

	public static StateManagerWindow getStateManagerWindow() {
		return main.sm;
	}

	public void mousePressed(int button) {
		Vec2 mousePos = MouseInput.getMousePos();
		this.sm.selectWindow((int) mousePos.x, (int) mousePos.y, false);
		this.sm.mousePressed(button);
	}

	public void mouseReleased(int button) {
		this.sm.mouseReleased(button);
	}

	public void mouseScrolled(float wheelOffset, float smoothOffset) {
		this.sm.mouseScrolled(wheelOffset, smoothOffset);
	}

	public void keyPressed(int key) {
		this.sm.keyPressed(key);
	}

	public void keyReleased(int key) {
		this.sm.keyReleased(key);
	}

}