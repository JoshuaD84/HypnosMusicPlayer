package net.joshuad.hypnos.audio;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.*;

import javafx.scene.control.Slider;
import net.joshuad.hypnos.FXUI;
import net.joshuad.hypnos.PlayerController;
import net.joshuad.hypnos.Track;

public final class FlacPlayer extends AbstractPlayer implements Runnable {

	private static final Logger LOGGER = Logger.getLogger( FlacPlayer.class.getName() );
	
	private FlacDecoderLogic decodedInput;
	
	public FlacPlayer ( Track track, PlayerController player, Slider trackPositionSlider, boolean startPaused ) {
		this.track = track;
		this.player = player;
		this.trackPosition = trackPositionSlider;
		this.pauseRequested = startPaused;
		
		openStreamsAtRequestedOffset(); //We do this here to throw an exception we can catch if we can't open the streams with this player. 
		closeAllResources();
		
		Thread t = new Thread ( this );
		t.setDaemon( true );
		t.start();
	}
	
	public FlacPlayer ( Track track, PlayerController player, Slider trackPositionSlider ) {
		this ( track, player, trackPositionSlider, false );
	}
	
	public void run() {

		boolean streamsOpen = openStreamsAtRequestedOffset();
		if ( !streamsOpen ) {
			closeAllResources();
			player.songFinishedPlaying( false );
			return;
		}
		
		while ( true ) {	
			if ( stopRequested ) {
				closeAllResources();
				player.songFinishedPlaying( true );
				return;
			}				
				
			if ( pauseRequested ) {
				audioOutput.stop();
				pauseRequested = false;
				paused = true;
			}
			
			if ( playRequested ) {
				audioOutput.start();
				playRequested = false;
				paused = false;
			}
			
			if ( seekRequestPercent != -1 ) {
				
				closeAllResources();
				streamsOpen = openStreamsAtRequestedOffset();
				if ( !streamsOpen ) {
					closeAllResources();
					player.songFinishedPlaying( false );
					return;
				}
				
				seekRequestPercent = NO_SEEK_REQUESTED;
				updateTransport();
			}
									
			if ( !paused ) {
			
				try {
					long[][] samples = null;
					Object[] temp;
					temp = decodedInput.readNextBlock();
				
					if (temp != null) samples = (long[][])temp[0];
									
					if (samples == null) { // End of stream
						closeAllResources();
						player.songFinishedPlaying( false );
						return;
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
			} else {
				try {
					Thread.sleep ( 5 );
				} catch (InterruptedException e) {
					LOGGER.log ( Level.FINER, "Sleep interrupted during paused" );
				}
			}
			
			if ( seekRequestPercent == -1 && !stopRequested ) {
				updateTransport();
			}
		}
	}
	
	public boolean openStreamsAtRequestedOffset () {
		try {

			decodedInput = new FlacDecoderLogic ( track.getPath().toAbsolutePath().toFile() );
			if ( decodedInput.numSamples == 0 ) throw new FlacDecoderLogic.FormatException("Unknown audio length");
		} catch (IOException e) {
			String message = "Unable to decode flac file:\n\n" + track.getPath().toString() + "\n\nIt may be corrupt." ;
			LOGGER.log( Level.WARNING, message );
			FXUI.notifyUserError ( message );
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
			
			if ( seekRequestPercent != NO_SEEK_REQUESTED ) {
				long samplePos = Math.round ( seekRequestPercent * decodedInput.numSamples );
				
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
				
				clipStartTimeMS = (long)( ( track.getLengthS() * 1000 ) * seekRequestPercent );
				seekRequestPercent = NO_SEEK_REQUESTED;
			}

			audioOutput.start();
		
		return true;
	}
		
	private void closeAllResources()  {
		audioOutput.drain();
		audioOutput.stop();
		audioOutput.close();
		
		try {
			if ( decodedInput != null ) {
				decodedInput.close();
			}
			
		} catch ( IOException e) {
			LOGGER.log ( Level.INFO, "Unable to close flac file reader for: " + track.getPath() );
		}
	}
}
	
	
	
	
	
	
	
