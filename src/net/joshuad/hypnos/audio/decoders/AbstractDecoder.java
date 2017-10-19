package net.joshuad.hypnos.audio.decoders;

import java.util.logging.Logger;

import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

import net.joshuad.hypnos.Track;

public abstract class AbstractDecoder {

	private static final Logger LOGGER = Logger.getLogger( AbstractDecoder.class.getName() );
	
	SourceDataLine audioOutput; 
	Track track;
	long clipStartTimeMS = 0; //If we seek, we need to remember where we started so we can make the seek bar look right. 

	public abstract void closeAllResources ();	
	public abstract boolean playSingleFrame ();
	public abstract boolean openStreamsAt ( double seekPercent );
		
	public void initialize() {
		boolean streamOpened = openStreamsAt ( 0 );
		if ( streamOpened == false ) {
			//TODO: handle this better? 
			throw new IllegalStateException ( "Unable to open stream" );
		}
		
		if ( audioOutput == null ) {
			throw new IllegalStateException ( "Stream opened, but audioOutput not initialized." );
		}
	}
	
	public void setVolumePercent ( double percent ) throws IllegalArgumentException {
		
		if ( audioOutput == null ) {
			LOGGER.info( "Cannot set volume, audioOutput is null" );
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
			throw new IllegalArgumentException( "Volume Control not supported by system for this audio format." );
		}
	}
	
	public boolean volumeChangeSupported() {
		
		if ( audioOutput == null ) {
			return false;
		}
		
		boolean volumeSupported = true, masterGainSupported = true;
		try {
			FloatControl volume = (FloatControl)audioOutput.getControl( FloatControl.Type.VOLUME );
		} catch ( Exception e ) {
			volumeSupported = false;
		}
		
		try {
			FloatControl volume = (FloatControl)audioOutput.getControl( FloatControl.Type.VOLUME );
			volume = (FloatControl)audioOutput.getControl( FloatControl.Type.MASTER_GAIN );
		} catch ( Exception e ) {
			masterGainSupported = false;
		}
		
		if ( !volumeSupported && !masterGainSupported ) {
			return false;
		}
	
		return true;
	}
		
	
	public long getPositionMS() {
		return (long)( audioOutput.getMicrosecondPosition() / 1e3 ) + clipStartTimeMS;
	}
	
	public Track getTrack () {
		return track;
	}
	
	public double getVolumePercent () {
		try { 
			if ( audioOutput.isControlSupported( FloatControl.Type.VOLUME ) ) {
				FloatControl volume = (FloatControl)audioOutput.getControl( FloatControl.Type.VOLUME );
				double min = volume.getMinimum();
				double max = volume.getMaximum();
				double value = volume.getValue();
				
				return ( value - min ) / ( max - min );
				
			} else if ( audioOutput.isControlSupported( FloatControl.Type.MASTER_GAIN ) ) {
				FloatControl volume = (FloatControl)audioOutput.getControl( FloatControl.Type.MASTER_GAIN );
				double min = volume.getMinimum();
				double max = volume.getMaximum();
				double value = volume.getValue();
				
				return ( value - min ) / ( max - min );
				
			} else {
				return 1;
			}
		} catch ( Exception e ) {
			//TODO: Log? I don't know
			return 1;
		}
	}

	public void seekTo ( double seekPercent ) {
		closeAllResources();
		openStreamsAt ( seekPercent );
	}
	
	public void pause() {
		audioOutput.stop();
	}
	
	public void unpause() {
		audioOutput.start();
	}
}


