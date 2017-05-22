package org.joshuad.musicplayer.players;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.mp3.MP3AudioHeader;
import org.joshuad.musicplayer.MusicPlayerUI;
import org.joshuad.musicplayer.Track;

import javafx.scene.control.Slider;

public class OldMP3Player extends AbstractPlayer implements Runnable {

	private Track track;
	
	private static final int NO_SEEK_REQUESTED = -1;

	AudioInputStream encodedInput;
	AudioInputStream decodedInput;
	SourceDataLine audioOutput;

	private boolean pauseRequested = false;
	private boolean playRequested = false;
	private boolean stopRequested = false;
	private double seekRequestPercent = NO_SEEK_REQUESTED;	
	private long clipStartTime = 0; //If we seek, we need to remember where we started so we can make the seek bar look right. 
	
	private boolean paused = false;
	
	private Slider trackPosition;
	
	public OldMP3Player ( Track track, Slider trackPosition ) {
		this ( track, trackPosition, false );
	}
	
	public OldMP3Player ( Track track, Slider trackPosition, boolean startPaused ) {
		this.track = track;
		this.trackPosition = trackPosition;
		this.pauseRequested = startPaused;

		Thread t = new Thread ( this );
		t.setDaemon( true );
		t.start();
	}
	
	public void run() {

		try {
			openStreamsAtRequestedOffset ();
			audioOutput.start(); //TODO: deal with startPaused here instead of where I do it? 
			
		} catch ( LineUnavailableException | UnsupportedAudioFileException | IOException e ) {
			//TODO: We should break here; if the thing's not open then it's going to fail later. 
			e.printStackTrace();
		}

	    long bytePosition = 0;
	    
		while ( true ) {
			if ( stopRequested ) {
				closeAllResources();
				MusicPlayerUI.songFinishedPlaying( true );
				stopRequested = false;
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
			
			if ( seekRequestPercent != NO_SEEK_REQUESTED ) {
				
				try {
					closeAllResources();
					openStreamsAtRequestedOffset();
					audioOutput.start();
					 
				} catch ( UnsupportedAudioFileException | IOException | LineUnavailableException e ) {
					//TODO: 
				}
				
				seekRequestPercent = NO_SEEK_REQUESTED;
				updateTransport();
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
						bytePosition += bytesRead;
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
			updateTransport();
		}
	}
	
	private void updateTransport() {
		if ( seekRequestPercent == NO_SEEK_REQUESTED ) {
			double positionPercent = (double) ( audioOutput.getMicrosecondPosition() + clipStartTime * 1000 ) / ( (double) track.getLength() * 1000000 );
			int timeElapsed = (int)(track.getLength() * positionPercent);
			int timeRemaining = track.getLength() - timeElapsed;
			MusicPlayerUI.updateTransport ( timeElapsed, -timeRemaining, positionPercent );
		}
	}
	

	//I think these two functions are the only places where codec-specific magic has to happen? 
	private void openStreamsAtRequestedOffset ( ) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
		encodedInput = AudioSystem.getAudioInputStream( track.getPath().toFile() );
		
		if ( seekRequestPercent != NO_SEEK_REQUESTED ) {
			int seekPositionMS = (int) ( track.getLength() * 1000 * seekRequestPercent );
			long seekPositionByte = getBytePosition ( track.getPath().toFile(), seekPositionMS );
			byte[] data = new byte[ (int) seekPositionByte ]; 
			int bytesRead = encodedInput.read ( data, 0, data.length ); //For some reason, skip() doesn't work as well. 
			clipStartTime = seekPositionMS;
		}
		
		AudioFormat baseFormat = encodedInput.getFormat();
		AudioFormat decoderFormat = new AudioFormat (
				AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(),
				16, baseFormat.getChannels(), baseFormat.getChannels() * 2,
				baseFormat.getSampleRate(), false );
		
		decodedInput = AudioSystem.getAudioInputStream ( decoderFormat, encodedInput );
		
		DataLine.Info info = new DataLine.Info ( SourceDataLine.class, decoderFormat );
		audioOutput = (SourceDataLine) AudioSystem.getLine(info);
		audioOutput.open( decoderFormat );
	}
	
	public long getBytePosition ( File file, long targetTimeMS ) {

		long bytePosition = -1;

		try {

			AudioFile audioFile = AudioFileIO.read( file );
			AudioHeader audioHeader = audioFile.getAudioHeader();

			if ( audioHeader instanceof MP3AudioHeader ) {
				MP3AudioHeader mp3AudioHeader = (MP3AudioHeader) audioHeader;
				long audioStartByte = mp3AudioHeader.getMp3StartByte();
				long audioSize = file.length() - audioStartByte;
				long frameCount = mp3AudioHeader.getNumberOfFrames();
				long frameSize = audioSize / frameCount;

				double frameDurationInMs = (mp3AudioHeader.getPreciseTrackLength() / (double) frameCount) * 1000;
				double framesForMs = targetTimeMS / frameDurationInMs;
				long bytePositionForMs = (long) (audioStartByte + (framesForMs * frameSize));
				bytePosition = bytePositionForMs;
			}

			return bytePosition;

		} catch ( Exception e ) {
			return bytePosition;
		}
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
	}
	
	@Override 
	public void stop() {
		stopRequested = true;
	}
	
	@Override 
	public void seek ( double positionPercent ) {
		seekRequestPercent = positionPercent;
	}

	@Override
	public boolean isPaused() {
		return paused;
	}
	
	@Override
	public Track getTrack () {
		return track;
	}
}

