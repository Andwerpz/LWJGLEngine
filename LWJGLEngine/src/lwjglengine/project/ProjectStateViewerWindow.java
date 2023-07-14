package lwjglengine.project;

import java.awt.Color;
import java.util.ArrayList;

import lwjglengine.asset.Asset;
import lwjglengine.asset.StateAsset;
import lwjglengine.graphics.Framebuffer;
import lwjglengine.input.Button;
import lwjglengine.ui.UIElement;
import lwjglengine.window.AdjustableWindow;
import lwjglengine.window.ListViewerWindow;
import lwjglengine.window.Window;

public class ProjectStateViewerWindow extends ListViewerWindow {
	//just a wrapper to display the list of state assets. 

	private Project project;

	private Button createStateBtn;

	public ProjectStateViewerWindow(int xOffset, int yOffset, int width, int height, Project project, Window callbackWindow, Window parentWindow) {
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
			if (a instanceof StateAsset) {
				filteredAssets.add(a);
				filteredStrings.add(a.getName());
			}
		}
		this.setList(filteredAssets, filteredStrings);

		//create new state button
		this.createStateBtn = new Button(3, 0, 100, 16, "btn_new_state", "New State", 12, this.topBarSection.getSelectionScene(), this.topBarSection.getTextScene());
		this.createStateBtn.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_TOP);
		this.createStateBtn.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
		this.createStateBtn.getButtonText().setDoAntialiasing(false);
		this.createStateBtn.bind(this.topBarSection.getBackgroundRect());
	}

	@Override
	protected void submitEntries(Object[] objects) {
		Object o = objects[0];

		if (!(o instanceof StateAsset)) {
			return;
		}

		StateAsset state = (StateAsset) o;

		//open new state editor for this state. 
		Window w = new AdjustableWindow("Editing State: " + state.getName(), new ProjectStateEditorWindow(0, 0, 400, 400, this.project, state, null), this);
	}

	@Override
	protected void _mouseReleased(int button) {
		super._mouseReleased(button);

		if (this.createStateBtn.isClicked()) {
			//create new state dialogue window
			Window w = new AdjustableWindow("Create New State", new NewProjectStateWindow(0, 0, 400, 400, this.project, null), this);
		}
	}

}
