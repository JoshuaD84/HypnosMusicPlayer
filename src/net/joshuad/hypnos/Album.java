package net.joshuad.hypnos;

import java.io.File;
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
	
	Album ( Path albumDirectory ) throws Exception {
		this.directory = albumDirectory.toFile();
		
		updateData();
	}
	
	public void updateData () throws Exception {
		tracks = new ArrayList <Track> ();
		
		try (
			DirectoryStream <Path> albumDirectoryStream = Files.newDirectoryStream ( directory.toPath(), Utils.musicFileFilter );	
		) {
			tracks = new ArrayList <Track> ();
					
			for ( Path trackPath : albumDirectoryStream ) {
				tracks.add( new Track ( trackPath, directory.toPath() ) );
			}
			
			tracks.sort ( Comparator.comparing( Track::getTrackNumber ) );
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

	
