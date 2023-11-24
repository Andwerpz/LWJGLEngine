package lwjglengine.window;

import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;

import java.io.File;

import lwjglengine.graphics.Framebuffer;
import lwjglengine.graphics.Material;
import lwjglengine.graphics.Shader;
import lwjglengine.graphics.Texture;
import lwjglengine.graphics.TextureMaterial;
import lwjglengine.input.Input;
import lwjglengine.input.ToggleButton;
import lwjglengine.main.Main;
import lwjglengine.model.FilledRectangle;
import lwjglengine.screen.ScreenQuad;
import lwjglengine.screen.UIScreen;
import lwjglengine.ui.UIElement;
import lwjglengine.ui.UIFilledRectangle;
import lwjglengine.ui.UISection;
import myutils.file.FileUtils;

public class TextureViewerWindow extends Window {

	private UISection uiSection;

	private boolean shouldUnload = false;
	private Texture texture;

	private ToggleButton displayRedTb;
	private ToggleButton displayGreenTb;
	private ToggleButton displayBlueTb;
	private ToggleButton displayAlphaTb;

	private int displayWidth, displayHeight;
	private int displayXOffset, displayYOffset;

	public TextureViewerWindow(int xOffset, int yOffset, int width, int height, Window parentWindow) {
		super(xOffset, yOffset, width, height, parentWindow);
		this.init();
	}

	public TextureViewerWindow(Texture texture) {
		super(0, 0, 400, 300, null);
		this.init();
		this.setTexture(texture);
	}

	private void init() {
		this.uiSection = new UISection();
		this.uiSection.getBackgroundRect().bind(this.rootUIElement);
		this.uiSection.getBackgroundRect().setFillWidth(true);
		this.uiSection.getBackgroundRect().setFillHeight(true);
		this.uiSection.getBackgroundRect().setMaterial(Material.transparent());

		this.displayRedTb = new ToggleButton(5, 5, 100, 20, "tb_red", "Red", 12, this.uiSection.getSelectionScene(), this.uiSection.getTextScene());
		this.displayRedTb.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.displayRedTb.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		this.displayRedTb.getButtonText().setDoAntialiasing(false);
		this.displayRedTb.setIsToggled(true);
		this.displayRedTb.bind(this.uiSection.getBackgroundRect());

		this.displayGreenTb = new ToggleButton(5, 30, 100, 20, "tb_green", "Green", 12, this.uiSection.getSelectionScene(), this.uiSection.getTextScene());
		this.displayGreenTb.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.displayGreenTb.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		this.displayGreenTb.getButtonText().setDoAntialiasing(false);
		this.displayGreenTb.setIsToggled(true);
		this.displayGreenTb.bind(this.uiSection.getBackgroundRect());

		this.displayBlueTb = new ToggleButton(5, 55, 100, 20, "tb_blue", "Blue", 12, this.uiSection.getSelectionScene(), this.uiSection.getTextScene());
		this.displayBlueTb.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.displayBlueTb.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		this.displayBlueTb.getButtonText().setDoAntialiasing(false);
		this.displayBlueTb.setIsToggled(true);
		this.displayBlueTb.bind(this.uiSection.getBackgroundRect());

		this.displayAlphaTb = new ToggleButton(5, 80, 100, 20, "tb_alpha", "Alpha", 12, this.uiSection.getSelectionScene(), this.uiSection.getTextScene());
		this.displayAlphaTb.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.displayAlphaTb.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		this.displayAlphaTb.getButtonText().setDoAntialiasing(false);
		this.displayAlphaTb.setIsToggled(false);
		this.displayAlphaTb.bind(this.uiSection.getBackgroundRect());

		this.alignTextureDisplayRect();

		this._resize();
	}

	@Override
	public String getDefaultTitle() {
		return "Texture Viewer";
	}

	public void setTexture(Texture texture) {
		if (this.shouldUnload) {
			if (this.texture != null) {
				this.texture.kill();
			}
			this.texture = null;
		}
		this.shouldUnload = false;

		this.texture = texture;

		this.alignTextureDisplayRect();
	}

	public void setTexture(File file) {
		Texture texture = new Texture(FileUtils.loadImage(file));
		this.setTexture(texture);
		this.shouldUnload = true;
	}

	public void alignTextureDisplayRect() {
		if (this.texture == null) {
			return;
		}

		int textureWidth = this.texture.getWidth();
		int textureHeight = this.texture.getHeight();

		//we want to keep the aspect ratio of the texture, but also make the texture as large as possible within the window
		float textureAspectRatio = ((float) textureWidth) / ((float) textureHeight);
		float windowAspectRatio = ((float) this.getWidth()) / ((float) this.getHeight());

		int rectWidth = 0;
		int rectHeight = 0;

		if (windowAspectRatio > textureAspectRatio) {
			//window is height limited
			rectHeight = this.getHeight();
			rectWidth = (int) (textureAspectRatio * this.getHeight());
		}
		else {
			//window is width limited
			rectWidth = this.getWidth();
			rectHeight = (int) (this.getWidth() / textureAspectRatio);
		}

		this.displayWidth = rectWidth;
		this.displayHeight = rectHeight;

		this.displayXOffset = (this.getWidth() - rectWidth) / 2;
		this.displayYOffset = (this.getHeight() - rectHeight) / 2;
	}

	@Override
	protected void _kill() {
		this.uiSection.kill();

		if (this.shouldUnload) {
			this.texture.kill();
		}
	}

	@Override
	protected void _resize() {
		this.uiSection.setScreenDimensions(this.getWidth(), this.getHeight());
		this.alignTextureDisplayRect();
	}

	@Override
	protected void _update() {
		this.uiSection.update();
	}

	@Override
	protected void renderContent(Framebuffer outputBuffer) {
		//first, render the 2d texture preview
		if (this.texture != null) {
			glViewport(this.displayXOffset, this.displayYOffset, this.displayWidth, this.displayHeight);
			Shader.TEXTURE_DISPLAY.enable();
			Shader.TEXTURE_DISPLAY.setUniform1i("display_red", this.displayRedTb.isToggled() ? 1 : 0);
			Shader.TEXTURE_DISPLAY.setUniform1i("display_blue", this.displayBlueTb.isToggled() ? 1 : 0);
			Shader.TEXTURE_DISPLAY.setUniform1i("display_green", this.displayGreenTb.isToggled() ? 1 : 0);
			Shader.TEXTURE_DISPLAY.setUniform1i("display_alpha", this.displayAlphaTb.isToggled() ? 1 : 0);
			this.texture.bind(GL_TEXTURE0);
			outputBuffer.bind();
			ScreenQuad.screenQuad.render();
			glViewport(0, 0, Main.windowWidth, Main.windowHeight);
		}

		this.uiSection.render(outputBuffer, this.getWindowMousePos());
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
		this.uiSection.mousePressed(button);
	}

	@Override
	protected void _mouseReleased(int button) {
		this.uiSection.mouseReleased(button);
	}

	@Override
	protected void _mouseScrolled(float wheelOffset, float smoothOffset) {
		this.uiSection.mouseScrolled(wheelOffset, smoothOffset);
	}

	@Override
	protected void _keyPressed(int key) {
		this.uiSection.keyPressed(key);
	}

	@Override
	protected void _keyReleased(int key) {
		this.uiSection.keyReleased(key);
	}

}
