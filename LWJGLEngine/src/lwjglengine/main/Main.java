package lwjglengine.main;

import static org.lwjgl.glfw.GLFW.*;
import org.lwjgl.glfw.GLFW;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL21.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL33.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT;
import static org.lwjgl.opengl.GL42.glMemoryBarrier;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.text.DecimalFormat;

import static org.lwjgl.openal.ALC10.*;

import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;
import org.lwjgl.opengl.GLUtil;

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
import lwjglengine.screen.UIScreen;
import lwjglengine.state.SplashState;
import lwjglengine.state.StateFactory;
import lwjglengine.state.StateManagerWindow;
import lwjglengine.state.TestState;
import lwjglengine.state.TestStateFactory;
import lwjglengine.ui.UIElement;
import lwjglengine.window.AdjustableWindow;
import lwjglengine.window.Window;
import myutils.graphics.FontUtils;
import myutils.math.Mat4;
import myutils.math.Vec2;

import static org.lwjgl.opengl.GL.*;

public class Main implements Runnable {
	// seems like the maximum size the viewport can be is equal to the dimensions of the window

	//OpenGL Error Code Key:
	//1280: invalid enum
	//1281: invalid value, maybe when binding?
	//1282: invalid operation, probably a framebuffer operation
	//1285: something to do with out of memory
	//1286: invalid framebuffer operation

	//TODO :
	// - make screens faster by making them render directly to the window, or framebuffer 0
	//   - instead of using intermediate framebuffers, we can just make screens render in the correct order. 
	//   - use glViewport and glScissor to control what pixels are rendered. 
	//   - i suppose in the ui pipeline, we're using intermediate framebuffers, so this won't be that much of a performance increase i think...
	// - make list viewer use scroll bar feature from uiSection

	private static int windowedWidth = 1280;
	private static int windowedHeight = 720;

	public static int windowWidth = 1280;
	public static int windowHeight = 720;

	public static boolean allowWindowResizing = false;

	public static Main main;

	private Thread thread;
	private boolean running = false;

	public static long window;
	private static boolean fullscreen = false;

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

