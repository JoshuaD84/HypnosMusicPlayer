package net.joshuad.hypnos.audio;

import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.audio.PlayerController.RepeatMode;
import net.joshuad.hypnos.audio.PlayerController.ShuffleMode;

public interface PlayerListener {
	public void playerPositionChanged ( int positionMS, int lengthMS );
	public void playerStopped ( Track track, boolean userRequested );
	public void playerStarted ( Track track );
	public void playerPaused ();
	public void playerUnpaused (); 
	public void playerVolumeChanged ( double newVolumePercent );
	public void playerShuffleModeChanged ( ShuffleMode newMode );
	public void playerRepeatModeChanged ( RepeatMode newMode );
}
