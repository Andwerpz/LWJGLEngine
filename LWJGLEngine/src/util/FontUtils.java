package util;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FontUtils {

	public static Font CSGOFont, segoe_ui, ggsans;

	public static void loadFonts() {
		FontUtils.CSGOFont = loadFont("font/cs_regular.ttf");
		FontUtils.segoe_ui = loadFont("font/segoe_ui.ttf");
		FontUtils.ggsans = loadFont("font/ggsans-Normal.ttf");
	}

	public static Font loadFont(String path) {
		try {
			FileInputStream is = new FileInputStream(FileUtils.loadFile(path));
			Font font = Font.createFont(Font.TRUETYPE_FONT, is);
			return font;
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (FontFormatException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Font deriveSize(int size, Font font) {
		return font.deriveFont((float) size);
	}

}
