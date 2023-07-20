package lwjglengine.window;

import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL30.*;

import lwjglengine.graphics.Framebuffer;
import lwjglengine.graphics.Material;
import lwjglengine.graphics.Shader;
import lwjglengine.graphics.Texture3D;
import lwjglengine.input.Input;
import lwjglengine.input.TextField;
import lwjglengine.input.ToggleButton;
import lwjglengine.main.Main;
import lwjglengine.screen.ScreenQuad;
import lwjglengine.screen.UIScreen;
import lwjglengine.ui.UIElement;
import lwjglengine.ui.UISection;

public class Texture3DViewerWindow extends Window {

	private UIScreen uiScreen;
	private UISection uiSection;

	private Texture3D texture;

	private int displayWidth, displayHeight;
	private int displayXOffset, displayYOffset;

	//note that if the display alpha toggle is on, then it will override all the other colors. 
	private ToggleButton displayRedTb, displayGreenTb, displayBlueTb, displayAlphaTb;

	public Texture3DViewerWindow(Texture3D texture, Window parentWindow) {
		super(0, 0, 400, 300, parentWindow);
		this.init();

		this.setTexture(texture);
	}

	private void init() {
		this.uiScreen = new UIScreen();

		this.uiSection = new UISection(0, 0, this.getWidth(), this.getHeight(), this.uiScreen);
		this.uiSection.getBackgroundRect().bind(this.rootUIElement);
		this.uiSection.getBackgroundRect().setFillWidth(true);
		this.uiSection.getBackgroundRect().setFillHeight(true);
		this.uiSection.getBackgroundRect().setMaterial(Material.transparent());

		TextField zLevelTf = new TextField(5, 5, 100, 20, "tf_zlevel", "Depth", 12, this.uiSection.getSelectionScene(), this.uiSection.getTextScene());
		zLevelTf.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		zLevelTf.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		zLevelTf.getTextUIElement().setDoAntialiasing(false);
		zLevelTf.setFloatFieldDragIncrement(0.01f);
		zLevelTf.setIsFloatField(true);
		zLevelTf.bind(this.uiSection.getBackgroundRect());

		this.displayRedTb = new ToggleButton(5, 30, 100, 20, "tb_red", "Red", 12, this.uiSection.getSelectionScene(), this.uiSection.getTextScene());
		this.displayRedTb.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.displayRedTb.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		this.displayRedTb.getButtonText().setDoAntialiasing(false);
		this.displayRedTb.setIsToggled(true);
		this.displayRedTb.bind(this.uiSection.getBackgroundRect());

		this.displayGreenTb = new ToggleButton(5, 55, 100, 20, "tb_green", "Green", 12, this.uiSection.getSelectionScene(), this.uiSection.getTextScene());
		this.displayGreenTb.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.displayGreenTb.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		this.displayGreenTb.getButtonText().setDoAntialiasing(false);
		this.displayGreenTb.setIsToggled(true);
		this.displayGreenTb.bind(this.uiSection.getBackgroundRect());

		this.displayBlueTb = new ToggleButton(5, 80, 100, 20, "tb_blue", "Blue", 12, this.uiSection.getSelectionScene(), this.uiSection.getTextScene());
		this.displayBlueTb.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.displayBlueTb.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		this.displayBlueTb.getButtonText().setDoAntialiasing(false);
		this.displayBlueTb.setIsToggled(true);
		this.displayBlueTb.bind(this.uiSection.getBackgroundRect());

		this.displayAlphaTb = new ToggleButton(5, 105, 100, 20, "tb_alpha", "Alpha", 12, this.uiSection.getSelectionScene(), this.uiSection.getTextScene());
		this.displayAlphaTb.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_TOP);
		this.displayAlphaTb.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_TOP);
		this.displayAlphaTb.getButtonText().setDoAntialiasing(false);
		this.displayAlphaTb.setIsToggled(false);
		this.displayAlphaTb.bind(this.uiSection.getBackgroundRect());

		this.texture = null;
	}

	public void setTexture(Texture3D t) {
		this.texture = t;
		this.alignTextureDisplayRect();
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

		this.displayWidth = rectWidth;
		this.displayHeight = rectHeight;

		this.displayXOffset = (this.getWidth() - rectWidth) / 2;
		this.displayYOffset = (this.getHeight() - rectHeight) / 2;
	}

	@Override
	public String getDefaultTitle() {
		return "3D Texture Viewer";
	}

	@Override
	protected void _kill() {
		this.uiScreen.kill();
		this.uiSection.kill();
	}

	@Override
	protected void _resize() {
		this.uiScreen.setScreenDimensions(this.getWidth(), this.getHeight());
		this.alignTextureDisplayRect();
	}

	@Override
	protected void _update() {
		this.uiSection.update();
	}

	@Override
	protected void renderContent(Framebuffer outputBuffer) {
		//first, render the 3d texture preview
		if (this.texture != null) {
			glViewport(this.displayXOffset, this.displayYOffset, this.displayWidth, this.displayHeight);
			Shader.TEXTURE3D_DISPLAY.enable();
			Shader.TEXTURE3D_DISPLAY.setUniform1f("sample_depth", Float.parseFloat(Input.getText("tf_zlevel")));
			Shader.TEXTURE3D_DISPLAY.setUniform1i("display_red", this.displayRedTb.isToggled() ? 1 : 0);
			Shader.TEXTURE3D_DISPLAY.setUniform1i("display_blue", this.displayBlueTb.isToggled() ? 1 : 0);
			Shader.TEXTURE3D_DISPLAY.setUniform1i("display_green", this.displayGreenTb.isToggled() ? 1 : 0);
			Shader.TEXTURE3D_DISPLAY.setUniform1i("display_alpha", this.displayAlphaTb.isToggled() ? 1 : 0);
			this.texture.bind(GL_TEXTURE0);
			outputBuffer.bind();
			ScreenQuad.screenQuad.render();
			glViewport(0, 0, Main.windowWidth, Main.windowHeight);
		}

		//then render the ui
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
