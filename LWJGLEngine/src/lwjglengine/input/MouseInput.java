package lwjglengine.input;

import static org.lwjgl.glfw.GLFW.*;

import java.nio.DoubleBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWMouseButtonCallback;

import lwjglengine.main.Main;
import myutils.v10.math.Vec2;

public class MouseInput extends GLFWMouseButtonCallback {

	public static final int LEFT_MOUSE_BUTTON = GLFW_MOUSE_BUTTON_1;
	public static final int RIGHT_MOUSE_BUTTON = GLFW_MOUSE_BUTTON_2;

	public static boolean[] buttons = new boolean[65536];

	@Override
	public void invoke(long window, int key, int action, int mods) {
		if (action == GLFW_PRESS) {
			Main.main.mousePressed(key);
		}
		else if (action == GLFW_RELEASE) {
			Main.main.mouseReleased(key);
		}
		buttons[key] = action == GLFW_PRESS;
	}

	public static boolean isButtonPressed(int button) {
		return buttons[button];
	}

	/**
	 * Returns the mouse position relative to the bottom left corner of the screen. 
	 * @return
	 */
	public static Vec2 getMousePos() {
		DoubleBuffer xBuffer = BufferUtils.createDoubleBuffer(1);
		DoubleBuffer yBuffer = BufferUtils.createDoubleBuffer(1);
		glfwGetCursorPos(Main.window, xBuffer, yBuffer);
		double x = xBuffer.get(0);
		double y = yBuffer.get(0);
		y = Main.windowHeight - y;
		return new Vec2(x, y);
	}

}