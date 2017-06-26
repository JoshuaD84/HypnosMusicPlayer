package net.joshuad.hypnos.players;

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

import javafx.scene.control.Slider;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.decoder.SampleBuffer;
import net.joshuad.hypnos.MusicPlayerUI;
import net.joshuad.hypnos.Track;

public class MP3Player extends AbstractPlayer implements Runnable {

	private static final Logger LOGGER = Logger.getLogger( MP3Player.class.getName() );
	
	private Bitstream encodedInput;
	private Decoder decoder;

	public MP3Player ( Track track, Slider trackPosition ) {
		this ( track, trackPosition, false );
	}
	
	public MP3Player ( Track track, Slider trackPosition, boolean startPaused ) {
		this.track = track;
		this.trackPosition = trackPosition;
		this.pauseRequested = startPaused;

		Thread t = new Thread ( this );
		t.setDaemon( true );
		t.start();
	}
	
	public void run() {

		boolean streamsOpen = openStreamsAtRequestedOffset();
						
		if ( !streamsOpen ) {
			closeAllResources();
			MusicPlayerUI.songFinishedPlaying( false );
			return;
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
				pauseRequested = false;
				paused = true;
			}
			
			if ( playRequested ) {
				playRequested = false;
				paused = false;
			}
			
			if ( seekRequestPercent != NO_SEEK_REQUESTED ) {
				
				closeAllResources();
				
				streamsOpen = openStreamsAtRequestedOffset();
				
				if ( !streamsOpen ) {
					closeAllResources();
					MusicPlayerUI.songFinishedPlaying( false );
					return;
				}
				
				seekRequestPercent = NO_SEEK_REQUESTED;
				updateTransport();
			}
			
			if ( !paused ) {
				try {
					Header header = encodedInput.readFrame();
					
					if ( header == null ) {
						closeAllResources();
						MusicPlayerUI.songFinishedPlaying( false );
						return; 
					}
	
					SampleBuffer output = (SampleBuffer) decoder.decodeFrame( header, encodedInput );
					
					byte[] writeMe = convertToBytes ( output.getBuffer() );
					
					audioOutput.write( writeMe, 0, writeMe.length );
					
					
					encodedInput.closeFrame();
					
				} catch ( JavaLayerException e ) {
					e.printStackTrace();
				}
			} else {
				try {
					Thread.sleep ( 5 );
				} catch (InterruptedException e) {
					LOGGER.log ( Level.FINER, "Sleep interrupted during paused" );
				}
			}
			updateTransport();
		}
	}
	
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
	
	public boolean openStreamsAtRequestedOffset () {
		FileInputStream fis;
		try {
			fis = new FileInputStream( track.getPath().toFile() );
		} catch (IOException e) {
			String message = "Unable to open mp3 file:\n\n" + track.getPath().toString() + "\n\nIt may be corrupt." ;
			LOGGER.log( Level.WARNING, message );
			MusicPlayerUI.notifyUserError ( message );
			return false;
		}
        BufferedInputStream bis = new BufferedInputStream(fis);
		
		if ( seekRequestPercent != NO_SEEK_REQUESTED ) {
			int seekPositionMS = (int) ( track.getLengthS() * 1000 * seekRequestPercent );
			long seekPositionByte = getBytePosition ( track.getPath().toFile(), seekPositionMS );
			
			try {
				bis.skip( seekPositionByte ); 
				clipStartTimeMS = seekPositionMS;
			} catch ( IOException e ) {
				String message = "Unable to seek.";
				LOGGER.log( Level.WARNING, message, e );
				MusicPlayerUI.notifyUserError( message );
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
			MusicPlayerUI.notifyUserError( message );
			return false;
		} catch ( IllegalStateException exception ) {
			String message = "The audio output line is already open.";
			LOGGER.log( Level.WARNING, message, exception );
			MusicPlayerUI.notifyUserError( message );
			return false;
		} catch ( SecurityException exception ) {
			String message = "The audio output line could not be opened due to security restrictions.";
			LOGGER.log( Level.WARNING, message, exception );
			MusicPlayerUI.notifyUserError( message );
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
	
	private void closeAllResources() {
		try {
			audioOutput.flush();
			audioOutput.close();
			encodedInput.close();
		} catch ( BitstreamException e) {
			LOGGER.log ( Level.INFO, "Unable to close mp3 file: " + track.getPath() );
		}
	}
	
	protected boolean decodeFrame () throws JavaLayerException {
		try {

			
		} catch ( RuntimeException ex ) {
			throw new JavaLayerException( "Exception decoding audio frame", ex );
		}
		return true;
	}
}