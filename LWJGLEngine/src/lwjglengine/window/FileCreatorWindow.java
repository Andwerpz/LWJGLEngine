package lwjglengine.window;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.imageio.ImageIO;

import lwjglengine.graphics.Framebuffer;
import lwjglengine.graphics.Material;
import lwjglengine.input.Button;
import lwjglengine.input.Input;
import lwjglengine.input.TextField;
import lwjglengine.ui.Text;
import lwjglengine.ui.UIElement;
import lwjglengine.ui.UIFilledRectangle;
import lwjglengine.ui.UISection;
import myutils.file.FileUtils;
import myutils.math.Vec3;

public class FileCreatorWindow extends Window {

	private UISection bottomBarSection;

	private static int bottomBarHeight = 24;
	private UIFilledRectangle bottomBarRect;
	public static Material bottomBarMaterial = new Material(new Vec3((float) (20 / 255.0)));
	private TextField bottomBarFilenameTf;
	private static int confirmBtnWidth = 100;
	private Button bottomBarConfirmBtn;

	private FileExplorerWindow fileExplorer;

	private String defaultExt = "txt";
	private Object toWrite;

	public FileCreatorWindow(Object toWrite) {
		super(0, 0, 400, 300, null);
		this.init(toWrite);
	}

	private void init(Object toWrite) {
		this.toWrite = toWrite;
		//determine default object extension
		if (this.toWrite instanceof BufferedImage) {
			this.defaultExt = "png";
		}

		this.fileExplorer = new FileExplorerWindow(this);
		this.fileExplorer.setAlignmentStyle(Window.FROM_LEFT, Window.FROM_TOP);
		this.fileExplorer.setOffset(0, 0);
		this.fileExplorer.setFillWidth(true);

		this.bottomBarSection = new UISection();

		this.bottomBarRect = this.bottomBarSection.getBackgroundRect();
		this.bottomBarRect.setFrameAlignmentOffset(0, 0);
		this.bottomBarRect.setDimensions(this.getWidth(), bottomBarHeight);
		this.bottomBarRect.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_BOTTOM);
		this.bottomBarRect.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_BOTTOM);
		this.bottomBarRect.setFillWidth(true);
		this.bottomBarRect.setFillWidthMargin(0);
		this.bottomBarRect.setMaterial(bottomBarMaterial);
		this.bottomBarRect.bind(this.rootUIElement);

		this.bottomBarFilenameTf = new TextField(3, 3, this.getWidth() - confirmBtnWidth - 8, bottomBarHeight - 5, "tf_filename", "Enter New File Name", 12, this.bottomBarSection.getSelectionScene(), this.bottomBarSection.getTextScene());
		this.bottomBarFilenameTf.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_BOTTOM);
		this.bottomBarFilenameTf.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_BOTTOM);
		this.bottomBarFilenameTf.getTextUIElement().setDoAntialiasing(false);
		this.bottomBarFilenameTf.bind(this.bottomBarRect);

		this.bottomBarConfirmBtn = new Button(3, 3, confirmBtnWidth, bottomBarHeight - 5, "btn_confirm", "Confirm", 12, this.bottomBarSection.getSelectionScene(), this.bottomBarSection.getTextScene());
		this.bottomBarConfirmBtn.setFrameAlignmentStyle(UIElement.FROM_RIGHT, UIElement.FROM_BOTTOM);
		this.bottomBarConfirmBtn.setContentAlignmentStyle(UIElement.ALIGN_RIGHT, UIElement.ALIGN_BOTTOM);
		this.bottomBarConfirmBtn.getButtonText().setDoAntialiasing(false);
		this.bottomBarConfirmBtn.bind(this.bottomBarRect);

		this._resize();
	}

	@Override
	protected void _kill() {
		this.bottomBarSection.kill();
	}

	@Override
	protected void _resize() {
		this.bottomBarSection.setScreenDimensions(this.getWidth(), this.getHeight());

		this.bottomBarFilenameTf.setWidth(this.getWidth() - confirmBtnWidth - 8);

		this.fileExplorer.setHeight(this.getHeight() - bottomBarHeight);
	}

	@Override
	public String getDefaultTitle() {
		return "File Creator";
	}

	@Override
	protected void _update() {
		this.bottomBarSection.update();
	}

	@Override
	protected void renderContent(Framebuffer outputBuffer) {
		this.bottomBarSection.render(outputBuffer, this.getWindowMousePos());
	}

	@Override
	protected void renderOverlay(Framebuffer outputBuffer) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void selected() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void deselected() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void subtreeSelected() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void subtreeDeselected() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _mousePressed(int button) {
		this.bottomBarSection.mousePressed(button);
	}

	@Override
	protected void _mouseReleased(int button) {
		this.bottomBarSection.mouseReleased(button);

		switch (Input.getClicked(this.bottomBarSection.getSelectionScene())) {
		case "btn_confirm": {
			String directory = this.fileExplorer.getCurrentDirectory();
			String filename = this.bottomBarFilenameTf.getText();
			String extension = FileUtils.getFileExtension(filename);
			if (extension == null) {
				extension = this.defaultExt;
				filename += "." + this.defaultExt;
			}

			String filepath = directory + filename;
			if (FileUtils.isFilepathValid(filepath)) {
				System.out.println("Creating new file : " + filename);
				System.out.println(filepath);

				File file = new File(filepath);

				//overwrite the old file
				if (file.exists()) {
					file.delete();
				}

				//create new file
				try {
					file.createNewFile();
				}
				catch (IOException e1) {
					e1.printStackTrace();
				}

				//write to file 
				try {
					switch (extension) {
					case "png": {
						ImageIO.write((BufferedImage) this.toWrite, extension, file);
						break;
					}

					case "txt":
						FileWriter fout = new FileWriter(file);
						fout.write(this.toWrite.toString());
						break;
					}
				}
				catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				this.close();
			}
			break;
		}
		}
	}

	@Override
	protected void _mouseScrolled(float wheelOffset, float smoothOffset) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _keyPressed(int key) {
		this.bottomBarSection.keyPressed(key);
	}

	@Override
	protected void _keyReleased(int key) {
		this.bottomBarSection.keyReleased(key);
	}

}
