package util;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FontUtils {

	public static Font CSGOFont;

	public static void loadFonts() {
		try {
			FileInputStream is = new FileInputStream(FileUtils.loadFile("font/cs_regular.ttf"));
			// InputStream is =
			// FontUtils.class.getResourceAsStream("/fonts/cs_regular.ttf");
			FontUtils.CSGOFont = Font.createFont(Font.TRUETYPE_FONT, is);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (FontFormatException e) {
			e.printStackTrace();
		}
	}

	public static Font deriveSize(int size, Font font) {
		return font.deriveFont((float) size);
	}

}
