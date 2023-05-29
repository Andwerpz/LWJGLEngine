package lwjglengine.v10.project;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;

import lwjglengine.v10.asset.Asset;
import lwjglengine.v10.asset.ModelAsset;
import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.graphics.Material;
import lwjglengine.v10.scene.Scene;
import lwjglengine.v10.screen.UIScreen;
import lwjglengine.v10.ui.Text;
import lwjglengine.v10.ui.UIElement;
import lwjglengine.v10.ui.UIFilledRectangle;
import lwjglengine.v10.window.ListViewerWindow;
import lwjglengine.v10.window.ModelAssetViewerWindow;
import lwjglengine.v10.window.Window;
import lwjglengine.v10.window.AdjustableWindow;
import myutils.v10.math.Vec3;

public class ProjectAssetViewerWindow extends Window {

	//views all file assets. 

	//TODO
	// - allow user to create folders within the project. 
	// - drag and drop files between folders?
	// - reuse file explorer window for this?

	public static final String OTHER_STR = "Other";
	public static final String STATE_STR = "States";
	public static final String ENTITY_STR = "Entities";
	public static final String MODEL_STR = "Models";
	public static final String TEXTURE_STR = "Textures";
	public static final String SOUND_STR = "Sounds";
	public static final String CUBEMAP_STR = "Cubemaps";

	private int topBarHeightPx = 20;

	private ListViewerWindow topBarWindow;
	private ListViewerWindow contentWindow;

	private Project project;

	private String selectedTopBarString = "";

	private Window callbackWindow;

	private boolean oneCategory = false;

	public ProjectAssetViewerWindow(int xOffset, int yOffset, int width, int height, Project project, Window callbackWindow, Window parentWindow) {
		super(xOffset, yOffset, width, height, parentWindow);
		this.init(project, callbackWindow);
	}

	public ProjectAssetViewerWindow(int xOffset, int yOffset, int width, int height, Project project, Window parentWindow) {
		super(xOffset, yOffset, width, height, parentWindow);
		this.init(project, null);
	}

	public ProjectAssetViewerWindow(Project project, Window callbackWindow, Window parentWindow) {
		super(0, 0, 300, 300, parentWindow);
		this.init(project, callbackWindow);
	}

	public ProjectAssetViewerWindow(Project project, Window parentWindow) {
		super(0, 0, 300, 300, parentWindow);
		this.init(project, null);
	}

	private void init(Project project, Window callbackWindow) {
		this.project = project;

		this.callbackWindow = callbackWindow;

		this.contentWindow = new ListViewerWindow(this, this);
		this.contentWindow.setAlignmentStyle(Window.FROM_LEFT, Window.FROM_BOTTOM);
		this.contentWindow.setCloseOnSubmit(false);
		this.contentWindow.setSingleEntrySelection(true);
		this.contentWindow.setRenderBottomBar(false);
		this.contentWindow.setSubmitOnClickingSelectedListEntry(true);

		this.topBarWindow = new ListViewerWindow(this, this);
		this.topBarWindow.setSortEntries(false);
		this.topBarWindow.setCloseOnSubmit(false);
		this.topBarWindow.setRenderTopBar(false);
		this.topBarWindow.setIsHorizontal(true);
		this.topBarWindow.setRenderBottomBar(false);
		this.topBarWindow.setSingleEntrySelection(true);
		this.topBarWindow.setSubmitOnClickingSelectedListEntry(false);
		this.topBarWindow.setEntryHeightPx(this.topBarHeightPx);

		this.topBarWindow.setAlignmentStyle(Window.FROM_LEFT, Window.FROM_TOP);
		this.topBarWindow.setOffset(0, topBarHeightPx);

		ArrayList<String> options = new ArrayList<>();
		options.add(STATE_STR);
		options.add(ENTITY_STR);
		options.add(MODEL_STR);
		options.add(TEXTURE_STR);
		options.add(SOUND_STR);
		options.add(CUBEMAP_STR);
		options.add(OTHER_STR);
		this.setAssetTypeCategories(options);

		this._resize();
	}

	public void setAssetTypeCategories(String[] categories) {
		ArrayList<String> arr = new ArrayList<>();
		for (String s : categories) {
			arr.add(s);
		}
		this.setAssetTypeCategories(arr);
	}

	public void setAssetTypeCategories(ArrayList<String> categories) {
		System.out.println("SETTING CATEGORIES : " + categories);
		this.topBarWindow.setList(categories);

		if (categories.size() == 1) {
			this.setSelectedAssetCategory(categories.get(0));

			this.oneCategory = true;

			this._resize();
		}
	}

	private void setSelectedAssetCategory(String category) {
		System.out.println("SELECTED CATEGORY : " + category);

		int type = -1;
		switch (category) {
		case STATE_STR:
			type = Asset.TYPE_STATE;
			break;

		case ENTITY_STR:
			type = Asset.TYPE_ENTITY;
			break;

		case MODEL_STR:
			type = Asset.TYPE_MODEL;
			break;

		case TEXTURE_STR:
			type = Asset.TYPE_TEXTURE;
			break;

		case SOUND_STR:
			type = Asset.TYPE_SOUND;
			break;

		case CUBEMAP_STR:
			type = Asset.TYPE_CUBEMAP;
			break;

		case OTHER_STR:
			type = Asset.TYPE_UNKNOWN;
			break;

		}

		System.out.println("SELECTED TYPE : " + type);

		ArrayList<Asset> filteredAssets = new ArrayList<>();
		ArrayList<String> filteredStrings = new ArrayList<>();
		ArrayList<Asset> assets = this.project.getAssetList();

		for (Asset a : assets) {
			if (a.getType() == type) {
				filteredAssets.add(a);
				filteredStrings.add(a.getName());

				System.out.println("FILTERED ASSET : " + a.getType() + " " + a.getName());
			}
		}

		this.contentWindow.setList(filteredAssets, filteredStrings);
	}

	@Override
	public void handleObjects(Object[] objects) {
		Object o = objects[0];

		if (!(o instanceof Asset)) {
			return;
		}

		if (this.callbackWindow != null) {
			this.callbackWindow.handleObjects(new Object[] { o });
			this.close();
			return;
		}

		Asset a = (Asset) o;

		switch (a.getType()) {
		case Asset.TYPE_MODEL: {
			try {
				Window w = new AdjustableWindow("Model Viewer", new ModelAssetViewerWindow((ModelAsset) a, this.project), this);
			}
			catch (IOException e) {
				System.err.println("Failed to generate model viewer");
				e.printStackTrace();
			}
			break;
		}
		}
	}

	@Override
	protected void _kill() {
	}

	@Override
	protected void _resize() {
		if (this.oneCategory) {
			this.contentWindow.setHeight(this.getHeight());
			this.contentWindow.setWidth(this.getWidth());

			this.topBarWindow.setHeight(this.topBarHeightPx);
			this.topBarWindow.setWidth(this.getWidth());

			this.topBarWindow.setXOffset(this.getWidth());
		}
		else {
			this.contentWindow.setHeight(this.getHeight() - this.topBarHeightPx);
			this.contentWindow.setWidth(this.getWidth());

			this.topBarWindow.setHeight(this.topBarHeightPx);
			this.topBarWindow.setWidth(this.getWidth());

			this.topBarWindow.setXOffset(0);
		}

	}

	@Override
	protected void _update() {
		if (this.topBarWindow.getSelectedEntryString() != this.selectedTopBarString) {
			this.selectedTopBarString = this.topBarWindow.getSelectedEntryString();
			this.setSelectedAssetCategory(this.selectedTopBarString);
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
