package net.joshuad.musicplayer.players;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

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
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;
import net.joshuad.musicplayer.MusicPlayerUI;
import net.joshuad.musicplayer.Track;

public class MP3Player extends AbstractPlayer implements Runnable {
	
	private Bitstream encodedInput;
	private Decoder decoder;
	private AudioDevice audioOut;

	private Track track;
	
	private static final int NO_SEEK_REQUESTED = -1;
	
	private boolean pauseRequested = false;
	private boolean playRequested = false;
	private boolean stopRequested = false;
	private double seekRequestPercent = NO_SEEK_REQUESTED;	
	private long clipStartTimeMS = 0; //If we seek, we need to remember where we started so we can make the seek bar look right. 
	
	private boolean paused = false;
	
	private Slider trackPosition;

	
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

		try {
			openStreamsAtRequestedOffset();
						
		} catch ( IOException | JavaLayerException e ) {
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
				pauseRequested = false;
				paused = true;
			}
			
			if ( playRequested ) {
				playRequested = false;
				paused = false;
			}
			
			if ( seekRequestPercent != NO_SEEK_REQUESTED ) {
				
				try {
					closeAllResources();
					openStreamsAtRequestedOffset();
					 
				} catch ( JavaLayerException | IOException e ) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				seekRequestPercent = NO_SEEK_REQUESTED;
				updateTransport();
			}
			
			if ( !paused ) {
				try {
					Header header = encodedInput.readFrame();
					if ( header == null ) {
						// last frame, ensure all data flushed to the audio device.
						if ( audioOut != null ) {
							audioOut.flush();
						}

						MusicPlayerUI.songFinishedPlaying( false );
						break; // We reached the end of the file. 
					}
	
					SampleBuffer output = (SampleBuffer) decoder.decodeFrame( header, encodedInput );
					audioOut.write( output.getBuffer(), 0, output.getBufferLength() );
					encodedInput.closeFrame();
				} catch ( JavaLayerException e ) {
					e.printStackTrace();
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
			double positionPercent = (double) ( audioOut.getPosition() + clipStartTimeMS ) / ( (double) track.getLengthS() * 1000 );
			int timeElapsed = (int)(track.getLengthS() * positionPercent);
			int timeRemaining = track.getLengthS() - timeElapsed;
			MusicPlayerUI.updateTransport ( timeElapsed, -timeRemaining, positionPercent );
		} else {
			int timeElapsed = (int)(track.getLengthS() * seekRequestPercent);
			int timeRemaining = track.getLengthS() - timeElapsed;
			MusicPlayerUI.updateTransport ( timeElapsed, -timeRemaining, seekRequestPercent );
		}
	}
	
	public void openStreamsAtRequestedOffset () throws IOException, JavaLayerException {
		FileInputStream fis = new FileInputStream( track.getPath().toFile() );
        BufferedInputStream bis = new BufferedInputStream(fis);
		
		if ( seekRequestPercent != NO_SEEK_REQUESTED ) {
			int seekPositionMS = (int) ( track.getLengthS() * 1000 * seekRequestPercent );
			long seekPositionByte = getBytePosition ( track.getPath().toFile(), seekPositionMS );
			bis.skip( seekPositionByte ); 
			clipStartTimeMS = seekPositionMS;
		}
		
		encodedInput = new Bitstream( bis );
		audioOut = FactoryRegistry.systemRegistry().createAudioDevice();
		decoder = new Decoder();
		audioOut.open( decoder );
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
			audioOut.flush();
			audioOut.close();
			encodedInput.close();
		} catch ( BitstreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	protected boolean decodeFrame () throws JavaLayerException {
		try {

			
		} catch ( RuntimeException ex ) {
			throw new JavaLayerException( "Exception decoding audio frame", ex );
		}
		return true;
	}
	
	@Override
	public long getPositionMS() {
		return audioOut.getPosition() + clipStartTimeMS;
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
		seekRequestPercent = milliseconds / (double)( track.getLengthS() * 1000 );	

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