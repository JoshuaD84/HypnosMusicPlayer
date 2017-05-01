import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

public class MusicFileVisitor extends SimpleFileVisitor <Path> {
	
	static ArrayList <Album> albums = new ArrayList <Album> ();
	

	
	@Override
	public FileVisitResult visitFile ( Path file, BasicFileAttributes attr ) {

		
		boolean isMusicFile = Utils.isMusicFile ( file );
		
		if ( isMusicFile ) {
			System.out.println ( "adding" );
			try {
				AudioFile audioFile = AudioFileIO.read( file.toFile() );
				Tag tag = audioFile.getTag();
		        
				if ( tag != null ) {
					//TODO: Expand how we read artist name
					Album album = new Album ( tag.getFirst ( FieldKey.ARTIST ), tag.getFirst( FieldKey.YEAR ), tag.getFirst( FieldKey.ALBUM ), file.getParent() );
					albums.add ( album );
				}
				
				return FileVisitResult.SKIP_SIBLINGS;
				
			} catch (CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException e) {
				// TODO Better error checking? 
				e.printStackTrace();
				return FileVisitResult.CONTINUE;
			}
	        
		} else {
			return FileVisitResult.CONTINUE;
		}
			
	}
	
	@Override
	public FileVisitResult postVisitDirectory( Path dir, IOException exc ) {
		return FileVisitResult.CONTINUE;
	}
	
	public FileVisitResult visitFileFailed( Path file, IOException exc ) {
		return FileVisitResult.CONTINUE;
	}

}

