package players;

public abstract class AudioPlayer {
	
	public abstract void play();
	public abstract void stop();
	public abstract void seek();
	public abstract void pause();
	public abstract void unpause();
	public abstract void togglePause();
}
