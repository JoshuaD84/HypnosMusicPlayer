package org.joshuad.musicplayer;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

import javafx.collections.ObservableList;

public class MusicFileVisitor extends SimpleFileVisitor <Path> {
	
	ObservableList <Album> albums;
	ObservableList <Track> tracks;
	
	public MusicFileVisitor ( ObservableList <Album> albums, ObservableList <Track> tracks  ) {
		this.albums = albums;
		this.tracks = tracks;
	}
	
	@Override
	public FileVisitResult visitFile ( Path file, BasicFileAttributes attr ) {

		
		boolean isMusicFile = Utils.isMusicFile ( file );
		
		if ( isMusicFile ) {
			try {
				Track track = new Track ( file );
		        
				Album album = new Album ( track.getArtist(), track.getYear(), track.getAlbum(), file.getParent() );
				
				if ( !albums.contains( album ) ) {
					albums.add ( album );
				}
				
				tracks.add ( track );
			
				return FileVisitResult.CONTINUE;
				
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

