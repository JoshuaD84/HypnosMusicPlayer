package org.joshuad.musicplayer.players;

public abstract class AbstractPlayer {
	public abstract void pause();
	public abstract void play();
	public abstract void stop();
	public abstract void seek ( double positionPercent );
	public abstract boolean isPaused();
}
