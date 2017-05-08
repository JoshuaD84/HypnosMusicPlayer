package org.joshuad.musicplayer.players;

//TODO: get rid of these *'s
import java.io.*;
import javax.sound.sampled.*;

import org.joshuad.musicplayer.MusicPlayerUI;
import org.joshuad.musicplayer.Track;

import javafx.scene.control.Slider;

public final class FlacPlayer extends AbstractPlayer implements Runnable {
	
	private Track track;
	private FlacDecoder decodedInput;
	private SourceDataLine audioOutput;
	
	private boolean pauseRequested = false;
	private boolean playRequested = false;
	private boolean stopRequested = false;
	private double seekRequestPercent = -1;	// -1 means no seek request pending. 
	private long clipStartTime = 0; //If we seek, we need to remember where we started so we can make the seek bar look right. 
	
	private boolean paused = false;
	
	Slider trackPosition;
	
	public FlacPlayer ( Track track, Slider trackPositionSlider ) {
		this.track = track;
		this.trackPosition = trackPositionSlider;
		track.setIsCurrentTrack( true );
		
		Thread t = new Thread ( this );
		t.setDaemon( true );
		t.start();
	}
	
	
	public void run() {
		try {
			decodedInput = new FlacDecoder ( track.getPath().toAbsolutePath().toFile() );
			if (decodedInput.numSamples == 0) throw new FlacDecoder.FormatException("Unknown audio length");
			
			AudioFormat outputFormat = new AudioFormat ( decodedInput.sampleRate, decodedInput.sampleDepth, decodedInput.numChannels, true, false );
			
			audioOutput = (SourceDataLine)AudioSystem.getLine( new DataLine.Info( SourceDataLine.class, outputFormat ) );
			audioOutput.open ( outputFormat );
			clipStartTime = 0;
			audioOutput.start();
			
		} catch (IOException | LineUnavailableException e) {
			//TODO: IOException decoder failed to load. 
			//TODO: LineUnavailableException - if AudioSystem.getLine fails
			
			e.printStackTrace();
			track.setIsCurrentTrack( false );
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
				
				try {
					long samplePos = Math.round ( seekRequestPercent * decodedInput.numSamples );
	
					long[][] samples = decodedInput.seekAndReadBlock ( samplePos );
					audioOutput.flush();
					clipStartTime = audioOutput.getMicrosecondPosition() - Math.round(samplePos * 1e6 / decodedInput.sampleRate);
				
					seekRequestPercent = -1;
					
					if (samples == null) {
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
				double timePos = ( audioOutput.getMicrosecondPosition() - clipStartTime ) / 1e6;
				double positionPercent = timePos * decodedInput.sampleRate / decodedInput.numSamples;
				int timeElapsed = (int)(track.getLength() * positionPercent);
				int timeRemaining = track.getLength() - timeElapsed;
				MusicPlayerUI.updateTransport ( timeElapsed, -timeRemaining, positionPercent );
			}
		}
	}
	
	private void closeAllResources()  {
		try {
			audioOutput.drain();
			audioOutput.stop();
			audioOutput.close();
			decodedInput.close();
		} catch ( IOException e) {
			//TODO: 
			e.printStackTrace();
		}
	}
	
	@Override 
	public void pause() {
		pauseRequested = true;
	}
	
	@Override 
	public void play() {
		playRequested = true;
		track.setIsCurrentTrack( true );
	}
	
	@Override 
	public void stop() {
		stopRequested = true;
		track.setIsCurrentTrack( false );
	}
	
	@Override 
	public void seek ( double positionPercent ) {
		seekRequestPercent = positionPercent;
	}
	
	@Override 
	public boolean isPaused() {
		return paused;
	}
	
	
		
}
	
	
	
	
	
	
	
