package net.joshuad.musicplayer.players;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import javafx.scene.control.Slider;
import net.joshuad.musicplayer.MusicPlayerUI;
import net.joshuad.musicplayer.Track;
import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.aac.SampleBuffer;
import net.sourceforge.jaad.mp4.MP4Container;
import net.sourceforge.jaad.mp4.api.AudioTrack;
import net.sourceforge.jaad.mp4.api.Frame;
import net.sourceforge.jaad.mp4.api.Movie;

public class MP4Player extends AbstractPlayer implements Runnable {

	private Track track;
	
	private static final int NO_SEEK_REQUESTED = -1;

	SourceDataLine audioOutput;
	
	Decoder decoder;
	AudioTrack audioTrack;
	SampleBuffer buffer;
	

	private boolean pauseRequested = false;
	private boolean playRequested = false;
	private boolean stopRequested = false;
	private double seekRequestPercent = NO_SEEK_REQUESTED;	
	private long clipStartTime = 0; //If we seek, we need to remember where we started so we can make the seek bar look right. 
	
	private boolean paused = false;
	
	private Slider trackPosition;
	
	public MP4Player ( Track track, Slider trackPosition ) {
		this ( track, trackPosition, false );
	}
	
	public MP4Player ( Track track, Slider trackPosition, boolean startPaused ) {
		this.track = track;
		this.trackPosition = trackPosition;
		this.pauseRequested = startPaused;

		Thread t = new Thread ( this );
		t.setDaemon( true );
		t.start();
	}
	
	public void run() {

		openStreamsAtRequestedOffset ();
			
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
				
				closeAllResources();
				openStreamsAtRequestedOffset();
				audioOutput.start();
				 
				seekRequestPercent = NO_SEEK_REQUESTED;
				updateTransport();
			}
			
			if ( !paused ) {
				if ( audioTrack.hasMoreFrames() ) {
					try {
						Frame frame = audioTrack.readNextFrame();
						decoder.decodeFrame( frame.getData(), buffer );
						byte[] bytes = buffer.getData();
						audioOutput.write( bytes, 0, bytes.length );
					} catch ( IOException e ) {
						e.printStackTrace(); //TODO:
					}
				} else {
					MusicPlayerUI.songFinishedPlaying( false );
					return;
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
	
	private void updateTransport() {
		if ( seekRequestPercent == NO_SEEK_REQUESTED ) {
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
	

	private void openStreamsAtRequestedOffset() {
		try {
			RandomAccessFile input = new RandomAccessFile( track.getPath().toFile(), "r" );
			
			final MP4Container cont = new MP4Container( input );
			final Movie movie = cont.getMovie();
			final List <net.sourceforge.jaad.mp4.api.Track> tracks = movie.getTracks( AudioTrack.AudioCodec.AAC );
			if ( tracks.isEmpty() ) {
				//TODO: 
			}
			audioTrack = (AudioTrack) tracks.get( 0 );
			
			final AudioFormat decodedFormat = new AudioFormat( audioTrack.getSampleRate(), audioTrack.getSampleSize(), audioTrack.getChannelCount(), true, true );
			audioOutput = AudioSystem.getSourceDataLine( decodedFormat );
			audioOutput.open();
			
			decoder = new Decoder( audioTrack.getDecoderSpecificInfo() );

			buffer = new SampleBuffer();

			audioOutput.start(); //TODO: deal with startPaused here instead of where I do it?
			
			if ( seekRequestPercent != NO_SEEK_REQUESTED ) {
				
				double lengthMS = movie.getDuration() * 1000;
				
				int framesDumped = 0;
				Frame frame;
				do {
					frame = audioTrack.readNextFrame();
					framesDumped++;
				} while ( frame.getTime() == 0 && framesDumped < 10 );
				
				double frameLengthMS = frame.getTime() * 1000;
					
				int framesToDump = (int)(lengthMS * seekRequestPercent / frameLengthMS );
				
				for ( int k = framesDumped; k < framesToDump; k++ ) { 
					audioTrack.readNextFrame();
				}
			
				clipStartTime = (long)(lengthMS * seekRequestPercent);
			}
		
		} catch ( IOException | LineUnavailableException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
		
	private void closeAllResources() {
		audioOutput.drain();
		audioOutput.stop();
		audioOutput.close();
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

