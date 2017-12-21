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
			throw new IllegalStateException ( "Unable to open stream" );
		}
		
		if ( audioOutput == null ) {
			throw new IllegalStateException ( "Stream opened, but audioOutput not initialized." );
		}
	}
	
	private static double volumeCurve ( double input ) {
		//Stubs for functions if we want to implement this in the future. 
		return input;
	}
	
	private static double inverseVolumeCurve ( double input ) {
		//Stubs for functions if we want to implement this in the future. 
		return input;
	}
	
	private void setVolume ( FloatControl control, double percent ) {
		String units = control.getUnits().toLowerCase();
		
		//I have not seen "decibel" in the wild, but it seems safe to check for it
		if ( units.equals( "db" ) || units.equals( "decibel" ) ) { 
			double min = control.getMinimum();
			double max = 0;			
			double value = (max - min) * volumeCurve ( percent ) + min;
			control.setValue( (float)value );
			
		} else {
			double min = control.getMinimum();
			double max = control.getMaximum();	
			double value = (max - min) * volumeCurve ( percent ) + min;
			System.out.println ( "setting to: " + value );
			control.setValue( (float)value );
		}
	}
	
	private double getVolume ( FloatControl control ) {
		String units = control.getUnits().toLowerCase();
	
		//I have not seen "decibel" in the wild, but it seems safe to check for it
		if ( units.equals( "db" ) || units.equals( "decibel" ) ) { 
			
			double min = control.getMinimum();
			double max = 0;
			double value = control.getValue();
			
			return inverseVolumeCurve ( ( value - min ) / ( max - min ) );
			
		} else {
			double min = control.getMinimum();
			double max = control.getMaximum();
			double value = control.getValue();
			
			return inverseVolumeCurve ( ( value - min ) / ( max - min ) );
		}
	}
	
	public double getVolumePercent () {
		try { 
			if ( audioOutput.isControlSupported( FloatControl.Type.VOLUME ) ) {
				FloatControl volume = (FloatControl)audioOutput.getControl( FloatControl.Type.VOLUME );
				return getVolume ( volume );
				
			} else if ( audioOutput.isControlSupported( FloatControl.Type.MASTER_GAIN ) ) {
				FloatControl masterGain = (FloatControl)audioOutput.getControl( FloatControl.Type.MASTER_GAIN );
				return getVolume ( masterGain );
				
			} else {
				return 1;
			}
		} catch ( Exception e ) {
			LOGGER.info ( "Unable to get volume percent, assuming 100%" );
			return 1;
		}
	}
	
	public void setVolumePercent ( double percent ) throws IllegalArgumentException {
		
		if ( audioOutput == null ) {
			LOGGER.info( "Cannot set volume, audioOutput is null" );
			return;
		}
		
		
		System.out.println( "Volume: " +  audioOutput.isControlSupported( FloatControl.Type.VOLUME ) );
		System.out.println( "Gain: " +  audioOutput.isControlSupported( FloatControl.Type.MASTER_GAIN ) );
		
		if ( audioOutput.isControlSupported( FloatControl.Type.VOLUME ) ) {
			FloatControl volume = (FloatControl)audioOutput.getControl( FloatControl.Type.VOLUME );
			setVolume ( volume, percent );
			
		} else if ( audioOutput.isControlSupported( FloatControl.Type.MASTER_GAIN ) ) {
			FloatControl masterGain = (FloatControl)audioOutput.getControl( FloatControl.Type.MASTER_GAIN );
			setVolume ( masterGain, percent );
			
		} else {
			throw new IllegalArgumentException( "Volume Control not supported by system for this audio format." );
		}
	}
	
	public boolean volumeChangeSupported() {
		
		if ( audioOutput == null ) {
			return true;
		}
		
		boolean volumeSupported = true, masterGainSupported = true;
		
		try {
			FloatControl volume = (FloatControl)audioOutput.getControl( FloatControl.Type.VOLUME );
		} catch ( Exception e ) {
			volumeSupported = false;
		}
		
		try {
			FloatControl volume = (FloatControl)audioOutput.getControl( FloatControl.Type.MASTER_GAIN );
		} catch ( Exception e ) {
			masterGainSupported = false;
		}
		
		if ( !volumeSupported && !masterGainSupported ) {
			return false;
		}
	
		return true;
	}
		
	
	public long getPositionMS() {
		if ( audioOutput != null ) {
			return (long)( audioOutput.getMicrosecondPosition() / 1e3 ) + clipStartTimeMS;
		} else {
			return 0;
		}
	}
	
	public Track getTrack () {
		return track;
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


