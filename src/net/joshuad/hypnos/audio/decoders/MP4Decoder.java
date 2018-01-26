package net.joshuad.hypnos.audio.decoders;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;

import net.joshuad.hypnos.Track;
import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.aac.SampleBuffer;
import net.sourceforge.jaad.mp4.MP4Container;
import net.sourceforge.jaad.mp4.api.AudioTrack;
import net.sourceforge.jaad.mp4.api.Frame;
import net.sourceforge.jaad.mp4.api.Movie;
import net.sourceforge.jaad.mp4.api.Type;

public class MP4Decoder extends AbstractDecoder {
	

	private static final Logger LOGGER = Logger.getLogger( MP4Decoder.class.getName() );

	Decoder decoder;
	AudioTrack audioTrack;
	SampleBuffer buffer;
	RandomAccessFile input;
	Track track;
	
	public MP4Decoder ( Track track ) {
		this.track = track;
		initialize();
	}
	
	@Override
	public void closeAllResources () {
		audioOutput.close();
		
		if ( input != null ) {
			try {
				input.close();
			} catch ( IOException e ) {
				LOGGER.info( "Unable to close mp4 file: " + track.getFilename() );
			}
		}
	}

	@Override
	public boolean playSingleFrame () {
		if ( audioTrack.hasMoreFrames() ) {
			try {
				Frame frame = audioTrack.readNextFrame();
				decoder.decodeFrame( frame.getData(), buffer );
				byte[] bytes = buffer.getData();
				audioOutput.write( bytes, 0, bytes.length );
			} catch ( Exception e ) {
				LOGGER.log( Level.WARNING, "Unable to decode frame for file:" + track.getFilename(), e );
			}
			return false;
			
		} else {
			return true;
		}
	}

	@Override
	public boolean openStreamsAt ( double seekPercent ) {
		try {
			input = new RandomAccessFile( track.getPath().toFile(), "r" );
			
			final MP4Container cont = new MP4Container( input );
			final Movie movie = cont.getMovie();
			final List <net.sourceforge.jaad.mp4.api.Track> tracks = movie.getTracks( Type.AUDIO );
			
			if ( tracks.isEmpty() ) {
				throw new IOException ( "No Audio Tracks Found" );
			}
			
			audioTrack = (AudioTrack) tracks.get( 0 );
			
			int sampleRate = audioTrack.getSampleRate();
			int sampleSize = audioTrack.getSampleSize();
			int channelCount = audioTrack.getChannelCount();
			
			final AudioFormat outputFormat = new AudioFormat( sampleRate, sampleSize, channelCount, true, true );
			
			audioOutput = AudioSystem.getSourceDataLine( outputFormat );
			audioOutput.open();
			
			decoder = new Decoder( audioTrack.getDecoderSpecificInfo() );

			buffer = new SampleBuffer();

			audioOutput.start(); //TODO: deal with startPaused here instead of where I do it?
			
			if ( seekPercent != 0 ) {				
				double lengthMS = movie.getDuration() * 1000;
				
				int framesDumped = 0;
				Frame frame;
				do {
					frame = audioTrack.readNextFrame();
					framesDumped++;
				} while ( frame.getTime() == 0 && framesDumped < 10 );
				
				double frameLengthMS = frame.getTime() * 1000;
					
				int framesToDump = (int)(lengthMS * seekPercent / frameLengthMS );
				
				for ( int k = framesDumped; k < framesToDump; k++ ) { 
					audioTrack.readNextFrame();
				}
			
				clipStartTimeMS = (long)(lengthMS * seekPercent);
			}
		
		} catch ( IOException | LineUnavailableException e ) {
			LOGGER.log( Level.WARNING, "Unable to get output line for audio, for file: " + track.getFilename(), e );
			return false;
		}
		
		return true;
	}

}
