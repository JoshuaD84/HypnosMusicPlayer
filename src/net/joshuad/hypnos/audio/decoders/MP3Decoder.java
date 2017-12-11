package net.joshuad.hypnos.audio.decoders;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.mp3.MP3AudioHeader;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import net.joshuad.hypnos.Track;
import net.joshuad.hypnos.fxui.FXUI;

public class MP3Decoder extends AbstractDecoder {

	private static final Logger LOGGER = Logger.getLogger( MP3Decoder.class.getName() );
	
	private Bitstream encodedInput;
	private Decoder decoder;
	
	Track track;

	public MP3Decoder ( Track track ) {
		this.track = track;
		initialize();
	}
	
	public boolean openStreamsAt ( double seekPercent ) {
		FileInputStream fis;
		try {
			fis = new FileInputStream( track.getPath().toFile() );
		} catch (IOException e) {
			String message = "Unable to open mp3 file:\n\n" + track.getPath().toString() + "\n\nIt may be corrupt." ;
			LOGGER.log( Level.WARNING, message );
			FXUI.notifyUserError ( message );
			return false;
		}
        BufferedInputStream bis = new BufferedInputStream(fis);
		
		if ( seekPercent != 0 ) {
			int seekPositionMS = (int) ( track.getLengthS() * 1000 * seekPercent );
			long seekPositionByte = getBytePosition ( track.getPath().toFile(), seekPositionMS );
			
			try {
				bis.skip( seekPositionByte ); 
				clipStartTimeMS = seekPositionMS;
			} catch ( IOException e ) {
				String message = "Unable to seek.";
				LOGGER.log( Level.WARNING, message, e );
				FXUI.notifyUserError( message );
			}
		}
		
		encodedInput = new Bitstream( bis );
			
		decoder = new Decoder();
		
		AudioFormat outputFormat = new AudioFormat( 44100, 16, 2, true, false ); //TODO: Do we need to generalize this? 
		DataLine.Info datalineInfo = new DataLine.Info( SourceDataLine.class, outputFormat, AudioSystem.NOT_SPECIFIED );

		try {
			audioOutput = (SourceDataLine) AudioSystem.getLine( datalineInfo );
			audioOutput.open( outputFormat );
		} catch ( LineUnavailableException exception ) {
			String message = "The audio output line could not be opened due to resource restrictions.";
			LOGGER.log( Level.WARNING, message, exception );
			FXUI.notifyUserError( message );
			return false;
		} catch ( IllegalStateException exception ) {
			String message = "The audio output line is already open.";
			LOGGER.log( Level.WARNING, message, exception );
			FXUI.notifyUserError( message );
			return false;
		} catch ( SecurityException exception ) {
			String message = "The audio output line could not be opened due to security restrictions.";
			LOGGER.log( Level.WARNING, message, exception );
			FXUI.notifyUserError( message );
			return false;
		}

		audioOutput.start();
		
		return true;
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
	
	public void closeAllResources() {
		if ( audioOutput != null ) {
			audioOutput.flush();
			audioOutput.close();
		}
		
		try {
			encodedInput.close();
		} catch ( BitstreamException e) {
			LOGGER.log ( Level.INFO, "Unable to close mp3 file: " + track.getPath() );
		}
	}
	
	@Override
	public boolean playSingleFrame () {
		try {
			Header header = encodedInput.readFrame();
			
			if ( header == null ) {
				return true; 
			}

			SampleBuffer output = (SampleBuffer) decoder.decodeFrame( header, encodedInput );
			
			byte[] writeMe = convertToBytes ( output.getBuffer() );
			
			audioOutput.write( writeMe, 0, writeMe.length );
			
			encodedInput.closeFrame();
			
		} catch ( Exception e ) {
			LOGGER.log( Level.WARNING, "Unable to decode frame for file:" + track.getFilename(), e );
		}
		
		return false;
	}
	
	//REFACTOR: maybe just inline this and get rid of the function; it's only called in one place. 
	private byte[] convertToBytes ( short[] samples ) {
		
		byte[] b = new byte[ samples.length*2];
		int idx = 0;
		short s;
		int len = samples.length;
		int offs = 0;
		while ( len-- > 0)
		{
			s = samples[offs++];
			b[idx++] = (byte)s;
			b[idx++] = (byte)(s>>>8);
		}
		return b;
	}
}