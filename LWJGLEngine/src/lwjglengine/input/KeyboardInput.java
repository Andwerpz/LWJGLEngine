package lwjglengine.input;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;

import java.nio.DoubleBuffer;
import java.util.HashMap;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallback;

import lwjglengine.main.Main;
import myutils.math.Vec2;

public class KeyboardInput extends GLFWKeyCallback {

	public static HashMap<Character, Character> shiftMap = new HashMap<Character, Character>() {
		{
			put('`', '~');
			put('1', '!');
			put('2', '@');
			put('3', '#');
			put('4', '$');
			put('5', '%');
			put('6', '^');
			put('7', '&');
			put('8', '*');
			put('9', '(');
			put('0', ')');
			put('-', '_');
			put('=', '+');
			put('q', 'Q');
			put('w', 'W');
			put('e', 'E');
			put('r', 'R');
			put('t', 'T');
			put('y', 'Y');
			put('u', 'U');
			put('i', 'I');
			put('o', 'O');
			put('p', 'P');
			put('[', '{');
			put(']', '}');
			put('\\', '|');
			put('a', 'A');
			put('s', 'S');
			put('d', 'D');
			put('f', 'F');
			put('g', 'G');
			put('h', 'H');
			put('j', 'J');
			put('k', 'K');
			put('l', 'L');
			put(';', ':');
			put('\'', '"');
			put('z', 'Z');
			put('x', 'X');
			put('c', 'C');
			put('v', 'V');
			put('b', 'B');
			put('n', 'N');
			put('m', 'M');
			put(',', '<');
			put('.', '>');
			put('/', '?');
		}
	};

	public static boolean[] keys = new boolean[65536];

	@Override
	public void invoke(long window, int key, int scancode, int action, int mods) {
		if (key == GLFW.GLFW_KEY_UNKNOWN) {
			return;
		}
		if (action == GLFW_PRESS) {
			Main.main.keyPressed(key);
		}
		else if (action == GLFW_RELEASE) {
			Main.main.keyReleased(key);
		}
		keys[key] = action != GLFW.GLFW_RELEASE;
	}

	public static boolean isKeyPressed(int code) {
		return keys[code];
	}

	public static boolean isControlPressed() {
		return isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL) || isKeyPressed(GLFW.GLFW_KEY_RIGHT_CONTROL);
	}

	public static boolean isShiftPressed() {
		return isKeyPressed(GLFW.GLFW_KEY_LEFT_SHIFT) || isKeyPressed(GLFW.GLFW_KEY_RIGHT_SHIFT);
	}

	public static boolean isAltPressed() {
		return isKeyPressed(GLFW.GLFW_KEY_LEFT_ALT) || isKeyPressed(GLFW.GLFW_KEY_RIGHT_ALT);
	}

}
