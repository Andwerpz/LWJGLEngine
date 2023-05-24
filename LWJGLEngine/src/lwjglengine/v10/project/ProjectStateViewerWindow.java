package lwjglengine.v10.project;

import java.awt.Color;
import java.util.ArrayList;

import lwjglengine.v10.asset.Asset;
import lwjglengine.v10.asset.StateAsset;
import lwjglengine.v10.graphics.Framebuffer;
import lwjglengine.v10.input.Button;
import lwjglengine.v10.ui.UIElement;
import lwjglengine.v10.window.AdjustableWindow;
import lwjglengine.v10.window.ListViewerWindow;
import lwjglengine.v10.window.Window;

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
		this.createStateBtn = new Button(3, 0, 100, 16, "btn_new_state", "New State", 12, TOP_BAR_SELECTION_SCENE, TOP_BAR_TEXT_SCENE);
		this.createStateBtn.setFrameAlignmentStyle(UIElement.FROM_LEFT, UIElement.FROM_CENTER_TOP);
		this.createStateBtn.setContentAlignmentStyle(UIElement.ALIGN_LEFT, UIElement.ALIGN_CENTER);
		this.createStateBtn.getButtonText().setDoAntialiasing(false);
		this.createStateBtn.bind(this.topBarBackgroundRect);
	}

	@Override
	protected void submitEntry(Object o) {
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
