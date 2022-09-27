package input;

import entity.Entity;

public abstract class Input extends Entity {

	protected boolean pressed, hovered, clicked;

	public Input() {
		super();

		this.pressed = false;
		this.hovered = false;
		this.clicked = false;
	}

	protected abstract void update();

	protected abstract void _kill();

	public void hovered(long entityID) {
		if(this.getID() != entityID) {
			this.hovered = false;
		}
		else {
			this.hovered = true;
		}
	}

	public void pressed(long entityID) {
		if(this.getID() != entityID) {
			return;
		}
		this.pressed = true;
	}

	public void released(long entityID) {
		if(this.pressed && entityID == this.getID()) {
			this.clicked = true;
		}
		else {
			this.clicked = false;
		}
		this.pressed = false;
	}

	public boolean isClicked() {
		return this.clicked;
	}

}
