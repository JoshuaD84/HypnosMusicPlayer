package net.joshuad.library;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javafx.scene.image.Image;

public class Album implements Serializable, AlbumInfoSource {
	private static transient final Logger LOGGER = Logger.getLogger( Album.class.getName() );
	
	private static final long serialVersionUID = 2L;

	private File directory;
	List <Track> tracks;
	long creationTimeMS = 0;

	public Album( Path albumDirectory, List<Track> tracks ) {
		this.directory = albumDirectory.toFile();
		this.tracks = new ArrayList<>( tracks );
		for ( Track track : tracks ) {
			track.setAlbum ( this );
		}
	}
	
	void setData( Album album ) {
		this.directory = album.directory;
		
		List<Track> newTracks = new ArrayList<>( album.tracks.size() );
		
		for( int k = 0; k < newTracks.size(); k++ ) {
			int indexOfDuplicate = tracks.indexOf( newTracks.get( k ) );
			
			if( indexOfDuplicate != -1 ) {
				tracks.get( indexOfDuplicate ).setData( album.tracks.get( k ) );
				newTracks.set( k, tracks.get( indexOfDuplicate ) );
			} else {
				newTracks.set( k, album.tracks.get( k ) );
			}
		}
		
		this.tracks = newTracks;
	}
	

	public String getAlbumArtist () {
		if ( tracks.size() == 0 || tracks.get( 0 ) == null ) {
			return ""; 
		} else {
			return tracks.get( 0 ).getAlbumArtist();
		}
			
	}
	
	public String getYear () {
		if ( tracks.size() == 0 || tracks.get( 0 ) == null ) {
			return "";
		} else {
			if ( tracks.get( 0 ).getYear().length() > 4 ) {
				return tracks.get( 0 ).getYear().substring( 0, 4 );
			} else {
				return tracks.get( 0 ).getYear();
			}
		}
	}
	
	public String getAlbumTitle () {
		if ( tracks.size() == 0 || tracks.get( 0 ) == null ) {
			return ""; 
		} else {
			return tracks.get( 0 ).getAlbumTitle();
		}
	}
	
	public String getFullAlbumTitle () {
		if ( tracks.size() == 0 || tracks.get( 0 ) == null ) {
			return ""; 
		} else {
			return tracks.get( 0 ).getFullAlbumTitle();
		}
	}
	
	public String getDateAddedString () {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		return sdf.format( new Date ( this.creationTimeMS ) );
	}
	
	public Integer getDiscNumber() {
		if ( tracks.size() == 0 || tracks.get( 0 ) == null ) {
			return null; 
		} else {
			return tracks.get( 0 ).getDiscNumber();
		}
	}
	
	public Integer getDiscCount() {
		if ( tracks.size() == 0 || tracks.get( 0 ) == null ) {
			return null; 
		} else {
			return tracks.get( 0 ).getDiscCount();
		}
	}
	
	public String getReleaseType () {
		if ( tracks.size() == 0 || tracks.get( 0 ) == null ) {
			return ""; 
		} else {
			return tracks.get( 0 ).getReleaseType();
		}
	}	
	
	public String getDiscSubtitle () {
		if ( tracks.size() == 0 || tracks.get( 0 ) == null ) {
			return ""; 
		} else {
			return tracks.get( 0 ).getDiscSubtitle();
		}
	}
	
	public String getTitle () {
		if ( tracks.size() == 0 || tracks.get( 0 ) == null ) {
			return null; 
		} else {
			return tracks.get( 0 ).getTitle();
		}
	}
	
	public Path getPath () {
		return directory.toPath();
	}
	
	public List <Track> getTracks() {
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
}

