package net.joshuad.musicplayer.players;

import net.joshuad.musicplayer.Track;

public abstract class AbstractPlayer {
	public abstract void pause();
	public abstract void play();
	public abstract void stop();
	public abstract void seek ( double positionPercent );
	public abstract boolean isPaused();
	public abstract Track getTrack();
}
