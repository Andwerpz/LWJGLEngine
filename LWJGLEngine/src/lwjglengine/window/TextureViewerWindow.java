package lwjglengine.window;

import java.io.File;

import lwjglengine.graphics.Framebuffer;
import lwjglengine.graphics.Texture;
import lwjglengine.graphics.TextureMaterial;
import lwjglengine.model.FilledRectangle;
import lwjglengine.screen.UIScreen;
import lwjglengine.ui.UIElement;
import lwjglengine.ui.UIFilledRectangle;
import lwjglengine.ui.UISection;
import myutils.v11.file.FileUtils;

public class TextureViewerWindow extends Window {

	private UIScreen uiScreen;

	private UISection uiSection;

	private UIFilledRectangle textureDisplayRect;

	private boolean shouldUnload = false;
	private Texture texture;

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
		this.uiScreen = new UIScreen();

		this.uiSection = new UISection(0, 0, this.getWidth(), this.getHeight(), this.uiScreen);
		this.uiSection.getBackgroundRect().bind(this.rootUIElement);
		this.uiSection.getBackgroundRect().setFillWidth(true);
		this.uiSection.getBackgroundRect().setFillHeight(true);
		this.uiSection.getBackgroundRect().setMaterial(this.contentDefaultMaterial);

		this.textureDisplayRect = new UIFilledRectangle(0, 0, 0, this.getWidth(), this.getHeight(), new FilledRectangle(), this.uiSection.getBackgroundScene());
		this.textureDisplayRect.setFrameAlignmentStyle(UIElement.FROM_CENTER_LEFT, UIElement.FROM_CENTER_BOTTOM);
		this.textureDisplayRect.setContentAlignmentStyle(UIElement.ALIGN_CENTER, UIElement.ALIGN_CENTER);
		this.textureDisplayRect.bind(this.uiSection.getBackgroundRect());

		this._resize();
	}

	@Override
	public String getDefaultTitle() {
		return "Texture Viewer";
	}

	public void setTexture(Texture texture) {
		this.textureDisplayRect.setTextureMaterial(TextureMaterial.defaultTextureMaterial());
		if (this.shouldUnload) {
			if (this.texture != null) {
				this.texture.kill();
			}
			this.texture = null;
		}
		this.shouldUnload = false;

		this.texture = texture;
		this.textureDisplayRect.setTextureMaterial(new TextureMaterial(this.texture));

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
			rectHeight = (int) (textureAspectRatio * this.getWidth());
		}

		this.textureDisplayRect.setDimensions(rectWidth, rectHeight);
	}

	@Override
	protected void _kill() {
		this.uiScreen.kill();
		this.uiSection.kill();

		if (this.shouldUnload) {
			this.texture.kill();
		}
	}

	@Override
	protected void _resize() {
		this.uiScreen.setScreenDimensions(this.getWidth(), this.getHeight());

		this.alignTextureDisplayRect();
	}

	@Override
	protected void _update() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void renderContent(Framebuffer outputBuffer) {
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
		// TODO Auto-generated method stub

	}

	@Override
	protected void _mouseReleased(int button) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _mouseScrolled(float wheelOffset, float smoothOffset) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _keyPressed(int key) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _keyReleased(int key) {
		// TODO Auto-generated method stub

	}

}
