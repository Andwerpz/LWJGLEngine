package lwjglengine.v10.entity;

import java.util.ArrayList;

import lwjglengine.v10.asset.Asset;
import lwjglengine.v10.asset.EntityAsset;
import lwjglengine.v10.asset.StateAsset;
import lwjglengine.v10.input.Button;
import lwjglengine.v10.project.Project;
import lwjglengine.v10.ui.UIElement;
import lwjglengine.v10.window.AdjustableWindow;
import lwjglengine.v10.window.ListViewerWindow;
import lwjglengine.v10.window.Window;

public class ProjectEntityViewerWindow extends ListViewerWindow {

	private Project project;

	private Button createEntityBtn;

	public ProjectEntityViewerWindow(int xOffset, int yOffset, int width, int height, Project project, Window callbackWindow, Window parentWindow) {
		super(xOffset, yOffset, width, height, callbackWindow, parentWindow);
		this.init(project);
	}

	private void init(Project project) {
		this.project = project;

		this.setDisplaySelectedEntryOnTopBar(false);
		this.setRenderBottomBar(false);
		this.setSubmitOnClickingSelectedListEntry(true);
		this.setSingleEntrySelection(true);
		this.setCloseOnSubmit(false);

		//extract all state assets. 
		ArrayList<Asset> assets = this.project.getAssetList();
		ArrayList<Asset> filteredAssets = new ArrayList<>();
		ArrayList<String> filteredStrings = new ArrayList<>();
		for (Asset a : assets) {
			if (a instanceof EntityAsset) {
				filteredAssets.add(a);
				filteredStrings.add(a.getName());
			}
		}
		this.setList(filteredAssets, filteredStrings);

		//create new state button
		this.createEntityBtn = new Button(3, 0, 100, 16, "btn_new_entity", "New Entity", 12, this.topBarSection.getSelectionScene(), this.topBarSection.getTextScene());
		this.createEntityBtn.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_TOP);
		this.createEntityBtn.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
		this.createEntityBtn.getButtonText().setDoAntialiasing(false);
		this.createEntityBtn.bind(this.topBarSection.getBackgroundRect());
	}

	@Override
	protected void submitEntries(Object[] objects) {
		Object o = objects[0];

		if (!(o instanceof EntityAsset)) {
			return;
		}

		EntityAsset entity = (EntityAsset) o;

		//open new state editor for this state. 
		Window w = new AdjustableWindow("Editing State: " + entity.getName(), new ProjectEntityEditorWindow(0, 0, 400, 400, this.project, entity, null), this);
	}

	@Override
	protected void _mouseReleased(int button) {
		super._mouseReleased(button);

		if (this.createEntityBtn.isClicked()) {
			//create new state dialogue window
			Window w = new AdjustableWindow("Create New Entity", new NewProjectEntityWindow(0, 0, 400, 400, this.project, null), this);
		}
	}

}
