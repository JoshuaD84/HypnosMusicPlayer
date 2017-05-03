package org.joshuad.musicplayer.players;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.joshuad.musicplayer.MusicPlayerUI;
import org.joshuad.musicplayer.Track;
import org.tritonus.share.sampled.file.TAudioFileFormat;

import javafx.scene.control.Slider;

public class MP3Player extends AbstractPlayer implements Runnable {

	private Track track;

	AudioInputStream encodedInput;
	AudioInputStream decodedInput;
	SourceDataLine audioOutput;

	private boolean pauseRequested = false;
	private boolean playRequested = false;
	private boolean stopRequested = false;
	private double seekRequestPercent = -1;	// -1 means no seek request pending. 
	private long clipStartTime = 0; //If we seek, we need to remember where we started so we can make the seek bar look right. 
	
	private boolean paused = false;
	
	private Slider trackPosition;
		
	public MP3Player ( Track track, Slider trackPosition ) {
		this.track = track;
		this.trackPosition = trackPosition;
		track.setIsCurrentTrack( true );

		Thread t = new Thread ( this );
		t.setDaemon( true );
		t.start();
	}
	
	public void run() {

	    long framePosition = 0;
		try {
			encodedInput = AudioSystem.getAudioInputStream( track.getPath().toFile() );
			
			AudioFormat baseFormat = encodedInput.getFormat();
			AudioFormat decoderFormat = new AudioFormat(
					AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(),
					16, baseFormat.getChannels(), baseFormat.getChannels() * 2,
					baseFormat.getSampleRate(), false );
			
			decodedInput = AudioSystem.getAudioInputStream ( decoderFormat, encodedInput );
			
			audioOutput = getLine ( decoderFormat );
			audioOutput.start();
			
		} catch ( LineUnavailableException | UnsupportedAudioFileException | IOException e ) {
			e.printStackTrace();
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
								
				System.out.println ( "Current Position (ms): " + audioOutput.getMicrosecondPosition() );
				System.out.println ( "Current Position (Frames): " + framePosition );
				System.out.println ( "Length of Song (ms): " + track.getLength() * 1000000 );
				System.out.println ( "Length of song, in frames (decoded): " + decodedInput.getFrameLength() );
				System.out.println ( "Length of song, in frames (encoded): " + encodedInput.getFrameLength() );
				System.out.println ( "Frame size: " + decodedInput.getFormat().getFrameSize() );
				System.out.println ( "Sample Rate: " + decodedInput.getFormat().getSampleRate() );
				System.out.println ( "Frame Length (property): " +	decodedInput.getFormat().getProperty("mp3.length.frames" ) );
				
				
				//Frames = bytesRead / frameSize

				
				
				//TODO: 
				
				/* 
				 * tools:
				 *
				 *decodedInput.skip( long n ) // Skip n bytes
				 * audioOutput.getMicrosecondPosition()
				 * track.getLength() * 1000000 )
				 */
				
				//(double) audioOutput.getMicrosecondPosition() / ( (double) track.getLength() * 1000000 );
				
				
				
				
				seekRequestPercent = -1;
				 
			}
			
			if ( !paused ) {
				try {
					byte[] data = new byte[4096];
					int bytesRead = decodedInput.read ( data, 0, data.length );
					
					if ( bytesRead < 0 ) {
						closeAllResources();
						MusicPlayerUI.songFinishedPlaying( false );
						return;
						
					} else {
						audioOutput.write(data, 0, bytesRead);
						framePosition += bytesRead / decodedInput.getFormat().getFrameSize();
					}
				} catch ( IOException e ) {
					//TODO: 
				}
			} else {
				try {
					Thread.sleep ( 5 );
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			double timePos = ( audioOutput.getMicrosecondPosition() - clipStartTime ) / 1e6;
			
			double positionPercent = (double) audioOutput.getMicrosecondPosition() / ( (double) track.getLength() * 1000000 );
			int timeElapsed = (int)(track.getLength() * positionPercent);
			int timeRemaining = track.getLength() - timeElapsed;
			MusicPlayerUI.updateTransport ( timeElapsed, -timeRemaining, positionPercent );
		}

	}

	private SourceDataLine getLine(AudioFormat audioFormat) throws LineUnavailableException {
		SourceDataLine res = null;
		DataLine.Info info = new DataLine.Info ( SourceDataLine.class, audioFormat );
		res = (SourceDataLine) AudioSystem.getLine(info);
		res.open(audioFormat);
		return res;
	}
	
	private void closeAllResources() {
		try {
			audioOutput.drain();
			audioOutput.stop();
			audioOutput.close();
			decodedInput.close();
			encodedInput.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
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

