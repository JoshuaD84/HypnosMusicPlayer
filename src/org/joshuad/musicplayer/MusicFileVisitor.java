package org.joshuad.musicplayer;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

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

		if ( Utils.isMusicFile ( file ) ) {
				
			if ( Utils.isAlbumDirectory( file.getParent() ) ) {
				Track track = new Track ( file, true );
				Album album = new Album ( track.getAlbumArtist(), track.getYear(), track.getAlbum(), file.getParent() );
				
				if ( !albums.contains( album ) ) {
					albums.add ( album );
					tracks.addAll( album.getTracks() );
				}
				
				return FileVisitResult.SKIP_SIBLINGS;
				
			} else {
				Track track = new Track ( file, false );
				
				if ( !tracks.contains( track ) ) {
					tracks.add ( track );
				}
				
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

