package net.joshuad.hypnos.audio.decoders;

import java.io.IOException;
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
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

import net.joshuad.hypnos.Track;

public class WavDecoder extends AbstractDecoder {
	
	private static final Logger LOGGER = Logger.getLogger( WavDecoder.class.getName() );

	AudioInputStream decodedInput;
	Track track;

	public WavDecoder ( Track track ) {
		this.track = track;
		initialize();
	}
	
	@Override
	public void closeAllResources() {
		if ( audioOutput != null ) {
			audioOutput.drain();
			audioOutput.stop();
			audioOutput.close();
		}
		
		try {
			if ( decodedInput != null ) {
				decodedInput.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public boolean playSingleFrame () {
		
		try {
			byte[] data = new byte[ 4096 ];
			int bytesRead = decodedInput.read ( data, 0, data.length );
			
			if ( bytesRead < 0 ) {
				closeAllResources();
				return true;
				
			} else {
				audioOutput.write(data, 0, bytesRead);
			}
		} catch ( IOException e ) {
			LOGGER.warning( "Error reading from wav file." );
		}
		
		System.out.println ( " Returning false from play frame" );
		return false;
	}

	@Override
	public boolean openStreamsAt ( double seekPercent ) {
	
		try {
			decodedInput = AudioSystem.getAudioInputStream( track.getPath().toFile() );
		} catch ( UnsupportedAudioFileException | IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		if ( seekPercent != 0 ) {
			int seekPositionMS = (int) ( track.getLengthS() * 1000 * seekPercent );
			
			try {
				long bytesRead = decodedInput.skip ( getBytePosition ( seekPositionMS ) );
			} catch ( IOException e ) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false; //TODO: maybe return true? I don't think so. If it can't seek, probably best to stop? Maybe keep playing.. 
			}
			clipStartTimeMS = seekPositionMS;
		}
		
		AudioFormat decoderFormat = decodedInput.getFormat();
		
		DataLine.Info info = new DataLine.Info( SourceDataLine.class, decoderFormat );
		try {
			audioOutput = (SourceDataLine) AudioSystem.getLine( info );
			audioOutput.open( decoderFormat );
		} catch ( LineUnavailableException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		audioOutput.start(); 
		return true;
	}
	
	private long getBytePosition ( long targetTimeMS ) {
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

}