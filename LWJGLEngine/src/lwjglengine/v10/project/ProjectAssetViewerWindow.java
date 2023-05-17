package lwjglengine.v10.project;

import java.awt.Color;
import java.util.ArrayList;

import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.graphics.Material;
import lwjglengine.v10.scene.Scene;
import lwjglengine.v10.screen.UIScreen;
import lwjglengine.v10.ui.Text;
import lwjglengine.v10.ui.UIElement;
import lwjglengine.v10.ui.UIFilledRectangle;
import lwjglengine.v10.window.ListViewerWindow;
import lwjglengine.v10.window.ModelViewerWindow;
import lwjglengine.v10.window.Window;
import lwjglengine.v10.window.AdjustableWindow;
import myutils.v10.math.Vec3;

public class ProjectAssetViewerWindow extends Window {
	
	//TODO
	// - allow user to create folders within the project. 
	// - drag and drop files between folders?
	
	private static final String MODEL_STR = "Models";
	private static final String TEXTURE_STR = "Textures";
	private static final String SOUND_STR = "Sounds";
	private static final String OTHER_STR = "Other";
	
	private int topBarHeightPx = 20;
	
	private Material topBarDefaultMaterial = new Material(new Vec3((float) (20 / 255.0)));
	private Material topBarHoveredMaterial = new Material(new Vec3((float) (30 / 255.0)));
	private Material topBarSelectedMaterial = new Material(new Vec3((float) (40 / 255.0)));
	
	private ListViewerWindow topBarWindow;
	private ListViewerWindow contentWindow;
	
	private Project project;

	private String selectedTopBarString = "";
	
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
		this.topBarWindow.setEntryHeightPx(this.topBarHeightPx);
		
		this.topBarWindow.setAlignmentStyle(Window.FROM_LEFT, Window.FROM_TOP);
		this.topBarWindow.setOffset(0, topBarHeightPx);
		
		this.topBarWindow.addToList(MODEL_STR);
		this.topBarWindow.addToList(TEXTURE_STR);
		this.topBarWindow.addToList(SOUND_STR);
		this.topBarWindow.addToList(OTHER_STR);
		
		this._resize();
	}
	
	@Override
	public void handleObject(Object o) {
		if(!(o instanceof Asset)) {
			return;
		}
		
		Asset a = (Asset) o;
		
		switch(a.getType()) {
		case Asset.TYPE_MODEL:{
			//Model m = new Model
			Window w = new AdjustableWindow("Model Viewer", new ModelViewerWindow(), this);
			break;
		}
		
		case Asset.TYPE_SOUND: {
			break;
		}
		
		case Asset.TYPE_TEXTURE: {
			break;
		}
		}
	}

	@Override
	protected void _kill() {
	}

	@Override
	protected void _resize() {
		this.contentWindow.setHeight(this.getHeight() - this.topBarHeightPx);
		this.contentWindow.setWidth(this.getWidth());
		
		this.topBarWindow.setHeight(this.topBarHeightPx);
		this.topBarWindow.setWidth(this.getWidth());
	}

	@Override
	protected void _update() {
		if(this.topBarWindow.getSelectedEntryString() != this.selectedTopBarString) {
			this.selectedTopBarString = this.topBarWindow.getSelectedEntryString();
			
			int type = -1;
			switch(this.selectedTopBarString) {
			case MODEL_STR: {
				type = Asset.TYPE_MODEL;
				break;
			}
				
			case TEXTURE_STR: {
				type = Asset.TYPE_TEXTURE;
				break;
			}
			
			case SOUND_STR: {
				type = Asset.TYPE_SOUND;
				break;
			}
			
			case OTHER_STR: {
				type = Asset.TYPE_OTHER;
				break;
			}
			}
			
			ArrayList<Asset> filteredAssets = new ArrayList<>();
			ArrayList<String> filteredStrings = new ArrayList<>();
			ArrayList<Asset> assets = this.project.getAssetList();
			
			for(Asset a : assets) {
				if(a.getType() == type) {
					filteredAssets.add(a);
					filteredStrings.add(a.getFilename());
				}
			}
			
			this.contentWindow.setList(filteredAssets, filteredStrings);
		}
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


