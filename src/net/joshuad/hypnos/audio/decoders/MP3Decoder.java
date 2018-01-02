package net.joshuad.hypnos.audio.decoders;

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
import org.jaudiotagger.audio.mp3.MP3AudioHeader;

import net.joshuad.hypnos.Hypnos;
import net.joshuad.hypnos.Track;

public class MP3Decoder extends AbstractDecoder {

	private static final Logger LOGGER = Logger.getLogger( MP3Decoder.class.getName() );
	
	AudioInputStream encodedInput;
	AudioInputStream decodedInput;
	
	private byte[] data = new byte [ 4096 ]; //probably better to avoid reallocating this on the stack 
	
	Track track;

	public MP3Decoder ( Track track ) {
		this.track = track;
		initialize();
	}
	
	@Override
	public boolean openStreamsAt ( double seekPercent ) {
		try {
			encodedInput = AudioSystem.getAudioInputStream( track.getPath().toFile() );
			
			AudioFormat baseFormat = encodedInput.getFormat();
			
			AudioFormat decodedFormat = new AudioFormat( 
				AudioFormat.Encoding.PCM_SIGNED, 
				baseFormat.getSampleRate(), 
				16, 
				baseFormat.getChannels(), 
				baseFormat.getChannels() * 2,
				baseFormat.getSampleRate(), 
				false 
			);
			
			DataLine.Info info = new DataLine.Info( SourceDataLine.class, decodedFormat );
			
			decodedInput = AudioSystem.getAudioInputStream( decodedFormat, encodedInput );
			
			if ( seekPercent != 0 ) {
				int seekPositionMS = (int) ( track.getLengthS() * 1000 * seekPercent );
				long seekPositionByte = getBytePosition ( track.getPath().toFile(), seekPositionMS );
				
				try {
					decodedInput.skip( seekPositionByte );
					clipStartTimeMS = seekPositionMS;
				} catch ( IOException e ) {
					String message = "Unable to seek.";
					LOGGER.log( Level.WARNING, message, e );
					Hypnos.getUI().notifyUserError( message );//TODO: get rid of this call to hypnos, inject the ui instead
				}
			}
			
			audioOutput = (SourceDataLine) AudioSystem.getLine( info );
			audioOutput.open( decodedFormat );
			audioOutput.start();
					
		} catch ( UnsupportedAudioFileException e ) {
			String message = track.getPath().toString() + "\n\n" +
				"The audio format is not supported.";
			LOGGER.log( Level.WARNING, message, e );
			Hypnos.getUI().notifyUserError( message + "\n\n" + e.getMessage() ); //TODO: get rid of this call to hypnos, inject the ui instead
			return false;
			
		} catch ( SecurityException e ) {
			String message = track.getPath().toString() + "\n\n" +
				"The audio output line could not be opened due to security restrictions.";
			LOGGER.log( Level.WARNING, message, e );
			Hypnos.getUI().notifyUserError( message + "\n\n" + e.getMessage() ); //TODO: get rid of this call to hypnos, inject the ui instead
			return false;
			
		} catch ( IOException e ) {
			String message = track.getPath().toString() + "\n\n" +
				"Difficulty reading the file.";
			LOGGER.log( Level.WARNING, message, e );
			Hypnos.getUI().notifyUserError( message + "\n\n" + e.getMessage() ); //TODO: get rid of this call to hypnos, inject the ui instead
			return false;
		
		} catch ( LineUnavailableException e ) {
			String message = track.getPath().toString() + "\n\n" +
				"Unable to get an audio output line.";
			LOGGER.log( Level.WARNING, message, e );
			Hypnos.getUI().notifyUserError( message + "\n\n" + e.getMessage() ); //TODO: get rid of this call to hypnos, inject the ui instead
			return false;
			
		} catch ( IllegalArgumentException e ) {
			String message = 
				track.getPath().toString() + "\n\n" +
				"Hypnos and your sound system are not able to support this audio format.";
			LOGGER.log( Level.WARNING, message, e ); 
			Hypnos.getUI().notifyUserError( message + "\n\n" + e.getMessage() ); //TODO: get rid of this call to hypnos, inject the ui instead
			return false;
		}
				
		return true;	
	}
	
	@Override
	public void closeAllResources() {
		if ( audioOutput != null ) {
			audioOutput.flush();
			audioOutput.close();
			try {
				decodedInput.close();
				encodedInput.close();
			} catch ( IOException e) {
				LOGGER.log ( Level.INFO, "Unable to close mp3 file: " + track.getPath() );
			}
		}
	}
	
	@Override
	public boolean playSingleFrame () {
		try {
			int nBytesRead = decodedInput.read( data, 0, data.length );
			if ( nBytesRead != -1 ) {
				int nBytesWritten = audioOutput.write( data, 0, nBytesRead );
			} else {
				return true;
			}
			
		} catch ( IOException e ) {
			e.printStackTrace();
			return true;
		}
		
		return false;
	}
	
	private long getBytePosition ( File file, long targetTimeMS ) {

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
}