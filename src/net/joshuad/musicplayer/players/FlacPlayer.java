package net.joshuad.musicplayer.players;

import java.io.*;
import javax.sound.sampled.*;

import javafx.scene.control.Slider;
import net.joshuad.musicplayer.MusicPlayerUI;
import net.joshuad.musicplayer.Track;

public final class FlacPlayer extends AbstractPlayer implements Runnable {
	
	private FlacDecoderLogic decodedInput;
	
	public FlacPlayer ( Track track, Slider trackPositionSlider, boolean startPaused ) throws IOException, LineUnavailableException {
		this.track = track;
		this.trackPosition = trackPositionSlider;
		this.pauseRequested = startPaused;
		
		decodedInput = new FlacDecoderLogic ( track.getPath().toAbsolutePath().toFile() );
		if ( decodedInput.numSamples == 0 ) throw new FlacDecoderLogic.FormatException("Unknown audio length");
		
		AudioFormat outputFormat = new AudioFormat ( decodedInput.sampleRate, decodedInput.sampleDepth, decodedInput.numChannels, true, false );
		
		audioOutput = (SourceDataLine)AudioSystem.getLine( new DataLine.Info( SourceDataLine.class, outputFormat ) );
		
		audioOutput.open ( outputFormat ); 
		clipStartTimeMS = 0;
		
		Thread t = new Thread ( this );
		t.setDaemon( true );
		t.start();
	}
	
	public FlacPlayer ( Track track, Slider trackPositionSlider ) throws IOException, LineUnavailableException {
		this ( track, trackPositionSlider, false );
	}
	
	public void run() {

		audioOutput.start();

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
				
				try {
					long samplePos = Math.round ( seekRequestPercent * decodedInput.numSamples );
	
					long[][] samples = decodedInput.seekAndReadBlock ( samplePos );
					audioOutput.flush();
					clipStartTimeMS = audioOutput.getMicrosecondPosition() - Math.round(samplePos * 1e6 / decodedInput.sampleRate);
				
					seekRequestPercent = -1;
					
					if (samples == null) {
						MusicPlayerUI.songFinishedPlaying( false );
						return;
					}
					
					//TODO: Why are we doing thi shere? looks wrong. 
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
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
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
				double timePos = ( audioOutput.getMicrosecondPosition() - clipStartTimeMS ) / 1e6;
				double positionPercent = timePos * decodedInput.sampleRate / decodedInput.numSamples;
				int timeElapsed = (int)(track.getLengthS() * positionPercent);
				int timeRemaining = track.getLengthS() - timeElapsed;
				MusicPlayerUI.updateTransport ( timeElapsed, -timeRemaining, positionPercent );
			}
		}
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
	
	
	
	
	
	
	
