package lwjglengine.v10.project;

import java.awt.Color;

import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.scene.Scene;
import lwjglengine.v10.screen.UIScreen;
import lwjglengine.v10.ui.Text;
import lwjglengine.v10.ui.UIElement;
import lwjglengine.v10.ui.UIFilledRectangle;
import lwjglengine.v10.window.ListViewerWindow;
import lwjglengine.v10.window.Window;

public class ProjectAssetViewerWindow extends Window {
	
	private static final String MODEL_STR = "Models";
	private static final String TEXTURE_STR = "Textures";
	private static final String SOUND_STR = "Sounds";
	
	private int topBarHeightPx = 20;
	
	private ListViewerWindow topBarWindow;
	private ListViewerWindow contentWindow;
	
	private Project project;

	public ProjectAssetViewerWindow(int xOffset, int yOffset, int width, int height, Project project, Window parentWindow) {
		super(xOffset, yOffset, width, height, parentWindow);
		this.init(project);
	}
	
	private void init(Project project) {
		this.project = project;
		
		this.contentWindow = new ListViewerWindow(this, this);
		this.contentWindow.setAlignmentStyle(Window.FROM_LEFT, Window.FROM_BOTTOM);
		this.contentWindow.setCloseOnSubmit(false);
		
		this.topBarWindow = new ListViewerWindow(this, this);
		this.topBarWindow.setCloseOnSubmit(false);
		this.topBarWindow.setRenderTopBar(false);
		this.topBarWindow.setIsHorizontal(true);
		
		this.topBarWindow.setAlignmentStyle(Window.FROM_LEFT, Window.FROM_TOP);
		this.topBarWindow.setOffset(0, 0);
		
		this.topBarWindow.addToList(MODEL_STR);
		this.topBarWindow.addToList(TEXTURE_STR);
		this.topBarWindow.addToList(SOUND_STR);
		
		this._resize();
	}

	@Override
	protected void _kill() {
	}

	@Override
	protected void _resize() {
		this.contentWindow.setHeight(this.getHeight() - this.topBarHeightPx);
		
		this.topBarWindow.setHeight(this.topBarHeightPx);
	}

	@Override
	protected void _update() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void renderContent(Framebuffer outputBuffer) {
		// TODO Auto-generated method stub
		
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


