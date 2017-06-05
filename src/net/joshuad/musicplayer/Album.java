package net.joshuad.musicplayer;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;

public class Album implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String albumArtist;
	private String year;
	private String title;
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
		
		refreshTagData();
	}
	
	public void refreshTagData () {
		synchronized ( tracks ) {
			if ( tracks.size() > 0 ) {
				this.albumArtist = tracks.get( 0 ).getAlbumArtist();
				this.year = tracks.get( 0 ).getYear();
				this.title = tracks.get( 0 ).getAlbum();
			}
		}
	}
	
	public String getAlbumArtist () {
		return albumArtist;
	}
	
	public String getYear () {
		return year;
	}
	
	public String getTitle () {
		return title;
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
}

	
