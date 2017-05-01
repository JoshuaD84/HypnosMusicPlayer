package players;
import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;

public class FLACPlayer extends AudioPlayer implements Runnable {

	private String file;
	private org.kc7bfi.jflac.apps.Player player;
	private Thread playerThread;
	
	
	public FLACPlayer(String str) {
	    file = str;
	
	    player = new org.kc7bfi.jflac.apps.Player();
	    this.playerThread = new Thread(this, "AudioPlayerThread");
	}
	
	public void play() {
	    playerThread.start();
	}
	
	public void stop() {
		player.stop();
	}

	@Override
	public void run() {
	    try {
	        player.decode(file);
	    } catch (IOException | LineUnavailableException e) {
	    	//TODO: 
	    }
	}

	@Override
	public void seek() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pause() {
		player.pause();
	}
	
	@Override
	public void unpause() {
		player.unpause();
	}
	
	@Override
	public void togglePause() {
		player.togglePause();
	}
}