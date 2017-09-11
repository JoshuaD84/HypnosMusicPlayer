package net.joshuad.hypnos.audio.decoders;

import java.io.File;
import java.io.FileNotFoundException;
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

import net.joshuad.hypnos.Track;

public class BackupFlacDecoder extends AbstractDecoder {

	private static final Logger LOGGER = Logger.getLogger( BackupFlacDecoder.class.getName() );

	AudioInputStream encodedInput;
	AudioInputStream decodedInput;
	
	public BackupFlacDecoder ( Track track ) {
		this.track = track;
		initialize();
	}
	
	@Override
	public void closeAllResources () {
		audioOutput.stop();
		audioOutput.close();
		try {
			decodedInput.close();
			encodedInput.close();
		} catch (IOException e) {
			LOGGER.log ( Level.INFO, "Unable to close flac file reader for: " + track.getPath() );
		}
		
	}

	@Override
	public boolean playSingleFrame () {
		try {
			byte[] data = new byte[4096];
			int bytesRead = decodedInput.read ( data, 0, data.length );
			
			if ( bytesRead < 0 ) {
				closeAllResources();
				return true;
				
			} else {
				audioOutput.write(data, 0, bytesRead);
			}
		} catch (IOException e ) {
			//TODO: 
			e.printStackTrace();
		}
		
		return false;
	}

	@Override
	public boolean openStreamsAt ( double seekPercent ) {
		try {
			encodedInput = AudioSystem.getAudioInputStream( track.getPath().toFile() );
		} catch ( FileNotFoundException e ) {
			LOGGER.warning( "File not found: " + track.getPath().toString() );
			return false;
		} catch ( IOException e ) {
			LOGGER.warning( "Unable to open: " + track.getPath().toString() );
			return false;
		} catch ( UnsupportedAudioFileException e ) {
			LOGGER.warning( "Unsupported file type: " + track.getPath().toString() );
			return false;
		}
			
		
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
		
		if ( seekPercent != 0 ) {
			long seekPositionByte = getBytePosition ( track.getPath().toFile(), seekPercent );
			int bytesRead = 0;

			byte[] skippedData = new byte[ 256 ];
			while ( bytesRead < seekPositionByte ) {
				int bytesSkipped;
				try {
					bytesSkipped = decodedInput.read ( skippedData );
				} catch ( IOException e ) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return false; //TODO: Maybe return true? see OggPlayer or M4APlyaer same question
				}
				bytesRead += bytesSkipped;
			}
			
			clipStartTimeMS = (long)(track.getLengthS() * seekPercent) * 1000;
		}

		
		DataLine.Info info = new DataLine.Info ( SourceDataLine.class, decoderFormat );
		try {
			audioOutput = (SourceDataLine) AudioSystem.getLine(info);
			audioOutput.open( decoderFormat );
		} catch ( LineUnavailableException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		audioOutput.start();
		return true;
	}
	
	private long getBytePosition ( File file, double requestPercent ) {
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

}
