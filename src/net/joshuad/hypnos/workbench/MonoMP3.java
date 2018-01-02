package net.joshuad.hypnos.workbench;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class MonoMP3 {
	
	public static void main ( String[] args ) {
		MonoMP3 player = new MonoMP3();
		player.testPlay ( "/d/programming/workspace/MusicPlayer/music-files-broken/mp3/russian.mp3" );
	}
		
		
	public void testPlay ( String filename ) {
		try {
			File file = new File( filename );
			AudioInputStream in = AudioSystem.getAudioInputStream( file );
			AudioInputStream din = null;
			AudioFormat baseFormat = in.getFormat();
			
			AudioFormat decodedFormat = new AudioFormat( 
				AudioFormat.Encoding.PCM_SIGNED, 
				baseFormat.getSampleRate(), 
				16, 
				baseFormat.getChannels(), 
				baseFormat.getChannels() * 2,
				baseFormat.getSampleRate(), 
				false 
			);
			
			din = AudioSystem.getAudioInputStream( decodedFormat, in );
			// Play now.
			rawplay( decodedFormat, din );
			in.close();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}

	private void rawplay ( AudioFormat targetFormat, AudioInputStream din ) throws IOException, LineUnavailableException {
		byte[] data = new byte [ 4096 ];
		SourceDataLine line = getLine( targetFormat );
		if ( line != null ) {
			// Start
			line.start();
			int nBytesRead = 0, nBytesWritten = 0;
			while ( nBytesRead != -1 ) {
				nBytesRead = din.read( data, 0, data.length );
				if ( nBytesRead != -1 )
					nBytesWritten = line.write( data, 0, nBytesRead );
			}
			// Stop
			line.drain();
			line.stop();
			line.close();
			din.close();
		}
	}

	private SourceDataLine getLine ( AudioFormat audioFormat ) throws LineUnavailableException {
		SourceDataLine res = null;
		DataLine.Info info = new DataLine.Info( SourceDataLine.class, audioFormat );
		res = (SourceDataLine) AudioSystem.getLine( info );
		res.open( audioFormat );
		return res;
	}
}
