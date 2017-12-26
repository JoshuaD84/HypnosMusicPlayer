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
	
	public static double logOfBase ( int base, double num ) {
		return Math.log ( num ) / Math.log ( base );
	}

	private static double volumeCurvePulse ( double input ) {
		if ( input <= 0 ) return 0;
		if ( input >= 1 ) return 1;
	
		double value = logOfBase( 5, 4 * input + 1 );
		
		if ( value < 0 ) value = 0;
		if ( value > 1 ) value = 1;
		
		return value;
	}
	
	private static double inverseVolumeCurvePulse ( double input ) {
		if ( input <= 0 ) return 0;
		if ( input >= 1 ) return 1;

		double value = ( Math.pow( 5, input ) - 1 ) / 4;
		
		if ( value < 0 ) value = 0;
		if ( value > 1 ) value = 1;
		
		return value;
	}
	
	private static double volumeCurveDB ( double input ) {
		if ( input <= 0 ) return 0;
		if ( input >= 1 ) return 1;
	
		double value = logOfBase( 200, 199 * input + 1 );
		
		if ( value < 0 ) value = 0;
		if ( value > 1 ) value = 1;
		
		return value;
	}
	
	private static double inverseVolumeCurveDB ( double input ) {
		if ( input <= 0 ) return 0;
		if ( input >= 1 ) return 1;

		double value = ( Math.pow( 200, input ) - 1 ) / 199;
		
		if ( value < 0 ) value = 0;
		if ( value > 1 ) value = 1;
		
		return value;
	}
	
	private void setVolume ( FloatControl control, double percent ) {
		String units = control.getUnits().toLowerCase();
		
		//I have not seen "decibel" in the wild, but it seems safe to check for it
		if ( units.equals( "db" ) || units.equals( "decibel" ) ) { 
			double min = control.getMinimum();
			double max = 0;			
			double value = (max - min) * volumeCurveDB ( percent ) + min;
			control.setValue( (float)value );
			
		} else {
			double min = control.getMinimum();
			double max = control.getMaximum();	
			double value = (max - min) * volumeCurvePulse ( percent ) + min;
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
			
			return inverseVolumeCurveDB ( ( value - min ) / ( max - min ) );
			
		} else {
			double min = control.getMinimum();
			double max = control.getMaximum();
			double value = control.getValue();
			
			return inverseVolumeCurvePulse ( ( value - min ) / ( max - min ) );
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
			audioOutput.getControl( FloatControl.Type.VOLUME );
		} catch ( Exception e ) {
			volumeSupported = false;
		}
		
		try {
			audioOutput.getControl( FloatControl.Type.MASTER_GAIN );
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


