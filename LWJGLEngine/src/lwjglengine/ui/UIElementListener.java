package lwjglengine.ui;

public interface UIElementListener {

	public void uiElementChangedDimensions(UIElement e);

	public void uiElementChangedFrameAlignmentOffset(UIElement e);

	public void uiElementChangedFrameAlignmentStyle(UIElement e);

	public void uiElementChangedContentAlignmentStyle(UIElement e);

	public void uiElementChangedRotationRads(UIElement e);

}
