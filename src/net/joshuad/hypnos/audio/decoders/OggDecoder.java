package net.joshuad.hypnos.audio.decoders;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import net.joshuad.hypnos.Track;

public class OggDecoder extends AbstractDecoder {

	OggDecoderLogic decoder;
	Track track;
	
	public OggDecoder ( Track track ) {
		this.track = track;
		initialize();
	}
	
	@Override
	public void closeAllResources () {
		audioOutput.drain();
		audioOutput.stop();
		audioOutput.close();
		if ( decoder != null ) {
			decoder.close();
		}
	}

	@Override
	public boolean playSingleFrame () {
		boolean hasMore = decoder.processSomeFrames( audioOutput );
		
		if ( hasMore ) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public boolean openStreamsAt ( double seekPercent ) {

		try {
			decoder = new OggDecoderLogic ( track.getPath().toFile() );
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		int channels = decoder.getChannels();
		int rate = decoder.getRate();

		AudioFormat outputFormat = new AudioFormat( (float) rate, 16, channels, true, false );
		DataLine.Info datalineInfo = new DataLine.Info( SourceDataLine.class, outputFormat, AudioSystem.NOT_SPECIFIED );

		try {
			audioOutput = (SourceDataLine) AudioSystem.getLine( datalineInfo );
			audioOutput.open( outputFormat );
		} catch ( LineUnavailableException exception ) {
			//TODO: Logging
			System.out.println( "The audio output line could not be opened due to resource restrictions." );
			System.err.println( exception );
			return false;
		} catch ( IllegalStateException exception ) {
			System.out.println( "The audio output line is already open." );
			System.err.println( exception );
			return false;
		} catch ( SecurityException exception ) {
			System.out.println( "The audio output line could not be opened due to security restrictions." );
			System.err.println( exception );
			return false;
		}

		audioOutput.start();
		
		if ( seekPercent != 0 ) {
			decoder.skipToPercent( seekPercent ); 
			int seekPositionMS = (int) ( track.getLengthS() * 1000 * seekPercent );
			clipStartTimeMS = seekPositionMS;
		}
		
		return true;
	}

}
