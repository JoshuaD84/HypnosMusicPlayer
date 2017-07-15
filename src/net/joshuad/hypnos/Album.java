package net.joshuad.hypnos;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;

import javafx.scene.image.Image;

public class Album implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private File directory;
	ArrayList <Track> tracks;
	
	Album ( Path directoryPath ) {
		this.directory = directoryPath.toFile();
		

		tracks = new ArrayList <Track> ();
		
		try (
				DirectoryStream <Path> albumDirectoryStream = Files.newDirectoryStream ( directoryPath, Utils.musicFileFilter );	
		) {
			tracks = new ArrayList <Track> ();
					
			for ( Path trackPath : albumDirectoryStream ) {
				tracks.add( new Track ( trackPath, true ) );
			}
			
			tracks.sort ( Comparator.comparing( Track::getTrackNumber ) );
		} catch ( IOException e) {
			e.printStackTrace(); 
		} 
	}
	
	public String getAlbumArtist () {
		return tracks.get( 0 ).getAlbumArtist();
	}
	
	public String getYear () {
		return tracks.get( 0 ).getYear();
	}
	
	public String getSimpleTitle() {
		return tracks.get( 0 ).getSimpleAlbumTitle();
	}
	
	public String getFullTitle () {
		return tracks.get( 0 ).getFullAlbumTitle();
	}		
	
	public Path getPath () {
		return directory.toPath();
	}
	
	public ArrayList <Track> getTracks() {
		return tracks;
	}
	
	@Override
	public boolean equals ( Object e ) {
		
		if ( ! ( e instanceof Album ) ) return false;
		
		Album compareTo = (Album)e;
		
		return compareTo.getPath().toAbsolutePath().equals( this.getPath().toAbsolutePath() );
	}

	public Image getAlbumCoverImage () {
		for ( Track track : tracks ) {
			if ( track.getAlbumCoverImage() != null ) {
				return track.getAlbumCoverImage();
			}
		}
		return null;
	}

	public Image getAlbumArtistImage () {
		for ( Track track : tracks ) {
			if ( track.getAlbumCoverImage() != null ) {
				return track.getAlbumArtistImage();
			}
		}
		return null;
	}
}

	
