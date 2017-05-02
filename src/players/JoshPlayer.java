package players;

import java.io.*;
import javax.sound.sampled.*;

import gui.MusicPlayer;
import gui.Track;
import javafx.scene.control.Slider;

public final class JoshPlayer implements Runnable {
	
	private Track track = null;
	private FlacDecoder decodedInput;
	private SourceDataLine audioOutput;
	
	private boolean pauseRequested = false;
	private boolean playRequested = false;
	private boolean stopRequested = false;
	
	private boolean paused = false;
	
	Slider trackPosition;
	
	public JoshPlayer ( Track track, Slider trackPosition ) {
		this.track = track;
		this.trackPosition = trackPosition;
		
		try {
			decodedInput = new FlacDecoder ( track.getPath().toAbsolutePath().toFile() );
			if (decodedInput.numSamples == 0) throw new FlacDecoder.FormatException("Unknown audio length");
			
			AudioFormat outputFormat = new AudioFormat ( decodedInput.sampleRate, decodedInput.sampleDepth, decodedInput.numChannels, true, false );
			
			audioOutput = (SourceDataLine)AudioSystem.getLine( new DataLine.Info( SourceDataLine.class, outputFormat ) );
			audioOutput.open ( outputFormat );
			audioOutput.start();
			
			Thread t = new Thread ( this );
			t.setDaemon( true );
			t.start();
			
		} catch (IOException | LineUnavailableException e) {
			//TODO: IOException decoder failed to load. 
			//TODO: LineUnavailableException - if AudioSystem.getLine fails
			
			e.printStackTrace();
		}
	}
	
	
	public void run() {
		while (true) {
			
			if ( stopRequested ) {
				try {
					audioOutput.stop();
					audioOutput.close();
					decodedInput.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
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
									
			if ( !paused ) {
			
				try {
					long[][] samples = null;
					Object[] temp;
					temp = decodedInput.readNextBlock();
				
					if (temp != null) samples = (long[][])temp[0];
					
									
					// Wait when end of stream reached
					if (samples == null) {
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
			
			double timePos = audioOutput.getMicrosecondPosition() / 1e6;
			double positionPercent = timePos * decodedInput.sampleRate / decodedInput.numSamples;
			int timeElapsed = (int)(track.getLength() * positionPercent);
			int timeRemaining = track.getLength() - timeElapsed;
			MusicPlayer.updateTransport ( timeElapsed, -timeRemaining, positionPercent );
					
			
		}
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
	
	public boolean isPaused() {
		return paused;
	}
		
}
	
	
	
	
	
	
	
	
	
	
	
	
