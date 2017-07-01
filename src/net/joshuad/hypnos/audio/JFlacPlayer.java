package net.joshuad.hypnos.audio;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import org.jaudiotagger.audio.flac.FlacAudioHeader;

import javafx.scene.control.Slider;
import net.joshuad.hypnos.PlayerController;
import net.joshuad.hypnos.Track;

public class JFlacPlayer extends AbstractPlayer implements Runnable {
	
	private static final Logger LOGGER = Logger.getLogger( JFlacPlayer.class.getName() );

	AudioInputStream encodedInput;
	AudioInputStream decodedInput;

	public JFlacPlayer (Track track, PlayerController player, Slider trackPositionSlider ) {
		this ( track, player, trackPositionSlider, false );
	}
	
	public JFlacPlayer ( Track track, PlayerController player, Slider trackPositionSlider, boolean startPaused ) {
		this.track = track;
		this.player = player;
		this.trackPosition = trackPositionSlider;
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
				player.songFinishedPlaying( true );
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
					e.printStackTrace ( System.out ); //TODO: 
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
						player.songFinishedPlaying( false );
						return;
						
					} else {
						audioOutput.write(data, 0, bytesRead);
						bytePosition += bytesRead;
					}
				} catch ( IOException e ) {
					e.printStackTrace ( System.out ); //TODO: 
				}
			} else {
				try {
					Thread.sleep ( 5 );
				} catch (InterruptedException e) {
					e.printStackTrace ( System.out ); //TODO: 
				}
			}
			updateTransport();
		}
	}

	//I think these two functions are the only places where codec-specific magic has to happen? 
	private void openStreamsAtRequestedOffset ( ) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
		encodedInput = AudioSystem.getAudioInputStream( track.getPath().toFile() );
		
		AudioFormat baseFormat = encodedInput.getFormat();
		AudioFormat decoderFormat;
		
		if ( baseFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED ) {
			
			decoderFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED, 
                baseFormat.getSampleRate(),
                (baseFormat.getSampleSizeInBits() > 0) ? baseFormat.getSampleSizeInBits() : 16,
                baseFormat.getChannels(),
                (baseFormat.getSampleSizeInBits() > 0) ? baseFormat.getChannels() * baseFormat.getSampleSizeInBits() / 8 : baseFormat.getChannels() * 2,
                baseFormat.getSampleRate(),
                false
            );

			decodedInput = AudioSystem.getAudioInputStream ( decoderFormat, encodedInput );
			
		} else {
			decodedInput = encodedInput;
			decoderFormat = baseFormat;
		}
		
		if ( seekRequestPercent != NO_SEEK_REQUESTED ) {
			long seekPositionByte = getBytePosition ( track.getPath().toFile(), seekRequestPercent );
			int bytesRead = 0;

			byte[] skippedData = new byte[ 256 ];
			while ( bytesRead < seekPositionByte ) {
				int bytesSkipped = decodedInput.read ( skippedData );
				bytesRead += bytesSkipped;
			}
			
			clipStartTimeMS = (long)(track.getLengthS() * seekRequestPercent) * 1000;
		}

		
		DataLine.Info info = new DataLine.Info ( SourceDataLine.class, decoderFormat );
		audioOutput = (SourceDataLine) AudioSystem.getLine(info);
		audioOutput.open( decoderFormat );
	}
	
	public long getBytePosition ( File file, double requestPercent ) {
		try {

			AudioFile audioFile = AudioFileIO.read( file );
			AudioHeader audioHeader = audioFile.getAudioHeader();
			
			if ( audioHeader instanceof FlacAudioHeader ) {
				FlacAudioHeader flacAudioHeader = (FlacAudioHeader) audioHeader;
				
				long decodedLengthBytes = flacAudioHeader.getNoOfSamples() * flacAudioHeader.getBitsPerSample() / 8  * 2;
				double lengthMS = flacAudioHeader.getPreciseTrackLength() * 1000;
				int byteTarget = (int)(requestPercent * decodedLengthBytes);
				
				return byteTarget;
			}

			return 0;

		} catch ( Exception e ) {
			e.printStackTrace( System.out );
			return 0;
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
			LOGGER.log ( Level.INFO, "Unable to close flac file reader for: " + track.getPath() );
		}
	}
}

