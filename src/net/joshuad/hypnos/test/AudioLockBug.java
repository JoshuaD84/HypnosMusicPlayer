package net.joshuad.hypnos.test;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

import net.joshuad.hypnos.audio.decoders.AbstractDecoder;
import net.joshuad.hypnos.audio.decoders.FlacDecoderLogic;

public class AudioLockBug {
	private static final Logger LOGGER = Logger.getLogger( AudioLockBug.class.getName() );
	
	
	public static void main ( String[] args ) {
		
		for ( int k = 0; k < 50 ; k ++ ) {
			openAndCloseLine();
			System.out.println ( "Successes: " + ( k + 1 ) );
		}
	}
	
	public static void openAndCloseLine() {
		try {
			FlacDecoderLogic decodedInput = new FlacDecoderLogic ( new File ( "/home/joshua/Desktop/hypnos-test/test.flac" ) );
			if ( decodedInput.numSamples == 0 ) throw new FlacDecoderLogic.FormatException("Unknown audio length");
			
			AudioFormat outputFormat = new AudioFormat ( 44100, 16, 2, true, false ); 
			DataLine.Info datalineInfo = new DataLine.Info( SourceDataLine.class, outputFormat );
			SourceDataLine audioOutput = (SourceDataLine) AudioSystem.getLine( datalineInfo );
			
			audioOutput.open( outputFormat );
			
			//Seek percent? FlacDecoder line 130ish
			
			audioOutput.start();
			
			setVolumePercent ( 1, audioOutput ); // I assumed value is 1 here, if we can't reproduce try confirming this value
			
			
			try {
				long[][] samples = null;
				Object[] temp;
				temp = decodedInput.readNextBlock();
			
				if (temp != null) samples = (long[][])temp[0];
								
				if (samples == null) { // End of stream
					audioOutput.close();
				} else {
					
					int bytesPerSample = decodedInput.sampleDepth / 8;
					byte[] sampleBytes = new byte[samples[0].length * samples.length * bytesPerSample];
					for (int i = 0, k = 0; i < samples[0].length; i++) {
						for (int ch = 0; ch < samples.length; ch++) {
							for (int j = 0; j < bytesPerSample; j++, k++) {
								sampleBytes[k] = (byte)(samples[ch][i] >>> (j << 3));
							}
						}
					}
					
					audioOutput.write ( sampleBytes, 0, sampleBytes.length );
				}
				
			} catch (IOException e) {
				LOGGER.log( Level.INFO, "Error reading block from flac file.", e );
			}
			
			audioOutput.close();
			
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}
	
	public static void setVolumePercent ( double percent, SourceDataLine audioOutput ) throws IllegalArgumentException {
		
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
}
