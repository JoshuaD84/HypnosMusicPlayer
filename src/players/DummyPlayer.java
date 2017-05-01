package players;

//This class doesn't do anything, but it means we don't have to check for null all the time.

public class DummyPlayer extends AudioPlayer {

	@Override
	public void play() {
	}

	@Override
	public void stop() {
	}

	@Override
	public void seek() {
	}

	@Override
	public void pause() {
	}
	
	@Override
	public void unpause() {
	}
	
	@Override
	public void togglePause() {
	}

}
