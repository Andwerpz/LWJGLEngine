package util;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.imageio.ImageIO;

public class GraphicsTools {

	// -- OPENGL --

	// -- JAVA SWING --

	public static AlphaComposite makeComposite(double alpha) {
		int type = AlphaComposite.SRC_OVER;
		return (AlphaComposite.getInstance(type, (float) alpha));
	}

	public static void drawCenteredString(int x, int y, Graphics g, Font f, String s, Color c) {
		int width = GraphicsTools.calculateTextWidth(s, f);
		g.setFont(f);
		g.setColor(c);
		g.drawString(s, x - width / 2, y - f.getSize() / 2);
	}

	public static int calculateTextWidth(String text, Font font) {
		BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		FontMetrics fm = img.getGraphics().getFontMetrics(font);
		return fm.stringWidth(text);
	}

	public static int getFontHeight(Font font) {
		BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		FontMetrics fm = img.getGraphics().getFontMetrics(font);
		return fm.getHeight();
	}

	public static int getFontMaxAscent(Font font) {
		BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		FontMetrics fm = img.getGraphics().getFontMetrics(font);
		return fm.getMaxAscent();
	}

	public static int getFontAscent(Font font) {
		BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		FontMetrics fm = img.getGraphics().getFontMetrics(font);
		return fm.getAscent();
	}

	public static int getFontMaxDescent(Font font) {
		BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		FontMetrics fm = img.getGraphics().getFontMetrics(font);
		return fm.getMaxDescent();
	}

	public static BufferedImage generateTextImage(String text, Font font, Color c, int widthCutoff) {
		if (widthCutoff <= 0) {
			System.err.println("Width cutoff has to be greater than 0");
			return null;
		}

		int textMaxDescent = GraphicsTools.getFontMaxDescent(font);
		int textMaxAscent = GraphicsTools.getFontMaxAscent(font);

		int textSampleAscent = GraphicsTools.getFontSampleAscent(font);
		int textSampleDescent = GraphicsTools.getFontSampleDescent(font);

		textMaxAscent = Math.max(textSampleAscent, textMaxAscent);
		textMaxDescent = Math.max(textSampleDescent, textMaxDescent);

		BufferedImage img = new BufferedImage(widthCutoff, textMaxDescent + textMaxAscent, BufferedImage.TYPE_INT_ARGB);
		Graphics g = img.getGraphics();
		enableAntialiasing(g);
		g.setFont(font);
		g.setColor(c);
		g.drawString(text, 0, textMaxAscent);

		return img;
	}

	public static Rectangle getStringBounds(Graphics g, String str, float x, float y) {
		Graphics2D g2 = (Graphics2D) g;
		FontRenderContext frc = g2.getFontRenderContext();
		GlyphVector gv = g2.getFont().createGlyphVector(frc, str);
		return gv.getPixelBounds(null, x, y);
	}

	public static int getFontSampleDescent(Font font) {
		BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics g = img.getGraphics();
		g.setFont(font);
		String sampleText = "AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwXxYyZz1234567890";
		Rectangle textBounds = getStringBounds(g, sampleText, 0, 0);
		return (int) (textBounds.getMaxY());
	}

	public static int getFontSampleAscent(Font font) {
		BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics g = img.getGraphics();
		g.setFont(font);
		String sampleText = "AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwXxYyZz1234567890";
		Rectangle textBounds = getStringBounds(g, sampleText, 0, 0);
		return (int) (textBounds.getHeight() - textBounds.getMaxY());
	}

	public static int getFontSampleMaxHeight(Font font) {
		return GraphicsTools.getFontSampleDescent(font) + GraphicsTools.getFontSampleAscent(font);
	}

	public static void enableTextAntialiasing(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	}

	public static void enableAntialiasing(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	}

	public static BufferedImage verticalFlip(BufferedImage img) {
		int w = img.getWidth();
		int h = img.getHeight();
		BufferedImage flippedImage = new BufferedImage(w, h, img.getColorModel().getTransparency());
		Graphics2D g = flippedImage.createGraphics();
		g.drawImage(img, 0, 0, w, h, 0, h, w, 0, null);
		g.dispose();
		return flippedImage;
	}

	public static BufferedImage horizontalFlip(BufferedImage img) {
		int w = img.getWidth();
		int h = img.getHeight();
		BufferedImage flippedImage = new BufferedImage(w, h, img.getColorModel().getTransparency());
		Graphics2D g = flippedImage.createGraphics();
		g.drawImage(img, 0, 0, w, h, w, 0, 0, h, null);
		g.dispose();
		return flippedImage;
	}

	// ty MadProgrammer
	public static BufferedImage rotateImageByDegrees(BufferedImage img, double angle) {
		double rads = Math.toRadians(angle);
		double sin = Math.abs(Math.sin(rads)), cos = Math.abs(Math.cos(rads));
		int w = img.getWidth();
		int h = img.getHeight();
		int newWidth = (int) Math.floor(w * cos + h * sin);
		int newHeight = (int) Math.floor(h * cos + w * sin);

		BufferedImage rotated = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = rotated.createGraphics();
		AffineTransform at = new AffineTransform();
		at.translate((newWidth - w) / 2, (newHeight - h) / 2);

		int x = w / 2;
		int y = h / 2;

		at.rotate(rads, x, y);
		g2d.setTransform(at);
		g2d.drawImage(img, null, 0, 0);
		g2d.dispose();

		return rotated;
	}

	// combines two images
	// useful when combining images that are usually drawn together
	public static BufferedImage combineImages(BufferedImage a, BufferedImage b) {

		// create the new image, canvas size is the max. of both image sizes
		int w = Math.max(a.getWidth(), b.getWidth());
		int h = Math.max(a.getHeight(), b.getHeight());
		BufferedImage combined = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

		// paint both images, preserving the alpha channels
		Graphics g = combined.getGraphics();
		g.drawImage(a, 0, 0, null);
		g.drawImage(b, 0, 0, null);

		g.dispose();

		return combined;
	}

	// make a copy of an image
	public static BufferedImage copyImage(BufferedImage source) {
		BufferedImage b = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
		Graphics g = b.getGraphics();
		g.drawImage(source, 0, 0, null);
		g.dispose();
		return b;
	}

	// darkening an image according to a float value
	public static BufferedImage darkenImage(double d, BufferedImage b) {
		BufferedImage output = new BufferedImage(b.getWidth(), b.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = (Graphics2D) output.getGraphics();

		g2d.drawImage(b, 0, 0, null);

		g2d.setComposite(makeComposite(1d - d));
		g2d.setColor(Color.black);
		g2d.fillRect(0, 0, b.getWidth(), b.getHeight());

		return output;
	}

	public static ArrayList<BufferedImage> loadAnimation(String filepath, int width, int height) {
		BufferedImage animationPng = FileUtils.loadImage(filepath);
		return GraphicsTools.loadAnimation(animationPng, width, height);
	}

	// loads images from spritesheet
	public static ArrayList<BufferedImage> loadAnimation(BufferedImage animationPng, int width, int height) {
		ArrayList<BufferedImage> animation = new ArrayList<BufferedImage>();

		int spritesheetHeight = animationPng.getHeight();
		int spritesheetWidth = animationPng.getWidth();

		for (int i = 0; i < spritesheetHeight / height; i++) {
			for (int j = 0; j < spritesheetWidth / width; j++) {
				BufferedImage next = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				Graphics g = next.getGraphics();
				g.drawImage(animationPng, -(j * width), -(i * height), null);
				animation.add(next);
			}
		}

		return animation;
	}
}
