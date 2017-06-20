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
	
	OggDecoderLogic decoder;
	
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

	private void openStreamsAtRequestedOffset() throws IOException {

		decoder = new OggDecoderLogic ( track.getPath().toFile() );
		
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
	
	private void closeAllResources () {

		audioOutput.drain();
		audioOutput.stop();
		audioOutput.close();
		decoder.close();
		
	}
}