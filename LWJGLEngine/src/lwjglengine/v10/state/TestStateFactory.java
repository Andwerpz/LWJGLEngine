package lwjglengine.v10.state;

public class TestStateFactory extends StateFactory {

	@Override
	public State createState() {
		return new TestState();
	}

}
