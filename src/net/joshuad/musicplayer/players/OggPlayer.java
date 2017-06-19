package net.joshuad.musicplayer.players;

import javafx.scene.control.Slider;
import net.joshuad.musicplayer.MusicPlayerUI;
import net.joshuad.musicplayer.Track;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class OggPlayer extends AbstractPlayer implements Runnable {
	
	OggDecoder decoder;
	
	private Track track;
	
	private boolean pauseRequested = false;
	private boolean playRequested = false;
	private boolean stopRequested = false;
	private double seekRequestPercent = NO_SEEK_REQUESTED;
	private long clipStartTimeMS = 0; //If we seek, we need to remember where we started so we can make the seek bar look right. 
	
	private boolean paused = false;
	
	Slider trackPosition;
	
	public OggPlayer ( Track track, Slider trackPositionSlider, boolean startPaused  ) throws IOException {
		this.track = track;
		this.trackPosition = trackPositionSlider;
		this.pauseRequested = startPaused;
		
		Thread playerThread = new Thread ( this );
		playerThread.start();
	}
	
	public OggPlayer ( Track track, Slider trackPositionSlider ) throws IOException {
		this ( track, trackPositionSlider, false );
	}
	
	public void run () {
		
		try {
			openStreamsAtRequestedOffset();
		} catch ( IOException e1 ) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}
		
		boolean needMoreData = true;

		while ( needMoreData ) {
			
			if ( stopRequested ) {
				closeAllResources();
				MusicPlayerUI.songFinishedPlaying( true );
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
				try {
					openStreamsAtRequestedOffset();
				} catch ( IOException e ) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return;
				}
			
				seekRequestPercent = NO_SEEK_REQUESTED;
				updateTransport();
			}
			
			if ( !paused ) {
				needMoreData = decoder.processSomeFrames( audioOutput );			
				updateTransport();
				
			} else {
				try {
					Thread.sleep ( 5 );
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		MusicPlayerUI.songFinishedPlaying( false );
		closeAllResources();
	}
	
	private void updateTransport() {
		if ( seekRequestPercent == NO_SEEK_REQUESTED ) {

			double positionPercent = (double) ( getPositionMS() + clipStartTimeMS ) / ( (double) track.getLengthS() * 1000 );
			int timeElapsed = (int)(track.getLengthS() * positionPercent);
			int timeRemaining = track.getLengthS() - timeElapsed;
			MusicPlayerUI.updateTransport ( timeElapsed, -timeRemaining, positionPercent );
		} else {
			int timeElapsed = (int)(track.getLengthS() * seekRequestPercent);
			int timeRemaining = track.getLengthS() - timeElapsed;
			MusicPlayerUI.updateTransport ( timeElapsed, -timeRemaining, seekRequestPercent );
		}
	}

	private void openStreamsAtRequestedOffset() throws IOException {

		decoder = new OggDecoder ( track.getPath().toFile() );
		
		int channels = decoder.getChannels();
		int rate = decoder.getRate();

		AudioFormat outputFormat = new AudioFormat( (float) rate, 16, channels, true, false );
		DataLine.Info datalineInfo = new DataLine.Info( SourceDataLine.class, outputFormat, AudioSystem.NOT_SPECIFIED );

		try {
			audioOutput = (SourceDataLine) AudioSystem.getLine( datalineInfo );
			audioOutput.open( outputFormat );
		} catch ( LineUnavailableException exception ) {
			System.out.println( "The audio output line could not be opened due to resource restrictions." );
			System.err.println( exception );
			return;
		} catch ( IllegalStateException exception ) {
			System.out.println( "The audio output line is already open." );
			System.err.println( exception );
			return;
		} catch ( SecurityException exception ) {
			System.out.println( "The audio output line could not be opened due to security restrictions." );
			System.err.println( exception );
			return;
		}

		audioOutput.start();
		
		if ( seekRequestPercent != NO_SEEK_REQUESTED ) {
			decoder.skipToPercent( seekRequestPercent ); 
			int seekPositionMS = (int) ( track.getLengthS() * 1000 * seekRequestPercent );
			clipStartTimeMS = seekPositionMS;
		}
	}
	
	@Override
	public long getPositionMS() {
		return (long)( audioOutput.getMicrosecondPosition() / 1e3 );
	}
	
	private void closeAllResources () {

		audioOutput.drain();
		audioOutput.stop();
		audioOutput.close();
		decoder.close();
		
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