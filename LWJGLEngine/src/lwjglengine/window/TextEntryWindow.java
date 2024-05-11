package lwjglengine.window;

import lwjglengine.graphics.Framebuffer;

public class TextEntryWindow extends Window {

	private TextEntryWindowCallback callback;

	public TextEntryWindow(TextEntryWindowCallback callback, Window parentWindow) {
		super(parentWindow);
		this.init(callback);
	}

	private void init(TextEntryWindowCallback callback) {
		this.callback = callback;
	}

	@Override
	protected void _kill() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _resize() {
		// TODO Auto-generated method stub

	}

	@Override
	public String getDefaultTitle() {
		// TODO Auto-generated method stub
		return null;
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

	public interface TextEntryWindowCallback {
		void handleCallback(String text);
	}

}
