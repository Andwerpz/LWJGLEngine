package lwjglengine.ui;

import lwjglengine.graphics.Material;
import lwjglengine.model.FilledRectangle;
import lwjglengine.model.Line;
import lwjglengine.model.ModelInstance;
import lwjglengine.scene.Scene;
import myutils.math.Vec2;

public class UILine implements UIElementListener {
	//a jank way to align both ends of the line. 
	//for each line, we'll have a 1x1 rectangle that the user can define where to align. 
	//then the line is just drawn between these two rectangles. 
	
	private int scene;
	private UIFilledRectangle e1, e2;
	
	private ModelInstance line;

	public UILine(int _scene) {
		this.init(_scene);
	}
	
	private void init(int _scene) {
		this.scene = _scene;
		this.e1 = new UIFilledRectangle(0, 0, 0, 1, 1, Scene.TEMP_SCENE);
		this.e2 = new UIFilledRectangle(0, 0, 0, 1, 1, Scene.TEMP_SCENE);
		
		this.e1.addListener(this);
		this.e2.addListener(this);
		
		this.line = Line.addDefaultLine(0, 0, 0, 0, this.scene);
	}
	
	public UIFilledRectangle getE1() {
		return this.e1;
	}
	
	public UIFilledRectangle getE2() {
		return this.e2;
	}
	
	public void align() {
		Vec2 v1 = new Vec2(this.e1.getGlobalAlignedX(), this.e1.getGlobalAlignedY());
		Vec2 v2 = new Vec2(this.e2.getGlobalAlignedX(), this.e2.getGlobalAlignedY());
		this.line.setModelTransform(Line.generateLineModelTransform(v1, v2));
	}
	
	public void setMaterial(Material m) {
		this.line.setMaterial(m);
	}

	public void kill() {
		this.e1.kill();
		this.e2.kill();
		this.line.kill();
	}

	@Override
	public void uiElementChangedDimensions(UIElement e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void uiElementChangedFrameAlignmentOffset(UIElement e) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void uiElementChangedGlobalFrameAlignmentOffset(UIElement e) {
		this.align();
	}

	@Override
	public void uiElementChangedFrameAlignmentStyle(UIElement e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void uiElementChangedContentAlignmentStyle(UIElement e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void uiElementChangedRotationRads(UIElement e) {
		// TODO Auto-generated method stub
		
	}

}
