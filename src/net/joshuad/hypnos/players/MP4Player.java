package net.joshuad.hypnos.players;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;

import javafx.scene.control.Slider;
import net.joshuad.hypnos.MusicPlayerUI;
import net.joshuad.hypnos.Track;
import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.aac.SampleBuffer;
import net.sourceforge.jaad.mp4.MP4Container;
import net.sourceforge.jaad.mp4.api.AudioTrack;
import net.sourceforge.jaad.mp4.api.Frame;
import net.sourceforge.jaad.mp4.api.Movie;

public class MP4Player extends AbstractPlayer implements Runnable {

	Decoder decoder;
	AudioTrack audioTrack;
	SampleBuffer buffer;
	
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
	
	private void openStreamsAtRequestedOffset() {
		try {
			RandomAccessFile input = new RandomAccessFile( track.getPath().toFile(), "r" );
			
			final MP4Container cont = new MP4Container( input );
			final Movie movie = cont.getMovie();
			final List <net.sourceforge.jaad.mp4.api.Track> tracks = movie.getTracks( AudioTrack.AudioCodec.AAC );
			if ( tracks.isEmpty() ) {
				//TODO: This happens in Test Cases/last-minstrel.m4a
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
			
				clipStartTimeMS = (long)(lengthMS * seekRequestPercent);
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
}

