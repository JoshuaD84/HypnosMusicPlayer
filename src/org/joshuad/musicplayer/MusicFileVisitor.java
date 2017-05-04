package org.joshuad.musicplayer;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

public class MusicFileVisitor extends SimpleFileVisitor <Path> {
	
	static ArrayList <Album> albums = new ArrayList <Album> ();
	
	@Override
	public FileVisitResult visitFile ( Path file, BasicFileAttributes attr ) {

		
		boolean isMusicFile = Utils.isMusicFile ( file );
		
		if ( isMusicFile ) {
			try {
				Track track = new Track ( file );
		        
				Album album = new Album ( track.getArtist(), track.getYear(), track.getAlbum(), file.getParent() );
				albums.add ( album );
			
				return FileVisitResult.SKIP_SIBLINGS;
				
			} catch (CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException e) {
				// If we can't read the tags on this file, keep trying. No big deal			
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

