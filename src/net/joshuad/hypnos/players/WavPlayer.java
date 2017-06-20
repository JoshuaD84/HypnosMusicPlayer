package net.joshuad.hypnos.players;

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
import net.joshuad.hypnos.MusicPlayerUI;
import net.joshuad.hypnos.Track;

public class WavPlayer extends AbstractPlayer implements Runnable {
	
	AudioInputStream decodedInput;
	
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
					byte[] data = new byte[ 4096 ];
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
	
	private void openStreamsAtRequestedOffset ( ) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
		decodedInput = AudioSystem.getAudioInputStream( track.getPath().toFile() );
		
		if ( seekRequestPercent != NO_SEEK_REQUESTED ) {
			int seekPositionMS = (int) ( track.getLengthS() * 1000 * seekRequestPercent );
			long bytesRead = decodedInput.skip ( getBytePosition ( seekPositionMS ) );
			clipStartTimeMS = seekPositionMS;
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
}