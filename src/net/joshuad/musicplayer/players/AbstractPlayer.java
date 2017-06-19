package net.joshuad.musicplayer.players;

import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

import net.joshuad.musicplayer.Track;

public abstract class AbstractPlayer {

	static final int NO_SEEK_REQUESTED = -1;

	SourceDataLine audioOutput; //Part of the contract is that the various players need to set this

	public abstract void pause();
	public abstract void play();
	public abstract void stop();
	public abstract void seekPercent ( double positionPercent );
	public abstract void seekMS ( long positionMS );
	public abstract boolean isPaused();
	public abstract Track getTrack();
	
	public void setVolumePercent ( double percent ) {
		
		if ( audioOutput == null ) {
			System.out.println ( "Cannot set volume, audioOutput is null" );
			return;
		}
		
		if ( audioOutput.isControlSupported( FloatControl.Type.VOLUME ) ) {
			FloatControl volume = (FloatControl)audioOutput.getControl( FloatControl.Type.VOLUME );
			
			double min = volume.getMinimum();
			double max = volume.getMaximum();
			double value = (max - min) * percent + min;
			
			volume.setValue( (float)value );
			
		} else if ( audioOutput.isControlSupported( FloatControl.Type.MASTER_GAIN ) ) {
			FloatControl volume = (FloatControl)audioOutput.getControl( FloatControl.Type.MASTER_GAIN );
			
			double min = volume.getMinimum();
			double max = volume.getMaximum();
			double value = (max - min) * percent + min;
			
			volume.setValue( (float)value );
			
		} else {
			System.out.println( "Volume control not supported." );
			//TODO: better UI stuff
		}
	}
	
	public long getPositionMS() {
		return (long)( audioOutput.getMicrosecondPosition() / 1e3 );
	}
}
