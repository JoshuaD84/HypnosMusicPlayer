package net.joshuad.hypnos.players;

import java.io.*;
import javax.sound.sampled.*;

import javafx.scene.control.Slider;
import net.joshuad.hypnos.MusicPlayerUI;
import net.joshuad.hypnos.Track;

public final class FlacPlayer extends AbstractPlayer implements Runnable {
	
	private FlacDecoderLogic decodedInput;
	
	public FlacPlayer ( Track track, Slider trackPositionSlider, boolean startPaused ) throws IOException, LineUnavailableException {
		this.track = track;
		this.trackPosition = trackPositionSlider;
		this.pauseRequested = startPaused;
		
		Thread t = new Thread ( this );
		t.setDaemon( true );
		t.start();
	}
	
	public FlacPlayer ( Track track, Slider trackPositionSlider ) throws IOException, LineUnavailableException {
		this ( track, trackPositionSlider, false );
	}
	
	public void run() {

		boolean streamsOpen = openStreamsAtRequestedOffset();
		if ( !streamsOpen ) {
			//TODO: Logging
			System.out.println ( "Unable to open audio stream, not playing track." );
		}
		
		while ( true ) {	
			if ( stopRequested ) {
				closeAllResources();
				MusicPlayerUI.songFinishedPlaying( true );
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
				boolean streamsOpen = openStreamsAtRequestedOffset();
				if ( !streamsOpen ) {
					//TODO: Logging
					System.out.println ( "Unable to open audio stream, not playing track." );
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
					
									
					// Wait when end of stream reached
					if (samples == null) {
						closeAllResources();
						MusicPlayerUI.songFinishedPlaying( false );
						return;
					}
					
					// Convert samples to channel-interleaved bytes in little endian
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
					// TODO decodedInput.readNextBlock()
					e.printStackTrace();
				}
			} else {
				try {
					Thread.sleep ( 5 );
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
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
			
			AudioFormat outputFormat = new AudioFormat ( decodedInput.sampleRate, decodedInput.sampleDepth, decodedInput.numChannels, true, false );
			DataLine.Info datalineInfo = new DataLine.Info( SourceDataLine.class, outputFormat );

			try {
				audioOutput = (SourceDataLine) AudioSystem.getLine( datalineInfo );
				audioOutput.open( outputFormat );
			} catch ( LineUnavailableException exception ) {
				System.out.println( "The audio output line could not be opened due to resource restrictions." );
				System.err.println( exception );
				return false;
			} catch ( IllegalStateException exception ) {
				System.out.println( "The audio output line is already open." );
				System.err.println( exception );
				return false;
			} catch ( SecurityException exception ) {
				System.out.println( "The audio output line could not be opened due to security restrictions." );
				System.err.println( exception );
				return false;
			}
			
			
			clipStartTimeMS = 0;
			
			if ( seekRequestPercent != NO_SEEK_REQUESTED ) {
				long samplePos = Math.round ( seekRequestPercent * decodedInput.numSamples );
	
				long[][] samples = decodedInput.seekAndReadBlock ( samplePos );
				clipStartTimeMS = (long)( ( track.getLengthS() * 1000 ) * seekRequestPercent );

				seekRequestPercent = NO_SEEK_REQUESTED;
				
				if (samples == null) {
					//TODO: This is a problem, since it doesn't break teh loop up above now. 
					MusicPlayerUI.songFinishedPlaying( false );
					return false;
				}
			}

			audioOutput.start();
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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
			//TODO: 
			e.printStackTrace();
		}
	}
}
	
	
	
	
	
	
	
