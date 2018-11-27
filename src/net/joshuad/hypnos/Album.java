package net.joshuad.hypnos;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.logging.Logger;

import javafx.scene.image.Image;

public class Album implements Serializable, AlbumInfoSource {
	private static transient final Logger LOGGER = Logger.getLogger( Album.class.getName() );
	
	private static final long serialVersionUID = 2L;

	private File directory;
	ArrayList <Track> tracks;
	long creationTimeMS = 0;

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
		
		try {
			creationTimeMS = Files.readAttributes( directory.toPath(), BasicFileAttributes.class ).creationTime().toMillis();
		} catch ( IOException e ) {
			LOGGER.info( "Unable to determine file creation time for album, assuming it is very old." + directory.toString() );
		}
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
}

	