	private static int currentCursorShape;

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

		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 5);

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
		glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);
		glCullFace(GL_BACK);
		System.out.println("OpenGL : " + glGetString(GL_VERSION));

		setCursorShape(GLFW.GLFW_ARROW_CURSOR);

		// INIT
		Shader.init();
		this.sm = new StateManagerWindow(Main.initialStateFactory);

		Main.main = this;

		this.outputBuffer = new Framebuffer(0);

		//DEBUG MODE
		//		GLUtil.setupDebugMessageCallback();
		//System.err.println("Max SSBO Size : " + glGetInteger(GL_MAX_SHADER_STORAGE_BLOCK_SIZE));
	}

	public static long getDeltaMillis() {
		return Main.main.deltaMillis;
	}

	public static float getDeltaSeconds() {
		return Main.main.deltaMillis / 1000.0f;
	}

	private static void setCursorShape(int shape) {
		if (currentCursorShape == shape) {
			return;
		}
		currentCursorShape = shape;
		GLFW.glfwSetCursor(window, GLFW.glfwCreateStandardCursor(currentCursorShape));
	}

	class WindowSizeCallback extends GLFWWindowSizeCallback {
		@Override
		public void invoke(long window, int width, int height) {
			//for now, assume that the window is referring to the main game window
			Main.setWindowDimensions(width, height);
		}
	}

	public static void setWindowDimensions(int width, int height) {
		Main.windowWidth = width;
		Main.windowHeight = height;
		Main.main.sm.setDimensions(width, height);
		System.out.println("NEW RESOLUTION : " + Main.windowWidth + " " + Main.windowHeight);
	}

	public static void setWindowPosition(int x, int y) {
		glfwSetWindowPos(Main.window, x, y);
	}

	public static void toggleFullscreen() {
		GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
		long primaryMonitor = glfwGetPrimaryMonitor();
		if (Main.fullscreen) {
			Main.setWindowDimensions(Main.windowedWidth, Main.windowedHeight);
			glfwSetWindowMonitor(window, NULL, 0, 0, Main.windowWidth, Main.windowHeight, vidmode.refreshRate());
			glfwSetWindowSize(window, Main.windowWidth, Main.windowHeight);
			glfwSetWindowPos(window, (vidmode.width() - windowWidth) / 2, (vidmode.height() - windowHeight) / 2);
		}
		else {
			glfwSetWindowMonitor(window, primaryMonitor, 0, 0, vidmode.width(), vidmode.height(), vidmode.refreshRate());
			Main.setWindowDimensions(vidmode.width(), vidmode.height());
		}

		Main.fullscreen = !Main.fullscreen;
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

		DecimalFormat df = new DecimalFormat("#.##");
		float renderAvgMillis = 0;
		float updateAvgMillis = 0;

		int updates = 0;
		int frames = 0;
		while (running) {
			long now = System.nanoTime();
			delta += (now - lastTime) / ns;
			lastTime = now;
			if (delta >= 1.0) {
				long startMillis = System.currentTimeMillis();
				this.deltaMillis = (System.nanoTime() - lastUpdateTime) / 1000000;
				lastUpdateTime = now;
				update();
				delta--;
				delta = Math.min(delta, 1);
				updates++;
				updateAvgMillis += System.currentTimeMillis() - startMillis;
			}

			{
				long startMillis = System.currentTimeMillis();
				render();
				frames++;
				renderAvgMillis += System.currentTimeMillis() - startMillis;
			}

			if (System.currentTimeMillis() - timer > 1000) {
				timer += 1000;
				this.lastSecondFrames = frames;
				this.lastSecondUpdates = updates;
				renderAvgMillis /= frames;
				updateAvgMillis /= updates;
				System.out.println(frames + " fps \\ " + updates + " ups, millis avg : " + df.format(renderAvgMillis) + " \\ " + df.format(updateAvgMillis));
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
		MouseInput.updateMousePos();

		if (UIElement.shouldAlignUIElements) {
			UIElement.alignAllUIElements();
		}

		this.sm.update();

		//update hovered window
		Vec2 mousePos = MouseInput.getMousePos();
		Window.setHoveredWindow(this.sm.selectWindow((int) mousePos.x, (int) mousePos.y, false));

		//set cursor
		if (Window.hoveredWindow != null) {
			int adjCursorShape = GLFW.GLFW_ARROW_CURSOR;
			if (Window.hoveredWindow.getParent() instanceof AdjustableWindow) {
				adjCursorShape = Window.hoveredWindow.getParent().getCursorShape();
			}
			if (adjCursorShape == GLFW.GLFW_ARROW_CURSOR) {
				setCursorShape(Window.hoveredWindow.getCursorShape());
			}
			else {
				setCursorShape(adjCursorShape);
			}
		}

		Screen.updateActiveScreens();

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
		while (error != GL_NO_ERROR) {
			if (error != GL_NO_ERROR) {
				System.err.println("Main: GL ERROR : " + error);
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
		if (!Main.cursorLocked) {
			Vec2 mousePos = MouseInput.getMousePos();
			Window.setSelectedWindow(this.sm.selectWindow((int) mousePos.x, (int) mousePos.y, true));
		}
		this.sm.mousePressed(button);
	}

	public void mouseReleased(int button) {
		this.sm.mouseReleased(button);
	}

	public void mouseScrolled(float wheelOffset, float smoothOffset) {
		this.sm.mouseScrolled(wheelOffset, smoothOffset);
	}

	public void keyPressed(int key) {
		if (key == GLFW.GLFW_KEY_F11) {
			Main.toggleFullscreen();
			return;
		}

		this.sm.keyPressed(key);
	}

	public void keyReleased(int key) {
		this.sm.keyReleased(key);
	}

}