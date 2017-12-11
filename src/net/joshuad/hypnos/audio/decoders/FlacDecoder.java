package net.joshuad.hypnos.audio.decoders;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.fxui.FXUI;

public class FlacDecoder extends AbstractDecoder {

	private static final Logger LOGGER = Logger.getLogger( FlacDecoder.class.getName() );
	
	private FlacDecoderLogic decodedInput;
	
	private boolean resourcesOpened = false;
	private boolean resourcesClosed = false;
	
	public FlacDecoder ( Track track ) {
		this.track = track;
		initialize();
	}

	@Override
	public void closeAllResources() {
		
		if ( !resourcesClosed ) {
			resourcesClosed = true;
			System.out.println ( "-6" ); //TODO: DD
			if ( audioOutput.isOpen() ) {
				SourceDataLine closeMe = audioOutput;
				audioOutput = null;
				try {
					Thread.sleep ( 5 ); //This sleep fixes the close problem. 
				} catch ( InterruptedException e ) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println ( "-5" ); //TODO: DD
				closeMe.close(); //TODO: This is where the lock occurs. 
			}
			
			System.out.println ( "-4" ); //TODO: DD
			try {
				if ( decodedInput != null ) {
					System.out.println ( "-3" ); //TODO: DD
					decodedInput.close();
				}
				
			} catch ( IOException e ) {
				LOGGER.log ( Level.INFO, "Unable to close flac file reader for: " + track.getPath() );
			}
			System.out.println ( "-2" ); //TODO: DD
		} else {
			LOGGER.warning( "Tried to close resources twice. Ignored." );
		}
	}

	@Override
	public boolean playSingleFrame () {
		try {
			long[][] samples = null;
			Object[] temp;
			temp = decodedInput.readNextBlock();
		
			if (temp != null) samples = (long[][])temp[0];
							
			if (samples == null) { // End of stream
				//closeAllResources(); //TODO: DELETE THIS? It is called twice. 
				return true;
			}
			
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
			
		} catch (IOException e) {
			LOGGER.log( Level.INFO, "Error reading block from flac file.", e );
		}
		
		return false;
	}

	@Override
	public boolean openStreamsAt ( double seekPercent ) {
		if ( !resourcesOpened ) {
			try {
	
				decodedInput = new FlacDecoderLogic ( track.getPath().toAbsolutePath().toFile() );
				if ( decodedInput.numSamples == 0 ) throw new FlacDecoderLogic.FormatException("Unknown audio length");
			} catch ( IOException e ) {
				String message = "Unable to decode flac file:" + track.getPath().toString();
				LOGGER.log( Level.WARNING, message );
				FXUI.notifyUserError( message );
				return false;
			}
			
			AudioFormat outputFormat = new AudioFormat ( decodedInput.sampleRate, decodedInput.sampleDepth, decodedInput.numChannels, true, false );
			DataLine.Info datalineInfo = new DataLine.Info( SourceDataLine.class, outputFormat );
	
			try {
				audioOutput = (SourceDataLine) AudioSystem.getLine( datalineInfo );
				audioOutput.open( outputFormat );
			} catch ( LineUnavailableException exception ) {
				String message = "The audio output line could not be opened due to resource restrictions.";
				LOGGER.log( Level.WARNING, message, exception );
				FXUI.notifyUserError( message );
				return false;
			} catch ( IllegalStateException exception ) {
				String message = "The audio output line is already open.";
				LOGGER.log( Level.WARNING, message, exception );
				FXUI.notifyUserError( message );
				return false;
			} catch ( SecurityException exception ) {
				String message = "The audio output line could not be opened due to security restrictions.";
				LOGGER.log( Level.WARNING, message, exception );
				FXUI.notifyUserError( message );
				return false;
			} 
			
			
			clipStartTimeMS = 0;
			
			if ( seekPercent != 0 ) {
				long samplePos = Math.round ( seekPercent * decodedInput.numSamples );
				
				try {
					long[][] samples = decodedInput.seekAndReadBlock ( samplePos );
					
					if (samples == null) {
						return false;
					}
					
				} catch ( IOException e ) {
					String message = "Unable to seek.";
					LOGGER.log( Level.WARNING, message, e );
					FXUI.notifyUserError( message );
				}
				
				clipStartTimeMS = (long)( ( track.getLengthS() * 1000 ) * seekPercent );
			}
	
			audioOutput.start();
		
			return true;
		} else {
			LOGGER.warning( "Tried to open resources twice. Ignored." );
			return false;
		}
	}
}


