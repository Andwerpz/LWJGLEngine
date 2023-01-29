package input;

import static org.lwjgl.glfw.GLFW.*;

import java.nio.DoubleBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;

import main.Main;
import util.Vec2;

public class ScrollInput extends GLFWScrollCallback {

	@Override
	public void invoke(long window, double wheelOffset, double smoothOffset) {
		Main.main.mouseScrolled((float) wheelOffset, (float) smoothOffset);
	}

}
