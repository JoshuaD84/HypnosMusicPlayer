package net.joshuad.hypnos;

import java.io.File;
import java.io.Serializable;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;

import javafx.scene.image.Image;

public class Album implements Serializable, AlbumInfoSource {
	
	private static final long serialVersionUID = 1L;
	
	private File directory;
	ArrayList <Track> tracks;
	
	public Album ( Path albumDirectory ) throws Exception {
		this.directory = albumDirectory.toFile();
		
		updateData();
	}
	
	//TODO: look at this exception and decide if we should be throwing it or handling it right here. 
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
		if ( tracks.size() == 0 || tracks.get( 0 ) == null ) {
			return ""; //TODO: Maybe cache the info
		} else {
			return tracks.get( 0 ).getAlbumArtist();
		}
			
	}
	
	public String getYear () {
		if ( tracks.size() == 0 || tracks.get( 0 ) == null ) {
			return ""; //TODO: Maybe cache the info
		} else {
			return tracks.get( 0 ).getYear();
		}
	}
	
	public String getAlbumTitle () {
		if ( tracks.size() == 0 || tracks.get( 0 ) == null ) {
			return ""; //TODO: Maybe cache the info
		} else {
			return tracks.get( 0 ).getAlbumTitle();
		}
	}
	
	public String getFullAlbumTitle () {
		if ( tracks.size() == 0 || tracks.get( 0 ) == null ) {
			return ""; //TODO: Maybe cache the info
		} else {
			return tracks.get( 0 ).getFullAlbumTitle();
		}
	}
	
	public Integer getDiscNumber() {
		if ( tracks.size() == 0 || tracks.get( 0 ) == null ) {
			return null; //TODO: Maybe cache the info
		} else {
			return tracks.get( 0 ).getDiscNumber();
		}
	}
	
	public Integer getDiscCount() {
		if ( tracks.size() == 0 || tracks.get( 0 ) == null ) {
			return null; //TODO: Maybe cache the info
		} else {
			return tracks.get( 0 ).getDiscCount();
		}
	}
	
	public String getReleaseType () {
		if ( tracks.size() == 0 || tracks.get( 0 ) == null ) {
			return ""; //TODO: Maybe cache the info
		} else {
			return tracks.get( 0 ).getReleaseType();
		}
	}	
	
	public String getDiscSubtitle () {
		if ( tracks.size() == 0 || tracks.get( 0 ) == null ) {
			return ""; //TODO: Maybe cache the info
		} else {
			return tracks.get( 0 ).getDiscSubtitle();
		}
	}
	
	public String getTitle () {
		if ( tracks.size() == 0 || tracks.get( 0 ) == null ) {
			return null; //TODO: Maybe cache the info
		} else {
			return tracks.get( 0 ).getTitle();
		}
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
				return track.getArtistImage();
			}
		}
		return null;
	}
	
	public Album getThis() {
		return this;
	}
}

	
