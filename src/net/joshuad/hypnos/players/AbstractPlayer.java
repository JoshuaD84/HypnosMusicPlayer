package net.joshuad.hypnos.players;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

import javafx.scene.control.Slider;
import net.joshuad.hypnos.MusicPlayerUI;
import net.joshuad.hypnos.Track;

//TODO: finish refactoring this stuff
public class AbstractPlayer {

	private static final Logger LOGGER = Logger.getLogger( AbstractPlayer.class.getName() );

	static final int NO_SEEK_REQUESTED = -1;

	SourceDataLine audioOutput; //Part of the contract is that the various players need to set this
	Track track;
	Slider trackPosition;
	
	boolean pauseRequested = false;
	boolean playRequested = false;
	boolean stopRequested = false;
	double seekRequestPercent = -1;	// -1 means no seek request pending. 
	long clipStartTimeMS = 0; //If we seek, we need to remember where we started so we can make the seek bar look right. 
	
	boolean paused = false;
	
	public static AbstractPlayer getPlayer ( Track track, Slider trackPositionSlider, boolean startPaused ) {
		
		AbstractPlayer currentPlayer = null;
		
		switch ( track.getFormat() ) {
			case FLAC:
				try {
					currentPlayer = new FlacPlayer( track, trackPositionSlider, startPaused );
				} catch ( Exception e ) {
					LOGGER.log( Level.WARNING, "Using backup flac decoder for: " + track.getPath() );
					e.printStackTrace();
					currentPlayer = new JFlacPlayer ( track, trackPositionSlider, startPaused );
				}
				break;
				
			case MP3:
				currentPlayer = new MP3Player( track, trackPositionSlider, startPaused );
				break;
				
			case AAC:
				currentPlayer = new MP4Player( track, trackPositionSlider, startPaused );
				break;
				
			case OGG:
				try {
					currentPlayer = new OggPlayer( track, trackPositionSlider, startPaused );
				} catch ( IOException e ) {
					//TODO: 
					e.printStackTrace();
				}
				break;
				
			case WAV:
				currentPlayer = new WavPlayer ( track, trackPositionSlider, startPaused );
				break;
			
			case UNKNOWN:
			default:
				//TODO: 
				System.out.println ( "Unknown music file type" );
				break;
		}
		
		return currentPlayer;
	}
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
		System.out.println ( ( audioOutput.getMicrosecondPosition() / 1e3 ) + " + " + clipStartTimeMS );
		return (long)( audioOutput.getMicrosecondPosition() / 1e3 ) + clipStartTimeMS;
	}
	
	public void pause() {
		pauseRequested = true;
	}
	
	public void play() {
		playRequested = true;
	}
	
	public void stop() {
		stopRequested = true;
	}
	
	public void seekPercent ( double positionPercent ) {
		seekRequestPercent = positionPercent;
		updateTransport();
	}
	
	public void seekMS ( long milliseconds ) {
		seekRequestPercent = milliseconds / (double)( track.getLengthS() * 1000 );	

	}

	public boolean isPaused() {
		return paused;
	}
	
	public Track getTrack () {
		return track;
	}
	
	void updateTransport() {
		if ( seekRequestPercent == NO_SEEK_REQUESTED ) {
			double positionPercent = (double) getPositionMS() / ( (double) track.getLengthS() * 1000 );
			int timeElapsed = (int)(track.getLengthS() * positionPercent);
			int timeRemaining = track.getLengthS() - timeElapsed;
			MusicPlayerUI.updateTransport ( timeElapsed, -timeRemaining, positionPercent );
		} else {
			int timeElapsed = (int)(track.getLengthS() * seekRequestPercent);
			int timeRemaining = track.getLengthS() - timeElapsed;
			MusicPlayerUI.updateTransport ( timeElapsed, -timeRemaining, seekRequestPercent );
		}
	}
}


