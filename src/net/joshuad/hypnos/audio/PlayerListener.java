package net.joshuad.hypnos.audio;

import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.audio.AudioSystem.RepeatMode;
import net.joshuad.hypnos.audio.AudioSystem.ShuffleMode;
import net.joshuad.hypnos.audio.AudioSystem.StopReason;

public interface PlayerListener {
	public void playerPositionChanged ( int positionMS, int lengthMS );
	public void playerStopped ( Track track, StopReason reason );
	public void playerStarted ( Track track );
	public void playerPaused ();
	public void playerUnpaused (); 
	public void playerVolumeChanged ( double newVolumePercent );
	public void playerShuffleModeChanged ( ShuffleMode newMode );
	public void playerRepeatModeChanged ( RepeatMode newMode );
}
