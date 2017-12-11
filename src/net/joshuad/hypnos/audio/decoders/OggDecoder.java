package net.joshuad.hypnos.audio.decoders;

import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import net.joshuad.hypnos.Track;

public class OggDecoder extends AbstractDecoder {

	private static final Logger LOGGER = Logger.getLogger( OggDecoder.class.getName() ); 

	OggDecoderLogic decoder;
	Track track;
	
	public OggDecoder ( Track track ) {
		this.track = track;
		initialize();
	}
	
	@Override
	public void closeAllResources () {
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
		} catch ( FileNotFoundException e ) {
			LOGGER.log( Level.INFO, "File not found: " + track.getPath().toString(), e );
			return false;
		} catch ( SecurityException e ) {
			LOGGER.log( Level.INFO, "No read access: " + track.getPath().toString(), e );
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
			LOGGER.log( Level.INFO, "The audio output line could not be opened due to resource restrictions.", exception );
			return false;
		} catch ( IllegalStateException exception ) {
			LOGGER.log( Level.INFO, "The audio output line is already open.", exception );
			return false;
		} catch ( SecurityException exception ) {
			LOGGER.log( Level.INFO, "The audio output line could not be opened due to security restrictions.", exception );
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
