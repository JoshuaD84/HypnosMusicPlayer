package org.joshuad.musicplayer;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

public class Album implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private final String artist;
	private final String year;
	private final String title;
	private File directory;
	ArrayList <Track> tracks;
	
	Album ( String artist, String year, String title, Path directoryPath ) {
		this.artist = artist;
		this.year = year;
		this.title = title;
		this.directory = directoryPath.toFile();
		

		tracks = new ArrayList <Track> ();
		
		try {
			tracks = new ArrayList <Track> ();
		
			DirectoryStream <Path> albumDirectoryStream = Files.newDirectoryStream ( directoryPath, Utils.musicFileFilter );
					
			for ( Path trackPath : albumDirectoryStream ) {
				try {
					tracks.add( new Track ( trackPath, true ) );
				} catch ( TagException e) {
					System.out.println ( "Unable to read tags on: " + trackPath.toString() +", skipping." );
				} catch (CannotReadException e) {
					System.out.println ( trackPath.toString() );
					e.printStackTrace();
				} catch (ReadOnlyFileException e) {
					System.out.println ( trackPath.toString() +", is read only, unable to edit, skipping." );
				} catch (InvalidAudioFrameException e) {
					System.out.println ( trackPath.toString() +", has bad audio fram data, skipping." );
				}
			}
			
			tracks.sort ( Comparator.comparing( Track::getTrackNumber ) );
		} catch ( IOException e) {
			e.printStackTrace(); 
		} 
	}
	
	public String getArtist () {
		return artist;
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

	
