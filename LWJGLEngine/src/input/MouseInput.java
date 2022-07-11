package input;

import static org.lwjgl.glfw.GLFW.*;

import java.nio.DoubleBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWMouseButtonCallback;

import main.Main;
import util.Vec2;

public class MouseInput extends GLFWMouseButtonCallback {
	
	public static boolean[] buttons = new boolean[65536];
	
	@Override
	public void invoke(long window, int key, int action, int mods) {
		buttons[key] = action == GLFW_PRESS;
	}
	
	public static boolean isButtonPressed(int button) {
		return buttons[button];
	}

	public static Vec2 getMousePos() {
		DoubleBuffer xBuffer = BufferUtils.createDoubleBuffer(1);
		DoubleBuffer yBuffer = BufferUtils.createDoubleBuffer(1);
		glfwGetCursorPos(Main.window, xBuffer, yBuffer);
		double x = xBuffer.get(0);
		double y = yBuffer.get(0);
		return new Vec2(x, y);
	}
	
}
