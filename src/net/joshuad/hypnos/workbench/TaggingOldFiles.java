package net.joshuad.hypnos.workbench;

import java.io.File;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

public class TaggingOldFiles {
	
	private static String problemFile = "/home/joshua/Desktop/hypnos-test/vangelis.mp3";
	
	public static void main ( String[] args ) throws Exception {
		AudioFile audioFile = AudioFileIO.read( new File ( problemFile ) );
		Tag tag = audioFile.getTag();
		
		Tag newTag = audioFile.createDefaultTag();
		
		newTag.deleteField( FieldKey.YEAR );
		//newTag.setField( FieldKey.YEAR );
		
		audioFile.setTag( newTag );
		
		System.out.println ( newTag.getClass() );
		
		//AudioFileIO.write( audioFile );
		
	}

}
