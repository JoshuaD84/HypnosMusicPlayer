package org.joshuad.musicplayer;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

import javafx.beans.property.SimpleStringProperty;

public class Album {
	private final SimpleStringProperty artist;
	private final SimpleStringProperty year;
	private final SimpleStringProperty title;
	private File directoryPath;
	
	
	
	Album ( String artist, String year, String title, Path directoryPath ) {
		this.artist = new SimpleStringProperty ( artist );
		this.year = new SimpleStringProperty ( year );
		this.title = new SimpleStringProperty ( title );
		this.directoryPath = directoryPath.toFile();
	}
	
	public String getArtist () {
		return artist.get();
	}
	
	public String getYear () {
		return year.get();
	}
	
	public String getTitle () {
		return title.get();
	}		
	
	public Path getPath () {
		return directoryPath.toPath();
	}
	
	public ArrayList <Track> getTracks() {
		try {
			ArrayList <Track> retMe = new ArrayList <Track> ();
		
			DirectoryStream <Path> albumDirectoryStream = Files.newDirectoryStream ( directoryPath.toPath(), Utils.musicFileFilter );
					
			for ( Path trackPath : albumDirectoryStream ) {
				try {
					retMe.add( new Track ( trackPath ) );
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
			
			retMe.sort ( Comparator.comparing( Track::getTrackNumber ) );
			return retMe;
		} catch ( IOException e) {
			System.out.println ( "Unable to get list of files in directory: " + directoryPath.toString() + ", skipping album." );
			return null;

		} 
	}
	
	@Override
	public boolean equals ( Object e ) {
		
		if ( ! ( e instanceof Album ) ) return false;
		
		Album compareTo = (Album)e;
		
		if ( ! compareTo.getPath().equals( this.getPath() ) ) return false;
		if ( ! compareTo.getTitle().toLowerCase().equals( this.getTitle().toLowerCase() ) ) return false;
		if ( ! compareTo.getYear().toLowerCase().equals( this.getYear().toLowerCase() ) ) return false;
		
		return true;
		
		
		
	}
}

	