package net.joshuad.musicplayer.players;

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
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

import javafx.scene.control.Slider;
import net.joshuad.musicplayer.MusicPlayerUI;
import net.joshuad.musicplayer.Track;

public class WavPlayer extends AbstractPlayer implements Runnable {

	private Track track;

	private static final int NO_SEEK_REQUESTED = -1;

	AudioInputStream decodedInput;
	SourceDataLine audioOutput;

	private boolean pauseRequested = false;
	private boolean playRequested = false;
	private boolean stopRequested = false;
	private double seekRequestPercent = -1;	// -1 means no seek request pending. 
	private long clipStartTime = 0; //If we seek, we need to remember where we started so we can make the seek bar look right. 
	
	private boolean paused = false;
	
	private Slider trackPosition;
	
	private final int EXTERNAL_BUFFER_SIZE = 4096; 
	
	
	public WavPlayer ( Track track, Slider trackPosition ) {
		this ( track, trackPosition, false );
	}

	public WavPlayer ( Track track, Slider trackPosition, boolean startPaused ) {
		this.track = track;
		this.trackPosition = trackPosition;
		this.pauseRequested = startPaused;

		Thread t = new Thread ( this );
		t.setDaemon( true );
		t.start();
	}
	

	public void run () {

		try {
			openStreamsAtRequestedOffset ();
			audioOutput.start();
			
		} catch ( IOException | UnsupportedAudioFileException | LineUnavailableException e1 ) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
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
					byte[] data = new byte[ EXTERNAL_BUFFER_SIZE ];
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
			//System.out.println ( "Clip start time: " + clipStartTime );
			double positionPercent = (double) ( audioOutput.getMicrosecondPosition() + clipStartTime * 1000 ) / ( (double) track.getLengthS() * 1000000 );
			int timeElapsed = (int)(track.getLengthS() * positionPercent);
			int timeRemaining = track.getLengthS() - timeElapsed;
			MusicPlayerUI.updateTransport ( timeElapsed, -timeRemaining, positionPercent );
		} else {
			int timeElapsed = (int)(track.getLengthS() * seekRequestPercent);
			int timeRemaining = track.getLengthS() - timeElapsed;
			MusicPlayerUI.updateTransport ( timeElapsed, -timeRemaining, seekRequestPercent );
		}
	}
	
	private void openStreamsAtRequestedOffset ( ) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
		decodedInput = AudioSystem.getAudioInputStream( track.getPath().toFile() );
		
		if ( seekRequestPercent != NO_SEEK_REQUESTED ) {
			int seekPositionMS = (int) ( track.getLengthS() * 1000 * seekRequestPercent );
			long bytesRead = decodedInput.skip ( getBytePosition ( seekPositionMS ) );
			clipStartTime = seekPositionMS;
		}
		
		AudioFormat decoderFormat = decodedInput.getFormat();
		
		DataLine.Info info = new DataLine.Info( SourceDataLine.class, decoderFormat );
		audioOutput = (SourceDataLine) AudioSystem.getLine( info );
		audioOutput.open( decoderFormat );
		
	}
	
	public long getBytePosition ( long targetTimeMS ) {
		int headerOffset = 0;
		try {
			AudioFile audioFile = AudioFileIO.read( track.getPath().toFile() );
			AudioHeader audioHeader = audioFile.getAudioHeader();
		} catch ( CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException e ) {
			//Can't read header, no big deal. Just assume 0. 
		}

		double lengthMS = decodedInput.getFrameLength() / decodedInput.getFormat().getFrameRate() * 1000;
		long lengthBytes = track.getPath().toFile().length();
		
		double targetPositionBytes = headerOffset + ( targetTimeMS / lengthMS ) * lengthBytes;
		
		return Math.round ( targetPositionBytes );
	}
	
	private void closeAllResources() {
		try {
			audioOutput.drain();
			audioOutput.stop();
			audioOutput.close();
			decodedInput.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public long getPositionMS() {
		return (long)( audioOutput.getMicrosecondPosition() / 1e6 );
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
	public void seekPercent ( double positionPercent ) {
		seekRequestPercent = positionPercent;
		updateTransport();
	}
	
	@Override 
	public void seekMS ( long milliseconds ) {
		seekRequestPercent = milliseconds / ( track.getLengthS() * 1000 );
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